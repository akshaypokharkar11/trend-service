package com.example.trendservice.events.producer;

import com.example.trendservice.events.model.TrendEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka producer for publishing trend events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${trend.kafka.topics.ten-second:trend-data-10s}")
    private String tenSecondTopic;

    @Value("${trend.kafka.topics.one-minute:trend-data-1m}")
    private String oneMinuteTopic;

    /**
     * Publish a trend event to the 10-second topic.
     */
    public void publishTenSecondEvent(TrendEvent event) {
        publish(tenSecondTopic, event);
    }

    /**
     * Publish a trend event to the 1-minute topic.
     */
    public void publishOneMinuteEvent(TrendEvent event) {
        publish(oneMinuteTopic, event);
    }

    private void publish(String topic, TrendEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }
            event.setProducedAt(Instant.now());

            String payload = objectMapper.writeValueAsString(event);
            String key = event.getMetricName();

            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic={}: {}", topic, ex.getMessage(), ex);
                        } else {
                            log.debug("Published event to topic={}, partition={}, offset={}",
                                    topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize trend event: {}", e.getMessage(), e);
        }
    }
}
