package com.example.trendservice.impl.repository;

import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.impl.entity.TrendEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnableAutoConfiguration(exclude = RedisAutoConfiguration.class)
@DisplayName("TrendRepository Integration Tests")
class TrendRepositoryIT {

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
    }

    @Autowired
    private TrendRepository trendRepository;

    private Instant baseTime;

    @BeforeEach
    void setUp() {
        trendRepository.deleteAll();
        baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private TrendEntity createEntity(String metricName, String dimension,
                                      TrendGranularity granularity, BigDecimal value, Instant timestamp) {
        return TrendEntity.builder()
                .metricName(metricName)
                .metricValue(value)
                .dimension(dimension)
                .granularity(granularity)
                .timestamp(timestamp)
                .source("test")
                .build();
    }

    @Nested
    @DisplayName("findByMetricAndTimeRange")
    class FindByMetricAndTimeRange {

        @Test
        @DisplayName("should find entries within time range for metric and granularity")
        void shouldFindEntriesInTimeRange() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), start.plusSeconds(600)),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("60.0"), start.plusSeconds(1200)),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("70.0"), start.minusSeconds(600)) // outside range
            ));

            Page<TrendEntity> result = trendRepository.findByMetricAndTimeRange(
                    "cpu", TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getMetricValue())
                    .isEqualByComparingTo(new BigDecimal("50.0"));
        }

        @Test
        @DisplayName("should filter by granularity")
        void shouldFilterByGranularity() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), start.plusSeconds(60)),
                    createEntity("cpu", "s1", TrendGranularity.HOURLY, new BigDecimal("55.0"), start.plusSeconds(60))
            ));

            Page<TrendEntity> result = trendRepository.findByMetricAndTimeRange(
                    "cpu", TrendGranularity.HOURLY, start, end, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getGranularity()).isEqualTo(TrendGranularity.HOURLY);
        }

        @Test
        @DisplayName("should return results ordered by timestamp ASC")
        void shouldReturnOrderedByTimestamp() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("80.0"), start.plusSeconds(1800)),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), start.plusSeconds(600)),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("70.0"), start.plusSeconds(1200))
            ));

            Page<TrendEntity> result = trendRepository.findByMetricAndTimeRange(
                    "cpu", TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).getMetricValue()).isEqualByComparingTo(new BigDecimal("50.0"));
            assertThat(result.getContent().get(1).getMetricValue()).isEqualByComparingTo(new BigDecimal("70.0"));
            assertThat(result.getContent().get(2).getMetricValue()).isEqualByComparingTo(new BigDecimal("80.0"));
        }

        @Test
        @DisplayName("should support pagination")
        void shouldSupportPagination() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            for (int i = 0; i < 5; i++) {
                trendRepository.save(createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE,
                        new BigDecimal(50 + i), start.plusSeconds(60L * (i + 1))));
            }

            Page<TrendEntity> page1 = trendRepository.findByMetricAndTimeRange(
                    "cpu", TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(0, 2));
            Page<TrendEntity> page2 = trendRepository.findByMetricAndTimeRange(
                    "cpu", TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(1, 2));

            assertThat(page1.getContent()).hasSize(2);
            assertThat(page1.getTotalElements()).isEqualTo(5);
            assertThat(page1.hasNext()).isTrue();
            assertThat(page2.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByMetricDimensionAndTimeRange")
    class FindByMetricDimensionAndTimeRange {

        @Test
        @DisplayName("should filter by dimension when provided")
        void shouldFilterByDimension() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "server-1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), start.plusSeconds(60)),
                    createEntity("cpu", "server-2", TrendGranularity.ONE_MINUTE, new BigDecimal("60.0"), start.plusSeconds(60))
            ));

            Page<TrendEntity> result = trendRepository.findByMetricDimensionAndTimeRange(
                    "cpu", "server-1", TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getDimension()).isEqualTo("server-1");
        }

        @Test
        @DisplayName("should return all dimensions when dimension is null")
        void shouldReturnAllDimensionsWhenNull() {
            Instant start = baseTime.minus(1, ChronoUnit.HOURS);
            Instant end = baseTime;

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "server-1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), start.plusSeconds(60)),
                    createEntity("cpu", "server-2", TrendGranularity.ONE_MINUTE, new BigDecimal("60.0"), start.plusSeconds(120))
            ));

            Page<TrendEntity> result = trendRepository.findByMetricDimensionAndTimeRange(
                    "cpu", null, TrendGranularity.ONE_MINUTE, start, end, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findFirstByMetricNameAndDimensionOrderByTimestampDesc")
    class FindLatest {

        @Test
        @DisplayName("should return the most recent entry")
        void shouldReturnMostRecent() {
            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("50.0"), baseTime.minusSeconds(120)),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("90.0"), baseTime),
                    createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE, new BigDecimal("70.0"), baseTime.minusSeconds(60))
            ));

            Optional<TrendEntity> result = trendRepository
                    .findFirstByMetricNameAndDimensionOrderByTimestampDesc("cpu", "s1");

            assertThat(result).isPresent();
            assertThat(result.get().getMetricValue()).isEqualByComparingTo(new BigDecimal("90.0"));
        }

        @Test
        @DisplayName("should return empty when no matching metric found")
        void shouldReturnEmptyWhenNoMatch() {
            Optional<TrendEntity> result = trendRepository
                    .findFirstByMetricNameAndDimensionOrderByTimestampDesc("nonexistent", "s1");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Cleanup operations")
    class CleanupOperations {

        @Test
        @DisplayName("should count records before cutoff timestamp")
        void shouldCountOldRecords() {
            Instant cutoff = baseTime.minus(7, ChronoUnit.DAYS);

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("50.0"), cutoff.minusSeconds(86400)),
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("60.0"), cutoff.minusSeconds(172800)),
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("70.0"), baseTime)
            ));

            long count = trendRepository.countByTimestampBefore(cutoff);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("should delete records before cutoff timestamp")
        void shouldDeleteOldRecords() {
            Instant cutoff = baseTime.minus(7, ChronoUnit.DAYS);

            trendRepository.saveAll(List.of(
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("50.0"), cutoff.minusSeconds(86400)),
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("60.0"), cutoff.minusSeconds(172800)),
                    createEntity("cpu", "s1", TrendGranularity.DAILY, new BigDecimal("70.0"), baseTime)
            ));

            trendRepository.deleteByTimestampBefore(cutoff);

            assertThat(trendRepository.findAll()).hasSize(1);
            assertThat(trendRepository.findAll().get(0).getMetricValue())
                    .isEqualByComparingTo(new BigDecimal("70.0"));
        }
    }

    @Nested
    @DisplayName("Entity lifecycle")
    class EntityLifecycle {

        @Test
        @DisplayName("should auto-populate createdAt and updatedAt on save")
        void shouldAutoPopulateTimestamps() {
            TrendEntity entity = createEntity("cpu", "s1", TrendGranularity.ONE_MINUTE,
                    new BigDecimal("50.0"), baseTime);

            TrendEntity saved = trendRepository.save(entity);

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }
    }
}
