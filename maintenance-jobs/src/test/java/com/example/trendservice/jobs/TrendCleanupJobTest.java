package com.example.trendservice.jobs;

import com.example.trendservice.impl.repository.TrendRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendCleanupJob Unit Tests")
class TrendCleanupJobTest {

    @Mock
    private TrendRepository trendRepository;

    @InjectMocks
    private TrendCleanupJob cleanupJob;

    @Test
    @DisplayName("should delete old records when they exist")
    void shouldDeleteOldRecords() {
        ReflectionTestUtils.setField(cleanupJob, "retentionDays", 30);
        when(trendRepository.countByTimestampBefore(any(Instant.class))).thenReturn(100L);

        cleanupJob.cleanupOldTrendData();

        verify(trendRepository).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    @DisplayName("should skip deletion when no old records found")
    void shouldSkipDeletionWhenNoRecords() {
        ReflectionTestUtils.setField(cleanupJob, "retentionDays", 30);
        when(trendRepository.countByTimestampBefore(any(Instant.class))).thenReturn(0L);

        cleanupJob.cleanupOldTrendData();

        verify(trendRepository, never()).deleteByTimestampBefore(any(Instant.class));
    }

    @Test
    @DisplayName("should use correct cutoff date based on retention days")
    void shouldUseCorrectCutoffDate() {
        ReflectionTestUtils.setField(cleanupJob, "retentionDays", 7);
        when(trendRepository.countByTimestampBefore(any(Instant.class))).thenReturn(0L);

        Instant before = Instant.now();
        cleanupJob.cleanupOldTrendData();

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(trendRepository).countByTimestampBefore(captor.capture());

        Instant cutoff = captor.getValue();
        Instant expected = before.minus(7, ChronoUnit.DAYS);
        // Allow 5 seconds tolerance for test execution time
        assertThat(cutoff).isBetween(expected.minusSeconds(5), expected.plusSeconds(5));
    }
}
