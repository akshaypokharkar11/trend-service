package com.example.trendservice.web.controller;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.api.model.TrendRequest;
import com.example.trendservice.api.model.TrendResponse;
import com.example.trendservice.api.service.TrendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for the Trend API.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/trends")
@RequiredArgsConstructor
public class TrendController {

    private final TrendService trendService;

    /**
     * Query trend data with filters.
     * GET /api/v1/trends?metricName=cpu_usage&granularity=ONE_MINUTE&startTime=...&endTime=...
     */
    @GetMapping
    public ResponseEntity<TrendResponse> queryTrends(
            @RequestParam String metricName,
            @RequestParam TrendGranularity granularity,
            @RequestParam Instant startTime,
            @RequestParam Instant endTime,
            @RequestParam(required = false) String dimension,
            @RequestParam(defaultValue = "1000") int limit) {

        TrendRequest request = TrendRequest.builder()
                .metricName(metricName)
                .granularity(granularity)
                .startTime(startTime)
                .endTime(endTime)
                .dimension(dimension)
                .limit(limit)
                .build();

        TrendResponse response = trendService.queryTrends(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Ingest a single trend data point.
     * POST /api/v1/trends
     */
    @PostMapping
    public ResponseEntity<TrendData> ingestTrend(@Valid @RequestBody TrendData data) {
        TrendData saved = trendService.ingest(data);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Batch ingest multiple trend data points.
     * POST /api/v1/trends/batch
     */
    @PostMapping("/batch")
    public ResponseEntity<List<TrendData>> ingestBatch(@Valid @RequestBody List<TrendData> dataPoints) {
        List<TrendData> saved = trendService.ingestBatch(dataPoints);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Get the latest trend data point for a metric.
     * GET /api/v1/trends/latest/{metricName}
     */
    @GetMapping("/latest/{metricName}")
    public ResponseEntity<TrendData> getLatest(
            @PathVariable String metricName,
            @RequestParam(required = false) String dimension) {
        TrendData latest = trendService.getLatest(metricName, dimension);
        return ResponseEntity.ok(latest);
    }
}
