package com.example.trendservice.impl.repository;

import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.impl.entity.TrendEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA repository for trend data persistence.
 */
@Repository
public interface TrendRepository extends JpaRepository<TrendEntity, Long> {

    /**
     * Find trend data by metric, granularity, and time range.
     */
    @Query("SELECT t FROM TrendEntity t WHERE t.metricName = :metricName " +
           "AND t.granularity = :granularity " +
           "AND t.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY t.timestamp ASC")
    Page<TrendEntity> findByMetricAndTimeRange(
            @Param("metricName") String metricName,
            @Param("granularity") TrendGranularity granularity,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Find trend data with optional dimension filter.
     */
    @Query("SELECT t FROM TrendEntity t WHERE t.metricName = :metricName " +
           "AND t.granularity = :granularity " +
           "AND (:dimension IS NULL OR t.dimension = :dimension) " +
           "AND t.timestamp BETWEEN :startTime AND :endTime " +
           "ORDER BY t.timestamp ASC")
    Page<TrendEntity> findByMetricDimensionAndTimeRange(
            @Param("metricName") String metricName,
            @Param("dimension") String dimension,
            @Param("granularity") TrendGranularity granularity,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Get the latest data point for a metric and dimension.
     */
    Optional<TrendEntity> findFirstByMetricNameAndDimensionOrderByTimestampDesc(
            String metricName, String dimension);

    /**
     * Count data points for cleanup/maintenance purposes.
     */
    long countByTimestampBefore(Instant before);

    /**
     * Delete old data points for maintenance.
     */
    void deleteByTimestampBefore(Instant before);
}
