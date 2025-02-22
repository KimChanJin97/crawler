package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.RESTAURANT_FILE_NAME;

import com.rouleatt.demo.dto.RestaurantDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class RestaurantWriter {

    public void write(Set<RestaurantDto> restaurantDtos) {

        File file = new File(RESTAURANT_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("x").append(DELIMITER)
                        .append("y").append(DELIMITER)
                        .append("category").append(DELIMITER)
                        .append("address").append(DELIMITER)
                        .append("road_address");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (RestaurantDto restaurantDto : restaurantDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(restaurantDto.restaurantPk()).append(DELIMITER)
                        .append(restaurantDto.name()).append(DELIMITER)
                        .append(restaurantDto.x()).append(DELIMITER)
                        .append(restaurantDto.y()).append(DELIMITER)
                        .append(restaurantDto.category()).append(DELIMITER)
                        .append(restaurantDto.address()).append(DELIMITER)
                        .append(restaurantDto.roadAddress());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
