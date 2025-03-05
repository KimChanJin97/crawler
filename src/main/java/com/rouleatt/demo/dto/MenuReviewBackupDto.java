package com.rouleatt.demo.dto;

public record MenuReviewBackupDto(
        int restaurantPk,
        String restaurantId
) {
    public static MenuReviewBackupDto of(int restaurantPk, String restaurantId) {
        return new MenuReviewBackupDto(restaurantPk, restaurantId);
    }
}
