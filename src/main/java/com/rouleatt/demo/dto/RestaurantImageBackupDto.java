package com.rouleatt.demo.dto;

import com.rouleatt.demo.utils.Region;

public record RestaurantImageBackupDto(
        String fullName,
        String shortName,
        double minX,
        double minY,
        double maxX,
        double maxY
) {
    public static RestaurantImageBackupDto from(Region region) {
        return new RestaurantImageBackupDto(
                region.getFullName(),
                region.getShortName(),
                region.getMinX(),
                region.getMinY(),
                region.getMaxX(),
                region.getMaxY());
    }

    public static RestaurantImageBackupDto of(
            String fullName,
            String shortName,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        return new RestaurantImageBackupDto(fullName, shortName, minX, minY, maxX, maxY);
    }
}
