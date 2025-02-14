package com.rouleatt.demo;

import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;
import com.rouleatt.demo.db.TableInitializer;

public class CrawlerApplication {

    public static void main(String[] args) {

        TableInitializer tableInitializer = new TableInitializer();
        tableInitializer.init();

        RestaurantImageBatchCrawler restaurantImageBatchCrawler = new RestaurantImageBatchCrawler();
        restaurantImageBatchCrawler.crawlAll();
    }
}
