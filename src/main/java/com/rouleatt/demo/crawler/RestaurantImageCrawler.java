package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.dto.ImageDto.parseAndInitImageDto;
import static com.rouleatt.demo.dto.RestaurantDto.parseAndInitRestaurantDto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.dto.Region;
import com.rouleatt.demo.dto.ImageDto;
import com.rouleatt.demo.dto.RestaurantDto;
import com.rouleatt.demo.utils.EnvLoader;
import com.rouleatt.demo.writer.RestaurantImageWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RestaurantImageCrawler {

    private final ObjectMapper mapper;
    private final RestaurantImageWriter writer;

    private final String RI_URI = EnvLoader.get("RI_URI");
    private final String RI_SEARCH_COORD_KEY = EnvLoader.get("RI_SEARCH_COORD_KEY");
    private final String RI_BOUNDARY_KEY = EnvLoader.get("RI_BOUNDARY_KEY");
    private final String RI_REFERER_KEY = EnvLoader.get("RI_REFERER_KEY");
    private final String RI_REFERER_VALUE = EnvLoader.get("RI_REFERER_VALUE");
    private final String RI_LIMIT_KEY = EnvLoader.get("RI_LIMIT_KEY");
    private final String RI_LIMIT_VALUE = EnvLoader.get("RI_LIMIT_VALUE");

    public RestaurantImageCrawler() {
        this.mapper = new ObjectMapper();
        this.writer = new RestaurantImageWriter();
    }

    public void parallelCrawl() {
        Region[] regions = Region.values();
        ExecutorService executor = Executors.newFixedThreadPool(regions.length);

        for (Region region : regions) {
            Set<String> idSet = new HashSet<>();
            Set<Set<String>> idSetSet = new HashSet<>();

            String engName = region.getEngName();
            String korFullName = region.getKorFullName();
            String korShortName = region.getKorShortName();
            double minX = region.getMinX();
            double minY = region.getMinY();
            double maxX = region.getMaxX();
            double maxY = region.getMaxY();

            executor.execute(() -> crawl(engName, korFullName, korShortName, minX, minY, maxX, maxY, idSet, idSetSet));
        }
    }

    private void crawl(
            String engName,
            String fullName,
            String shortName,
            double minX,
            double minY,
            double maxX,
            double maxY,
            Set<String> idSet,
            Set<Set<String>> idSetSet
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

                    for (JsonNode itemNode : listNode) {
                        String id = itemNode.path("id").asText();
                        String address = address(itemNode.path("address"));

                        // 중복 크롤링이 아님 && 탐색하고자 하는 주소
                        if (isNotDuplicated(idSet, id) && isTarget(address, fullName, shortName)) {
                            // 새로 조회한 음식점 id 저장
                            idSet.add(id);

                            // 음식점 DTO 저장
                            restaurantDtoSet.add(parseAndInitRestaurantDto(id, itemNode));
                            // 이미지 DTO 저장
                            for (JsonNode imagesNode : itemNode.path("images")) {
                                imageDtoSet.add(parseAndInitImageDto(id, imagesNode));
                            }
                        }
                    }

                    // 조회된 음식점이 100개 미만이라면 CSV 기록
                    if (countNode.asInt() < 100) {
                        writer.writerRestaurant(engName, restaurantDtoSet);
                        writer.writeImage(engName, imageDtoSet);
                        idSet.addAll(idSet);
                    }
                    // 조회된 음식점이 최대(100)이고 해당 음식점들을 조회한 적이 있다면 범위 쪼개지 않고 저장
                    else if (countNode.asInt() >= 100 && idSetSet.contains(idSet)) {
                        writer.writerRestaurant(engName, restaurantDtoSet);
                        writer.writeImage(engName, imageDtoSet);
                        idSet.addAll(idSet);
                    }
                    // 조회된 음식점이 최대(100)이고 해당 음식점들을 조회한 적이 없다면 범위 쪼개기
                    else if (countNode.asInt() >= 100 && !idSetSet.contains(idSet)) {
                        double midX = (currentMinX + currentMaxX) / 2;
                        double midY = (currentMinY + currentMaxY) / 2;

                        stack.push(new double[]{midX, currentMinY, currentMaxX, midY}); // 4
                        stack.push(new double[]{currentMinX, currentMinY, midX, midY}); // 3
                        stack.push(new double[]{midX, midY, currentMaxX, currentMaxY}); // 2
                        stack.push(new double[]{currentMinX, midY, midX, currentMaxY}); // 1
                    }

                    // CSV 저장 또는 범위 쪼개기가 성공적으로 이뤄졌음을 기록
                    success = true;
                    // 해당 음식점을 조회한 적이 있음을 기록
                    idSetSet.add(idSet);

                } catch (IOException e) {
                    retryCount++;
                    if (retryCount >= 60) {
//                        log.error("[RI] IOException Max Retry");
                    }
                    try {
                        Thread.sleep(10_000 * retryCount); // 재시도 간격 증가
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                } catch (Exception e) {
                    retryCount++;
                    if (retryCount >= 60) {
//                        log.error("[RI] Exception Max Retry");
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

        return URI.create(String.format("%s?%s=%f;%f&%s=%f;%f;%f;%f&%s=%s",
                RI_URI,
                RI_SEARCH_COORD_KEY, midX, midY,
                RI_BOUNDARY_KEY, minX, minY, maxX, maxY,
                RI_LIMIT_KEY, RI_LIMIT_VALUE));
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

    private boolean isNotDuplicated(Set<String> idSet, String id) {
        return !idSet.contains(id);
    }

    private boolean isTarget(String address, String fullName, String shortName) {
        return address != null && (address.contains(fullName) || address.contains(shortName));
    }
}

