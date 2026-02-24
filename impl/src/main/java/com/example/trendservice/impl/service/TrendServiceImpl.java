package com.example.trendservice.impl.service;

import com.example.trendservice.api.exception.TrendNotFoundException;
import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendRequest;
import com.example.trendservice.api.model.TrendResponse;
import com.example.trendservice.api.service.TrendService;
import com.example.trendservice.impl.entity.TrendEntity;
import com.example.trendservice.impl.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of TrendService with caching and database persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrendServiceImpl implements TrendService {

    private final TrendRepository trendRepository;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "trends", key = "#request.metricName + ':' + #request.granularity + ':' + #request.startTime + ':' + #request.endTime")
    public TrendResponse queryTrends(TrendRequest request) {
        log.info("Querying trends for metric={}, granularity={}, range=[{}, {}]",
                request.getMetricName(), request.getGranularity(),
                request.getStartTime(), request.getEndTime());

        PageRequest pageRequest = PageRequest.of(0, request.getLimit());
        Page<TrendEntity> page = trendRepository.findByMetricDimensionAndTimeRange(
                request.getMetricName(),
                request.getDimension(),
                request.getGranularity(),
                request.getStartTime(),
                request.getEndTime(),
                pageRequest);

        List<TrendData> dataPoints = page.getContent().stream()
                .map(this::toModel)
                .toList();

        return TrendResponse.builder()
                .metricName(request.getMetricName())
                .granularity(request.getGranularity())
                .dataPoints(dataPoints)
                .totalCount(page.getTotalElements())
                .hasMore(page.hasNext())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "trends", allEntries = true)
    public TrendData ingest(TrendData data) {
        log.debug("Ingesting trend data point: metric={}, value={}", data.getMetricName(), data.getMetricValue());
        TrendEntity entity = toEntity(data);
        TrendEntity saved = trendRepository.save(entity);
        return toModel(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "trends", allEntries = true)
    public List<TrendData> ingestBatch(List<TrendData> dataPoints) {
        log.info("Batch ingesting {} trend data points", dataPoints.size());
        List<TrendEntity> entities = dataPoints.stream()
                .map(this::toEntity)
                .toList();
        List<TrendEntity> saved = trendRepository.saveAll(entities);
        return saved.stream()
                .map(this::toModel)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrendData getLatest(String metricName, String dimension) {
        log.debug("Getting latest trend for metric={}, dimension={}", metricName, dimension);
        return trendRepository
                .findFirstByMetricNameAndDimensionOrderByTimestampDesc(metricName, dimension)
                .map(this::toModel)
                .orElseThrow(() -> new TrendNotFoundException(metricName, dimension));
    }

    private TrendData toModel(TrendEntity entity) {
        return TrendData.builder()
                .id(entity.getId())
                .metricName(entity.getMetricName())
                .metricValue(entity.getMetricValue())
                .dimension(entity.getDimension())
                .granularity(entity.getGranularity())
                .timestamp(entity.getTimestamp())
                .source(entity.getSource())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private TrendEntity toEntity(TrendData data) {
        return TrendEntity.builder()
                .metricName(data.getMetricName())
                .metricValue(data.getMetricValue())
                .dimension(data.getDimension())
                .granularity(data.getGranularity())
                .timestamp(data.getTimestamp())
                .source(data.getSource())
                .build();
    }
}
