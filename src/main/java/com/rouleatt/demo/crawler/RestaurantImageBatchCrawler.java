package com.rouleatt.demo.crawler;

import static java.lang.Integer.MAX_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.db.JdbcBatchExecutor;
import com.rouleatt.demo.db.RestaurantIdGenerator;
import com.rouleatt.demo.utils.Region;
import com.rouleatt.demo.utils.EnvLoader;
import java.io.IOException;
import java.net.URI;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

@Slf4j
public class RestaurantImageBatchCrawler {

    private final ObjectMapper mapper;
    private final JdbcBatchExecutor jdbcBatchExecutor;
    private final MenuReviewBatchCrawler menuReviewCrawler;

    // uri
    private final static String RI_URI_FORMAT = EnvLoader.get("RI_URI_FORMAT");
    // query param: searchCoord
    private final static String RI_SEARCH_COORD_KEY = EnvLoader.get("RI_SEARCH_COORD_KEY");
    // query param: boundary
    private final static String RI_BOUNDARY_KEY = EnvLoader.get("RI_BOUNDARY_KEY");
    // query param: code
    private final static String RI_CODE_KEY = EnvLoader.get("RI_CODE_KEY");
    private final static String RI_CODE_VALUE = EnvLoader.get("RI_CODE_VALUE");
    // query param: limit
    private final static String RI_LIMIT_KEY = EnvLoader.get("RI_LIMIT_KEY");
    private final static String RI_LIMIT_VALUE = EnvLoader.get("RI_LIMIT_VALUE");
    // query param: timeCode
    private final static String RI_TIME_CODE_KEY = EnvLoader.get("RI_TIME_CODE_KEY");
    // query param: sortType
    private final static String RI_SORT_TYPE_KEY = EnvLoader.get("RI_SORT_TYPE_KEY");
    private final static String RI_SORT_TYPE_VALUE = EnvLoader.get("RI_SORT_TYPE_VALUE");
    // header
    private final static String RI_REFERER_KEY = EnvLoader.get("RI_REFERER_KEY");
    private final static String RI_REFERER_VALUE = EnvLoader.get("RI_REFERER_VALUE");

    public RestaurantImageBatchCrawler() {
        this.mapper = new ObjectMapper();
        this.jdbcBatchExecutor = new JdbcBatchExecutor();
        this.menuReviewCrawler = new MenuReviewBatchCrawler();
    }

    public void crawlAll() {
        for (Region region : Region.values()) {

            String fullName = region.getFullName();
            String shortName = region.getShortName();
            double minX = region.getMinX();
            double minY = region.getMinY();
            double maxX = region.getMaxX();
            double maxY = region.getMaxY();

            crawl(fullName, shortName, minX, minY, maxX, maxY);
        }
    }

