package com.rouleatt.demo;

import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;
import com.rouleatt.demo.db.TableManager;

public class CrawlerApplication {

    public static void main(String[] args) {

        TableManager tableManager = new TableManager();
        tableManager.init();

        RestaurantImageBatchCrawler crawler = new RestaurantImageBatchCrawler();
        crawler.crawl();

    }
}