package com.example.trendservice.api;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for TrendData model.
 */
class TrendDataTest {

    @Test
    void shouldBuildTrendData() {
        TrendData data = TrendData.builder()
                .metricName("cpu_usage")
                .metricValue(BigDecimal.valueOf(85.5))
                .dimension("server-01")
                .granularity(TrendGranularity.TEN_SECONDS)
                .timestamp(Instant.now())
                .source("test")
                .build();

        assertNotNull(data);
        assertEquals("cpu_usage", data.getMetricName());
        assertEquals(TrendGranularity.TEN_SECONDS, data.getGranularity());
    }
}
