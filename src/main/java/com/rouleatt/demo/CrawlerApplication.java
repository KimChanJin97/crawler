package com.rouleatt.demo;

import com.rouleatt.demo.batch.TableManager;
import com.rouleatt.demo.crawler.MenuReviewBatchCrawler;
import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;

public class CrawlerApplication {

    public static void main(String[] args) {

        // DB 드랍 주의. 최초에만 사용
//        TableManager tableManager = new TableManager();
//        tableManager.dropAndCreateAllTables();

//        RestaurantImageBatchCrawler restaurantImageBatchCrawler = new RestaurantImageBatchCrawler();
//        restaurantImageBatchCrawler.crawl();

        MenuReviewBatchCrawler menuReviewBatchCrawler = new MenuReviewBatchCrawler();
        menuReviewBatchCrawler.crawl();

    }
}