package com.nse.analyser.services.jobs;

import com.nse.analyser.CustomCache;
import com.nse.analyser.models.MarketProjection;
import com.nse.analyser.services.impl.MarketProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "projection.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class ProjectionJob {

    private final MarketProjectionService service;

    // Controlled by cron from application properties.
    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.1m:5 * * * * *}")
    public void run1m() {
        runFor("1m");
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.3m:10 */3 * * * * *}")
    public void run3m() {
        runFor("3m");
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.5m:15 */5 * * * * *}")
    public void run5m() {
        runFor("5m");
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.15m:20 */15 * * * * *}")
    public void run15m() {
        runFor("15m");
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.30m:25 */30 * * * * *}")
    public void run30m() {
        runFor("30m");
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "${projection.jobs.cron.1h:30 0 * * * *}")
    public void run1h() {
        runFor("1h");
    }
    private void runFor(String timeframe) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

        CustomCache.cache.forEach((symbol, expires) -> {

            expires.forEach(expiry -> {

                try {

                    MarketProjection projection = service.calculateAndStore(
                            timeframe,
                            symbol,
                            LocalDate.parse(expiry, formatter),
                            1200
                    );

                    if (projection != null) {
                        log.info("Projection [{}] {} {} score={} regime={}",
                                timeframe,
                                symbol,
                                expiry,
                                projection.getMarketProjectionScore(),
                                projection.getMarketRegime());
                    }

                } catch (Exception ex) {
                    log.error("Projection failed for {} {} {}",
                            timeframe,
                            symbol,
                            expiry,
                            ex);
                }
            });
        });
    }
}


