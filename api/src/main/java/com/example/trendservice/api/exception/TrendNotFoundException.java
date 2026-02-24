package com.example.trendservice.api.exception;

/**
 * Exception thrown when requested trend data is not found.
 */
public class TrendNotFoundException extends RuntimeException {

    public TrendNotFoundException(String message) {
        super(message);
    }

    public TrendNotFoundException(String metricName, String dimension) {
        super(String.format("Trend data not found for metric='%s', dimension='%s'", metricName, dimension));
    }
}
