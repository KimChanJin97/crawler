package com.rouleatt.demo.dto;

import lombok.Builder;

@Builder
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
}
