package com.example.trendservice.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Kafka event model for trend data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("metric_name")
    private String metricName;

    @JsonProperty("metric_value")
    private BigDecimal metricValue;

    @JsonProperty("dimension")
    private String dimension;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("produced_at")
    private Instant producedAt;
}
