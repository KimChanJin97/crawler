package com.rouleatt.demo.dto;

public record MenuImageDto(
        int menuFk,
        String imageUrl
) {
    public static MenuImageDto of(int menuFk, String imageUrl) {
        return new MenuImageDto(menuFk, imageUrl);
    }
}
