package com.rouleatt.demo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.regex.Matcher;

public record MenuDto(
        String restaurantId,
        int menuIndex,
        String name,
        boolean isRecommended,
        String price,
        String description,
        String image
) {

    public static MenuDto of(
            String restaurantId,
            int menuIndex,
            String name,
            boolean isRecommended,
            String price,
            String description,
            String image
    ) {
        return new MenuDto(
                restaurantId,
                menuIndex,
                name,
                isRecommended,
                price,
                description,
                image);
    }
}
