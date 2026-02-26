package com.example.trendservice.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Trend Service Web API.
 */
@SpringBootApplication(scanBasePackages = "com.example.trendservice")
public class TrendWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendWebApplication.class, args);
    }
}
