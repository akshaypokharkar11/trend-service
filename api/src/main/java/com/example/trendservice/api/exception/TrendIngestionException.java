package com.example.trendservice.api.exception;

/**
 * Exception thrown when trend data ingestion fails.
 */
public class TrendIngestionException extends RuntimeException {

    public TrendIngestionException(String message) {
        super(message);
    }

    public TrendIngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
