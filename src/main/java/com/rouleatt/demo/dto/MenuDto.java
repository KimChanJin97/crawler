package com.rouleatt.demo.dto;

import lombok.Builder;

@Builder
public record MenuDto(
        String restaurantId,
        int menuIndex,
        String name,
        boolean isRecommended,
        String price,
        String description,
        String image
) {
}
