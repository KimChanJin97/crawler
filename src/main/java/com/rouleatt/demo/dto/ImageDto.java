package com.rouleatt.demo.dto;

import lombok.Builder;

@Builder
public record ImageDto(
        String restaurantId,
        String url
) {
}
