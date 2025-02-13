package com.rouleatt.demo.dto;

public record MenuDto(
        int menuPk,
        int restaurantFk,
        String name,
        String price,
        boolean isRecommended,
        String description,
        int menuIdx
) {
    public static MenuDto of(
            int menuPk,
            int restaurantFk,
            String name,
            String price,
            boolean isRecommended,
            String description,
            int menuIdx
    ) {
        return new MenuDto(
                menuPk,
                restaurantFk,
                name,
                price,
                isRecommended,
                description,
                menuIdx);
    }
}
