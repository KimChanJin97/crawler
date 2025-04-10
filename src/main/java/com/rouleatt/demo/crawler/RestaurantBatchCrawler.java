package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.batch.RestaurantBatchExecutor;
import com.rouleatt.demo.batch.RestaurantIdGenerator;
import com.rouleatt.demo.backup.RestaurantBackupManager;
import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.dto.RestaurantBackupDto;
import com.rouleatt.demo.utils.Region;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;


@Slf4j
public class RestaurantBatchCrawler {

    private static int MAX_RETRY = 60;
    private static int BATCH_COUNT = 0;
    private static final int BATCH_SIZE = 50;
    private static final Stack<RestaurantBackupDto> STACK = new Stack<>();

    private final RestaurantBackupManager backupManager;
    private final TableManager tableManager;
    private final RestaurantBatchExecutor batchExecutor;
    private final ObjectMapper mapper;

    public RestaurantBatchCrawler() {
        this.backupManager = new RestaurantBackupManager();
        this.tableManager = new TableManager();
        this.mapper = new ObjectMapper();
        this.batchExecutor = new RestaurantBatchExecutor();
    }

    public void crawl() {

        // 좌표 데이터가 백업되어있다면 백업 데이터(차단 시점의 좌표들)부터 크롤링
        if (backupManager.hasFirstRestaurantBackup()) {
            log.info("[R] IP 차단 시점의 좌표부터 크롤링 시작");
            List<RestaurantBackupDto> backupDtos = backupManager.getAllRestaurantBackupsOrderByIdDesc();
            backupManager.dropAndCreateRestaurantBackupTable();
            backupDtos.forEach(backupDto -> STACK.push(backupDto));
        }
        // 좌표 데이터가 백업되어있지 않다면 데이터베이스 드랍하고 처음부터 크롤링
        else {
            log.info("[R] 처음부터 크롤링 시작");
            tableManager.dropAndCreateAllTables();
            Arrays.stream(Region.values()).forEach(region -> STACK.push(RestaurantBackupDto.from(region)));
        }

        while (!STACK.isEmpty()) {

            RestaurantBackupDto backupDto = STACK.peek(); // 재시도 로직 때문에 성공한 이후에 pop 해야함
            String fullName = backupDto.fullName();
            String shortName = backupDto.shortName();
            double minX = backupDto.minX();
            double minY = backupDto.minY();
            double maxX = backupDto.maxX();
            double maxY = backupDto.maxY();

            int retry = 1;
            while (retry < MAX_RETRY) {

                try {

                    URI uri = setUri(minX, minY, maxX, maxY);
                    String response = sendHttpRequest(uri);

                    Thread.sleep(1_000);

                    log.info("[R] 스택 사이즈 {} | 지역 {} | 좌표 {} {} {} {} | 재시도 횟수 {} ",STACK.size(), shortName, minX, minY, maxX, maxY, retry);

                    JsonNode rootNode = mapper.readTree(response); // 파싱 (예외 발생 포인트)
                    JsonNode resultNode = rootNode.path("result");
                    JsonNode metaNode = resultNode.path("meta");
                    JsonNode countNode = metaNode.path("count");
                    JsonNode restaurantsNode = resultNode.path("list");

                    // 요청 성공시 스택 요소 제거
                    STACK.pop();

                    // 크롤링한 음식점이 0개 초과 100개 미만이라면
                    if (0 < countNode.asInt() && countNode.asInt() < 100) {

                        for (JsonNode restaurantNode : restaurantsNode) {

                            // 주소가 타겟팅한 행정구역이라면
                            String address = checkNull(restaurantNode.path("address").asText());

                            if (isTarget(address, fullName, shortName)) {

                                int restaurantPk = RestaurantIdGenerator.getNextId();
                                String restaurantId = restaurantNode.path("id").asText();

                                // 음식점 크롤링 및 배치
                                batchExecutor.addRestaurant(
                                        restaurantPk,
                                        restaurantId,
                                        restaurantNode.path("name").asText(),
                                        Double.parseDouble(restaurantNode.path("x").asText()),
                                        Double.parseDouble(restaurantNode.path("y").asText()),
                                        checkNull(restaurantNode.path("categoryName").asText()).split(",")[0],
                                        checkNull(restaurantNode.path("address").asText()),
                                        checkNull(restaurantNode.path("roadAddress").asText()));

                                // 음식점 이미지 크롤링 및 배치
                                for (JsonNode imageNode : restaurantNode.path("images")) {
                                    batchExecutor.addRestaurantImage(restaurantPk, imageNode.asText());
                                }
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

                    // 크롤링한 음식점이 100개 이상이라면 영역을 쪼개기 위해 스택 푸시
                    else if (countNode.asInt() >= 100) {

                        double midX = (minX + maxX) / 2;
                        double midY = (minY + maxY) / 2;

                        STACK.push(RestaurantBackupDto.of(fullName, shortName, minX, midY, midX, maxY)); // 4
                        STACK.push(RestaurantBackupDto.of(fullName, shortName, minX, minY, midX, midY)); // 3
                        STACK.push(RestaurantBackupDto.of(fullName, shortName, midX, midY, maxX, maxY)); // 2
                        STACK.push(RestaurantBackupDto.of(fullName, shortName, midX, minY, maxX, midY)); // 1
                    }

                    // 배치삽입 또는 영역쪼개기 둘 중 하나라도 성공했다면 백업 삭제
                    if (backupManager.hasFirstRestaurantBackup()) {
                        backupManager.dropAndCreateRestaurantBackupTable();
                        log.info("[R] 정상. 백업 데이터 삭제");
                    }
                    // while 탈출
                    break;

                } catch (Exception ex) {

                    // 배치에 쌓여있는 데이터 배치 삽입
                    batchExecutor.batchInsert();

                    // 백업 데이터가 존재하지 않다면 백업
                    if (!backupManager.hasFirstRestaurantBackup()) {
                        log.error("[R] 예외 발생. IP 차단 시점의 행정구역 이름과 좌표 저장\n", ex);
                        backupManager.setAllRestaurantBackups(STACK); // IP 차단 시점의 스택의 모든 요소들을 저장
                        log.info("[R] 백업 완료");
                    }

                    // 슬립 후 크롤링 재시도
                    try {
                        log.info("[R] {} 분 슬립 후 크롤링 재시도", 10 * retry);
                        Thread.sleep(10 * 60_000 * retry++); // 10분 단위로 슬립
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        // 마지막 배치 삽입
        batchExecutor.batchInsert();
        log.info("[R] 마지막 배치 삽입");
    }

    private URI setUri(double minX, double minY, double maxX, double maxY) {
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        return URI.create(String.format(
                RI_URI,
                RI_SEARCH_COORD_KEY, midX, midY,
                RI_BOUNDARY_KEY, minX, minY, maxX, maxY,
                RI_CODE_KEY, RI_CODE_VALUE,
                RI_LIMIT_KEY, RI_LIMIT_VALUE,
                RI_SORT_TYPE_KEY, RI_SORT_TYPE_VALUE,
                RI_TIME_CODE_KEY, "MORNING",
                RI_TIME_CODE_KEY, "LUNCH",
                RI_TIME_CODE_KEY, "AFTERNOON",
                RI_TIME_CODE_KEY, "EVENING",
                RI_TIME_CODE_KEY, "NIGHT"));
    }

    private String sendHttpRequest(URI uri) throws IOException, ParseException {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(uri);
            request.addHeader(RI_AUTHORITY_KEY, RI_AUTHORITY_VALUE);
            request.addHeader(RI_METHOD_KEY, RI_METHOD_VALUE);
            request.addHeader(RI_PATH_KEY, uri.toString().split(".com")[1]);
            request.addHeader(RI_SCHEME_KEY, RI_SCHEME_VALUE);
            request.addHeader(RI_ACCEPT_KEY, RI_ACCEPT_VALUE);
            request.addHeader(RI_ACCEPT_ENCODING_KEY, RI_ACCEPT_ENCODING_VALUE);
            request.addHeader(RI_ACCEPT_LANGUAGE_KEY, RI_ACCEPT_LANGUAGE_VALUE);
            request.addHeader(RI_CACHE_CONTROL_KEY, RI_CACHE_CONTROL_VALUE);
            request.addHeader(RI_EXPIRES_KEY, RI_EXPIRES_VALUE);
            request.addHeader(RI_PRAGMA_KEY, RI_PRAGMA_VALUE);
            request.addHeader(RI_PRIORITY_KEY, RI_PRIORITY_VALUE);
            request.addHeader(RI_REFERER_KEY, RI_REFERER_VALUE);
            request.addHeader(RI_SEC_CH_UA_KEY, RI_SEC_CH_UA_VALUE);
            request.addHeader(RI_SEC_CH_UA_MOBILE_KEY, RI_SEC_CH_UA_MOBILE_VALUE);
            request.addHeader(RI_SEC_CH_UA_PLATFORM_KEY, RI_SEC_CH_UA_PLATFORM_VALUE);
            request.addHeader(RI_SEC_FETCH_DEST_KEY, RI_SEC_FETCH_DEST_VALUE);
            request.addHeader(RI_SEC_FETCH_MODE_KEY, RI_SEC_FETCH_MODE_VALUE);
            request.addHeader(RI_SEC_FETCH_SITE_KEY, RI_SEC_FETCH_SITE_VALUE);
            request.addHeader(RI_USER_AGENT_KEY, RI_USER_AGENT_VALUE);

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

    private boolean isTarget(String address, String fullName, String shortName) {
        // 행정구역을 추출할 수 있는 길이의 주소라면
        if (address != null && address.length() >= 8) {
            String region = address.substring(0, 7);
            return region.contains(fullName) || region.contains(shortName);
        }
        // 행정구역을 추출할 수 없는 길이의 주소라면
        if (address != null) {
            return address.contains(fullName) || address.contains(shortName);
        }
        // 주소가 존재하지 않다면
        return false;
    }

    private String checkNull(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        return input;
    }
}

