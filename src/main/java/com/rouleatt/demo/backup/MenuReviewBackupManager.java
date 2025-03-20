package com.rouleatt.demo.backup;

import com.rouleatt.demo.dto.MenuReviewBackupDto;
import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MenuReviewBackupManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    // 메뉴&리뷰 백업 데이터 존재 여부 판단을 위한 SQL
    private static final String SELECT_FIRST_MR_BACKUP_SQL = "SELECT 1 FROM mr_backup WHERE id = 1 LIMIT 1";
    // 메뉴&리뷰 백업 데이터 존재하지 않을 경우 모든 음식점을 조회하기 위한 SQL (이후 스택 삽입)
    private static final String SELECT_RESTAURANT_PK_ID_SQL = "SELECT id, rid FROM restaurant";
    // 메뉴&리뷰 백업 데이터 존재할 경우 백업 데이터를 조회하기 위한 SQL (이후 스택 삽입)
    private static final String SELECT_ALL_MR_BACKUPS_SQL = "SELECT rpk, rid FROM mr_backup ORDER BY id DESC";
    // 메뉴&리뷰 백업 데이터를 저장하기 위한 SQL
    private static final String INSERT_MR_BACKUP_SQL = "INSERT INTO mr_backup (rpk, rid) VALUES (?, ?)";
    // 메뉴&리뷰 백업 데이터를 초기화하기 위한 SQL
    private static final String DROP_MR_BACKUP_TABLE_SQL = "DROP TABLE IF EXISTS mr_backup";
    private static final String CREATE_MR_BACKUP_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS mr_backup ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "rid INT NOT NULL "
            + ")";

    public List<MenuReviewBackupDto> getAllRestaurantPkId() {
        List<MenuReviewBackupDto> backupDtos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_RESTAURANT_PK_ID_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                backupDtos.add(MenuReviewBackupDto.of(
                        rs.getInt("id"),
                        rs.getString("rid")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return backupDtos;
    }

    public boolean hasFirstMrBackup() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FIRST_MR_BACKUP_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<MenuReviewBackupDto> getAllMrBackupsOrderByIdDesc() {
        List<MenuReviewBackupDto> restaurantIds = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_MR_BACKUPS_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                restaurantIds.add(MenuReviewBackupDto.of(
                        rs.getInt("rpk"),
                        rs.getString("rid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return restaurantIds;
    }

    public void setMrBackup(MenuReviewBackupDto backupDto) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(INSERT_MR_BACKUP_SQL)) {
            stmt.setInt(1, backupDto.restaurantPk());
            stmt.setString(2, backupDto.restaurantId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setAllMrBackups(Stack<MenuReviewBackupDto> stack) {
        while (!stack.isEmpty()) {
            setMrBackup(stack.pop());
        }
    }

    public void dropAndCreateMrBackupTable() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(DROP_MR_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_MR_BACKUP_TABLE_SQL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
