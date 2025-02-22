package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.RESTAURANT_IMAGE_FILE_NAME;

import com.rouleatt.demo.dto.RestaurantImageDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class RestaurantImageWriter {

    public void write(Set<RestaurantImageDto> restaurantImageDtos) {

        File file = new File(RESTAURANT_IMAGE_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("url");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (RestaurantImageDto restaurantImageDto : restaurantImageDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(restaurantImageDto.restaurantFk()).append(DELIMITER)
                        .append(restaurantImageDto.url());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}