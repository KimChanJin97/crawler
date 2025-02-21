package com.rouleatt.demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerUtils {

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
