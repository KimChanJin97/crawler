package com.rouleatt.demo.dto;

import com.rouleatt.demo.utils.Region;

public record RegionDto(
        String fullName,
        String shortName,
        double minX,
        double minY,
        double maxX,
        double maxY
) {
    public static RegionDto from(Region region) {
        return new RegionDto(
                region.getFullName(),
                region.getShortName(),
                region.getMinX(),
                region.getMinY(),
                region.getMaxX(),
                region.getMaxY());
    }

    public static RegionDto of(
            String fullName,
            String shortName,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        return new RegionDto(fullName, shortName, minX, minY, maxX, maxY);
    }
}
