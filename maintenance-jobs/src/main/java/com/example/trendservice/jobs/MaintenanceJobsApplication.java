package com.example.trendservice.jobs;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Maintenance Jobs application.
 * Runs scheduled tasks with distributed locking via ShedLock.
 */
@SpringBootApplication(scanBasePackages = "com.example.trendservice")
@EntityScan(basePackages = "com.example.trendservice.impl.entity")
@EnableJpaRepositories(basePackages = "com.example.trendservice.impl.repository")
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class MaintenanceJobsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaintenanceJobsApplication.class, args);
    }
}
