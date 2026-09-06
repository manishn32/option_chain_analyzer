package com.nse.analyser.services.jobs;

import com.nse.analyser.dtos.rest.AccuracySummary;
import com.nse.analyser.services.impl.MarketProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "projection.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ProjectionAccuracyJob {

    private final MarketProjectionService marketProjectionService;

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.accuracy:45 */10 * * * *}")
    public void runAccuracy() {
        try {
            List<AccuracySummary> summaries = marketProjectionService.trackAccuracyAll(
                    "NIFTY",
                    LocalDate.now().plusWeeks(1)
            );
            summaries.forEach(s -> log.info(
                    "Accuracy [{}] samples={} accuracy={}%%",
                    s.getTimeframe(),
                    s.getSampleSize(),
                    String.format("%.2f", s.getAccuracyPercent())
            ));
        } catch (Exception ex) {
            log.error("Projection accuracy job failed", ex);
        }
    }
}


