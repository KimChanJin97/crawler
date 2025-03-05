package com.rouleatt.demo.backup;

import com.rouleatt.demo.dto.RestaurantImageBackupDto;
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

public class RestaurantImageBackupManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final String SELECT_FIRST_REGION_BACKUP_SQL = "SELECT 1 FROM ri_backup WHERE id = 1 LIMIT 1";
    private static final String SELECT_ALL_REGION_BACKUPS_ORDER_BY_ID_DESC_SQL = "SELECT full_name, short_name, min_x, min_y, max_x, max_y FROM ri_backup ORDER BY id DESC";
    private static final String INSERT_REGION_BACKUP_SQL = "INSERT INTO ri_backup (full_name, short_name, min_x, min_y, max_x, max_y) VALUES (?, ?, ?, ?, ?, ?)";
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
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FIRST_REGION_BACKUP_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<RestaurantImageBackupDto> getAllRiBackupsOrderByIdDesc() {
        List<RestaurantImageBackupDto> backupDtos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_REGION_BACKUPS_ORDER_BY_ID_DESC_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                backupDtos.add(RestaurantImageBackupDto.of(
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

    public void setRiBackup(RestaurantImageBackupDto backupDto) {
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

    public void setAllRiBackups(Stack<RestaurantImageBackupDto> stack) {
        while (!stack.isEmpty()) {
            setRiBackup(stack.pop());
        }
    }

    public void dropAndCreateRiBackupTable() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute(DROP_REGION_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_REGION_BACKUP_TABLE_SQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
