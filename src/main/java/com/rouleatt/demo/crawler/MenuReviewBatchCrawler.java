package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.*;
import static com.rouleatt.demo.utils.CrawlerUtils.decodeUnicode;
import static java.lang.Integer.*;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.batch.MenuReviewBatchExecutor;
import com.rouleatt.demo.backup.MenuReviewBackupManager;
import com.rouleatt.demo.batch.MenuIdGenerator;
import com.rouleatt.demo.batch.ReviewIdGenerator;
import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.dto.MenuReviewBackupDto;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
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
public class MenuReviewBatchCrawler {

    private static int BATCH_COUNT = 0;
    private static final int BATCH_SIZE = 50;

    private final MenuReviewBackupManager backupManager;
    private final TableManager tableManager;
    private final MenuReviewBatchExecutor batchExecutor;
    private final ObjectMapper objectMapper;

    public MenuReviewBatchCrawler() {
        this.backupManager = new MenuReviewBackupManager();
        this.tableManager = new TableManager();
        this.batchExecutor = new MenuReviewBatchExecutor();
        this.objectMapper = new ObjectMapper();
    }

    public void crawl() {

        Stack<MenuReviewBackupDto> stack = new Stack<>();

        // 메뉴 데이터가 백업되어있다면 백업 데이터(차단 시점의 좌표들)부터 크롤링
        if (backupManager.hasFirstMrBackup()) {
            log.info("IP 차단 시점의 좌표부터 크롤링 시작");
            List<MenuReviewBackupDto> backupDtos = backupManager.getAllMrBackupsOrderByIdDesc();
            backupManager.dropAndCreateMrBackupTable();
            backupDtos.forEach(backupDto -> stack.push(backupDto));
        }
        // 메뉴 데이터가 백업되어있지 않다면 데이터베이스 드랍하고 처음부터 크롤링
        else {
            log.info("처음부터 크롤링 시작");
            tableManager.dropAndCreateMenuAndReviewTable();
            List<MenuReviewBackupDto> backupDtos = backupManager.getAllRestaurantPkId();
            backupDtos.forEach(backupDto -> stack.push(backupDto));
        }

        while (!stack.isEmpty()) {

            MenuReviewBackupDto backupDto = stack.pop();
            int restaurantPk = backupDto.restaurantPk();
            String restaurantId = backupDto.restaurantId();

            int retryCount = 1;

            while (retryCount < MAX_VALUE) {

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
                            batchExecutor.addMenu(
                                    menuPk,
                                    restaurantPk,
                                    checkNull(decode(value.path("name").asText())),
                                    checkNull((value.path("price").asText())),
                                    value.path("recommend").asBoolean(),
                                    checkNull(decode(value.path("description").asText())),
                                    value.path("index").asInt()
                            );

                            for (JsonNode image : value.path("images")) {
                                batchExecutor.addMenuImage(menuPk, decode(image.asText()));
                            }
                        }

                        // 리뷰, 리뷰 이미지 배치
                        if (MR_REVIEW_PATTERN.matcher(key).matches()) {
                            int reviewPk = ReviewIdGenerator.getNextId();
                            batchExecutor.addReview(
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
                                    checkNull(value.path("date").asText())
                            );

                            for (JsonNode thumbnailUrl : value.path("thumbnailUrlList")) {
                                batchExecutor.addReviewImage(reviewPk, checkNull(decode(thumbnailUrl.asText())));
                            }
                        }

                        // 영업시간, 브레이크타임, 라스트오더 배치
                        if (MR_ROOT_QUERY_PATTERN.matcher(key).matches()) {
                            JsonNode businessHours = value
                                    .path(String.format(MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT, restaurantId))
                                    .path(MR_BIZ_HOUR_SECOND_DEPTH_KEY).path(0)
                                    .path(MR_BIZ_HOUR_THIRD_DEPTH_KEY);

                            for (JsonNode businessHour : businessHours) {
                                batchExecutor.addBizHour(
                                        restaurantPk,
                                        checkDay(checkNull(businessHour.path("day").asText())),
                                        checkNull(businessHour.path("businessHours").path("start").asText()),
                                        checkNull(businessHour.path("breakHours").path(0).path("start").asText()),
                                        checkNull(businessHour.path("breakHours").path(0).path("end").asText()),
                                        checkNull(businessHour.path("lastOrderTimes").path(0).path("time").asText()),
                                        checkNull(businessHour.path("businessHours").path("end").asText())
                                );
                            }
                        }

                        // 타겟팅한 행정구역이 아닐 경우 배치 삽입 호출할 필요없음
                        if (batchExecutor.shouldBatchInsert()) {

                            // 배치 사이즈에 도달하지 않았다면 배치 카운트 증가
                            if (BATCH_COUNT < BATCH_SIZE) {
                                BATCH_COUNT++;
                            }
                            // 배치 사이즈에 도달했다면 배치 삽입. 배치 카운트 초기화
                            else {
                                batchExecutor.batchInsert();
                                BATCH_COUNT = 0;
                            }
                        }
                    }

                    // 배치 삽입 또는 영역 쪼개기 중 하나라도 성공했다면 재시도 반복문 종료
                    break;

                } catch (Exception ex) {
                    log.error("[MR] IOException 발생. 차단 시점의 rpk, rid 저장\n", ex);
                    batchExecutor.batchInsert(); // 배치에 쌓여있는 데이터 배치 삽입
                    backupManager.setMrBackup(backupDto); // IP 차단 시점의 좌표를 저장
                    backupManager.setAllMrBackups(stack); // IP 차단 시점의 스택의 모든 요소들을 저장
                    log.info("[MR] 백업 완료\n");

                    try {
                        log.info("[MR] {} 분 슬립 후 크롤링 재시도\n", 60_000 * retryCount++);
                        Thread.sleep(60_000 * retryCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        if (batchExecutor.shouldBatchInsert()) {
            batchExecutor.batchInsert();
            log.info("[MR] 배치 카운트에 도달하지 못해 배치에 남아있는 데이터 배치 삽입");
        }
    }

    private URI setUri(String restaurantId) {
        return URI.create(String.format(MR_URI, restaurantId, now()));
    }

    private String sendHttpRequest(URI uri, String restaurantId) throws IOException, ParseException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(uri);
            request.addHeader(MR_AUTHORITY_KEY, MR_AUTHORITY_VALUE);
            request.addHeader(MR_METHOD_KEY, MR_METHOD_VALUE);
            request.addHeader(MR_PATH_KEY, uri.toString().split("com")[1]);
            request.addHeader(MR_SCHEME_KEY, MR_SCHEME_VALUE);
            request.addHeader(MR_ACCEPT_KEY, MR_ACCEPT_VALUE);
            request.addHeader(MR_ACCEPT_ENCODING_KEY, MR_ACCEPT_ENCODING_VALUE);
            request.addHeader(MR_ACCEPT_LANGUAGE_KEY, MR_ACCEPT_LANGUAGE_VALUE);
            request.addHeader(MR_PRIORITY_KEY, MR_PRIORITY_VALUE);
            request.addHeader(MR_REFERER_KEY, String.format(MR_REFERER_VALUE, restaurantId));
            request.addHeader(MR_SEC_CH_UA_KEY, MR_SEC_CH_UA_VALUE);
            request.addHeader(MR_SEC_CH_UA_MOBILE_KEY, MR_SEC_CH_UA_MOBILE_VALUE);
            request.addHeader(MR_SEC_CH_UA_PLATFORM_KEY, MR_SEC_CH_UA_PLATFORM_VALUE);
            request.addHeader(MR_SEC_FETCH_DEST_KEY, MR_SEC_FETCH_DEST_VALUE);
            request.addHeader(MR_SEC_FETCH_MODE_KEY, MR_SEC_FETCH_MODE_VALUE);
            request.addHeader(MR_SEC_FETCH_SITE_KEY, MR_SEC_FETCH_SITE_VALUE);
            request.addHeader(MR_SEC_FETCH_USER_KEY, MR_SEC_FETCH_USER_VALUE);
            request.addHeader(MR_UPGRADE_INSECURE_REQUESTS_KEY, MR_UPGRADE_INSECURE_REQUESTS_VALUE);
            request.addHeader(MR_USER_AGENT_KEY, MR_USER_AGENT_VALUE);

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