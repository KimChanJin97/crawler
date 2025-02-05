package com.rouleatt.demo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

@Builder
public record ImageDto(
        String restaurantId,
        String url
) {
    public static ImageDto parseAndInitImageDto(
            String id,
            JsonNode imagesNode
    ) {
        return ImageDto.builder()
                .restaurantId(id)
                .url(imagesNode.asText())
                .build();
    }
}
