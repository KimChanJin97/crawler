package com.rouleatt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record RestaurantDto(
        String id,
        String name,
        String x,
        String y,
        String category,
        String address,
        String roadAddress
) {
    public static RestaurantDto of(
            String id,
            String name,
            String x,
            String y,
            String category,
            String address,
            String roadAddress
    ) {
        return new RestaurantDto(id, name, x, y, category,address,roadAddress);
    }

    public static String address(JsonNode addressNode) {
        String address = addressNode.asText();
        if (address.length() > 0) {
            return address;
        }
        return null;
    }
}
