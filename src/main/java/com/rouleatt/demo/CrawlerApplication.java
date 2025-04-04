package com.rouleatt.demo;

import com.rouleatt.demo.crawler.MenuReviewBatchCrawler;
import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;

public class CrawlerApplication {

    public static void main(String[] args) {

        RestaurantImageBatchCrawler restaurantImageBatchCrawler = new RestaurantImageBatchCrawler();
        restaurantImageBatchCrawler.crawl();

        MenuReviewBatchCrawler menuReviewBatchCrawler = new MenuReviewBatchCrawler();
        menuReviewBatchCrawler.crawl();

    }
}