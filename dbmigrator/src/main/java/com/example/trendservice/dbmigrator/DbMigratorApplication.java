package com.example.trendservice.dbmigrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Database migration runner.
 * Runs Flyway migrations and exits.
 */
@Slf4j
@SpringBootApplication(scanBasePackages = "com.example.trendservice")
public class DbMigratorApplication {

    public static void main(String[] args) {
        log.info("Starting database migration...");
        SpringApplication app = new SpringApplication(DbMigratorApplication.class);
        app.run(args);
        log.info("Database migration complete.");
    }
}
