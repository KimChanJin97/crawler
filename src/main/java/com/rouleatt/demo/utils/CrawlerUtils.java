package com.rouleatt.demo.utils;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerUtils {

    public static final String USER_AGENT_KEY = EnvLoader.get("USER_AGENT_KEY");
    private static final String[] USER_AGENT_VALUES = {
            EnvLoader.get("USER_AGENT_WINDOW_VALUE"),
            EnvLoader.get("USER_AGENT_MACINTOSH_VALUE"),
            EnvLoader.get("USER_AGENT_UBUNTU_VALUE")
    };

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

    public static String getUserAgentValue() {
        return USER_AGENT_VALUES[new Random().nextInt(USER_AGENT_VALUES.length)];
    }
}
