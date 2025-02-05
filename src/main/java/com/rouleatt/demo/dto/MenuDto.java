package com.rouleatt.demo.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.regex.Matcher;
import lombok.Builder;

@Builder
public record MenuDto(
        String restaurantId,
        int menuIndex,
        String name,
        boolean isRecommended,
        String price,
        String description,
        String image
) {
    public static ArrayList<MenuDto> parseAndInitMenuDtos(ObjectMapper objectMapper, String restaurantId, Matcher menuMatcher) {
        ArrayList<MenuDto> menuDtos = new ArrayList<>();

        try {
            while (menuMatcher.find()) {
                String node = menuMatcher.group().split(":", 3)[2]; // {"k":"v"}
                JsonNode menuNode = objectMapper.readTree(node);

                menuDtos.add(MenuDto.builder()
                        .restaurantId(restaurantId)
                        .menuIndex(menuNode.path("index").asInt())
                        .name(menuNode.path("name").asText())
                        .isRecommended(menuNode.path("recommend").asBoolean())
                        .price(menuNode.path("price").asText())
                        .description(description(menuNode.path("description")))
                        .image(image(menuNode.path("images")))
                        .build());
            }
        } catch (JsonProcessingException e) {
            // 메뉴 존재하지 않다면 파싱 예외 무시
        }
        return menuDtos;
    }

    private static String description(JsonNode descriptionNode) {
        String description = descriptionNode.asText();
        if (description.length() > 0) {
            return description;
        }
        return null;
    }

    private static String image(JsonNode imageNode) {
        if (imageNode.get(0) == null) {
            return null;
        }
        return imageNode.get(0).asText();
    }
}
