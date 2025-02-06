package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.IMAGE_FILE_NAME;
import static com.rouleatt.demo.utils.CrawlerUtils.RESTAURANT_FILE_NAME;

import com.rouleatt.demo.dto.ImageDto;
import com.rouleatt.demo.dto.RestaurantDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class RestaurantImageWriter {

    public void writerRestaurant(Set<RestaurantDto> restaurantDtos) {

        File file = new File(RESTAURANT_FILE_NAME);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("id").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("x").append(DELIMITER)
                        .append("y").append(DELIMITER)
                        .append("category").append(DELIMITER)
                        .append("address").append(DELIMITER)
                        .append("road_address");
                bw.write(sb.toString());
                bw.newLine();
            }
            // CSV 칼럼 작성
            for (RestaurantDto restaurantDto : restaurantDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(restaurantDto.id()).append(DELIMITER)
                        .append(restaurantDto.name()).append(DELIMITER)
                        .append(restaurantDto.x()).append(DELIMITER)
                        .append(restaurantDto.y()).append(DELIMITER)
                        .append(restaurantDto.category()).append(DELIMITER)
                        .append(restaurantDto.address()).append(DELIMITER)
                        .append(restaurantDto.roadAddress());
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeImage(Set<ImageDto> imageDtos) {

        File file = new File(IMAGE_FILE_NAME);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("url");
                bw.write(sb.toString());
                bw.newLine();
            }
            // CSV 칼럼 작성
            for (ImageDto imageDto : imageDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(imageDto.restaurantId()).append(DELIMITER)
                        .append(imageDto.url());
                bw.write(sb.toString());
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
