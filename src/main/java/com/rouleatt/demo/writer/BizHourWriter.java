package com.rouleatt.demo.writer;

import static com.rouleatt.demo.utils.CrawlerUtils.BIZ_HOUR_FILE_NAME;
import static com.rouleatt.demo.utils.CrawlerUtils.DELIMITER;

import com.rouleatt.demo.dto.BizHourDto;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class BizHourWriter {

    public void write(Set<BizHourDto> bizHourDtos) {

        File file = new File(BIZ_HOUR_FILE_NAME);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (file.length() == 0) {
                // CSV 헤더 작성
                StringBuilder sb = new StringBuilder()
                        .append("restaurant_id").append(DELIMITER)
                        .append("day").append(DELIMITER)
                        .append("biz_start").append(DELIMITER)
                        .append("biz_end").append(DELIMITER)
                        .append("last_order").append(DELIMITER)
                        .append("break_start").append(DELIMITER)
                        .append("break_end");
                writer.write(sb.toString());
                writer.newLine();
            }

            // CSV 칼럼 작성
            for (BizHourDto bizHourDto : bizHourDtos) {
                StringBuilder sb = new StringBuilder()
                        .append(bizHourDto.restaurantFk()).append(DELIMITER)
                        .append(bizHourDto.day()).append(DELIMITER)
                        .append(bizHourDto.bizStart()).append(DELIMITER)
                        .append(bizHourDto.bizEnd()).append(DELIMITER)
                        .append(bizHourDto.lastOrder()).append(DELIMITER)
                        .append(bizHourDto.breakStart()).append(DELIMITER)
                        .append(bizHourDto.breakEnd());
                writer.write(sb.toString());
                writer.newLine();
            }

            writer.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
