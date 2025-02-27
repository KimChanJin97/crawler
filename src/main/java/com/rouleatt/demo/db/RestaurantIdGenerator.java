package com.rouleatt.demo.db;

import java.util.concurrent.atomic.AtomicInteger;

public class RestaurantIdGenerator {

    private static final AtomicInteger ID = new AtomicInteger(1);

    public static int getNextId() {
        return ID.getAndIncrement();
    }
}
