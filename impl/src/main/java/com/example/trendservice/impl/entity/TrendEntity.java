package com.example.trendservice.impl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing trend data stored in PostgreSQL.
 */
@Entity
@Table(name = "trend_data", indexes = {
    @Index(name = "idx_trend_metric_ts", columnList = "metric_name, timestamp"),
    @Index(name = "idx_trend_dimension_ts", columnList = "dimension, timestamp"),
    @Index(name = "idx_trend_granularity", columnList = "granularity")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name", nullable = false, length = 255)
    private String metricName;

    @Column(name = "metric_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal metricValue;

    @Column(name = "dimension", length = 255)
    private String dimension;

    @Enumerated(EnumType.STRING)
    @Column(name = "granularity", nullable = false, length = 20)
    private com.example.trendservice.api.model.TrendGranularity granularity;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