    private void crawl(
            String fullName,
            String shortName,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        Stack<double[]> stack = new Stack<>();
        stack.push(new double[]{minX, minY, maxX, maxY});

        while (!stack.isEmpty()) {

            double[] bounds = stack.pop();
            double currentMinX = bounds[0];
            double currentMinY = bounds[1];
            double currentMaxX = bounds[2];
            double currentMaxY = bounds[3];

            int retryCount = 0;
            boolean success = false;

            while (retryCount < MAX_VALUE && !success) {

                try {
                    URI uri = setUri(currentMinX, currentMinY, currentMaxX, currentMaxY);
                    String response = sendHttpRequest(uri);

                    Thread.sleep(1000);

                    JsonNode rootNode = mapper.readTree(response);
                    JsonNode resultNode = rootNode.path("result");
                    JsonNode metaNode = resultNode.path("meta");
                    JsonNode countNode = metaNode.path("count");
                    JsonNode restaurantsNode = resultNode.path("list");

                    // 크롤링한 음식점이 100개 미만이라면
                    if (countNode.asInt() < 100) {

                        for (JsonNode restaurantNode : restaurantsNode) {

                            // 타겟팅한 행정구역이라면
                            if (isTarget(checkNull(restaurantNode.path("address").asText()), fullName, shortName)) {

                                int restaurantPk = RestaurantIdGenerator.getNextId();
                                String restaurantId = restaurantNode.path("id").asText();

                                // 음식점 크롤링 및 배치
                                jdbcBatchExecutor.addRestaurant(
                                        restaurantPk,
                                        restaurantNode.path("name").asText(), // cant be null
                                        Double.parseDouble(restaurantNode.path("x").asText()), // cant be null
                                        Double.parseDouble(restaurantNode.path("y").asText()), // cant be null
                                        checkNull(restaurantNode.path("categoryName").asText()),
                                        checkNull(restaurantNode.path("address").asText()),
                                        checkNull(restaurantNode.path("roadAddress").asText()));

                                // 음식점 이미지 크롤링 및 배치
                                for (JsonNode imageNode : restaurantNode.path("images")) {
                                    jdbcBatchExecutor.addRestaurantImage(restaurantPk, imageNode.asText());
                                }
                                // 메뉴, 메뉴 이미지, 리뷰, 리뷰 이미지 영업시간 크롤링 및 배치
                                menuReviewCrawler.crawl(restaurantPk, restaurantId);
                            }
                        }

                        // 100개 미만의 음식점 나왔더라도 타겟팅한 행정구역이 아닌 경우 방지
                        if (jdbcBatchExecutor.shouldBatchInsert()) {
                            jdbcBatchExecutor.batchInsert();
                        }
                    }
                    // 크롤링한 음식점이 100개 이상이라면 영역을 쪼개기 위해 스택 푸시
                    else {

                        double midX = (currentMinX + currentMaxX) / 2;
                        double midY = (currentMinY + currentMaxY) / 2;

                        stack.push(new double[]{midX, currentMinY, currentMaxX, midY}); // 4
                        stack.push(new double[]{currentMinX, currentMinY, midX, midY}); // 3
                        stack.push(new double[]{midX, midY, currentMaxX, currentMaxY}); // 2
                        stack.push(new double[]{currentMinX, midY, midX, currentMaxY}); // 1
                    }

                    // 크롤링 성공시 반복문 종료
                    success = true;
                    break;

                } catch (IOException e) {
                    retryCount++;
                    log.error("[RI] IOException 발생 (재시도 횟수 {})", retryCount, e);
                } catch (Exception e) {
                    retryCount++;
                    log.error("[RI] Exception 발생 (재시도 횟수 {})", retryCount, e);
                }

                // 크롤링 실패시 일정시간 슬립 후 재시도
                if (!success) {
                    try {
                        Thread.sleep(2_000 * retryCount);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            if (!success) {
                log.error("[RI] 크롤링 실패 : 최대 재시도 횟수 초과");
            }
        }
    }

    private URI setUri(double minX, double minY, double maxX, double maxY) {
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        return URI.create(String.format(
                RI_URI_FORMAT,
                RI_SEARCH_COORD_KEY, midX, midY,
                RI_BOUNDARY_KEY, minX, minY, maxX, maxY,
                RI_CODE_KEY, RI_CODE_VALUE,
                RI_LIMIT_KEY, RI_LIMIT_VALUE,
                RI_SORT_TYPE_KEY, RI_SORT_TYPE_VALUE,
                RI_TIME_CODE_KEY, "MORNING",
                RI_TIME_CODE_KEY, "LUNCH",
                RI_TIME_CODE_KEY, "AFTERNOON",
                RI_TIME_CODE_KEY, "EVENING",
                RI_TIME_CODE_KEY, "NIGHT"));
    }

    private String sendHttpRequest(URI uri) throws IOException, ParseException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(uri);
            request.addHeader(RI_REFERER_KEY, RI_REFERER_VALUE);

            try {
                Thread.sleep(1_000); // 2초 ~ 5초 랜덤 슬립
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private boolean isTarget(String address, String fullName, String shortName) {
        if (address != null) {
            String region = address.substring(0, 8);
            return region.contains(fullName) || region.contains(shortName);
        }
        return false;
    }

    private String checkNull(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input;
    }
}

