package com.rouleatt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record RestaurantDto(
        String id,
        String name,
        String x,
        String y,
        String category,
        String address,
        String roadAddress
) {
    public static RestaurantDto parseAndInitRestaurantDto(
            String id,
            JsonNode itemNode
    ) {
        return RestaurantDto.builder()
                .id(id)
                .name(itemNode.path("name").asText())
                .x(itemNode.path("x").asText())
                .y(itemNode.path("y").asText())
                .category(itemNode.path("categoryName").asText())
                .address(address(itemNode.path("address")))
                .roadAddress(address(itemNode.path("roadAddress")))
                .build();
    }

    public static String address(JsonNode addressNode) {
        String address = addressNode.asText();
        if (address.length() > 0) {
            return address;
        }
        return null;
    }
}
