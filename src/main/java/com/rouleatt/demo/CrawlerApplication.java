package com.rouleatt.demo;

import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;
import com.rouleatt.demo.db.TableInitializer;

public class CrawlerApplication {

    public static void main(String[] args) {

        TableInitializer initializer = new TableInitializer();
        initializer.init();

        RestaurantImageBatchCrawler crawler = new RestaurantImageBatchCrawler();
        crawler.crawlAll();

    }
}