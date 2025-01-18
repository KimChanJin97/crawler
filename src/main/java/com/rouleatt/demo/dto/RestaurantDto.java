package com.rouleatt.demo.dto;

import java.util.Objects;
import lombok.Builder;

@Builder
public record RestaurantDto(
        String id,
        String name,
        String x,
        String y,
        String category,
        String address,
        String roadAddress
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestaurantDto that = (RestaurantDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(x, that.x) &&
                Objects.equals(y, that.y) &&
                Objects.equals(category, that.category) &&
                Objects.equals(address, that.address) &&
                Objects.equals(roadAddress, that.roadAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, x, y, category, address, roadAddress);
    }
}
