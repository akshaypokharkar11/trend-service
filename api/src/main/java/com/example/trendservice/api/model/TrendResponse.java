package com.example.trendservice.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response model for trend API queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendResponse {

    @JsonProperty("metric_name")
    private String metricName;

    @JsonProperty("granularity")
    private TrendGranularity granularity;

    @JsonProperty("data_points")
    private List<TrendData> dataPoints;

    @JsonProperty("total_count")
    private long totalCount;

    @JsonProperty("has_more")
    private boolean hasMore;
}
