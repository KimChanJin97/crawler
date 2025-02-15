package com.rouleatt.demo.db;

import java.util.concurrent.atomic.AtomicInteger;

public class MenuIdGenerator {

    private static final AtomicInteger id = new AtomicInteger(0);

    public static int getNextId() {
        return id.getAndIncrement();
    }
}
