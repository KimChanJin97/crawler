package com.rouleatt.demo.crawler;

import static com.rouleatt.demo.utils.CrawlerUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rouleatt.demo.batch.RestaurantImageBatchExecutor;
import com.rouleatt.demo.batch.RestaurantIdGenerator;
import com.rouleatt.demo.backup.RestaurantImageBackupManager;
import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.dto.RestaurantImageBackupDto;
import com.rouleatt.demo.utils.Region;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;


@Slf4j
public class RestaurantImageBatchCrawler {

    private static int BATCH_COUNT = 0;
    private static final int BATCH_SIZE = 30;

    private final RestaurantImageBackupManager backupManager;
    private final TableManager tableManager;
    private final RestaurantImageBatchExecutor batchExecutor;
    private final ObjectMapper mapper;

    public RestaurantImageBatchCrawler() {
        this.backupManager = new RestaurantImageBackupManager();
        this.tableManager = new TableManager();
        this.mapper = new ObjectMapper();
        this.batchExecutor = new RestaurantImageBatchExecutor();
    }

    public void crawl() {

        Stack<RestaurantImageBackupDto> stack = new Stack<>();

        // 좌표 데이터가 백업되어있다면 백업 데이터(차단 시점의 좌표들)부터 크롤링
        if (backupManager.hasFirstRiBackup()) {
            log.info("IP 차단 시점의 좌표부터 크롤링 시작");
            List<RestaurantImageBackupDto> backupDtos = backupManager.getAllRiBackupsOrderByIdDesc();
            backupManager.dropAndCreateRiBackupTable();
            backupDtos.forEach(backupDto -> stack.push(backupDto));
        }
        // 좌표 데이터가 백업되어있지 않다면 데이터베이스 드랍하고 처음부터 크롤링
        else {
            log.info("처음부터 크롤링 시작");
            tableManager.dropAndCreateAllTables();
            Arrays.stream(Region.values()).forEach(region -> stack.push(RestaurantImageBackupDto.from(region)));
        }

        while (!stack.isEmpty()) {

            RestaurantImageBackupDto backupDto = stack.pop();

            String fullName = backupDto.fullName();
            String shortName = backupDto.shortName();
            double minX = backupDto.minX();
            double minY = backupDto.minY();
            double maxX = backupDto.maxX();
            double maxY = backupDto.maxY();

            String response = null;

            try {
                URI uri = setUri(minX, minY, maxX, maxY);
                response = sendHttpRequest(uri);

                Thread.sleep(1000);

                JsonNode rootNode = mapper.readTree(response);
                JsonNode resultNode = rootNode.path("result");
                JsonNode metaNode = resultNode.path("meta");
                JsonNode countNode = metaNode.path("count");
                JsonNode restaurantsNode = resultNode.path("list");

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
                                    checkNull(restaurantNode.path("categoryName").asText()),
                                    checkNull(restaurantNode.path("address").asText()),
                                    checkNull(restaurantNode.path("roadAddress").asText()));

                            // 음식점 이미지 크롤링 및 배치
                            for (JsonNode imageNode : restaurantNode.path("images")) {
                                batchExecutor.addRestaurantImage(restaurantPk, imageNode.asText());
                            }
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
                // 크롤링한 음식점이 100개 이상이라면 영역을 쪼개기 위해 스택 푸시
                else if (countNode.asInt() >= 100) {

                    double midX = (minX + maxX) / 2;
                    double midY = (minY + maxY) / 2;

                    stack.push(RestaurantImageBackupDto.of(fullName, shortName, midX, minY, maxX, midY));
                    stack.push(RestaurantImageBackupDto.of(fullName, shortName, minX, minY, midX, midY));
                    stack.push(RestaurantImageBackupDto.of(fullName, shortName, midX, midY, maxX, maxY));
                    stack.push(RestaurantImageBackupDto.of(fullName, shortName, minX, midY, midX, maxY));

                }
            } catch (IOException e) {
                log.error("[RI] IOException 발생. IP 차단 시점의 행정구역 이름과 좌표 저장\n", e);

                // 배치 삽입
                batchExecutor.batchInsert();

                // IP 차단 시점의 좌표를 저장
                backupManager.setRiBackup(backupDto);
                // IP 차단 시점의 스택의 모든 요소들을 저장
                backupManager.setAllRiBackups(stack);

                log.info("백업 완료");
                return;

            } catch (ParseException e) {
                log.error("[RI] ParseException 발생\n 응답 = {}\n", response);
                return;
            } catch (InterruptedException e) {
                log.error("[RI] InterruptedException 발생. 크롤링 종료");
                return;
            }
        }
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

