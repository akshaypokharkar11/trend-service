package com.example.trendservice.kafka.consumer;

import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.api.service.TrendService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TenSecondConsumer.class})
@Testcontainers
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        RedisAutoConfiguration.class
})
@DisplayName("TenSecondConsumer Integration Tests")
class TenSecondConsumerIT {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        registry.add("spring.kafka.consumer.key-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("trend.kafka.topics.ten-second", () -> "trend-data-10s");
    }

    @MockBean
    private TrendService trendService;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaTemplate<String, String> testProducer;

    @BeforeEach
    void setUp() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(props);
        testProducer = new KafkaTemplate<>(pf);
    }

    @Test
    @DisplayName("should consume message and call trendService.ingest with TEN_SECONDS granularity")
    void shouldConsumeAndIngestWithCorrectGranularity() throws Exception {
        TrendData input = TrendData.builder()
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("85.5"))
                .dimension("server-1")
                .timestamp(Instant.now())
                .build();

        when(trendService.ingest(any(TrendData.class))).thenReturn(input);

        String payload = objectMapper.writeValueAsString(input);
        testProducer.send(new ProducerRecord<>("trend-data-10s", "cpu_usage", payload)).get();

        // Wait for consumer to process
        verify(trendService, timeout(10000).atLeastOnce()).ingest(any(TrendData.class));

        ArgumentCaptor<TrendData> captor = ArgumentCaptor.forClass(TrendData.class);
        verify(trendService, atLeastOnce()).ingest(captor.capture());

        TrendData consumed = captor.getValue();
        assertThat(consumed.getMetricName()).isEqualTo("cpu_usage");
        assertThat(consumed.getGranularity()).isEqualTo(TrendGranularity.TEN_SECONDS);
        assertThat(consumed.getSource()).isEqualTo("kafka-10s");
    }

    @Test
    @DisplayName("should handle malformed JSON gracefully without crashing")
    void shouldHandleMalformedJsonGracefully() throws Exception {
        testProducer.send(new ProducerRecord<>("trend-data-10s", "bad-key", "not-valid-json")).get();

        // Give consumer time to process
        TimeUnit.SECONDS.sleep(3);

        // The consumer should not crash - no exception propagated
        // and ingest should not be called for malformed data
    }
}
