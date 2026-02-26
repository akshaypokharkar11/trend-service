package com.example.trendservice.impl.service;

import com.example.trendservice.api.exception.TrendNotFoundException;
import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.api.model.TrendRequest;
import com.example.trendservice.api.model.TrendResponse;
import com.example.trendservice.impl.entity.TrendEntity;
import com.example.trendservice.impl.repository.TrendRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendServiceImpl Unit Tests")
class TrendServiceImplTest {

    @Mock
    private TrendRepository trendRepository;

    @InjectMocks
    private TrendServiceImpl trendService;

    private TrendEntity sampleEntity;
    private TrendData sampleData;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        sampleEntity = TrendEntity.builder()
                .id(1L)
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("75.5000"))
                .dimension("server-1")
                .granularity(TrendGranularity.ONE_MINUTE)
                .timestamp(now)
                .source("test")
                .createdAt(now)
                .updatedAt(now)
                .build();

        sampleData = TrendData.builder()
                .metricName("cpu_usage")
                .metricValue(new BigDecimal("75.5000"))
                .dimension("server-1")
                .granularity(TrendGranularity.ONE_MINUTE)
                .timestamp(now)
                .source("test")
                .build();
    }

    @Nested
    @DisplayName("queryTrends")
    class QueryTrends {

        @Test
        @DisplayName("should return trend response with data points")
        void shouldReturnTrendResponseWithDataPoints() {
            Instant start = Instant.now().minusSeconds(3600);
            Instant end = Instant.now();
            TrendRequest request = TrendRequest.builder()
                    .metricName("cpu_usage")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .startTime(start)
                    .endTime(end)
                    .limit(100)
                    .build();

            Page<TrendEntity> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 100), 1);
            when(trendRepository.findByMetricDimensionAndTimeRange(
                    eq("cpu_usage"), any(), eq(TrendGranularity.ONE_MINUTE),
                    eq(start), eq(end), any(PageRequest.class)))
                    .thenReturn(page);

            TrendResponse response = trendService.queryTrends(request);

            assertThat(response.getMetricName()).isEqualTo("cpu_usage");
            assertThat(response.getGranularity()).isEqualTo(TrendGranularity.ONE_MINUTE);
            assertThat(response.getDataPoints()).hasSize(1);
            assertThat(response.getTotalCount()).isEqualTo(1);
            assertThat(response.isHasMore()).isFalse();
        }

        @Test
        @DisplayName("should return empty response when no data found")
        void shouldReturnEmptyResponseWhenNoData() {
            TrendRequest request = TrendRequest.builder()
                    .metricName("nonexistent")
                    .granularity(TrendGranularity.DAILY)
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .limit(100)
                    .build();

            Page<TrendEntity> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 100), 0);
            when(trendRepository.findByMetricDimensionAndTimeRange(
                    any(), any(), any(), any(), any(), any(PageRequest.class)))
                    .thenReturn(emptyPage);

            TrendResponse response = trendService.queryTrends(request);

            assertThat(response.getDataPoints()).isEmpty();
            assertThat(response.getTotalCount()).isZero();
        }

        @Test
        @DisplayName("should indicate hasMore when results exceed page size")
        void shouldIndicateHasMoreWhenPageIsFull() {
            TrendRequest request = TrendRequest.builder()
                    .metricName("cpu_usage")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .startTime(Instant.now().minusSeconds(3600))
                    .endTime(Instant.now())
                    .limit(1)
                    .build();

            Page<TrendEntity> page = new PageImpl<>(List.of(sampleEntity), PageRequest.of(0, 1), 5);
            when(trendRepository.findByMetricDimensionAndTimeRange(
                    any(), any(), any(), any(), any(), any(PageRequest.class)))
                    .thenReturn(page);

            TrendResponse response = trendService.queryTrends(request);

            assertThat(response.isHasMore()).isTrue();
            assertThat(response.getTotalCount()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("ingest")
    class Ingest {

        @Test
        @DisplayName("should save and return trend data")
        void shouldSaveAndReturnTrendData() {
            when(trendRepository.save(any(TrendEntity.class))).thenReturn(sampleEntity);

            TrendData result = trendService.ingest(sampleData);

            assertThat(result.getMetricName()).isEqualTo("cpu_usage");
            assertThat(result.getMetricValue()).isEqualByComparingTo(new BigDecimal("75.5000"));
            assertThat(result.getDimension()).isEqualTo("server-1");
            verify(trendRepository).save(any(TrendEntity.class));
        }

        @Test
        @DisplayName("should map all fields correctly from model to entity")
        void shouldMapFieldsCorrectly() {
            when(trendRepository.save(any(TrendEntity.class))).thenReturn(sampleEntity);

            trendService.ingest(sampleData);

            ArgumentCaptor<TrendEntity> captor = ArgumentCaptor.forClass(TrendEntity.class);
            verify(trendRepository).save(captor.capture());

            TrendEntity saved = captor.getValue();
            assertThat(saved.getMetricName()).isEqualTo(sampleData.getMetricName());
            assertThat(saved.getMetricValue()).isEqualByComparingTo(sampleData.getMetricValue());
            assertThat(saved.getDimension()).isEqualTo(sampleData.getDimension());
            assertThat(saved.getGranularity()).isEqualTo(sampleData.getGranularity());
        }
    }

    @Nested
    @DisplayName("ingestBatch")
    class IngestBatch {

        @Test
        @DisplayName("should save all data points in batch")
        void shouldSaveAllDataPointsInBatch() {
            TrendData data2 = TrendData.builder()
                    .metricName("memory_usage")
                    .metricValue(new BigDecimal("60.0000"))
                    .dimension("server-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(Instant.now())
                    .source("test")
                    .build();

            TrendEntity entity2 = TrendEntity.builder()
                    .id(2L)
                    .metricName("memory_usage")
                    .metricValue(new BigDecimal("60.0000"))
                    .dimension("server-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(Instant.now())
                    .source("test")
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();

            when(trendRepository.saveAll(anyList())).thenReturn(List.of(sampleEntity, entity2));

            List<TrendData> result = trendService.ingestBatch(List.of(sampleData, data2));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getMetricName()).isEqualTo("cpu_usage");
            assertThat(result.get(1).getMetricName()).isEqualTo("memory_usage");
            verify(trendRepository).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("getLatest")
    class GetLatest {

        @Test
        @DisplayName("should return latest trend data when found")
        void shouldReturnLatestWhenFound() {
            when(trendRepository.findFirstByMetricNameAndDimensionOrderByTimestampDesc("cpu_usage", "server-1"))
                    .thenReturn(Optional.of(sampleEntity));

            TrendData result = trendService.getLatest("cpu_usage", "server-1");

            assertThat(result.getMetricName()).isEqualTo("cpu_usage");
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw TrendNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(trendRepository.findFirstByMetricNameAndDimensionOrderByTimestampDesc("unknown", null))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> trendService.getLatest("unknown", null))
                    .isInstanceOf(TrendNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }
}
