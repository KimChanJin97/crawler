package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.decodeUnicode;
import static java.lang.Integer.*;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.db.JdbcBatchExecutor;
import com.rouleatt.demo.db.MenuIdGenerator;
import com.rouleatt.demo.db.ReviewIdGenerator;
import com.rouleatt.demo.utils.EnvLoader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.brotli.dec.BrotliInputStream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Slf4j
public class MenuReviewBatchCrawler {

    private final JdbcBatchExecutor jdbcBatchExecutor;
    private final ObjectMapper objectMapper;

    private static final String MR_URI_FORMAT = EnvLoader.get("MR_URI_FORMAT");
    private static final String MR_REFERER_KEY = EnvLoader.get("MR_REFERER_KEY");
    private static final String MR_REFERER_VALUE_FORMAT = EnvLoader.get("MR_REFERER_VALUE_FORMAT");

    private static final String MR_ACCEPT_ENCODING_KEY = EnvLoader.get("MR_ACCEPT_ENCODING_KEY");
    private static final String MR_ACCEPT_ENCODING_VALUE = EnvLoader.get("MR_ACCEPT_ENCODING_VALUE");

    private static final String MR_LOWER_BOUND = EnvLoader.get("MR_LOWER_BOUND").replace("\"", "");
    private static final String MR_UPPER_BOUND = EnvLoader.get("MR_UPPER_BOUND").replace("\"", "");
    private static final Pattern MR_MENU_PATTERN = Pattern.compile(EnvLoader.get("MR_MENU_PATTERN"));
    private static final Pattern MR_REVIEW_PATTERN = Pattern.compile(EnvLoader.get("MR_REVIEW_PATTERN"));
    private static final Pattern MR_ROOT_QUERY_PATTERN = Pattern.compile(EnvLoader.get("MR_ROOT_QUERY_PATTERN"));
    private static final String MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT = EnvLoader.get("MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT");
    private static final String MR_BIZ_HOUR_SECOND_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_SECOND_DEPTH_KEY");
    private static final String MR_BIZ_HOUR_THIRD_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_THIRD_DEPTH_KEY");

    public MenuReviewBatchCrawler() {
        this.jdbcBatchExecutor = new JdbcBatchExecutor();
        this.objectMapper = new ObjectMapper();
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
                    if (MR_MENU_PATTERN.matcher(key).matches()) {
                        int menuPk = MenuIdGenerator.getNextId();

                        jdbcBatchExecutor.addMenu(
                                menuPk,
                                restaurantPk,
                                checkNull(decode(value.path("name").asText())),
                                checkNull((value.path("price").asText())),
                                value.path("recommend").asBoolean(),
                                checkNull(decode(value.path("description").asText())),
                                value.path("index").asInt()
                        );

                        for (JsonNode image :  value.path("images")) {
                            jdbcBatchExecutor.addMenuImage(menuPk, decode(image.asText()));
                        }
                    }

                    // 리뷰, 리뷰 이미지 배치
                    if (MR_REVIEW_PATTERN.matcher(key).matches()) {
                        int reviewPk = ReviewIdGenerator.getNextId();

                        jdbcBatchExecutor.addReview(
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
                                checkNull(value.path("createdString").asText())
                        );

                        for (JsonNode thumbnailUrl : value.path("thumbnailUrlList")) {
                            jdbcBatchExecutor.addReviewImage(reviewPk, checkNull(decode(thumbnailUrl.asText())));
                        }
                    }

                    // 영업시간, 브레이크타임, 라스트오더 배치
                    if (MR_ROOT_QUERY_PATTERN.matcher(key).matches()) {
                        JsonNode businessHours = value
                                .path(String.format(MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT, restaurantId))
                                .path(MR_BIZ_HOUR_SECOND_DEPTH_KEY).path(0)
                                .path(MR_BIZ_HOUR_THIRD_DEPTH_KEY);

                        for (JsonNode businessHour : businessHours) {

                            jdbcBatchExecutor.addBizHour(
                                    restaurantPk,
                                    checkDay(checkNull(businessHour.path("day").asText())),
                                    checkNull(businessHour.path("businessHours").path("start").asText()),
                                    checkNull(businessHour.path("businessHours").path("end").asText()),
                                    checkNull(businessHour.path("breakHours").path(0).path("start").asText()),
                                    checkNull(businessHour.path("breakHours").path(0).path("end").asText()),
                                    checkNull(businessHour.path("lastOrderTimes").path(0).path("time").asText())
                            );
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
            log.error("[MR] 크롤링 실패 : 최대 재시도 횟수 초과");
        }
    }

    private URI setUri(String restaurantId) {
        return URI.create(String.format(MR_URI_FORMAT, restaurantId));
    }

    private String sendHttpRequest(URI uri, String restaurantId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(uri.toString()).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty(MR_ACCEPT_ENCODING_KEY, MR_ACCEPT_ENCODING_VALUE);
        conn.setRequestProperty(MR_REFERER_KEY, String.format(MR_REFERER_VALUE_FORMAT, restaurantId));
        conn.setRequestProperty("Connection", "Keep-Alive");

        try (InputStream responseStream = conn.getInputStream()) {
            if ("br".equalsIgnoreCase(conn.getContentEncoding())) {
                return readBrotliStream(responseStream);
            } else {
                return new Scanner(responseStream, "UTF-8").useDelimiter("\\A").next();
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String readBrotliStream(InputStream encodedStream) throws IOException {
        try (BrotliInputStream brotliInputStream = new BrotliInputStream(encodedStream);
             InputStreamReader reader = new InputStreamReader(brotliInputStream);
             BufferedReader br = new BufferedReader(reader, 8192)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
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
        if (input.contains("월")) {
            return "월";
        } else if (input.contains("화")) {
            return "화";
        } else if (input.contains("수")) {
            return "수";
        } else if (input.contains("목")) {
            return "목";
        } else if (input.contains("금")) {
            return "금";
        } else if (input.contains("토")) {
            return "토";
        } else if (input.contains("일")) {
            return "일";
        } else if (input.contains("매일")) {
            return "매일";
        } else if (input.contains("휴무")) {
            return "휴무";
        } else {
            return null;
        }
    }
}


