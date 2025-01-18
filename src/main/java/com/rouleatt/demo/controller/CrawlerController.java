package com.rouleatt.demo.controller;

import com.rouleatt.demo.crawler.MenuReviewCrawler;
import com.rouleatt.demo.crawler.RestaurantImageCrawler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CrawlerController {

    private static final double KOR_MIN_X = 33.11;
    private static final double KOR_MIN_Y = 124.60;
    private static final double KOR_MAX_X = 38.61;
    private static final double KOR_MAX_Y = 131.87;

    private final RestaurantImageCrawler restaurantImageCrawler;
    private final MenuReviewCrawler menuReviewCrawler;

    @GetMapping("/crawl")
    public void crawl() {
        restaurantImageCrawler.crawl(KOR_MIN_Y, KOR_MIN_X, KOR_MAX_Y, KOR_MAX_X);
        menuReviewCrawler.crawl();
    }
}
