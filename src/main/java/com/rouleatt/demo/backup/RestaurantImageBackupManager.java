package com.rouleatt.demo.db;

import com.rouleatt.demo.dto.RegionDto;
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

public class RegionBackupManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final String SELECT_FIRST_REGION_BACKUP_SQL = "SELECT 1 FROM region_backup WHERE id = 1 LIMIT 1";
    private static final String SELECT_ALL_REGION_BACKUPS_ORDER_BY_ID_DESC_SQL = "SELECT full_name, short_name, min_x, min_y, max_x, max_y FROM region_backup ORDER BY id DESC";
    private static final String INSERT_REGION_BACKUP_SQL = "INSERT INTO region_backup (full_name, short_name, min_x, min_y, max_x, max_y) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String DROP_REGION_BACKUP_TABLE_SQL = "DROP TABLE IF EXISTS region_backup;";
    private static final String CREATE_REGION_BACKUP_TABLE_SQL = "CREATE TABLE IF NOT EXISTS region_backup ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "full_name VARCHAR(10) NOT NULL, "
            + "short_name VARCHAR(10) NOT NULL, "
            + "min_x DOUBLE(13, 10) NOT NULL, "
            + "min_y DOUBLE(13, 10) NOT NULL, "
            + "max_x DOUBLE(13, 10) NOT NULL, "
            + "max_y DOUBLE(13, 10) NOT NULL "
            + ")";

    public boolean hasFirstRegionBackup() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_FIRST_REGION_BACKUP_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<RegionDto> getAllRegionBackupsOrderByIdDesc() {
        List<RegionDto> regionDtos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_REGION_BACKUPS_ORDER_BY_ID_DESC_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                regionDtos.add(RegionDto.of(
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
        return regionDtos;
    }

    public void setRegionBackup(RegionDto regionDto) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(INSERT_REGION_BACKUP_SQL)) {
            stmt.setString(1, regionDto.fullName());
            stmt.setString(2, regionDto.shortName());
            stmt.setDouble(3, regionDto.minX());
            stmt.setDouble(4, regionDto.minY());
            stmt.setDouble(5, regionDto.maxX());
            stmt.setDouble(6, regionDto.maxY());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setAllRegionBackups(Stack<RegionDto> stack) {
        while (!stack.isEmpty()) {
            setRegionBackup(stack.pop());
        }
    }

    public void dropAndCreateRegionBackupTable() {

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute(DROP_REGION_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_REGION_BACKUP_TABLE_SQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
