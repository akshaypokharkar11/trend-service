package com.example.trendservice.web.controller;

import com.example.trendservice.api.exception.TrendNotFoundException;
import com.example.trendservice.api.model.TrendData;
import com.example.trendservice.api.model.TrendGranularity;
import com.example.trendservice.api.model.TrendResponse;
import com.example.trendservice.api.service.TrendService;
import com.example.trendservice.web.config.SecurityConfig;
import com.example.trendservice.web.handler.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TrendController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("TrendController Unit Tests")
class TrendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrendService trendService;

    @Nested
    @DisplayName("GET /api/v1/trends")
    class QueryTrends {

        @Test
        @DisplayName("should return 200 with trend data")
        void shouldReturnTrendData() throws Exception {
            Instant now = Instant.now();
            TrendData dataPoint = TrendData.builder()
                    .id(1L)
                    .metricName("cpu_usage")
                    .metricValue(new BigDecimal("75.5000"))
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(now)
                    .build();

            TrendResponse response = TrendResponse.builder()
                    .metricName("cpu_usage")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .dataPoints(List.of(dataPoint))
                    .totalCount(1)
                    .hasMore(false)
                    .build();

            when(trendService.queryTrends(any())).thenReturn(response);

            mockMvc.perform(get("/api/v1/trends")
                            .param("metricName", "cpu_usage")
                            .param("granularity", "ONE_MINUTE")
                            .param("startTime", now.minusSeconds(3600).toString())
                            .param("endTime", now.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metric_name").value("cpu_usage"))
                    .andExpect(jsonPath("$.data_points", hasSize(1)))
                    .andExpect(jsonPath("$.total_count").value(1));
        }

        @Test
        @DisplayName("should return 400 when required params missing")
        void shouldReturn400WhenParamsMissing() throws Exception {
            mockMvc.perform(get("/api/v1/trends")
                            .param("metricName", "cpu_usage"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trends")
    class IngestTrend {

        @Test
        @DisplayName("should return 201 on successful ingest")
        void shouldReturn201OnIngest() throws Exception {
            Instant now = Instant.now();
            TrendData input = TrendData.builder()
                    .metricName("cpu_usage")
                    .metricValue(new BigDecimal("80.0"))
                    .granularity(TrendGranularity.TEN_SECONDS)
                    .timestamp(now)
                    .source("api")
                    .build();

            TrendData saved = TrendData.builder()
                    .id(1L)
                    .metricName("cpu_usage")
                    .metricValue(new BigDecimal("80.0"))
                    .granularity(TrendGranularity.TEN_SECONDS)
                    .timestamp(now)
                    .source("api")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(trendService.ingest(any(TrendData.class))).thenReturn(saved);

            mockMvc.perform(post("/api/v1/trends")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(input)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.metric_name").value("cpu_usage"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/trends/batch")
    class IngestBatch {

        @Test
        @DisplayName("should return 201 for batch ingest")
        void shouldReturn201ForBatch() throws Exception {
            Instant now = Instant.now();
            TrendData d1 = TrendData.builder()
                    .metricName("cpu_usage").metricValue(new BigDecimal("70.0"))
                    .granularity(TrendGranularity.ONE_MINUTE).timestamp(now).build();
            TrendData d2 = TrendData.builder()
                    .metricName("memory_usage").metricValue(new BigDecimal("55.0"))
                    .granularity(TrendGranularity.ONE_MINUTE).timestamp(now).build();

            TrendData saved1 = TrendData.builder().id(1L).metricName("cpu_usage")
                    .metricValue(new BigDecimal("70.0")).build();
            TrendData saved2 = TrendData.builder().id(2L).metricName("memory_usage")
                    .metricValue(new BigDecimal("55.0")).build();

            when(trendService.ingestBatch(any())).thenReturn(List.of(saved1, saved2));

            mockMvc.perform(post("/api/v1/trends/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(List.of(d1, d2))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/trends/latest/{metricName}")
    class GetLatest {

        @Test
        @DisplayName("should return 200 with latest data point")
        void shouldReturnLatest() throws Exception {
            TrendData latest = TrendData.builder()
                    .id(5L)
                    .metricName("cpu_usage")
                    .metricValue(new BigDecimal("92.3"))
                    .dimension("server-1")
                    .granularity(TrendGranularity.ONE_MINUTE)
                    .timestamp(Instant.now())
                    .build();

            when(trendService.getLatest("cpu_usage", null)).thenReturn(latest);

            mockMvc.perform(get("/api/v1/trends/latest/cpu_usage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metric_name").value("cpu_usage"))
                    .andExpect(jsonPath("$.metric_value").value(92.3));
        }

        @Test
        @DisplayName("should return 404 when metric not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(trendService.getLatest("nonexistent", null))
                    .thenThrow(new TrendNotFoundException("nonexistent", null));

            mockMvc.perform(get("/api/v1/trends/latest/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }
    }
}
