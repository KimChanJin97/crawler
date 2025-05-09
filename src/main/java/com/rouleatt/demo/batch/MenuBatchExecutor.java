package com.rouleatt.demo.batch;

import com.rouleatt.demo.dto.BizHourDto;
import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.MenuImageDto;
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
public class MenuBatchExecutor {

    private static int count = 0;

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final List<MenuDto> MENU_BATCH = new ArrayList<>();
    private static final List<MenuImageDto> MENU_IMAGE_BATCH = new ArrayList<>();
    private static final List<ReviewDto> REVIEW_BATCH = new ArrayList<>();
    private static final List<ReviewImageDto> REVIEW_IMAGE_BATCH = new ArrayList<>();
    private static final List<BizHourDto> BIZ_HOUR_BATCH = new ArrayList<>();

    private static final String INSERT_MENU_MANUAL_INCREMENT_SQL = "INSERT INTO menu (id, restaurant_id, name, price, is_recommended, description, menu_idx) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_MENU_IMAGE_AUTO_INCREMENT_SQL = "INSERT INTO menu_image (menu_id, image_url) VALUES (?, ?)";
    private static final String INSERT_REVIEW_MANUAL_INCREMENT_SQL = "INSERT INTO review (id, restaurant_id, name, type, url, title, review_idx, content, profile_url, author_name, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_REVIEW_IMAGE_AUTO_INCREMENT_SQL = "INSERT INTO review_image (review_id, thumbnail_url) VALUES (?, ?)";
    private static final String INSERT_BIZ_HOUR_AUTO_INCREMENT_SQL = "INSERT INTO biz_hour (restaurant_id, day, biz_start, biz_end, last_order, break_start, break_end) VALUES (?, ?, ?, ?, ?, ?, ?)";

    public void addMenu(
            int menuPk,
            int restaurantFk,
            String name,
            String price,
            boolean isRecommended,
            String description,
            int menuIdx
    ) {
        MENU_BATCH.add(MenuDto.of(menuPk, restaurantFk, name, price, isRecommended, description, menuIdx));
    }

    public void addMenuImage(
            int menuFk,
            String imageUrl
    ) {
        MENU_IMAGE_BATCH.add(MenuImageDto.of(menuFk, imageUrl));
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
        REVIEW_BATCH.add(ReviewDto.of(
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
        REVIEW_IMAGE_BATCH.add(ReviewImageDto.of(reviewFk, thumbnailUrl));
    }

    public void addBizHour(
            int restaurantFk,
            String day,
            String bizStart,
            String breakStart,
            String breakEnd,
            String lastOrder,
            String bizEnd
    ) {
        BIZ_HOUR_BATCH.add(BizHourDto.of(restaurantFk, day, bizStart, breakStart, breakEnd, lastOrder, bizEnd));
    }

    public void batchInsert() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD)) {

            conn.setAutoCommit(false);

            try (PreparedStatement mpstmt = conn.prepareStatement(INSERT_MENU_MANUAL_INCREMENT_SQL);
                 PreparedStatement mipstmt = conn.prepareStatement(INSERT_MENU_IMAGE_AUTO_INCREMENT_SQL);
                 PreparedStatement rvpstmt = conn.prepareStatement(INSERT_REVIEW_MANUAL_INCREMENT_SQL);
                 PreparedStatement rvipstmt = conn.prepareStatement(INSERT_REVIEW_IMAGE_AUTO_INCREMENT_SQL);
                 PreparedStatement bhpstmt = conn.prepareStatement(INSERT_BIZ_HOUR_AUTO_INCREMENT_SQL)) {

                for (MenuDto menuDto : MENU_BATCH) {
                    mpstmt.setInt(1, menuDto.menuPk());
                    mpstmt.setInt(2, menuDto.restaurantFk());
                    mpstmt.setString(3, menuDto.name());
                    mpstmt.setString(4, menuDto.price());
                    mpstmt.setBoolean(5, menuDto.isRecommended());
                    mpstmt.setString(6, menuDto.description());
                    mpstmt.setInt(7, menuDto.menuIdx());
                    mpstmt.addBatch();
                }

                for (MenuImageDto menuImageDto : MENU_IMAGE_BATCH) {
                    mipstmt.setInt(1, menuImageDto.menuFk());
                    mipstmt.setString(2, menuImageDto.imageUrl());
                    mipstmt.addBatch();
                }

                for (ReviewDto reviewDto : REVIEW_BATCH) {
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

                for (ReviewImageDto reviewImageDto : REVIEW_IMAGE_BATCH) {
                    rvipstmt.setInt(1, reviewImageDto.reviewFk());
                    rvipstmt.setString(2, reviewImageDto.thumbnailUrl());
                    rvipstmt.addBatch();
                }

                for (BizHourDto bizHourDto : BIZ_HOUR_BATCH) {
                    bhpstmt.setInt(1, bizHourDto.restaurantFk());
                    bhpstmt.setString(2, bizHourDto.day());
                    bhpstmt.setString(3, bizHourDto.bizStart());
                    bhpstmt.setString(4, bizHourDto.bizEnd());
                    bhpstmt.setString(5, bizHourDto.lastOrder());
                    bhpstmt.setString(6, bizHourDto.breakStart());
                    bhpstmt.setString(7, bizHourDto.breakEnd());
                    bhpstmt.addBatch();
                }

                // 배치 실행 (커밋X)
                mpstmt.executeBatch();
                mipstmt.executeBatch();
                rvpstmt.executeBatch();
                rvipstmt.executeBatch();
                bhpstmt.executeBatch();

                // 배치 실행이 끝나면 한번에 커밋
                conn.commit();
                log.info("[M] 배치 삽입 횟수 = {}", ++count);

                // 배치 후 리스트 비우기
                MENU_BATCH.clear();
                MENU_IMAGE_BATCH.clear();
                REVIEW_BATCH.clear();
                REVIEW_IMAGE_BATCH.clear();
                BIZ_HOUR_BATCH.clear();

            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
