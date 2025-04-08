package com.rouleatt.demo.dto;

import com.rouleatt.demo.utils.Region;

public record RestaurantBackupDto(
        String fullName,
        String shortName,
        double minX,
        double minY,
        double maxX,
        double maxY
) {
    public static RestaurantBackupDto from(Region region) {
        return new RestaurantBackupDto(
                region.getFullName(),
                region.getShortName(),
                region.getMinX(),
                region.getMinY(),
                region.getMaxX(),
                region.getMaxY());
    }

    public static RestaurantBackupDto of(
            String fullName,
            String shortName,
            double minX,
            double minY,
            double maxX,
            double maxY
    ) {
        return new RestaurantBackupDto(fullName, shortName, minX, minY, maxX, maxY);
    }
}
