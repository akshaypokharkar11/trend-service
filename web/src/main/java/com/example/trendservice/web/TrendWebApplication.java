package com.example.trendservice.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main entry point for the Trend Service Web API.
 */
@SpringBootApplication(scanBasePackages = "com.example.trendservice")
@EntityScan(basePackages = "com.example.trendservice.impl.entity")
@EnableJpaRepositories(basePackages = "com.example.trendservice.impl.repository")
public class TrendWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendWebApplication.class, args);
    }
}
