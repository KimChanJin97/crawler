package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static org.springframework.http.HttpMethod.GET;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.ReviewDto;
import com.rouleatt.demo.sender.MailSender;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.brotli.dec.BrotliInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class MenuReviewCrawler {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final MailSender mailSender;

    @Value("${naver.info.uri-format}")
    private String uriFormat;
    @Value("${naver.info.referer-key}")
    private String refererKey;
    @Value("${naver.info.referer-value-format}")
    private String refererValueFormat;
    @Value("${naver.info.menu-regex}")
    private String menuRegex;
    @Value("${naver.info.review-regex}")
    private String reviewRegex;
    @Value("${naver.info.accept-encoding-key}")
    private String acceptEncodingKey;
    @Value("${naver.info.accept-encoding-value}")
    private String acceptEncodingValue;

    public void crawl() {
        try (BufferedReader br = new BufferedReader(new FileReader("restaurant.csv"))) {
            br.readLine(); // 헤더 제외
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                String restaurantId = values[0];

                int retryCount = 0;
                boolean success = false;

                while (retryCount < 10 && !success) {
                    try {
                        HttpHeaders headers = setHeaders(restaurantId);
                        URI uri = setUri(restaurantId);
                        RequestEntity<?> requestEntity = new RequestEntity<>(headers, GET, uri);
                        ResponseEntity<byte[]> responseEntity = restTemplate.exchange(requestEntity, byte[].class);

                        Thread.sleep(5000);

                        String html = decode(responseEntity.getBody());
                        Document doc = Jsoup.parse(html, "UTF-8");
                        String script = doc.select("script").get(2).html(); // 3번째 <script> 태그

                        Pattern menuPattern = Pattern.compile(String.format(menuRegex, restaurantId));
                        Pattern reviewPattern = Pattern.compile(String.format(reviewRegex, restaurantId));

                        Matcher menuMatcher = menuPattern.matcher(script);
                        Matcher reviewMatcher = reviewPattern.matcher(script);

                        ArrayList<MenuDto> menuDtos = initMenuDtos(restaurantId, menuMatcher);
                        ArrayList<ReviewDto> reviewDtos = initReviewDtos(restaurantId, reviewMatcher);

                        saveMenuIntoCsv(menuDtos);
                        saveReviewIntoCsv(reviewDtos);

                        success = true;

                    } catch (IOException e) {
                        retryCount++;
                        mailSender.sendIOException(retryCount);
                        if (retryCount >= 10) {
                            mailSender.sendIOExceptionMaxRetry();
                        }
                        try {
                            Thread.sleep(3000 * retryCount); // 재시도 간격 증가
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            mailSender.sendIOExceptionSleepInterrupt();
                        }
                    } catch (Exception e) {
                        retryCount++;
                        mailSender.sendBlockException(retryCount);
                        if (retryCount >= 10) {
                            mailSender.sendIOExceptionMaxRetry();
                        }
                        try {
                            Thread.sleep(3000 * retryCount); // 재시도 간격 증가
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            mailSender.sendIOExceptionSleepInterrupt();
                        }
                    }
                }
            }
            mailSender.sendDone();
        } catch (IOException e) {
            mailSender.sendReadCsvException();
        }
    }

    private ArrayList<ReviewDto> initReviewDtos(String restaurantId, Matcher reviewMatcher) {
        ArrayList<ReviewDto> reviewDtos = new ArrayList<>();

        try {
            while (reviewMatcher.find()) {
                String node = reviewMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode reviewNode = objectMapper.readTree(node);
                reviewDtos.add(ReviewDto.builder()
                        .restaurantId(restaurantId)
                        .reviewerName(reviewNode.path("name").asText())
                        .typeName(reviewNode.path("typeName").asText())
                        .url(reviewNode.path("url").asText())
                        .thumbnailUrl(reviewNode.path("thumbnailUrl").asText())
                        .title(reviewNode.path("title").asText())
                        .reviewIndex(reviewNode.path("rank").asLong())
                        .content(reviewNode.path("contents").asText())
                        .createdAt(reviewNode.path("createdString").asText())
                        .build());
            }
        } catch (JsonProcessingException e) {
            // 리뷰 존재하지 않다면 파싱 에러 무시
        }
        return reviewDtos;
    }

    private ArrayList<MenuDto> initMenuDtos(String restaurantId, Matcher menuMatcher) {
        ArrayList<MenuDto> menuDtos = new ArrayList<>();
        try {
            while (menuMatcher.find()) {
                String node = menuMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode menuNode = objectMapper.readTree(node);

                menuDtos.add(MenuDto.builder()
                        .restaurantId(restaurantId)
                        .menuIndex(menuNode.path("index").asInt())
                        .name(menuNode.path("name").asText())
                        .isRecommended(menuNode.path("recommend").asBoolean())
                        .price(menuNode.path("price").asText())
                        .description(description(menuNode.path("description")))
                        .image(image(menuNode.path("images")))
                        .build());
            }
        } catch (JsonProcessingException e) {
            // 메뉴 존재하지 않다면 파싱 예외 무시
        }
        return menuDtos;
    }

    private String description(JsonNode descriptionNode) {
        String description = descriptionNode.asText();
        if (description.length() > 0) {
            return description;
        }
        return null;
    }

    private String image(JsonNode imageNode) {
        if (imageNode.get(0) == null) {
            return null;
        }
        return imageNode.get(0).asText();
    }

    private static String decode(byte[] encoded) throws IOException {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(encoded);
             BrotliInputStream brotliInputStream = new BrotliInputStream(byteArrayInputStream);
             InputStreamReader reader = new InputStreamReader(brotliInputStream)) {

            // br 압축을 해제하여 String 으로 변환
            StringBuilder stringBuilder = new StringBuilder();
            char[] buffer = new char[1024]; // 8192
            int data;
            while ((data = reader.read(buffer)) != -1) {
                stringBuilder.append(buffer, 0, data);
            }
            return stringBuilder.toString();
        }
    }

    private HttpHeaders setHeaders(String restaurantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(acceptEncodingKey, acceptEncodingValue);
        headers.set(refererKey, String.format(refererValueFormat, restaurantId));
        return headers;
    }

    private URI setUri(String restaurantId) {
        return UriComponentsBuilder
                .fromUriString(String.format(uriFormat, restaurantId))
                .build()
                .toUri();
    }

    private void saveMenuIntoCsv(List<MenuDto> menuDtos) {
        File file = new File("menu.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("menu_index").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("is_recommended").append(DELIMITER)
                        .append("price").append(DELIMITER)
                        .append("description").append(DELIMITER)
                        .append("image");
                writer.write(sb.toString());
                writer.newLine();
            }
            // CSV 칼럼 작성
            for (MenuDto menuDto : menuDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(menuDto.restaurantId()).append(DELIMITER)
                        .append(menuDto.menuIndex()).append(DELIMITER)
                        .append(menuDto.name()).append(DELIMITER)
                        .append(menuDto.isRecommended()).append(DELIMITER)
                        .append(menuDto.price()).append(DELIMITER)
                        .append(menuDto.description()).append(DELIMITER)
                        .append(menuDto.image());
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveReviewIntoCsv(List<ReviewDto> reviewDtos) {
        File file = new File("review.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("reviewer_name").append(DELIMITER)
                        .append("type_name").append(DELIMITER)
                        .append("url").append(DELIMITER)
                        .append("thumbnail_url").append(DELIMITER)
                        .append("title").append(DELIMITER)
                        .append("review_index").append(DELIMITER)
                        .append("content").append(DELIMITER)
                        .append("created_at");
                writer.write(sb.toString());
                writer.newLine();
            }
            // CSV 칼럼 작성
            for (ReviewDto reviewDto : reviewDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(reviewDto.restaurantId()).append(DELIMITER)
                        .append(reviewDto.reviewerName()).append(DELIMITER)
                        .append(reviewDto.typeName()).append(DELIMITER)
                        .append(reviewDto.url()).append(DELIMITER)
                        .append(reviewDto.thumbnailUrl()).append(DELIMITER)
                        .append(reviewDto.title()).append(DELIMITER)
                        .append(reviewDto.reviewIndex()).append(DELIMITER)
                        .append(reviewDto.content().substring(0, 100)).append(DELIMITER)
                        .append(reviewDto.createdAt());
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


