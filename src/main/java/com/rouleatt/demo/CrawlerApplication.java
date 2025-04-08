package com.rouleatt.demo;

import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.crawler.MenuBatchCrawler;
import com.rouleatt.demo.crawler.RestaurantBatchCrawler;

public class CrawlerApplication {

    public static void main(String[] args) {

        TableManager tableManager = new TableManager();
        tableManager.dropAndCreateAllTables();

        RestaurantBatchCrawler restaurantBatchCrawler = new RestaurantBatchCrawler();
        restaurantBatchCrawler.crawl();

        MenuBatchCrawler menuBatchCrawler = new MenuBatchCrawler();
        menuBatchCrawler.crawl();

    }
}