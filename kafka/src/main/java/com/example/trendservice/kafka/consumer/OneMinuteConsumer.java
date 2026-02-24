package com.example.trendservice.kafka.consumer;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.api.service.TrendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for 1-minute granularity trend data.
 * Consumes aggregated metrics and persists them to the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OneMinuteConsumer {

    private final TrendService trendService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${trend.kafka.topics.one-minute:trend-data-1m}",
            groupId = "${trend.kafka.consumer-groups.one-minute:trend-1m-consumer-group}",
            containerFactory = "oneMinuteKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            log.debug("Received 1m trend event: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            TrendData data = objectMapper.readValue(record.value(), TrendData.class);
            data.setGranularity(TrendGranularity.ONE_MINUTE);
            data.setSource("kafka-1m");

            trendService.ingest(data);

            log.debug("Successfully processed 1m trend event for metric={}", data.getMetricName());
        } catch (Exception e) {
            log.error("Error processing 1m trend event: key={}, error={}",
                    record.key(), e.getMessage(), e);
        }
    }
}
