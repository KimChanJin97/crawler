package com.rouleatt.demo.batch;

import com.rouleatt.demo.utils.EnvLoader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class TableManager {

    private static final String JDBC_URL = EnvLoader.get("JDBC_URL");
    private static final String USERNAME = EnvLoader.get("USERNAME");
    private static final String PASSWORD = EnvLoader.get("PASSWORD");

    private static final String DROP_DATABASE_SQL = "DROP DATABASE IF EXISTS rouleatt";
    private static final String CREATE_DATABASE_SQL = "CREATE DATABASE IF NOT EXISTS rouleatt";

    private static final String DROP_RI_BACKUP_TABLE_SQL = "DROP TABLE IF EXISTS ri_backup";
    private static final String DROP_RESTAURANT_TABLE_SQL = "DROP TABLE IF EXISTS restaurant";
    private static final String DROP_RESTAURANT_IMAGE_TABLE_SQL = "DROP TABLE IF EXISTS restaurant_image";

    private static final String DROP_MR_BACKUP_TABLE_SQL = "DROP TABLE IF EXISTS mr_backup";
    private static final String DROP_MENU_TABLE_SQL = "DROP TABLE IF EXISTS menu";
    private static final String DROP_MENU_IMAGE_TABLE_SQL = "DROP TABLE IF EXISTS menu_image";
    private static final String DROP_REVIEW_TABLE_SQL = "DROP TABLE IF EXISTS review";
    private static final String DROP_REVIEW_IMAGE_TABLE_SQL = "DROP TABLE IF EXISTS review_image";
    private static final String DROP_BIZ_HOUR_TABLE_SQL = "DROP TABLE IF EXISTS biz_hour";

    private static final String CREATE_RI_BACKUP_TABLE_SQL = "CREATE TABLE IF NOT EXISTS ri_backup ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "full_name VARCHAR(10) NOT NULL, "
            + "short_name VARCHAR(10) NOT NULL, "
            + "min_x DOUBLE(13, 10) NOT NULL, "
            + "min_y DOUBLE(13, 10) NOT NULL, "
            + "max_x DOUBLE(13, 10) NOT NULL, "
            + "max_y DOUBLE(13, 10) NOT NULL "
            + ")";

    private static final String CREATE_RESTAURANT_TABLE_SQL = "CREATE TABLE IF NOT EXISTS restaurant ("
            + "id INT NOT NULL PRIMARY KEY, "
            + "rid VARCHAR(25) NOT NULL, "
            + "name VARCHAR(50) NOT NULL, "
            + "coordinate POINT NOT NULL SRID 4326, "
            + "category VARCHAR(50), "
            + "address VARCHAR(255), "
            + "road_address VARCHAR(255), "
            + "SPATIAL INDEX idx_location (coordinate)" // 공간 인덱스
            + ")";

    private static final String CREATE_RESTAURANT_IMAGE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS restaurant_image ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "restaurant_id INT NOT NULL, "
            + "url TEXT, "
            + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
            + ")";

    private static final String CREATE_MR_BACKUP_TABLE_SQL = "CREATE TABLE IF NOT EXISTS mr_backup ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "rpk INT NOT NULL, "
            + "rid VARCHAR(10) NOT NULL "
            + ")";

    private static final String CREATE_MENU_TABLE_SQL = "CREATE TABLE IF NOT EXISTS menu ("
            + "id INT NOT NULL PRIMARY KEY, "
            + "restaurant_id INT NOT NULL, "
            + "name VARCHAR(255), "
            + "price VARCHAR(50), "
            + "is_recommended BOOLEAN, "
            + "description VARCHAR(255), "
            + "menu_idx INT, "
            + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
            + ")";

    private static final String CREATE_MENU_IMAGE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS menu_image ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "menu_id INT NOT NULL, "
            + "image_url TEXT, "
            + "FOREIGN KEY (menu_id) REFERENCES menu(id) ON DELETE CASCADE"
            + ")";

    private static final String CREATE_REVIEW_TABLE_SQL = "CREATE TABLE IF NOT EXISTS review ("
            + "id INT NOT NULL PRIMARY KEY, "
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
            + ")";

    private static final String CREATE_REVIEW_IMAGE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS review_image ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "review_id INT NOT NULL, "
            + "thumbnail_url TEXT, "
            + "FOREIGN KEY (review_id) REFERENCES review(id) ON DELETE CASCADE"
            + ")";

    private static final String CREATE_BIZ_HOUR_TABLE_SQL = "CREATE TABLE IF NOT EXISTS biz_hour ("
            + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, "
            + "restaurant_id INT NOT NULL, "
            + "day VARCHAR(10), "
            + "biz_start VARCHAR(10), "
            + "break_start VARCHAR(10), "
            + "break_end VARCHAR(10), "
            + "last_order VARCHAR(10), "
            + "biz_end VARCHAR(10), "
            + "FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE"
            + ")";

    public void dropAndCreateAllTables() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute(DROP_DATABASE_SQL);
            stmt.execute(CREATE_DATABASE_SQL);

            stmt.execute(DROP_BIZ_HOUR_TABLE_SQL);
            stmt.execute(DROP_REVIEW_IMAGE_TABLE_SQL);
            stmt.execute(DROP_REVIEW_TABLE_SQL);
            stmt.execute(DROP_MENU_IMAGE_TABLE_SQL);
            stmt.execute(DROP_MENU_TABLE_SQL);
            stmt.execute(DROP_MR_BACKUP_TABLE_SQL);

            stmt.execute(DROP_RESTAURANT_IMAGE_TABLE_SQL);
            stmt.execute(DROP_RESTAURANT_TABLE_SQL);
            stmt.execute(DROP_RI_BACKUP_TABLE_SQL);

            stmt.execute(CREATE_RI_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_RESTAURANT_TABLE_SQL);
            stmt.execute(CREATE_RESTAURANT_IMAGE_TABLE_SQL);
            stmt.execute(CREATE_MR_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_MENU_TABLE_SQL);
            stmt.execute(CREATE_MENU_IMAGE_TABLE_SQL);
            stmt.execute(CREATE_REVIEW_TABLE_SQL);
            stmt.execute(CREATE_REVIEW_IMAGE_TABLE_SQL);
            stmt.execute(CREATE_BIZ_HOUR_TABLE_SQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dropAndCreateMenuAndReviewTable() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.execute(DROP_BIZ_HOUR_TABLE_SQL);
            stmt.execute(DROP_REVIEW_IMAGE_TABLE_SQL);
            stmt.execute(DROP_REVIEW_TABLE_SQL);
            stmt.execute(DROP_MENU_IMAGE_TABLE_SQL);
            stmt.execute(DROP_MENU_TABLE_SQL);
            stmt.execute(DROP_MR_BACKUP_TABLE_SQL);

            stmt.execute(CREATE_MR_BACKUP_TABLE_SQL);
            stmt.execute(CREATE_MENU_TABLE_SQL);
            stmt.execute(CREATE_MENU_IMAGE_TABLE_SQL);
            stmt.execute(CREATE_REVIEW_TABLE_SQL);
            stmt.execute(CREATE_REVIEW_IMAGE_TABLE_SQL);
            stmt.execute(CREATE_BIZ_HOUR_TABLE_SQL);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
