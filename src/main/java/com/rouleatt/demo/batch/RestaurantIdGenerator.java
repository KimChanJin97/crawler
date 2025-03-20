package com.rouleatt.demo.batch;

import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantIdGenerator {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final String SELECT_RESTAURANT_LAST_ID = "SELECT MAX(id) AS last_id FROM restaurant";

    private static final AtomicInteger ID = new AtomicInteger(getRestaurantLastId() + 1);

    public static int getNextId() {
        return ID.getAndIncrement();
    }

    private static int getRestaurantLastId() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(SELECT_RESTAURANT_LAST_ID);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("last_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
