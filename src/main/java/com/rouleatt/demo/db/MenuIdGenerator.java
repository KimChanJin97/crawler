package com.rouleatt.demo.db;

public class MenuIdGenerator {
    private static int id = 1;

    public static int getNextId() {
        return id++;
    }
}
