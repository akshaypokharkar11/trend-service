package com.example.trendservice.web.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for entity scanning and repository registration.
 * Separated from TrendWebApplication so that @WebMvcTest slices
 * do not attempt to initialise JPA infrastructure.
 */
@Configuration
@EntityScan(basePackages = "com.example.trendservice.impl.entity")
@EnableJpaRepositories(basePackages = "com.example.trendservice.impl.repository")
public class JpaConfig {
}
