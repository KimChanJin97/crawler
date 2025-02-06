package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.MENU_FILE_NAME;
import static com.rouleatt.demo.utils.CrawlerUtils.REVIEW_FILE_NAME;

import com.rouleatt.demo.dto.MenuDto;
import com.rouleatt.demo.dto.ReviewDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MenuReviewWriter {

    public void writeMenu(List<MenuDto> menuDtos) {

        File file = new File(MENU_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("menu_index").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("is_recommended").append(DELIMITER)
                        .append("price").append(DELIMITER)
                        .append("description").append(DELIMITER)
                        .append("image");
                writer.write(sb.toString());
                writer.newLine();
            }
            // CSV 칼럼 작성
            for (MenuDto menuDto : menuDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(menuDto.restaurantId()).append(DELIMITER)
                        .append(menuDto.menuIndex()).append(DELIMITER)
                        .append(menuDto.name()).append(DELIMITER)
                        .append(menuDto.isRecommended()).append(DELIMITER)
                        .append(menuDto.price()).append(DELIMITER)
                        .append(menuDto.description()).append(DELIMITER)
                        .append(menuDto.image());
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeReview(List<ReviewDto> reviewDtos) {

        File file = new File(REVIEW_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("reviewer_name").append(DELIMITER)
                        .append("type_name").append(DELIMITER)
                        .append("url").append(DELIMITER)
                        .append("thumbnail_url").append(DELIMITER)
                        .append("title").append(DELIMITER)
                        .append("review_index").append(DELIMITER)
                        .append("content").append(DELIMITER)
                        .append("created_at");
                writer.write(sb.toString());
                writer.newLine();
            }
            // CSV 칼럼 작성
            for (ReviewDto reviewDto : reviewDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(reviewDto.restaurantId()).append(DELIMITER)
                        .append(reviewDto.reviewerName()).append(DELIMITER)
                        .append(reviewDto.typeName()).append(DELIMITER)
                        .append(reviewDto.url()).append(DELIMITER)
                        .append(reviewDto.thumbnailUrl()).append(DELIMITER)
                        .append(reviewDto.title()).append(DELIMITER)
                        .append(reviewDto.reviewIndex()).append(DELIMITER)
                        .append(reviewDto.content().substring(0, 100)).append(DELIMITER)
                        .append(reviewDto.createdAt());
                writer.write(sb.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
