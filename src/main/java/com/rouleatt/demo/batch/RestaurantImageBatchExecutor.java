package com.rouleatt.demo.batch;

import com.rouleatt.demo.dto.BizHourDto;
import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.MenuImageDto;
import com.rouleatt.demo.dto.RestaurantDto;
import com.rouleatt.demo.dto.RestaurantImageDto;
import com.rouleatt.demo.dto.ReviewDto;
import com.rouleatt.demo.dto.ReviewImageDto;
import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestaurantImageBatchExecutor {

    private static int count = 0;

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final List<RestaurantDto> RESTAURANT_BATCH = new ArrayList<>();
    private static final List<RestaurantImageDto> RESTAURANT_IMAGE_BATCH = new ArrayList<>();
    private static final List<MenuDto> MENU_BATCH = new ArrayList<>();
    private static final List<MenuImageDto> MENU_IMAGE_BATCH = new ArrayList<>();
    private static final List<ReviewDto> REVIEW_BATCH = new ArrayList<>();
    private static final List<ReviewImageDto> REVIEW_IMAGE_BATCH = new ArrayList<>();
    private static final List<BizHourDto> BIZ_HOUR_BATCH = new ArrayList<>();

    private static final String INSERT_RESTAURANT_MANUAL_INCREMENT_SQL = "INSERT INTO restaurant (id, rid, name, coordinate, category, address, road_address) VALUES (?, ?, ?, ST_GeomFromText(?, 4326), ?, ?, ?)";
    private static final String INSERT_RESTAURANT_IMAGE_AUTO_INCREMENT_SQL = "INSERT INTO restaurant_image (restaurant_id, url) VALUES (?, ?)";
    private static final String INSERT_MENU_MANUAL_INCREMENT_SQL = "INSERT INTO menu (id, restaurant_id, name, price, is_recommended, description, menu_idx) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MENU_IMAGE_AUTO_INCREMENT_SQL = "INSERT INTO menu_image (menu_id, image_url) VALUES (?, ?)";
    private static final String INSERT_REVIEW_MANUAL_INCREMENT_SQL = "INSERT INTO review (id, restaurant_id, name, type, url, title, review_idx, content, profile_url, author_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_REVIEW_IMAGE_AUTO_INCREMENT_SQL = "INSERT INTO review_image (review_id, thumbnail_url) VALUES (?, ?)";
    private static final String INSERT_BIZ_HOUR_AUTO_INCREMENT_SQL = "INSERT INTO biz_hour (restaurant_id, day, biz_start, biz_end, last_order, break_start, break_end) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public void addRestaurant(
            int restaurantPk,
            String restaurantId,
            String name,
            double x,
            double y,
            String category,
            String address,
            String roadAddress
    ) {
        RESTAURANT_BATCH.add(RestaurantDto.of(restaurantPk, restaurantId, name, x, y, category, address, roadAddress));
    }

    public void addRestaurantImage(
            int restaurantFk,
            String url
    ) {
        RESTAURANT_IMAGE_BATCH.add(RestaurantImageDto.of(restaurantFk, url));
    }

    public void batchInsert() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {

            conn.setAutoCommit(false);

            try (PreparedStatement rtpstmt = conn.prepareStatement(INSERT_RESTAURANT_MANUAL_INCREMENT_SQL);
                 PreparedStatement rtipstmt = conn.prepareStatement(INSERT_RESTAURANT_IMAGE_AUTO_INCREMENT_SQL);
                 PreparedStatement mpstmt = conn.prepareStatement(INSERT_MENU_MANUAL_INCREMENT_SQL);
                 PreparedStatement mipstmt = conn.prepareStatement(INSERT_MENU_IMAGE_AUTO_INCREMENT_SQL);
                 PreparedStatement rvpstmt = conn.prepareStatement(INSERT_REVIEW_MANUAL_INCREMENT_SQL);
                 PreparedStatement rvipstmt = conn.prepareStatement(INSERT_REVIEW_IMAGE_AUTO_INCREMENT_SQL);
                 PreparedStatement bhpstmt = conn.prepareStatement(INSERT_BIZ_HOUR_AUTO_INCREMENT_SQL)) {

                for (RestaurantDto restaurantDto : RESTAURANT_BATCH) {
                    rtpstmt.setInt(1, restaurantDto.restaurantPk());
                    rtpstmt.setString(2, restaurantDto.restaurantId());
                    rtpstmt.setString(3, restaurantDto.name());
                    rtpstmt.setString(4, String.format("POINT(%f %f)", restaurantDto.y(), restaurantDto.x()));
                    rtpstmt.setString(5, restaurantDto.category());
                    rtpstmt.setString(6, restaurantDto.address());
                    rtpstmt.setString(7, restaurantDto.roadAddress());
                    rtpstmt.addBatch();
                }

                for (RestaurantImageDto imageDto : RESTAURANT_IMAGE_BATCH) {
                    rtipstmt.setInt(1, imageDto.restaurantFk());
                    rtipstmt.setString(2, imageDto.url());
                    rtipstmt.addBatch();
                }

                // 배치 실행 (커밋X)
                rtpstmt.executeBatch();
                rtipstmt.executeBatch();

                // 배치 실행이 끝나면 한번에 커밋
                conn.commit();
                log.info("RI 배치 삽입 횟수 = {}", ++count);

                // 배치 후 리스트 비우기
                RESTAURANT_BATCH.clear();
                RESTAURANT_IMAGE_BATCH.clear();

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean shouldBatchInsert() {
        return !RESTAURANT_BATCH.isEmpty() ||
                !RESTAURANT_IMAGE_BATCH.isEmpty();
    }
}