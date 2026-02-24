package com.example.trendservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request model for querying trend data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendRequest {

    @NotBlank(message = "Metric name is required")
    @JsonProperty("metric_name")
    private String metricName;

    @JsonProperty("dimension")
    private String dimension;

    @NotNull(message = "Granularity is required")
    @JsonProperty("granularity")
    private TrendGranularity granularity;

    @NotNull(message = "Start time is required")
    @JsonProperty("start_time")
    private Instant startTime;

    @NotNull(message = "End time is required")
    @JsonProperty("end_time")
    private Instant endTime;

    @JsonProperty("limit")
    @Builder.Default
    private int limit = 1000;
}
