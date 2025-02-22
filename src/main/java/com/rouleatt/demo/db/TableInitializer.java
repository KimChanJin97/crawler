package com.rouleatt.demo.db;

import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TableInitializer {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    public void init() {

        String[] dropAndCreateDatabaseSqls = {
                "DROP DATABASE IF EXISTS rouleatt;",
                "CREATE DATABASE rouleatt;",
                "USE rouleatt;"
        };

        String createRestaurantTableSql = "CREATE TABLE IF NOT EXISTS restaurant ("
                + "id INT NOT NULL PRIMARY KEY, " // 배치를 위해 AUTO_INCREMENT 제거
                + "name VARCHAR(50) NOT NULL, "
                + "coordinate POINT NOT NULL SRID 4326, "
                + "category VARCHAR(50), "
                + "address VARCHAR(255), "
                + "road_address VARCHAR(255), "
                + "SPATIAL INDEX idx_location (coordinate)"
                + ");";

        String createRestaurantImageTableSql = "CREATE TABLE IF NOT EXISTS restaurant_image ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "restaurant_id INT NOT NULL, "
                + "url TEXT, "
                + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
                + ");";

        String createMenuTableSql = "CREATE TABLE IF NOT EXISTS menu ("
                + "id INT NOT NULL PRIMARY KEY, " // 배치를 위해 AUTO_INCREMENT 제거
                + "restaurant_id INT NOT NULL, "
                + "name VARCHAR(255), "
                + "price VARCHAR(50), "
                + "is_recommended BOOLEAN, "
                + "description VARCHAR(255), "
                + "menu_idx INT, "
                + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
                + ");";

        String createMenuImageTableSql = "CREATE TABLE IF NOT EXISTS menu_image ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "menu_id INT NOT NULL, "
                + "image_url TEXT, "
                + "FOREIGN KEY (menu_id) REFERENCES menu(id) ON DELETE CASCADE"
                + ");";

        String createReviewTableSql = "CREATE TABLE IF NOT EXISTS review ("
                + "id INT NOT NULL PRIMARY KEY, " // 배치를 위해 AUTO_INCREMENT 제거
                + "restaurant_id INT NOT NULL, "
                + "name VARCHAR(100), "
                + "type VARCHAR(10), "
                + "url TEXT, "
                + "title TEXT, "
                + "review_idx VARCHAR(10), "
                + "content TEXT, "
                + "profile_url TEXT, "
                + "author_name VARCHAR(50), "
                + "created_at VARCHAR(25), "
                + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
                + ");";

        String reviewImageTableSql = "CREATE TABLE IF NOT EXISTS review_image ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "review_id INT NOT NULL, "
                + "thumbnail_url TEXT, "
                + "FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE"
                + ");";

        String createBusinessHourTableSql = "CREATE TABLE IF NOT EXISTS biz_hour ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
                + "restaurant_id INT NOT NULL, "
                + "day VARCHAR(10), "
                + "biz_start VARCHAR(10), "
                + "biz_end VARCHAR(10), "
                + "last_order VARCHAR(10), "
                + "break_start VARCHAR(10), "
                + "break_end VARCHAR(10), "
                + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
                + ");";

        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            for (String dropAndCreateDatabaseSql : dropAndCreateDatabaseSqls) {
                stmt.execute(dropAndCreateDatabaseSql);
            }

            stmt.execute(createRestaurantTableSql);
            stmt.execute(createRestaurantImageTableSql);

            stmt.execute(createMenuTableSql);
            stmt.execute(createMenuImageTableSql);

            stmt.execute(createReviewTableSql);
            stmt.execute(reviewImageTableSql);

            stmt.execute(createBusinessHourTableSql);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
