package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.MENU_IMAGE_FILE_NAME;

import com.rouleatt.demo.dto.MenuImageDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class MenuImageWriter {

    public void write(Set<MenuImageDto> menuImageDtos) {

        File file = new File(MENU_IMAGE_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("menu_id").append(DELIMITER)
                        .append("image_url");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (MenuImageDto menuImageDto : menuImageDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(menuImageDto.menuFk()).append(DELIMITER)
                        .append(menuImageDto.imageUrl());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
