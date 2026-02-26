package com.example.trendservice.events.producer;

import com.example.trendservice.events.model.TrendEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendEventProducer Unit Tests")
class TrendEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private TrendEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        producer = new TrendEventProducer(kafkaTemplate, objectMapper);
        ReflectionTestUtils.setField(producer, "tenSecondTopic", "trend-data-10s");
        ReflectionTestUtils.setField(producer, "oneMinuteTopic", "trend-data-1m");
    }

    @Test
    @DisplayName("should publish to 10-second topic with correct key")
    void shouldPublishToTenSecondTopic() {
        TrendEvent event = TrendEvent.builder()
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("75.0"))
                .timestamp(Instant.now())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        producer.publishTenSecondEvent(event);

        verify(kafkaTemplate).send(eq("trend-data-10s"), eq("cpu_usage"), anyString());
    }

    @Test
    @DisplayName("should publish to 1-minute topic with correct key")
    void shouldPublishToOneMinuteTopic() {
        TrendEvent event = TrendEvent.builder()
                .metricName("memory_usage")
                .metricValue(new BigDecimal("60.0"))
                .timestamp(Instant.now())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        producer.publishOneMinuteEvent(event);

        verify(kafkaTemplate).send(eq("trend-data-1m"), eq("memory_usage"), anyString());
    }

    @Test
    @DisplayName("should auto-generate eventId when null")
    void shouldAutoGenerateEventId() {
        TrendEvent event = TrendEvent.builder()
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("75.0"))
                .timestamp(Instant.now())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        producer.publishTenSecondEvent(event);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getProducedAt()).isNotNull();
    }

    @Test
    @DisplayName("should preserve existing eventId")
    void shouldPreserveExistingEventId() {
        TrendEvent event = TrendEvent.builder()
                .eventId("custom-id-123")
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("75.0"))
                .timestamp(Instant.now())
                .build();

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(new CompletableFuture<>());

        producer.publishTenSecondEvent(event);

        assertThat(event.getEventId()).isEqualTo("custom-id-123");
    }

    @Test
    @DisplayName("should serialize event as JSON payload")
    void shouldSerializeEventAsJson() {
        TrendEvent event = TrendEvent.builder()
                .metricName("disk_io")
                .metricValue(new BigDecimal("120.5"))
                .dimension("sda1")
                .timestamp(Instant.now())
                .build();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        when(kafkaTemplate.send(anyString(), anyString(), payloadCaptor.capture()))
                .thenReturn(new CompletableFuture<>());

        producer.publishTenSecondEvent(event);

        String payload = payloadCaptor.getValue();
        assertThat(payload).contains("disk_io");
        assertThat(payload).contains("120.5");
        assertThat(payload).contains("sda1");
    }
}
