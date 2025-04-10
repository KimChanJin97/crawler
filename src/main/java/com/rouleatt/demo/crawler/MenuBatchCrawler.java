package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.*;
import static com.rouleatt.demo.utils.CrawlerUtils.decodeUnicode;
import static java.lang.Integer.*;
import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;
import static org.apache.commons.text.StringEscapeUtils.unescapeJava;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.batch.MenuBatchExecutor;
import com.rouleatt.demo.backup.MenuBackupManager;
import com.rouleatt.demo.batch.MenuIdGenerator;
import com.rouleatt.demo.batch.ReviewIdGenerator;
import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.dto.MenuBackupDto;
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
public class MenuBatchCrawler {

    private static int MAX_RETRY = 60;
    private static int BATCH_COUNT = 0;
    private static final int BATCH_SIZE = 50;
    private static final Stack<MenuBackupDto> STACK = new Stack<>();

    private final MenuBackupManager backupManager;
    private final TableManager tableManager;
    private final MenuBatchExecutor batchExecutor;
    private final ObjectMapper objectMapper;

    public MenuBatchCrawler() {
        this.backupManager = new MenuBackupManager();
        this.tableManager = new TableManager();
        this.batchExecutor = new MenuBatchExecutor();
        this.objectMapper = new ObjectMapper();
    }

    public void crawl() {

        // 메뉴 데이터가 백업되어있다면 백업 데이터(차단 시점의 좌표들)부터 크롤링
        if (backupManager.hasFirstMenuBackup()) {
            log.info("[M] IP 차단 시점의 좌표부터 크롤링 시작");
            List<MenuBackupDto> backupDtos = backupManager.getAllMenuBackupsOrderByIdDesc();
            backupManager.dropAndCreateMenuBackupTable();
            backupDtos.forEach(backupDto -> STACK.push(backupDto));
        }
        // 메뉴 데이터가 백업되어있지 않다면 데이터베이스 드랍하고 처음부터 크롤링
        else {
            log.info("[M] 처음부터 크롤링 시작");
            tableManager.dropAndCreateMenuAndReviewTable();
            List<MenuBackupDto> backupDtos = backupManager.getAllMenuBackupPkId();
            backupDtos.forEach(backupDto -> STACK.push(backupDto));
        }

        while (!STACK.isEmpty()) {

            MenuBackupDto backupDto = STACK.peek();
            int restaurantPk = backupDto.restaurantPk();
            String restaurantId = backupDto.restaurantId();

            int retry = 1;
            while (retry < MAX_RETRY) {

                try {
                    log.info("[M] {} 음식점 {} 번째 크롤링", restaurantId, retry);

                    URI uri = setUri(restaurantId);
                    String response = sendHttpRequest(uri, restaurantId);

                    // 요청 성공했다면 pop
                    STACK.pop();

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

                        // 배치 사이즈에 도달하지 않았다면 배치 카운트 증가
                        if (BATCH_COUNT < BATCH_SIZE) {
                            BATCH_COUNT++;
                        }
                        // 배치 사이즈에 도달했다면 배치 삽입. 배치 카운트 초기화
                        if (BATCH_COUNT >= BATCH_SIZE) {
                            batchExecutor.batchInsert();
                            BATCH_COUNT = 0;
                        }
                    }

                    // 배치 삽입이 성공했다면 백업 데이터 삭제
                    if (backupManager.hasFirstMenuBackup()) {
                        log.info("[R] 정상. 백업 데이터 삭제");
                        backupManager.dropAndCreateMenuBackupTable();
                    }
                    // while 탈출
                    break;

                } catch (Exception ex) {

                    // 백업 데이터가 존재하지 않다면
                    if (!backupManager.hasFirstMenuBackup()) {
                        log.error("[M] 예외 발생. IP 차단 시점의 음식점 저장\n", ex);
                        batchExecutor.batchInsert(); // 배치에 쌓여있는 데이터 배치 삽입
                        backupManager.setAllMenuBackups(STACK); // IP 차단 시점의 스택의 모든 요소들을 저장
                        log.info("[M] 백업 완료");
                    }

                    // 슬립 후 크롤링 재시도
                    try {
                        log.info("[M] {} 분 슬립 후 크롤링 재시도", 10 * retry);
                        Thread.sleep(10 * 60_000 * retry++); // 10분 단위로 슬립
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // 마지막 배치 삽입
        batchExecutor.batchInsert();
        log.info("[M] 마지막 배치 삽입");
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