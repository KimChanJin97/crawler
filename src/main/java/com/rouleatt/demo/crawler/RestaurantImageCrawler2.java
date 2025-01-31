package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.dto.ImageDto;
import com.rouleatt.demo.dto.RestaurantDto;
import com.rouleatt.demo.sender.MailSender;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class RestaurantImageCrawler2 {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MailSender mailSender;

    @Value("${naver.restaurant.uri}")
    private String uri;
    @Value("${naver.restaurant.search-coord-key}")
    private String searchCoordKey;
    @Value("${naver.restaurant.boundary-key}")
    private String boundaryKey;
    @Value("${naver.restaurant.referer-key}")
    private String refererKey;
    @Value("${naver.restaurant.referer-value}")
    private String refererValue;
    @Value("${naver.restaurant.limit-key}")
    private String limitKey;
    @Value("${naver.restaurant.limit-value}")
    private String limitValue;

    private static final Set<String> RESTAURANT_ID_SET = ConcurrentHashMap.newKeySet();
    private static final Set<Set<RestaurantDto>> RESTAURANT_DTO_SET = ConcurrentHashMap.newKeySet();

    public void crawl(double minX, double minY, double maxX, double maxY) {

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

            while (retryCount < 10 && !success) {
                try {
                    HttpHeaders headers = setHeaders();
                    URI uri = setUri(currentMinX, currentMinY, currentMaxX, currentMaxY);
                    RequestEntity<?> requestEntity = new RequestEntity<>(headers, GET, uri);
                    ResponseEntity<String> responseEntity = restTemplate.exchange(requestEntity, String.class);

                    Thread.sleep(3000);

                    JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
                    JsonNode resultNode = rootNode.path("result");
                    JsonNode metaNode = resultNode.path("meta");
                    JsonNode countNode = metaNode.path("count");
                    JsonNode listNode = resultNode.path("list");

                    Set<String> idSet = new HashSet<>();
                    Set<RestaurantDto> restaurantDtoSet = new LinkedHashSet<>();
                    Set<ImageDto> imageDtoSet = new LinkedHashSet<>();

                    for (JsonNode itemNode : listNode) {
                        String id = itemNode.path("id").asText();

                        // 이미 조회한 음식점이라면 패스
                        if (RESTAURANT_ID_SET.contains(id)) {
                            continue;
                        }
                        // 새로 조회한 음식점 id 저장
                        idSet.add(id);

                        // 음식점 DTO 저장 (좌표는 DTO 생성이 아닌 검증 목적)
                        restaurantDtoSet.add(parseRestaurant(
                                id,
                                itemNode,
                                currentMinX,
                                currentMinY,
                                currentMaxX,
                                currentMaxY)
                        );
                        // 이미지 DTO 저장
                        for (JsonNode imagesNode : itemNode.path("images")) {
                            imageDtoSet.add(parseRestaurantImage(id, imagesNode));
                        }
                    }

                    // 조회된 음식점이 100개 미만이라면 CSV 기록
                    if (countNode.asInt() < 100) {
                        saveRestaurantIntoCsv(restaurantDtoSet);
                        saveRestaurantImageIntoCsv(imageDtoSet);
                        RESTAURANT_ID_SET.addAll(idSet);
                    }
                    // 조회된 음식점이 최대(100)이고 해당 음식점들을 조회한 적이 있다면 범위 쪼개지 않고 저장
                    else if (countNode.asInt() >= 100 && RESTAURANT_DTO_SET.contains(restaurantDtoSet)) {
                        saveRestaurantIntoCsv(restaurantDtoSet);
                        saveRestaurantImageIntoCsv(imageDtoSet);
                        RESTAURANT_ID_SET.addAll(idSet);
                    }
                    // 조회된 음식점이 최대(100)이고 해당 음식점들을 조회한 적이 없다면 범위 쪼개기
                    else if (countNode.asInt() >= 100 && !RESTAURANT_DTO_SET.contains(restaurantDtoSet)) {
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
                    RESTAURANT_DTO_SET.add(restaurantDtoSet);

                } catch (IOException e) {
                    retryCount++;
                    mailSender.sendInterruptException(retryCount);
                    if (retryCount >= 10) {
                        mailSender.sendInterruptExceptionMaxRetry();
                    }
                    try {
                        Thread.sleep(3000 * retryCount); // 재시도 간격 증가
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        mailSender.sendInterruptExceptionSleepInterrupt();
                    }
                } catch (Exception e) {
                    retryCount++;
                    mailSender.sendBlockException(retryCount);
                    if (retryCount >= 10) {
                        mailSender.sendBlockExceptionMaxRetry();
                    }
                    try {
                        Thread.sleep(3000 * retryCount); // 재시도 간격 증가
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        mailSender.sendBlockExceptionSleepInterrupt();
                    }
                }
            }
        }

        mailSender.sendDone();
    }

    private HttpHeaders setHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(refererKey, refererValue);
        return headers;
    }

    private URI setUri(double minY, double minX, double maxY, double maxX) {
        double midY = (minY + maxY) / 2;
        double midX = (minX + maxX) / 2;

        String searchCoordValue = String.format("%f;%f", midY, midX);
        String boundaryValue = String.format("%f;%f;%f;%f", minY, minX, maxY, maxX);

        return UriComponentsBuilder
                .fromUriString(uri)
                .queryParam(searchCoordKey, searchCoordValue)
                .queryParam(boundaryKey, boundaryValue)
                .queryParam(limitKey, limitValue)
                .build()
                .toUri();
    }

    private String address(JsonNode addressNode) {
        String address = addressNode.asText();
        if (address.length() > 0) {
            return address;
        }
        return null;
    }

    private RestaurantDto parseRestaurant(
            String id,
            JsonNode itemNode,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        // 네이버는 좌표를 반대로 줌
        double x = itemNode.path("x").asDouble();
        double y = itemNode.path("y").asDouble();

        if (x < minX || maxX < x || y < minY || maxY < y) {
            mailSender.sendWrongBound(minX, x, maxX, minY, y, maxY);
        }

        return RestaurantDto.builder()
                .id(id)
                .name(itemNode.path("name").asText())
                .x(itemNode.path("x").asText())
                .y(itemNode.path("y").asText())
                .category(itemNode.path("categoryName").asText())
                .address(address(itemNode.path("address")))
                .roadAddress(address(itemNode.path("roadAddress")))
                .build();
    }

    private void saveRestaurantIntoCsv(Set<RestaurantDto> restaurantDtos) {
        File file = new File("restaurant.csv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("id").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("x").append(DELIMITER)
                        .append("y").append(DELIMITER)
                        .append("category").append(DELIMITER)
                        .append("address").append(DELIMITER)
                        .append("road_address");
                bw.write(sb.toString());
                bw.newLine();
            }
            // CSV 칼럼 작성
            for (RestaurantDto restaurantDto : restaurantDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(restaurantDto.id()).append(DELIMITER)
                        .append(restaurantDto.name()).append(DELIMITER)
                        .append(restaurantDto.x()).append(DELIMITER)
                        .append(restaurantDto.y()).append(DELIMITER)
                        .append(restaurantDto.category()).append(DELIMITER)
                        .append(restaurantDto.address()).append(DELIMITER)
                        .append(restaurantDto.roadAddress());
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ImageDto parseRestaurantImage(
            String id,
            JsonNode imagesNode
    ) {
        return ImageDto.builder()
                .restaurantId(id)
                .url(imagesNode.asText())
                .build();
    }

    private void saveRestaurantImageIntoCsv(Set<ImageDto> imageDtos) {
        File file = new File("image.csv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("url");
                bw.write(sb.toString());
                bw.newLine();
            }
            // CSV 칼럼 작성
            for (ImageDto imageDto : imageDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(imageDto.restaurantId()).append(DELIMITER)
                        .append(imageDto.url());
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

