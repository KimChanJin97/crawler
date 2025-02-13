package com.rouleatt.demo.dto;

public record ReviewImageDto(
        int reviewFk,
        String thumbnailUrl
) {
    public static ReviewImageDto of(
            int reviewFk,
            String thumbnailUrl
    ) {
        return new ReviewImageDto(reviewFk, thumbnailUrl);
    }

}
