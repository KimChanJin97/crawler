package com.rouleatt.demo.dto;

public record ImageDto(
        String restaurantId,
        String url
) {
    public static ImageDto of(String restaurantId, String url) {
        return new ImageDto(restaurantId, url);
    }
}
