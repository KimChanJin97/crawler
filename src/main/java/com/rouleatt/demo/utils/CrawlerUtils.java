package com.rouleatt.demo.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CrawlerUtils {

    // 음식점, 이미지 요청 헤더
    public static final String RI_URI = EnvLoader.get("RI_URI");
    public static final String RI_SEARCH_COORD_KEY = EnvLoader.get("RI_SEARCH_COORD_KEY");
    public static final String RI_BOUNDARY_KEY = EnvLoader.get("RI_BOUNDARY_KEY");
    public static final String RI_CODE_KEY = EnvLoader.get("RI_CODE_KEY");
    public static final String RI_CODE_VALUE = EnvLoader.get("RI_CODE_VALUE");
    public static final String RI_LIMIT_KEY = EnvLoader.get("RI_LIMIT_KEY");
    public static final String RI_LIMIT_VALUE = EnvLoader.get("RI_LIMIT_VALUE");
    public static final String RI_SORT_TYPE_KEY = EnvLoader.get("RI_SORT_TYPE_KEY");
    public static final String RI_SORT_TYPE_VALUE = EnvLoader.get("RI_SORT_TYPE_VALUE");
    public static final String RI_TIME_CODE_KEY = EnvLoader.get("RI_TIME_CODE_KEY");
    public static final String RI_AUTHORITY_KEY = EnvLoader.get("RI_AUTHORITY_KEY");
    public static final String RI_AUTHORITY_VALUE = EnvLoader.get("RI_AUTHORITY_VALUE");
    public static final String RI_METHOD_KEY = EnvLoader.get("RI_METHOD_KEY");
    public static final String RI_METHOD_VALUE = EnvLoader.get("RI_METHOD_VALUE");
    public static final String RI_PATH_KEY = EnvLoader.get("RI_PATH_KEY");
    public static final String RI_PATH_VALUE = EnvLoader.get("RI_PATH_VALUE");
    public static final String RI_SCHEME_KEY = EnvLoader.get("RI_SCHEME_KEY");
    public static final String RI_SCHEME_VALUE = EnvLoader.get("RI_SCHEME_VALUE");
    public static final String RI_ACCEPT_KEY = EnvLoader.get("RI_ACCEPT_KEY");
    public static final String RI_ACCEPT_VALUE = EnvLoader.get("RI_ACCEPT_VALUE");
    public static final String RI_ACCEPT_ENCODING_KEY = EnvLoader.get("RI_ACCEPT_ENCODING_KEY");
    public static final String RI_ACCEPT_ENCODING_VALUE = EnvLoader.get("RI_ACCEPT_ENCODING_VALUE");
    public static final String RI_ACCEPT_LANGUAGE_KEY = EnvLoader.get("RI_ACCEPT_LANGUAGE_KEY");
    public static final String RI_ACCEPT_LANGUAGE_VALUE = EnvLoader.get("RI_ACCEPT_LANGUAGE_VALUE");
    public static final String RI_CACHE_CONTROL_KEY = EnvLoader.get("RI_CACHE_CONTROL_KEY");
    public static final String RI_CACHE_CONTROL_VALUE = EnvLoader.get("RI_CACHE_CONTROL_VALUE");
    public static final String RI_EXPIRES_KEY = EnvLoader.get("RI_EXPIRES_KEY");
    public static final String RI_EXPIRES_VALUE = EnvLoader.get("RI_EXPIRES_VALUE");
    public static final String RI_PRAGMA_KEY = EnvLoader.get("RI_PRAGMA_KEY");
    public static final String RI_PRAGMA_VALUE = EnvLoader.get("RI_PRAGMA_VALUE");
    public static final String RI_PRIORITY_KEY = EnvLoader.get("RI_PRIORITY_KEY");
    public static final String RI_PRIORITY_VALUE = EnvLoader.get("RI_PRIORITY_VALUE");
    public static final String RI_REFERER_KEY = EnvLoader.get("RI_REFERER_KEY");
    public static final String RI_REFERER_VALUE = EnvLoader.get("RI_REFERER_VALUE");
    public static final String RI_SEC_CH_UA_KEY = EnvLoader.get("RI_SEC_CH_UA_KEY");
    public static final String RI_SEC_CH_UA_VALUE = EnvLoader.get("RI_SEC_CH_UA_VALUE");
    public static final String RI_SEC_CH_UA_MOBILE_KEY = EnvLoader.get("RI_SEC_CH_UA_MOBILE_KEY");
    public static final String RI_SEC_CH_UA_MOBILE_VALUE = EnvLoader.get("RI_SEC_CH_UA_MOBILE_VALUE");
    public static final String RI_SEC_CH_UA_PLATFORM_KEY = EnvLoader.get("RI_SEC_CH_UA_PLATFORM_KEY");
    public static final String RI_SEC_CH_UA_PLATFORM_VALUE = EnvLoader.get("RI_SEC_CH_UA_PLATFORM_VALUE");
    public static final String RI_SEC_FETCH_DEST_KEY = EnvLoader.get("RI_SEC_FETCH_DEST_KEY");
    public static final String RI_SEC_FETCH_DEST_VALUE = EnvLoader.get("RI_SEC_FETCH_DEST_VALUE");
    public static final String RI_SEC_FETCH_MODE_KEY = EnvLoader.get("RI_SEC_FETCH_MODE_KEY");
    public static final String RI_SEC_FETCH_MODE_VALUE = EnvLoader.get("RI_SEC_FETCH_MODE_VALUE");
    public static final String RI_SEC_FETCH_SITE_KEY = EnvLoader.get("RI_SEC_FETCH_SITE_KEY");
    public static final String RI_SEC_FETCH_SITE_VALUE = EnvLoader.get("RI_SEC_FETCH_SITE_VALUE");
    public static final String RI_USER_AGENT_KEY = EnvLoader.get("RI_USER_AGENT_KEY");
    public static final String RI_USER_AGENT_VALUE = EnvLoader.get("RI_USER_AGENT_VALUE");
    // 메뉴, 리뷰, 영업시간 헤더
    public static final String MR_URI = EnvLoader.get("MR_URI");
    public static final String MR_AUTHORITY_KEY = EnvLoader.get("MR_AUTHORITY_KEY");
    public static final String MR_AUTHORITY_VALUE = EnvLoader.get("MR_AUTHORITY_VALUE");
    public static final String MR_METHOD_KEY = EnvLoader.get("MR_METHOD_KEY");
    public static final String MR_METHOD_VALUE = EnvLoader.get("MR_METHOD_VALUE");
    public static final String MR_PATH_KEY = EnvLoader.get("MR_PATH_KEY");
    public static final String MR_SCHEME_KEY = EnvLoader.get("MR_SCHEME_KEY");
    public static final String MR_SCHEME_VALUE = EnvLoader.get("MR_SCHEME_VALUE");
    public static final String MR_ACCEPT_KEY = EnvLoader.get("MR_ACCEPT_KEY");
    public static final String MR_ACCEPT_VALUE = EnvLoader.get("MR_ACCEPT_VALUE");
    public static final String MR_ACCEPT_ENCODING_KEY = EnvLoader.get("MR_ACCEPT_ENCODING_KEY");
    public static final String MR_ACCEPT_ENCODING_VALUE = EnvLoader.get("MR_ACCEPT_ENCODING_VALUE");
    public static final String MR_ACCEPT_LANGUAGE_KEY = EnvLoader.get("MR_ACCEPT_LANGUAGE_KEY");
    public static final String MR_ACCEPT_LANGUAGE_VALUE = EnvLoader.get("MR_ACCEPT_LANGUAGE_VALUE");
    public static final String MR_PRIORITY_KEY = EnvLoader.get("MR_PRIORITY_KEY");
    public static final String MR_PRIORITY_VALUE = EnvLoader.get("MR_PRIORITY_VALUE");
    public static final String MR_REFERER_KEY = EnvLoader.get("MR_REFERER_KEY");
    public static final String MR_REFERER_VALUE = EnvLoader.get("MR_REFERER_VALUE");
    public static final String MR_SEC_CH_UA_KEY = EnvLoader.get("MR_SEC_CH_UA_KEY");
    public static final String MR_SEC_CH_UA_VALUE = EnvLoader.get("MR_SEC_CH_UA_VALUE");
    public static final String MR_SEC_CH_UA_MOBILE_KEY = EnvLoader.get("MR_SEC_CH_UA_MOBILE_KEY");
    public static final String MR_SEC_CH_UA_MOBILE_VALUE = EnvLoader.get("MR_SEC_CH_UA_MOBILE_VALUE");
    public static final String MR_SEC_CH_UA_PLATFORM_KEY = EnvLoader.get("MR_SEC_CH_UA_PLATFORM_KEY");
    public static final String MR_SEC_CH_UA_PLATFORM_VALUE = EnvLoader.get("MR_SEC_CH_UA_PLATFORM_VALUE");
    public static final String MR_SEC_FETCH_DEST_KEY = EnvLoader.get("MR_SEC_FETCH_DEST_KEY");
    public static final String MR_SEC_FETCH_DEST_VALUE = EnvLoader.get("MR_SEC_FETCH_DEST_VALUE");
    public static final String MR_SEC_FETCH_MODE_KEY = EnvLoader.get("MR_SEC_FETCH_MODE_KEY");
    public static final String MR_SEC_FETCH_MODE_VALUE = EnvLoader.get("MR_SEC_FETCH_MODE_VALUE");
    public static final String MR_SEC_FETCH_SITE_KEY = EnvLoader.get("MR_SEC_FETCH_SITE_KEY");
    public static final String MR_SEC_FETCH_SITE_VALUE = EnvLoader.get("MR_SEC_FETCH_SITE_VALUE");
    public static final String MR_SEC_FETCH_USER_KEY = EnvLoader.get("MR_SEC_FETCH_USER_KEY");
    public static final String MR_SEC_FETCH_USER_VALUE = EnvLoader.get("MR_SEC_FETCH_USER_VALUE");
    public static final String MR_UPGRADE_INSECURE_REQUESTS_KEY = EnvLoader.get("MR_UPGRADE_INSECURE_REQUESTS_KEY");
    public static final String MR_UPGRADE_INSECURE_REQUESTS_VALUE = EnvLoader.get("MR_UPGRADE_INSECURE_REQUESTS_VALUE");
    public static final String MR_USER_AGENT_KEY = EnvLoader.get("MR_USER_AGENT_KEY");
    public static final String MR_USER_AGENT_VALUE = EnvLoader.get("MR_USER_AGENT_VALUE");
    // 메뉴, 리뷰, 영업시간 파싱
    public static final String MR_LOWER_BOUND = EnvLoader.get("MR_LOWER_BOUND").replace("\"", "");
    public static final String MR_UPPER_BOUND = EnvLoader.get("MR_UPPER_BOUND").replace("\"", "");
    public static final Pattern MR_MENU_PATTERN = Pattern.compile(EnvLoader.get("MR_MENU_PATTERN"));
    public static final Pattern MR_REVIEW_PATTERN = Pattern.compile(EnvLoader.get("MR_REVIEW_PATTERN"));
    public static final Pattern MR_ROOT_QUERY_PATTERN = Pattern.compile(EnvLoader.get("MR_ROOT_QUERY_PATTERN"));
    public static final String MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT = EnvLoader.get("MR_BIZ_HOUR_FIRST_DEPTH_KEY_FORMAT");
    public static final String MR_BIZ_HOUR_SECOND_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_SECOND_DEPTH_KEY");
    public static final String MR_BIZ_HOUR_THIRD_DEPTH_KEY = EnvLoader.get("MR_BIZ_HOUR_THIRD_DEPTH_KEY");

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
