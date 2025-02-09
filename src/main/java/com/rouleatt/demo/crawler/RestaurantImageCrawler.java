package com.rouleatt.demo.crawler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.dto.ImageDto;
import com.rouleatt.demo.dto.Region;
import com.rouleatt.demo.dto.RestaurantDto;
import com.rouleatt.demo.utils.EnvLoader;
import com.rouleatt.demo.writer.RestaurantImageWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestaurantImageCrawler {

    private static final Set<String> ID_SET = ConcurrentHashMap.newKeySet();

    private final ObjectMapper mapper;
    private final RestaurantImageWriter writer;
    private final String RI_URI = EnvLoader.get("RI_URI");
    private final String RI_SEARCH_COORD_KEY = EnvLoader.get("RI_SEARCH_COORD_KEY");
    private final String RI_BOUNDARY_KEY = EnvLoader.get("RI_BOUNDARY_KEY");
    private final String RI_REFERER_KEY = EnvLoader.get("RI_REFERER_KEY");
    private final String RI_REFERER_VALUE = EnvLoader.get("RI_REFERER_VALUE");
    private final String RI_LIMIT_KEY = EnvLoader.get("RI_LIMIT_KEY");
    private final String RI_LIMIT_VALUE = EnvLoader.get("RI_LIMIT_VALUE");
    private final String RI_TIME_CODE = EnvLoader.get("RI_TIME_CODE");

    public RestaurantImageCrawler() {
        this.mapper = new ObjectMapper();
        this.writer = new RestaurantImageWriter();
    }

    public void crawlAll() {
        for (Region region : Region.values()) {

            String engName = region.getEngName();
            String fullName = region.getFullName();
            String shortName = region.getShortName();
            double minX = region.getMinX();
            double minY = region.getMinY();
            double maxX = region.getMaxX();
            double maxY = region.getMaxY();

            crawl(engName, fullName, shortName, minX, minY, maxX, maxY);
        }
    }

    private void crawl(
            String engName,
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

            while (retryCount < 60 && !success) {
                try {
                    URI uri = setUri(currentMinX, currentMinY, currentMaxX, currentMaxY);
                    String response = sendHttpRequest(uri);

                    Thread.sleep(1000);

                    JsonNode rootNode = mapper.readTree(response);
                    JsonNode resultNode = rootNode.path("result");
                    JsonNode metaNode = resultNode.path("meta");
                    JsonNode countNode = metaNode.path("count");
                    JsonNode listNode = resultNode.path("list");

                    Set<RestaurantDto> restaurantDtoSet = new LinkedHashSet<>();
                    Set<ImageDto> imageDtoSet = new LinkedHashSet<>();

                    // 크롤링한 음식점이 100개 미만이라면
                    if (countNode.asInt() < 100) {

                        // 파싱 후 DTO 리스트 담기
                        for (JsonNode itemNode : listNode) {

                            String id = itemNode.path("id").asText();
                            String address = address(itemNode.path("address"));

                            // 타겟팅한 행정구역이면서, 처음 크롤링하는 음식점 아이디라면
                            if (isTarget(address, fullName, shortName) && ID_SET.add(id)) {
                                // 음식점 DTO 생성
                                RestaurantDto restaurantDto = RestaurantDto.of(
                                        id,
                                        itemNode.path("name").asText(),
                                        itemNode.path("x").asText(),
                                        itemNode.path("y").asText(),
                                        itemNode.path("categoryName").asText(),
                                        address(itemNode.path("address")),
                                        address(itemNode.path("roadAddress")));
                                restaurantDtoSet.add(restaurantDto);
                                // 이미지 DTO 생성
                                for (JsonNode imageNode : itemNode.path("images")) {
                                    imageDtoSet.add(ImageDto.of(id, imageNode.asText()));
                                }
                            }
                        }
                        // 파일 쓰기
                        writer.writeRestaurant(engName, restaurantDtoSet);
                        writer.writeImage(engName, imageDtoSet);
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

                    // 파일 저장 또는 영역 쪼개기가 성공적으로 이뤄졌음을 업데이트
                    success = true;

                } catch (IOException e) {
                    retryCount++;
                    if (retryCount >= 60) {
                        log.error("[RI] Exception Max Retry");
                    }
                    try {
                        Thread.sleep(10_000 * retryCount); // 재시도 간격 증가
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount >= 60) {
                        log.error("[RI] Exception Max Retry");
                    }
                    try {
                        Thread.sleep(10_000 * retryCount); // 재시도 간격 증가
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }
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
        URL url = uri.toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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

    private String address(JsonNode addressNode) {
        String address = addressNode.asText();
        if (address.length() > 0) {
            return address;
        }
        return null;
    }

    private boolean isTarget(String address, String fullName, String shortName) {
        return address != null && (address.contains(fullName) || address.contains(shortName));
    }
}

