package com.rouleatt.demo.dto;

public record RestaurantImageDto(
        int restaurantFk,
        String url
) {
    public static RestaurantImageDto of(int restaurantFk, String url) {
        return new RestaurantImageDto(restaurantFk, url);
    }
}
