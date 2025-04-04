package com.rouleatt.demo.dto;

public record BizHourDto(
        int restaurantFk,
        String day,
        String bizStart,
        String breakStart,
        String breakEnd,
        String lastOrder,
        String bizEnd
) {
    public static BizHourDto of(
            int restaurantFk,
            String day,
            String bizStart,
            String breakStart,
            String breakEnd,
            String lastOrder,
            String bizEnd
    ) {
        return new BizHourDto(
                restaurantFk,
                day,
                bizStart,
                breakStart,
                breakEnd,
                lastOrder,
                bizEnd
        );
    }
}
