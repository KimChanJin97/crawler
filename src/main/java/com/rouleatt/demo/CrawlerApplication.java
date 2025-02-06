package com.rouleatt.demo;

import com.rouleatt.demo.crawler.MenuReviewCrawler;
import com.rouleatt.demo.crawler.RestaurantImageCrawler;

public class CrawlerApplication {

    public static void main(String[] args) {

        RestaurantImageCrawler restaurantImageCrawler = new RestaurantImageCrawler();
        restaurantImageCrawler.crawlAll();

        MenuReviewCrawler menuReviewCrawler = new MenuReviewCrawler();
        menuReviewCrawler.crawlAll();
    }
}
