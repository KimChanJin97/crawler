package com.rouleatt.demo.dto;

public record BizHourDto(
        int restaurantFk,
        String day,
        String bizStart,
        String bizEnd,
        String lastOrder,
        String breakStart,
        String breakEnd
) {
    public static BizHourDto of(
            int restaurantFk,
            String day,
            String bizStart,
            String bizEnd,
            String lastOrder,
            String breakStart,
            String breakEnd
    ) {
        return new BizHourDto(
                restaurantFk,
                day,
                bizStart,
                bizEnd,
                lastOrder,
                breakStart,
                breakEnd);
    }
}
