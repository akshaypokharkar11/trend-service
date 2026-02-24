package com.example.trendservice.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main entry point for the Kafka Consumer application.
 * Runs both 10-second and 1-minute consumers.
 */
@SpringBootApplication(scanBasePackages = "com.example.trendservice")
@EntityScan(basePackages = "com.example.trendservice.impl.entity")
@EnableJpaRepositories(basePackages = "com.example.trendservice.impl.repository")
@EnableKafka
public class TrendConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrendConsumerApplication.class, args);
    }
}
