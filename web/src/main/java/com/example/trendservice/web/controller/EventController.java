package com.example.trendservice.web.controller;

import com.example.trendservice.events.model.TrendEvent;
import com.example.trendservice.events.producer.TrendEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for publishing trend events to Kafka.
 *
 * Flow: POST /api/v1/events/publish/10s → Kafka (trend-data-10s) → TenSecondConsumer → DB
 *       POST /api/v1/events/publish/1m  → Kafka (trend-data-1m)  → OneMinuteConsumer → DB
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final TrendEventProducer trendEventProducer;

    /**
     * Publish a single event to the 10-second Kafka topic.
     */
    @PostMapping("/publish/10s")
    public ResponseEntity<Map<String, String>> publishTenSecondEvent(@RequestBody TrendEvent event) {
        String eventId = ensureEventId(event);
        log.info("Publishing 10s event: metric={}, eventId={}", event.getMetricName(), eventId);

        trendEventProducer.publishTenSecondEvent(event);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "event_id", eventId, "topic", "trend-data-10s"));
    }

    /**
     * Publish a single event to the 1-minute Kafka topic.
     */
    @PostMapping("/publish/1m")
    public ResponseEntity<Map<String, String>> publishOneMinuteEvent(@RequestBody TrendEvent event) {
        String eventId = ensureEventId(event);
        log.info("Publishing 1m event: metric={}, eventId={}", event.getMetricName(), eventId);

        trendEventProducer.publishOneMinuteEvent(event);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "event_id", eventId, "topic", "trend-data-1m"));
    }

    /**
     * Publish a batch of events to the 10-second Kafka topic.
     */
    @PostMapping("/publish/10s/batch")
    public ResponseEntity<Map<String, Object>> publishTenSecondBatch(@RequestBody List<TrendEvent> events) {
        log.info("Publishing batch of {} events to 10s topic", events.size());
        events.forEach(event -> {
            ensureEventId(event);
            trendEventProducer.publishTenSecondEvent(event);
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "count", events.size(), "topic", "trend-data-10s"));
    }

    /**
     * Publish a batch of events to the 1-minute Kafka topic.
     */
    @PostMapping("/publish/1m/batch")
    public ResponseEntity<Map<String, Object>> publishOneMinuteBatch(@RequestBody List<TrendEvent> events) {
        log.info("Publishing batch of {} events to 1m topic", events.size());
        events.forEach(event -> {
            ensureEventId(event);
            trendEventProducer.publishOneMinuteEvent(event);
        });

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("status", "ACCEPTED", "count", events.size(), "topic", "trend-data-1m"));
    }

    /**
     * Convenience endpoint: generate and publish a sample event for testing.
     */
    @PostMapping("/publish/sample")
    public ResponseEntity<Map<String, String>> publishSampleEvent() {
        TrendEvent sampleEvent = TrendEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("SAMPLE")
                .metricName("cpu_usage")
                .metricValue(BigDecimal.valueOf(Math.random() * 100))
                .dimension("server-01")
                .timestamp(Instant.now())
                .build();

        trendEventProducer.publishTenSecondEvent(sampleEvent);
        log.info("Published sample event: {}", sampleEvent.getEventId());

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "status", "ACCEPTED",
                        "event_id", sampleEvent.getEventId(),
                        "topic", "trend-data-10s",
                        "metric_name", sampleEvent.getMetricName()
                ));
    }

    private String ensureEventId(TrendEvent event) {
        if (event.getEventId() == null) {
            event.setEventId(UUID.randomUUID().toString());
        }
        return event.getEventId();
    }
}
