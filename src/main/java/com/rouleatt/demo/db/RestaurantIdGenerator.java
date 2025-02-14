package com.rouleatt.demo.db;

public class RestaurantIdGenerator {
    private static int id = 1;

    public static int getNextId() {
        return id++;
    }
}
