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
 * Kafka consumer for 10-second granularity trend data.
 * Consumes high-frequency metrics and persists them to the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenSecondConsumer {

    private final TrendService trendService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${trend.kafka.topics.ten-second:trend-data-10s}",
            groupId = "${trend.kafka.consumer-groups.ten-second:trend-10s-consumer-group}",
            containerFactory = "tenSecondKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        try {
            log.debug("Received 10s trend event: key={}, partition={}, offset={}",
                    record.key(), record.partition(), record.offset());

            TrendData data = objectMapper.readValue(record.value(), TrendData.class);
            data.setGranularity(TrendGranularity.TEN_SECONDS);
            data.setSource("kafka-10s");

            trendService.ingest(data);

            log.debug("Successfully processed 10s trend event for metric={}", data.getMetricName());
        } catch (Exception e) {
            log.error("Error processing 10s trend event: key={}, error={}",
                    record.key(), e.getMessage(), e);
        }
    }
}
