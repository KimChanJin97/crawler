package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.REVIEW_IMAGE_FILE_NAME;

import com.rouleatt.demo.dto.ReviewImageDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class ReviewImageWriter {

    public void write(Set<ReviewImageDto> reviewImageDtos) {

        File file = new File(REVIEW_IMAGE_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("review_id").append(DELIMITER)
                        .append("thumbnail_url");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (ReviewImageDto reviewImageDto : reviewImageDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(reviewImageDto.reviewFk()).append(DELIMITER)
                        .append(reviewImageDto.thumbnailUrl());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
