package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.decodeUnicode;
import static java.lang.Integer.MAX_VALUE;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.db.JdbcBatchExecutor;
import com.rouleatt.demo.db.MenuIdGenerator;
import com.rouleatt.demo.db.ReviewIdGenerator;
import com.rouleatt.demo.dto.BizHourDto;
import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.MenuImageDto;
import com.rouleatt.demo.dto.ReviewDto;
import com.rouleatt.demo.dto.ReviewImageDto;
import com.rouleatt.demo.utils.EnvLoader;
import com.rouleatt.demo.writer.BizHourWriter;
import com.rouleatt.demo.writer.MenuImageWriter;
import com.rouleatt.demo.writer.MenuWriter;
import com.rouleatt.demo.writer.ReviewImageWriter;
import com.rouleatt.demo.writer.ReviewWriter;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public class MenuReviewCrawler {

    private final ObjectMapper objectMapper;
    private final MenuWriter menuWriter;
    private final MenuImageWriter menuImageWriter;
    private final ReviewWriter reviewWriter;
    private final ReviewImageWriter reviewImageWriter;
    private final BizHourWriter bizHourWriter;
    // uri
    private static final String MR_URI_FORMAT = EnvLoader.get("MR_URI_FORMAT");
    // header
    private static final String MR_REFERER_KEY = EnvLoader.get("MR_REFERER_KEY");
    private static final String MR_REFERER_VALUE_FORMAT = EnvLoader.get("MR_REFERER_VALUE_FORMAT");
    // parsing
    private static final String MR_LOWER_BOUND = EnvLoader.get("MR_LOWER_BOUND").replace("\"", "");
    private static final String MR_UPPER_BOUND = EnvLoader.get("MR_UPPER_BOUND").replace("\"", "");
    private static final Pattern MR_MENU_PATTERN = Pattern.compile(EnvLoader.get("MR_MENU_PATTERN"));
    private static final Pattern MR_REVIEW_PATTERN = Pattern.compile(EnvLoader.get("MR_REVIEW_PATTERN"));
    private static final Pattern MR_ROOT_QUERY_PATTERN = Pattern.compile(EnvLoader.get("MR_ROOT_QUERY_PATTERN"));
    private static final String MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT = EnvLoader.get("MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT");
    private static final String MR_BIZ_HOUR_SECOND_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_SECOND_DEPTH_KEY");
    private static final String MR_BIZ_HOUR_THIRD_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_THIRD_DEPTH_KEY");

    public MenuReviewCrawler() {
        this.objectMapper = new ObjectMapper();
        this.menuWriter = new MenuWriter();
        this.menuImageWriter = new MenuImageWriter();
        this.reviewWriter = new ReviewWriter();
        this.reviewImageWriter = new ReviewImageWriter();
        this.bizHourWriter = new BizHourWriter();
    }

    public void crawl(int restaurantPk, String restaurantId) {

        int retryCount = 0;
        boolean success = false;

        while (retryCount < MAX_VALUE && !success) {

            try {

                URI uri = setUri(restaurantId);
                String response = sendHttpRequest(uri, restaurantId);

                Thread.sleep(1_000);

                Document doc = Jsoup.parse(response, "UTF-8");
                String script = doc.select("script").get(2).html(); // 3번째 <script> 태그
                String rootJson = script
                        .split(MR_LOWER_BOUND)[1] // 윗부분 제거
                        .split(MR_UPPER_BOUND)[0]; // 아랫부분 제거

                JsonNode rootNode = objectMapper.readTree(rootJson);
                Iterator<Entry<String, JsonNode>> fields = rootNode.fields();

                while (fields.hasNext()) {

                    Entry<String, JsonNode> entry = fields.next();
                    String key = entry.getKey();
                    JsonNode value = entry.getValue();

                    // 메뉴, 메뉴 이미지 배치
                    Set<MenuDto> menuDtos = new LinkedHashSet<>();
                    Set<MenuImageDto> menuImageDtos = new LinkedHashSet<>();

                    if (MR_MENU_PATTERN.matcher(key).matches()) {
                        int menuPk = MenuIdGenerator.getNextId();
                        menuDtos.add(MenuDto.of(
                                menuPk,
                                restaurantPk,
                                checkNull(decode(value.path("name").asText())),
                                checkNull((value.path("price").asText())),
                                value.path("recommend").asBoolean(),
                                checkNull(decode(value.path("description").asText())),
                                value.path("index").asInt()));
                        menuWriter.write(menuDtos);

                        for (JsonNode image : value.path("images")) {
                            menuImageDtos.add(MenuImageDto.of(
                                    menuPk,
                                    decode(image.asText())));
                            menuImageWriter.write(menuImageDtos);
                        }
                    }

                    // 리뷰, 리뷰 이미지 배치
                    Set<ReviewDto> reviewDtos = new LinkedHashSet<>();
                    Set<ReviewImageDto> reviewImageDtos = new LinkedHashSet<>();

                    if (MR_REVIEW_PATTERN.matcher(key).matches()) {
                        int reviewPk = ReviewIdGenerator.getNextId();
                        reviewDtos.add(ReviewDto.of(
                                reviewPk,
                                restaurantPk,
                                checkNull(decode(value.path("name").asText())),
                                checkNull(value.path("typeName").asText()),
                                checkNull(decode(value.path("url").asText())),
                                checkNull(decode(value.path("title").asText())),
                                checkNull(value.path("rank").asText()),
                                checkNull(decode(value.path("contents").asText())),
                                checkNull(decode(value.path("profileImageUrl").asText())),
                                checkNull(decode(value.path("authorName").asText())),
                                checkNull(value.path("date").asText())));
                        reviewWriter.write(reviewDtos);

                        for (JsonNode thumbnailUrl : value.path("thumbnailUrlList")) {
                            reviewImageDtos.add(ReviewImageDto.of(
                                    reviewPk,
                                    checkNull(decode(thumbnailUrl.asText()))));
                            reviewImageWriter.write(reviewImageDtos);
                        }
                    }

                    // 영업시간 배치
                    Set<BizHourDto> bizHourDtos = new LinkedHashSet<>();

                    if (MR_ROOT_QUERY_PATTERN.matcher(key).matches()) {
                        JsonNode businessHours = value
                                .path(String.format(MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT, restaurantId))
                                .path(MR_BIZ_HOUR_SECOND_DEPTH_KEY).path(0)
                                .path(MR_BIZ_HOUR_THIRD_DEPTH_KEY);

                        for (JsonNode businessHour : businessHours) {
                            bizHourDtos.add(BizHourDto.of(
                                    restaurantPk,
                                    checkDay(checkNull(businessHour.path("day").asText())),
                                    checkNull(businessHour.path("businessHours").path("start").asText()),
                                    checkNull(businessHour.path("businessHours").path("end").asText()),
                                    checkNull(businessHour.path("breakHours").path(0).path("start").asText()),
                                    checkNull(businessHour.path("breakHours").path(0).path("end").asText()),
                                    checkNull(businessHour.path("lastOrderTimes").path(0).path("time").asText())));
                            bizHourWriter.write(bizHourDtos);
                        }
                    }
                }

                // 크롤링 성공시 반복문 종료
                success = true;
                break;

            } catch (JsonProcessingException e) {
                success = true;
                break;
            } catch (IOException e) {
                retryCount++;
                log.error("[MR] IOException 발생 (재시도 횟수 {})", retryCount, e);
            } catch (Exception e) {
                retryCount++;
                log.error("[MR] Exception 발생 (재시도 횟수 {})", retryCount, e);
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
            log.error("[MR] 크롤링 실패 : 최대 재시도 횟수 초과");
        }
    }

    private URI setUri(String restaurantId) {
        return URI.create(String.format(MR_URI_FORMAT, restaurantId, now()));
    }

    private String sendHttpRequest(URI uri, String restaurantId) throws IOException, ParseException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(uri);
            request.addHeader(MR_REFERER_KEY, String.format(MR_REFERER_VALUE_FORMAT, restaurantId));

            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        }
    }

    private String decode(String input) {
        if (input == null) {
            return null;
        }
        return unescapeJava(unescapeHtml4(decodeUnicode(input)));
    }

    private String checkNull(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input;
    }

    private String checkDay(String input) {
        if (input.contains("(")) {
            return input.substring(0, input.indexOf("(")).trim();
        }
        return input;
    }

    private String now() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return now.format(formatter);
    }
}