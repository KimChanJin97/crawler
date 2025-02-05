package com.rouleatt.demo.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class EnvLoader {
    private static final String ENV_FILE_PATH = ".env";

    private static Properties properties = new Properties();

    static {
        try (FileInputStream fis = new FileInputStream(ENV_FILE_PATH)) {
            properties.load(fis); // .env 파일 로드
        } catch (IOException e) {
            throw new RuntimeException("Failed to load .env file", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
