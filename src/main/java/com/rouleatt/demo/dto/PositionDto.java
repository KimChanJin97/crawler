package com.rouleatt.demo.dto;

import java.util.Set;
import lombok.Builder;

@Builder
public record PositionDto(
        double minY,
        double minX,
        double maxY,
        double maxX,
        Set<String> restaurantIdSet,
        int bound
) {
}
