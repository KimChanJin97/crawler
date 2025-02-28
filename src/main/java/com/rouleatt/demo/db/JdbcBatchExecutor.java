package com.rouleatt.demo.db;

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
public class JdbcBatchExecutor {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private final ThreadLocal<List<RestaurantDto>> RESTAURANT_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<RestaurantImageDto>> RESTAURANT_IMAGE_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<MenuDto>> MENU_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<MenuImageDto>> MENU_IMAGE_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<ReviewDto>> REVIEW_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<ReviewImageDto>> REVIEW_IMAGE_BATCH = ThreadLocal.withInitial(ArrayList::new);
    private final ThreadLocal<List<BizHourDto>> BIZ_HOUR_BATCH = ThreadLocal.withInitial(ArrayList::new);

    private static final String RESTAURANT_INSERT_MANUAL_INCREMENT_SQL = "INSERT INTO restaurant (id, rid, name, coordinate, category, address, road_address) VALUES (?, ?, ?, ST_GeomFromText(?, 4326), ?, ?, ?)";
    private static final String RESTAURANT_IMAGE_AUTO_INCREMENT_INSERT_SQL = "INSERT INTO restaurant_image (restaurant_id, url) VALUES (?, ?)";
    private static final String MENU_INSERT_MANUAL_INCREMENT_SQL = "INSERT INTO menu (id, restaurant_id, name, price, is_recommended, description, menu_idx) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String MENU_IMAGE_AUTO_INCREMENT_INSERT_SQL = "INSERT INTO menu_image (menu_id, image_url) VALUES (?, ?)";
    private static final String REVIEW_INSERT_MANUAL_INCREMENT_SQL = "INSERT INTO review (id, restaurant_id, name, type, url, title, review_idx, content, profile_url, author_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String REVIEW_IMAGE_AUTO_INCREMENT_INSERT_SQL = "INSERT INTO review_image (review_id, thumbnail_url) VALUES (?, ?)";
    private static final String BIZ_HOUR_AUTO_INCREMENT_INSERT_SQL = "INSERT INTO biz_hour (restaurant_id, day, biz_start, biz_end, last_order, break_start, break_end) VALUES (?, ?, ?, ?, ?, ?, ?)";

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
        RESTAURANT_BATCH.get().add(RestaurantDto.of(restaurantPk, restaurantId, name, x, y, category, address, roadAddress));
    }

    public void addRestaurantImage(
            int restaurantFk,
            String url
    ) {
        RESTAURANT_IMAGE_BATCH.get().add(RestaurantImageDto.of(restaurantFk, url));
    }

    public void addMenu(
            int menuPk,
            int restaurantFk,
            String name,
            String price,
            boolean isRecommended,
            String description,
            int menuIdx
    ) {
        MENU_BATCH.get().add(MenuDto.of(menuPk, restaurantFk, name, price, isRecommended, description, menuIdx));
    }

    public void addMenuImage(
            int menuFk,
            String imageUrl
    ) {
        MENU_IMAGE_BATCH.get().add(MenuImageDto.of(menuFk, imageUrl));
    }

    public void addReview(
            int reviewPk,
            int restaurantFk,
            String name,
            String type,
            String url,
            String title,
            String reviewIdx,
            String content,
            String profileUrl,
            String authorName,
            String createdAt
    ) {
        REVIEW_BATCH.get().add(ReviewDto.of(
                reviewPk,
                restaurantFk,
                name,
                type,
                url,
                title,
                reviewIdx,
                content,
                profileUrl,
                authorName,
                createdAt));
    }

    public void addReviewImage(int reviewFk, String thumbnailUrl) {
        REVIEW_IMAGE_BATCH.get().add(ReviewImageDto.of(reviewFk, thumbnailUrl));
    }

    public void addBizHour(
            int restaurantFk,
            String day,
            String bizStart,
            String bizEnd,
            String lastOrder,
            String breakStart,
            String breakEnd
    ) {
        BIZ_HOUR_BATCH.get().add(BizHourDto.of(restaurantFk, day, bizStart, bizEnd, lastOrder, breakStart, breakEnd));
    }

    public void batchInsertRestaurant() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement rtpstmt = conn.prepareStatement(RESTAURANT_INSERT_MANUAL_INCREMENT_SQL)) {

                for (RestaurantDto restaurantDto : RESTAURANT_BATCH.get()) {
                    rtpstmt.setInt(1, restaurantDto.restaurantPk());
                    rtpstmt.setString(2, restaurantDto.restaurantId());
                    rtpstmt.setString(3, restaurantDto.name());
                    rtpstmt.setString(4, String.format("POINT(%f %f)", restaurantDto.y(), restaurantDto.x()));
                    rtpstmt.setString(5, restaurantDto.category());
                    rtpstmt.setString(6, restaurantDto.address());
                    rtpstmt.setString(7, restaurantDto.roadAddress());
                    rtpstmt.addBatch();
                }

                rtpstmt.executeBatch();
                conn.commit();
                RESTAURANT_BATCH.get().clear();
                log.info("[JBE] 음식점 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertRestaurantImage() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement rtipstmt = conn.prepareStatement(RESTAURANT_IMAGE_AUTO_INCREMENT_INSERT_SQL)) {

                for (RestaurantImageDto imageDto : RESTAURANT_IMAGE_BATCH.get()) {
                    rtipstmt.setInt(1, imageDto.restaurantFk());
                    rtipstmt.setString(2, imageDto.url());
                    rtipstmt.addBatch();
                }

                rtipstmt.executeBatch();
                conn.commit();
                RESTAURANT_IMAGE_BATCH.get().clear();
                log.info("[JBE] 음식점 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertMenu() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement mpstmt = conn.prepareStatement(MENU_INSERT_MANUAL_INCREMENT_SQL)) {

                for (MenuDto menuDto : MENU_BATCH.get()) {
                    mpstmt.setInt(1, menuDto.menuPk());
                    mpstmt.setInt(2, menuDto.restaurantFk());
                    mpstmt.setString(3, menuDto.name());
                    mpstmt.setString(4, menuDto.price());
                    mpstmt.setBoolean(5, menuDto.isRecommended());
                    mpstmt.setString(6, menuDto.description());
                    mpstmt.setInt(7, menuDto.menuIdx());
                    mpstmt.addBatch();
                }

                mpstmt.executeBatch();
                conn.commit();
                MENU_BATCH.get().clear();
                log.info("[JBE] 메뉴 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertMenuImage() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement mipstmt = conn.prepareStatement(MENU_IMAGE_AUTO_INCREMENT_INSERT_SQL)) {

                for (MenuImageDto menuImageDto : MENU_IMAGE_BATCH.get()) {
                    mipstmt.setInt(1, menuImageDto.menuFk());
                    mipstmt.setString(2, menuImageDto.imageUrl());
                    mipstmt.addBatch();
                }

                mipstmt.executeBatch();
                conn.commit();
                MENU_IMAGE_BATCH.get().clear();
                log.info("[JBE] 메뉴 이미지 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertReview() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement rvpstmt = conn.prepareStatement(REVIEW_INSERT_MANUAL_INCREMENT_SQL)) {

                for (ReviewDto reviewDto : REVIEW_BATCH.get()) {
                    rvpstmt.setInt(1, reviewDto.reviewPk());
                    rvpstmt.setInt(2, reviewDto.restaurantFk());
                    rvpstmt.setString(3, reviewDto.name());
                    rvpstmt.setString(4, reviewDto.type());
                    rvpstmt.setString(5, reviewDto.url());
                    rvpstmt.setString(6, reviewDto.title());
                    rvpstmt.setString(7, reviewDto.reviewIdx());
                    rvpstmt.setString(8, reviewDto.content());
                    rvpstmt.setString(9, reviewDto.profileUrl());
                    rvpstmt.setString(10, reviewDto.authorName());
                    rvpstmt.setString(11, reviewDto.createdAt());
                    rvpstmt.addBatch();
                }

                rvpstmt.executeBatch();
                conn.commit();
                REVIEW_BATCH.get().clear();
                log.info("[JBE] 리뷰 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertReviewImage() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement rvipstmt = conn.prepareStatement(REVIEW_IMAGE_AUTO_INCREMENT_INSERT_SQL)) {

                for (ReviewImageDto reviewImageDto : REVIEW_IMAGE_BATCH.get()) {
                    rvipstmt.setInt(1, reviewImageDto.reviewFk());
                    rvipstmt.setString(2, reviewImageDto.thumbnailUrl());
                    rvipstmt.addBatch();
                }

                rvipstmt.executeBatch();
                conn.commit();
                REVIEW_IMAGE_BATCH.get().clear();
                log.info("[JBE] 리뷰 이미지 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void batchInsertBizHour() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {
            conn.setAutoCommit(false);
            try (PreparedStatement bhpstmt = conn.prepareStatement(BIZ_HOUR_AUTO_INCREMENT_INSERT_SQL)) {

                for (BizHourDto bizHourDto : BIZ_HOUR_BATCH.get()) {
                    bhpstmt.setInt(1, bizHourDto.restaurantFk());
                    bhpstmt.setString(2, bizHourDto.day());
                    bhpstmt.setString(3, bizHourDto.bizStart());
                    bhpstmt.setString(4, bizHourDto.bizEnd());
                    bhpstmt.setString(5, bizHourDto.lastOrder());
                    bhpstmt.setString(6, bizHourDto.breakStart());
                    bhpstmt.setString(7, bizHourDto.breakEnd());
                    bhpstmt.addBatch();
                }

                bhpstmt.executeBatch();
                conn.commit();
                BIZ_HOUR_BATCH.get().clear();
                log.info("[JBE] 영업시간 커밋 완료 (쓰레드 {})", Thread.currentThread().getName());

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
