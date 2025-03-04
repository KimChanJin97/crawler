package com.rouleatt.demo;

import com.rouleatt.demo.crawler.RestaurantImageBatchCrawler;

public class CrawlerApplication {

    public static void main(String[] args) {

        RestaurantImageBatchCrawler crawler = new RestaurantImageBatchCrawler();
        crawler.crawl();

    }
}