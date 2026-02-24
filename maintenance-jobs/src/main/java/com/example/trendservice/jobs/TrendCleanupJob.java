package com.example.trendservice.jobs;

import com.example.trendservice.impl.repository.TrendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled job to clean up old trend data.
 * Uses ShedLock for distributed locking to prevent concurrent execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendCleanupJob {

    private final TrendRepository trendRepository;

    @Value("${trend.cleanup.retention-days:30}")
    private int retentionDays;

    /**
     * Runs daily at 2 AM to delete trend data older than the retention period.
     */
    @Scheduled(cron = "${trend.cleanup.cron:0 0 2 * * ?}")
    @SchedulerLock(name = "trendCleanupJob", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    @Transactional
    public void cleanupOldTrendData() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        log.info("Starting trend data cleanup. Deleting records older than {} (retention={} days)",
                cutoff, retentionDays);

        long count = trendRepository.countByTimestampBefore(cutoff);
        if (count > 0) {
            trendRepository.deleteByTimestampBefore(cutoff);
            log.info("Deleted {} old trend data records", count);
        } else {
            log.info("No old trend data records to clean up");
        }
    }
}
