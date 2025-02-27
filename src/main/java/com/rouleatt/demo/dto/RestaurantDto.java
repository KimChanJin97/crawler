package com.rouleatt.demo.dto;

public record RestaurantDto(
        int restaurantPk,
        String restaurantId,
        String name,
        double x,
        double y,
        String category,
        String address,
        String roadAddress
) {
    public static RestaurantDto of(
            int restaurantPk,
            String restaurantId,
            String name,
            double x,
            double y,
            String category,
            String address,
            String roadAddress
    ) {
        return new RestaurantDto(restaurantPk, restaurantId, name, x, y, category, address, roadAddress);
    }
}
