package com.rouleatt.demo.dto;

public record MenuBackupDto(
        int restaurantPk,
        String restaurantId
) {
    public static MenuBackupDto of(int restaurantPk, String restaurantId) {
        return new MenuBackupDto(restaurantPk, restaurantId);
    }
}
