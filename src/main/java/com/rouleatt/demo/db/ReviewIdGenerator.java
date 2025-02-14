package com.rouleatt.demo.db;

public class ReviewIdGenerator {
    private static int id = 1;

    public static int getNextId() {
        return id++;
    }
}
