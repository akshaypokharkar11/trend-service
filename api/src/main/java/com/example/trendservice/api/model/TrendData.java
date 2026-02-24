package com.example.trendservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Core trend data model representing a single data point in a trend series.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendData {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("metric_name")
    private String metricName;

    @JsonProperty("metric_value")
    private BigDecimal metricValue;

    @JsonProperty("dimension")
    private String dimension;

    @JsonProperty("granularity")
    private TrendGranularity granularity;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
