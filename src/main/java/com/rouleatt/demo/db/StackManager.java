package com.rouleatt.demo.db;

import com.rouleatt.demo.dto.RegionDto;
import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class StackManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final String REGION_SELECT_FIRST_SQL = "SELECT 1 FROM region WHERE id = 1 LIMIT 1";
    private static final String REGION_SELECT_ALL_ORDER_BY_ID_DESC_SQL = "SELECT full_name, short_name, min_x, min_y, max_x, max_y FROM region ORDER BY id DESC";
    private static final String REGION_INSERT_SQL = "INSERT INTO region (full_name, short_name, min_x, min_y, max_x, max_y) VALUES (?, ?, ?, ?, ?, ?)";

    public boolean hasFirstRegionObject() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(REGION_SELECT_FIRST_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<RegionDto> getAllRegionObjectsOrderByIdDesc() {
        List<RegionDto> regionDtos = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(REGION_SELECT_ALL_ORDER_BY_ID_DESC_SQL);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                regionDtos.add(new RegionDto(
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

    public void setRegionObject(RegionDto regionDto) {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(REGION_INSERT_SQL)) {
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

    public void setAllRegionObjects(Stack<RegionDto> stack) {
        while (!stack.isEmpty()) {
            setRegionObject(stack.pop());
        }
    }
}
