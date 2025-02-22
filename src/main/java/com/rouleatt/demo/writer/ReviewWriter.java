package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.REVIEW_FILE_NAME;

import com.rouleatt.demo.dto.ReviewDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class ReviewWriter {

    public void write(Set<ReviewDto> reviewDtos) {

        File file = new File(REVIEW_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("review_id").append(DELIMITER)
                        .append("restaurant_id").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("type").append(DELIMITER)
                        .append("url").append(DELIMITER)
                        .append("title").append(DELIMITER)
                        .append("review_index").append(DELIMITER)
                        .append("content").append(DELIMITER)
                        .append("profile_url").append(DELIMITER)
                        .append("author_name").append(DELIMITER)
                        .append("created_at");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (ReviewDto reviewDto : reviewDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(reviewDto.reviewPk()).append(DELIMITER)
                        .append(reviewDto.restaurantFk()).append(DELIMITER)
                        .append(reviewDto.name()).append(DELIMITER)
                        .append(reviewDto.type()).append(DELIMITER)
                        .append(reviewDto.url()).append(DELIMITER)
                        .append(reviewDto.title()).append(DELIMITER)
                        .append(reviewDto.reviewIdx()).append(DELIMITER)
                        .append(reviewDto.content()).append(DELIMITER)
                        .append(reviewDto.profileUrl()).append(DELIMITER)
                        .append(reviewDto.authorName()).append(DELIMITER)
                        .append(reviewDto.createdAt());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}