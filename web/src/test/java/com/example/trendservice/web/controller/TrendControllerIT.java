package com.example.trendservice.web.controller;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.web.TrendWebApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = TrendWebApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("TrendController Integration Tests")
class TrendControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("trend_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        // Disable Redis for integration test
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "16379");
        registry.add("spring.cache.type", () -> "none");
        // Disable Kafka for web integration test
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:19092");
        registry.add("spring.autoconfigure.exclude", () ->
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    @Nested
    @DisplayName("Full ingest-then-query flow")
    class IngestAndQuery {

        @Test
        @DisplayName("should ingest a data point and then query it back")
        void shouldIngestAndQueryBack() throws Exception {
            TrendData input = TrendData.builder()
                    .metricName("integration_cpu")
                    .metricValue(new BigDecimal("88.5"))
                    .dimension("host-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now)
                    .source("integration-test")
                    .build();

            // Ingest
            mockMvc.perform(post("/api/v1/trends")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.metric_name").value("integration_cpu"))
                    .andExpect(jsonPath("$.created_at").isNotEmpty());

            // Query
            mockMvc.perform(get("/api/v1/trends")
                            .param("metricName", "integration_cpu")
                            .param("granularity", "ONE_MINUTE")
                            .param("startTime", now.minusSeconds(60).toString())
                            .param("endTime", now.plusSeconds(60).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metric_name").value("integration_cpu"))
                    .andExpect(jsonPath("$.data_points", hasSize(1)))
                    .andExpect(jsonPath("$.data_points[0].metric_value").value(88.5));
        }

        @Test
        @DisplayName("should ingest batch and query all data points")
        void shouldIngestBatchAndQueryAll() throws Exception {
            TrendData d1 = TrendData.builder()
                    .metricName("batch_metric")
                    .metricValue(new BigDecimal("10.0"))
                    .granularity(TrendGranularity.HOURLY)
                    .timestamp(now.minusSeconds(3600))
                    .source("batch-test")
                    .build();
            TrendData d2 = TrendData.builder()
                    .metricName("batch_metric")
                    .metricValue(new BigDecimal("20.0"))
                    .granularity(TrendGranularity.HOURLY)
                    .timestamp(now.minusSeconds(1800))
                    .source("batch-test")
                    .build();
            TrendData d3 = TrendData.builder()
                    .metricName("batch_metric")
                    .metricValue(new BigDecimal("30.0"))
                    .granularity(TrendGranularity.HOURLY)
                    .timestamp(now)
                    .source("batch-test")
                    .build();

            // Batch ingest
            mockMvc.perform(post("/api/v1/trends/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(d1, d2, d3))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(3)));

            // Query all
            mockMvc.perform(get("/api/v1/trends")
                            .param("metricName", "batch_metric")
                            .param("granularity", "HOURLY")
                            .param("startTime", now.minusSeconds(7200).toString())
                            .param("endTime", now.plusSeconds(60).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data_points", hasSize(3)))
                    .andExpect(jsonPath("$.total_count").value(3));
        }
    }

    @Nested
    @DisplayName("Latest endpoint")
    class LatestEndpoint {

        @Test
        @DisplayName("should return the latest ingested data point")
        void shouldReturnLatestDataPoint() throws Exception {
            // Ingest two data points with different timestamps
            TrendData older = TrendData.builder()
                    .metricName("latest_test_metric")
                    .metricValue(new BigDecimal("10.0"))
                    .dimension("dim-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now.minusSeconds(120))
                    .build();
            TrendData newer = TrendData.builder()
                    .metricName("latest_test_metric")
                    .metricValue(new BigDecimal("99.9"))
                    .dimension("dim-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now)
                    .build();

            mockMvc.perform(post("/api/v1/trends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(older)));
            mockMvc.perform(post("/api/v1/trends")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newer)));

            // Get latest
            mockMvc.perform(get("/api/v1/trends/latest/latest_test_metric")
                            .param("dimension", "dim-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metric_value").value(99.9));
        }

        @Test
        @DisplayName("should return 404 for non-existent metric")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/v1/trends/latest/does_not_exist"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("Dimension filtering")
    class DimensionFiltering {

        @Test
        @DisplayName("should filter results by dimension")
        void shouldFilterByDimension() throws Exception {
            TrendData host1 = TrendData.builder()
                    .metricName("dim_filter_cpu")
                    .metricValue(new BigDecimal("50.0"))
                    .dimension("host-a")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now)
                    .build();
            TrendData host2 = TrendData.builder()
                    .metricName("dim_filter_cpu")
                    .metricValue(new BigDecimal("75.0"))
                    .dimension("host-b")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now)
                    .build();

            mockMvc.perform(post("/api/v1/trends/batch")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(List.of(host1, host2))));

            // Query with dimension filter
            mockMvc.perform(get("/api/v1/trends")
                            .param("metricName", "dim_filter_cpu")
                            .param("granularity", "ONE_MINUTE")
                            .param("startTime", now.minusSeconds(60).toString())
                            .param("endTime", now.plusSeconds(60).toString())
                            .param("dimension", "host-a"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data_points", hasSize(1)))
                    .andExpect(jsonPath("$.data_points[0].dimension").value("host-a"));
        }
    }
}
