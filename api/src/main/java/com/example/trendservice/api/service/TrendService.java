package com.example.trendservice.api.service;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendRequest;
import com.example.trendservice.api.model.TrendResponse;

import java.util.List;

/**
 * Service interface for trend data operations.
 */
public interface TrendService {

    /**
     * Query trend data based on the provided request parameters.
     */
    TrendResponse queryTrends(TrendRequest request);

    /**
     * Ingest a single trend data point.
     */
    TrendData ingest(TrendData data);

    /**
     * Batch ingest multiple trend data points.
     */
    List<TrendData> ingestBatch(List<TrendData> dataPoints);

    /**
     * Get the latest trend data point for a given metric.
     */
    TrendData getLatest(String metricName, String dimension);
}
