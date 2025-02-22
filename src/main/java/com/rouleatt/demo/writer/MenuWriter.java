package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;
import static com.rouleatt.demo.utils.CrawlerUtils.MENU_FILE_NAME;

import com.rouleatt.demo.dto.MenuDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class MenuWriter {

    public void write(Set<MenuDto> menuDtos) {

        File file = new File(MENU_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("menu_id").append(DELIMITER)
                        .append("restaurant_id").append(DELIMITER)
                        .append("name").append(DELIMITER)
                        .append("price").append(DELIMITER)
                        .append("is_recommend").append(DELIMITER)
                        .append("description").append(DELIMITER)
                        .append("menu_index");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (MenuDto menuDto : menuDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(menuDto.menuPk()).append(DELIMITER)
                        .append(menuDto.restaurantFk()).append(DELIMITER)
                        .append(menuDto.name()).append(DELIMITER)
                        .append(menuDto.price()).append(DELIMITER)
                        .append(menuDto.isRecommended()).append(DELIMITER)
                        .append(menuDto.description()).append(DELIMITER)
                        .append(menuDto.menuIdx());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
