package com.rouleatt.demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerUtils {

    // delimiter
    public static final String DELIMITER = "DELIMITER";
    // restaurant
    public static final String RESTAURANT_FILE_NAME = "restaurant.csv";
    public static final String RESTAURANT_IMAGE_FILE_NAME = "restaurant_image.csv";
    // menu, review, bizHour
    public static final String MENU_FILE_NAME = "menu.csv";
    public static final String MENU_IMAGE_FILE_NAME = "menu_image.csv";
    public static final String REVIEW_FILE_NAME = "review.csv";
    public static final String REVIEW_IMAGE_FILE_NAME = "review_image.csv";
    public static final String BIZ_HOUR_FILE_NAME = "biz_hour.csv";

    public static String decodeUnicode(String input) {
        Pattern pattern = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");
        Matcher matcher = pattern.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            int unicodeValue = Integer.parseInt(matcher.group(1), 16); // 16진수
            matcher.appendReplacement(buffer, Character.toString((char) unicodeValue));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
