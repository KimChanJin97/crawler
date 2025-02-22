package com.rouleatt.demo.dto;

public record ReviewDto(
        int reviewPk,
        int restaurantFk,
        String name,
        String type,
        String url,
        String title,
        String reviewIdx,
        String content,
        String profileUrl,
        String authorName,
        String createdAt
) {
    public static ReviewDto of(
            int reviewPk,
            int restaurantFk,
            String name,
            String type,
            String url,
            String title,
            String reviewIdx,
            String content,
            String profileUrl,
            String authorName,
            String createdAt) {
        return new ReviewDto(
                reviewPk,
                restaurantFk,
                name,
                type,
                url,
                title,
                reviewIdx,
                content,
                profileUrl,
                authorName,
                createdAt);
    }
}
