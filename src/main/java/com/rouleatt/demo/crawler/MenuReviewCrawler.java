package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.RESTAURANT_POSTFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.Region;
import com.rouleatt.demo.dto.ReviewDto;
import com.rouleatt.demo.utils.EnvLoader;
import com.rouleatt.demo.writer.MenuReviewWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public class MenuReviewCrawler {

    private final ObjectMapper mapper;
    private final MenuReviewWriter writer;

    private final String MR_URI_FORMAT = EnvLoader.get("MR_URI_FORMAT");
    private final String MR_REFERER_KEY = EnvLoader.get("MR_REFERER_KEY");
    private final String MR_REFERER_VALUE_FORMAT = EnvLoader.get("MR_REFERER_VALUE_FORMAT");
    private final String MR_MENU_REGEX = EnvLoader.get("MR_MENU_REGEX");
    private final String MR_REVIEW_REGEX = EnvLoader.get("MR_REVIEW_REGEX");
    private final String MR_ACCEPT_ENCODING_KEY = EnvLoader.get("MR_ACCEPT_ENCODING_KEY");
    private final String MR_ACCEPT_ENCODING_VALUE = EnvLoader.get("MR_ACCEPT_ENCODING_VALUE");

    public MenuReviewCrawler() {
        this.mapper = new ObjectMapper();
        this.writer = new MenuReviewWriter();
    }

    public void parallelCrawl() {
        Region[] regions = Region.values();
        ExecutorService executor = Executors.newFixedThreadPool(regions.length);

        for (Region region : regions) {
            executor.execute(() -> crawl(region.getEngName()));
        }
    }

    private void crawl(String engName) {

        String fileName = engName.concat(RESTAURANT_POSTFIX);

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            br.readLine(); // 헤더 제외
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(DELIMITER);
                String restaurantId = values[0];

                int retryCount = 0;
                boolean success = false;

                while (retryCount < 10 && !success) {
                    try {
                        URI uri = setUri(restaurantId);
                        String response = sendHttpRequest(uri, restaurantId);

                        Thread.sleep(5000);

                        Document doc = Jsoup.parse(response, "UTF-8");
                        String script = doc.select("script").get(2).html(); // 3번째 <script> 태그

                        Pattern menuPattern = Pattern.compile(String.format(MR_MENU_REGEX, restaurantId));
                        Pattern reviewPattern = Pattern.compile(String.format(MR_REVIEW_REGEX, restaurantId));

                        Matcher menuMatcher = menuPattern.matcher(script);
                        Matcher reviewMatcher = reviewPattern.matcher(script);

                        ArrayList<MenuDto> menuDtos = parseAndInitMenuDtos(mapper, restaurantId, menuMatcher);
                        ArrayList<ReviewDto> reviewDtos = parseAndInitReviewDtos(mapper, restaurantId, reviewMatcher);

                        writer.writeMenu(engName, menuDtos);
                        writer.writeReview(engName, reviewDtos);

                        success = true;

                    } catch (IOException e) {
                        retryCount++;
                        if (retryCount >= 60) {
                            log.error("[MR] IOException Max Retry");
                        }
                        try {
                            Thread.sleep(10_000 * retryCount); // 재시도 간격 증가
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    } catch (Exception e) {
                        retryCount++;
                        if (retryCount >= 60) {
                            log.error("[MR] Exception Max Retry");
                        }
                        try {
                            Thread.sleep(10_000 * retryCount); // 재시도 간격 증가
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("[MR] IOException");
        }
    }

    public static ArrayList<MenuDto> parseAndInitMenuDtos(ObjectMapper objectMapper, String restaurantId,
                                                          Matcher menuMatcher) {
        ArrayList<MenuDto> menuDtos = new ArrayList<>();

        try {
            while (menuMatcher.find()) {
                String node = menuMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode menuNode = objectMapper.readTree(node);

                MenuDto menuDto = MenuDto.of(
                        restaurantId,
                        menuNode.path("index").asInt(),
                        menuNode.path("name").asText(),
                        menuNode.path("recommend").asBoolean(),
                        menuNode.path("price").asText(),
                        description(menuNode.path("description")),
                        image(menuNode.path("images")));

                menuDtos.add(menuDto);
            }
        } catch (JsonProcessingException e) {
            // 메뉴 존재하지 않다면 파싱 예외 무시
        }
        return menuDtos;
    }

    private static String description(JsonNode descriptionNode) {
        String description = descriptionNode.asText();
        if (description.length() > 0) {
            return description;
        }
        return null;
    }

    private static String image(JsonNode imageNode) {
        if (imageNode.get(0) == null) {
            return null;
        }
        return imageNode.get(0).asText();
    }

    public static ArrayList<ReviewDto> parseAndInitReviewDtos(
            ObjectMapper objectMapper,
            String restaurantId,
            Matcher reviewMatcher
    ) {

        ArrayList<ReviewDto> reviewDtos = new ArrayList<>();

        try {
            while (reviewMatcher.find()) {
                String node = reviewMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode reviewNode = objectMapper.readTree(node);

                ReviewDto reviewDto = ReviewDto.of(
                        restaurantId,
                        reviewNode.path("name").asText(),
                        reviewNode.path("typeName").asText(),
                        reviewNode.path("url").asText(),
                        reviewNode.path("thumbnailUrl").asText(),
                        reviewNode.path("title").asText(),
                        reviewNode.path("rank").asLong(),
                        reviewNode.path("contents").asText(),
                        reviewNode.path("createdString").asText()
                );

                reviewDtos.add(reviewDto);
            }
        } catch (JsonProcessingException e) {
            // 리뷰 존재하지 않다면 파싱 에러 무시
        }
        return reviewDtos;
    }

    private URI setUri(String restaurantId) {
        return URI.create(String.format(MR_URI_FORMAT, restaurantId));
    }

    private String sendHttpRequest(URI uri, String restaurantId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty(MR_ACCEPT_ENCODING_KEY, MR_ACCEPT_ENCODING_VALUE);
        conn.setRequestProperty(MR_REFERER_KEY, String.format(MR_REFERER_VALUE_FORMAT, restaurantId));

        try (InputStream responseStream = conn.getInputStream()) {
            if ("br".equalsIgnoreCase(conn.getContentEncoding())) {
                return decode(responseStream); // Brotli 압축 해제
            } else {
                return new Scanner(responseStream, "UTF-8").useDelimiter("\\A").next();
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String decode(InputStream encodedStream) throws IOException {
        try (BrotliInputStream brotliInputStream = new BrotliInputStream(encodedStream);
             InputStreamReader reader = new InputStreamReader(brotliInputStream);
             BufferedReader br = new BufferedReader(reader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }
    }

}


