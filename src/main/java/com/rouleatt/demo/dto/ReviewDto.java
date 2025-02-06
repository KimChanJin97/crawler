package com.rouleatt.demo.dto;

import lombok.Builder;

public record ReviewDto(
        String restaurantId,
        String reviewerName,
        String typeName,
        String url,
        String thumbnailUrl,
        String title,
        Long reviewIndex,
        String content,
        String createdAt
) {
    public static ReviewDto of(
            String restaurantId,
            String reviewerName,
            String typeName,
            String url,
            String thumbnailUrl,
            String title,
            long reviewIndex,
            String content,
            String createdAt) {

        return new ReviewDto(
                restaurantId,
                reviewerName,
                typeName,
                url,
                thumbnailUrl,
                title,
                reviewIndex,
                content,
                createdAt);
    }
}
