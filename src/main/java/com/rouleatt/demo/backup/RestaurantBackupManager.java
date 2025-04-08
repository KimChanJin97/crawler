package com.rouleatt.demo.backup;

import com.rouleatt.demo.dto.RestaurantBackupDto;
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

public class RestaurantBackupManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    // 영역 백업 데이터 존재 여부 판단을 위한 SQL
    private static final String SELECT_FIRST_RI_BACKUP_SQL = "SELECT 1 FROM ri_backup WHERE id = 1 LIMIT 1";
    // 영역 백업 데이터 존재할 경우 모든 영역 백업 데이터를 조회하기 위한 SQL (이후 스택 삽입)
    private static final String SELECT_ALL_RESTAURANT_BACKUPS_SQL = "SELECT full_name, short_name, min_x, min_y, max_x, max_y FROM ri_backup ORDER BY id DESC";
    // 영역 백업 데이터를 저장하기 위한 SQL
    private static final String INSERT_REGION_BACKUP_SQL = "INSERT INTO ri_backup (full_name, short_name, min_x, min_y, max_x, max_y) VALUES (?, ?, ?, ?, ?, ?)";
    // 영역 백업 데이터를 초기화하기 위한 SQL
    private static final String DROP_REGION_BACKUP_TABLE_SQL = "DROP TABLE IF EXISTS ri_backup;";
    private static final String CREATE_REGION_BACKUP_TABLE_SQL = "CREATE TABLE IF NOT EXISTS ri_backup ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "full_name VARCHAR(10) NOT NULL, "
            + "short_name VARCHAR(10) NOT NULL, "
            + "min_x DOUBLE(13, 10) NOT NULL, "
            + "min_y DOUBLE(13, 10) NOT NULL, "
            + "max_x DOUBLE(13, 10) NOT NULL, "
            + "max_y DOUBLE(13, 10) NOT NULL "
            + ")";

    public boolean hasFirstRiBackup() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FIRST_RI_BACKUP_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<RestaurantBackupDto> getAllRestaurantBackupsOrderByIdDesc() {
        List<RestaurantBackupDto> backupDtos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_RESTAURANT_BACKUPS_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                backupDtos.add(RestaurantBackupDto.of(
                        rs.getString("full_name"),
                        rs.getString("short_name"),
                        rs.getDouble("min_x"),
                        rs.getDouble("min_y"),
                        rs.getDouble("max_x"),
                        rs.getDouble("max_y")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return backupDtos;
    }

    public void setRestaurantBackup(RestaurantBackupDto backupDto) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(INSERT_REGION_BACKUP_SQL)) {
            stmt.setString(1, backupDto.fullName());
            stmt.setString(2, backupDto.shortName());
            stmt.setDouble(3, backupDto.minX());
            stmt.setDouble(4, backupDto.minY());
            stmt.setDouble(5, backupDto.maxX());
            stmt.setDouble(6, backupDto.maxY());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setAllRestaurantBackups(Stack<RestaurantBackupDto> stack) {
        while (!stack.isEmpty()) {
            setRestaurantBackup(stack.pop());
        }
    }

    public void dropAndCreateRestaurantBackupTable() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute(DROP_REGION_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_REGION_BACKUP_TABLE_SQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
