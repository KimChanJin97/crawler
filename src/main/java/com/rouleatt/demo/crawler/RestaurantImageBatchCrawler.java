package com.rouleatt.demo.crawler;

import static java.lang.Integer.MAX_VALUE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.db.JdbcBatchExecutor;
import com.rouleatt.demo.db.RestaurantIdGenerator;
import com.rouleatt.demo.dto.Region;
import com.rouleatt.demo.proxy.ProxyManager;
import com.rouleatt.demo.proxy.ProxyManager.ProxyConfig;
import com.rouleatt.demo.utils.EnvLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestaurantImageBatchCrawler {

    private final ObjectMapper mapper;
    private final JdbcBatchExecutor jdbcBatchExecutor;
    private final MenuReviewBatchCrawler menuReviewCrawler;
    private final ExecutorService executorService;

    private final static String RI_URI = EnvLoader.get("RI_URI");
    private final static String RI_SEARCH_COORD_KEY = EnvLoader.get("RI_SEARCH_COORD_KEY");
    private final static String RI_BOUNDARY_KEY = EnvLoader.get("RI_BOUNDARY_KEY");
    private final static String RI_REFERER_KEY = EnvLoader.get("RI_REFERER_KEY");
    private final static String RI_REFERER_VALUE = EnvLoader.get("RI_REFERER_VALUE");
    private final static String RI_LIMIT_KEY = EnvLoader.get("RI_LIMIT_KEY");
    private final static String RI_LIMIT_VALUE = EnvLoader.get("RI_LIMIT_VALUE");
    private final static String RI_TIME_CODE = EnvLoader.get("RI_TIME_CODE_KEY");

    public RestaurantImageBatchCrawler() {
        this.mapper = new ObjectMapper();
        this.jdbcBatchExecutor = new JdbcBatchExecutor();
        this.menuReviewCrawler = new MenuReviewBatchCrawler();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void crawlAll() {
        for (Region region : Region.values()) {

            executorService.submit(() -> {
                String fullName = region.getFullName();
                String shortName = region.getShortName();
                double minX = region.getMinX();
                double minY = region.getMinY();
                double maxX = region.getMaxX();
                double maxY = region.getMaxY();

                crawl(fullName, shortName, minX, minY, maxX, maxY);
            });
        }
        executorService.shutdown();
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

                    Thread.sleep(1_000);

                    JsonNode rootNode = mapper.readTree(response);
                    JsonNode resultNode = rootNode.path("result");
                    JsonNode metaNode = resultNode.path("meta");
                    JsonNode countNode = metaNode.path("count");
                    JsonNode restaurantsNode = resultNode.path("list");

                    // 크롤링한 음식점이 100개 미만이라면
                    if (countNode.asInt() < 100) {

                        System.out.println("100개 미만");

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

                        System.out.println("100개 이상");

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

                // 크롤링 실패시 일정시간 슬립 후 재시도(반복문 순회)
                if (!success) {
                    try {
                        Thread.sleep(2_000 * retryCount); // 재시도 간격 증가
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
                "%s?%s=%f;%f&%s=%f;%f;%f;%f&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s&%s=%s",
                RI_URI,
                RI_SEARCH_COORD_KEY, midX, midY,
                RI_BOUNDARY_KEY, minX, minY, maxX, maxY,
                RI_LIMIT_KEY, RI_LIMIT_VALUE,
                RI_TIME_CODE, "MORNING",
                RI_TIME_CODE, "LUNCH",
                RI_TIME_CODE, "AFTERNOON",
                RI_TIME_CODE, "EVENING",
                RI_TIME_CODE, "NIGHT"));
    }

    private String sendHttpRequest(URI uri) throws IOException {

        ProxyConfig proxyConfig = ProxyManager.getNextProxyConfig(); // 라운드 로빈 방식
        Proxy proxy = proxyConfig.toProxy();

        // HTTPS 터널링 허용
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // 프록시 인증을 전역 설정
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(proxyConfig.username, proxyConfig.password.toCharArray());
            }
        });

        URL url = uri.toURL();
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(proxy);

        String auth = proxyConfig.username + ":" + proxyConfig.password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedAuth);

        conn.setRequestMethod("GET");
        conn.setRequestProperty(RI_REFERER_KEY, RI_REFERER_VALUE);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            conn.disconnect();
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

