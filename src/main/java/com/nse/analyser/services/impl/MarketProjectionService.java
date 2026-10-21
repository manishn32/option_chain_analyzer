package com.nse.analyser.services.impl;

import com.nse.analyser.dtos.rest.AccuracySummary;
import com.nse.analyser.dtos.rest.ProjectionDashboard;
import com.nse.analyser.dtos.rest.TimeframeProjection;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import com.nse.analyser.enums.MarketRegime;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.MarketProjection;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import com.nse.analyser.repositories.NseSnapshotRepository;
import com.nse.analyser.utils.ProjectionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketProjectionService {

    private static final Map<String, String> SNAPSHOT_TABLES = Map.of(
            "1m", "nse_snapshot_1m",
            "3m", "nse_snapshot_3m",
            "5m", "nse_snapshot_5m",
            "15m", "nse_snapshot_15m",
            "30m", "nse_snapshot_30m",
            "1h", "nse_snapshot_1h"
    );

    private static final Map<String, String> PROJECTION_TABLES = Map.of(
            "1m", "market_projection_1m",
            "3m", "market_projection_3m",
            "5m", "market_projection_5m",
            "15m", "market_projection_15m",
            "30m", "market_projection_30m",
            "1h", "market_projection_1h"
    );

    private final NseSnapshotRepository repository;

    @CircuitBreaker(name = "projectionService", fallbackMethod = "calculateAndStoreFallback")
    @Retry(name = "projectionService")
    public MarketProjection calculateAndStore(
            String timeframe,
            String underlying,
            LocalDate expiryDate,
            int limit) {

        String snapshotTable = getRequiredTable(SNAPSHOT_TABLES, timeframe);
        String projectionTable = getRequiredTable(PROJECTION_TABLES, timeframe);

        List<OptionLeg> snapshots = repository.findLatestForProjection(snapshotTable, underlying, expiryDate, limit);
        if (snapshots == null || snapshots.isEmpty()) {
            return null;
        }

        double bull = ProjectionCalculator.bullScore(snapshots);
        double bear = ProjectionCalculator.bearScore(snapshots);

        double support = ProjectionCalculator.findSupport(snapshots);
        double resistance = ProjectionCalculator.findResistance(snapshots);

        double supportStrength = ProjectionCalculator.strength(snapshots, support, OptionType.PE);
        double resistanceStrength = ProjectionCalculator.strength(snapshots, resistance, OptionType.CE);

        double dealerPressure = ProjectionCalculator.dealerPressure(snapshots);
        double breakoutProbability = ProjectionCalculator.breakoutProbability(dealerPressure, bull, bear);
        double smartMoneyFlow = ProjectionCalculator.smartMoneyFlow(snapshots);
        double ivBias = ProjectionCalculator.ivBias(snapshots);

        MarketProjection previous = repository.findLatestProjection(projectionTable, underlying, expiryDate);
        double migrationScore = previous == null
                ? 0
                : ProjectionCalculator.calculateMigrationScore(
                        support,
                        previous.getSupportStrike(),
                        resistance,
                        previous.getResistanceStrike()
                );

        double score = ProjectionCalculator.computeProjectionScore(
                bull,
                bear,
                supportStrength,
                resistanceStrength,
                smartMoneyFlow,
                migrationScore,
                dealerPressure,
                ivBias
        );

        MarketRegime regime = ProjectionCalculator.regime(score, breakoutProbability);

//        double spotPrice = snapshots.stream()
////                .mapToDouble(OptionLeg::getUnderlyingValue)
////                .average()
//                .orElse(0);

        MarketProjection projection = MarketProjection.builder()
                .timestamp(LocalDateTime.now())
                .timeframe(timeframe)
                .underlying(underlying)
                .expiryDate(expiryDate)
                .spotPrice(0.0)
                .supportStrike(support)
                .supportStrength(supportStrength)
                .resistanceStrike(resistance)
                .resistanceStrength(resistanceStrength)
                .bullScore(bull)
                .bearScore(bear)
                .smartMoneyFlow(smartMoneyFlow)
                .dealerPressure(dealerPressure)
                .breakoutProbability(breakoutProbability)
                .strikeMigrationScore(migrationScore)
                .ivBias(ivBias)
                .marketProjectionScore(score)
                .marketRegime(regime.name())
                .commentary(ProjectionCalculator.generateCommentary(
                        support,
                        resistance,
                        score,
                        regime,
                        breakoutProbability
                ))
                .build();

        repository.saveProjection(projectionTable, projection);
        repository.saveStrikeMigration(timeframe, underlying, expiryDate, support, resistance);

        return projection;
    }

    @RateLimiter(name = "projectionLatest")
    public MarketProjection latest(String timeframe, String underlying, LocalDate expiryDate) {
        String projectionTable = getRequiredTable(PROJECTION_TABLES, timeframe);
        return repository.findLatestProjection(projectionTable, underlying, expiryDate);
    }

    @CircuitBreaker(name = "projectionService", fallbackMethod = "dashboardFallback")
    @RateLimiter(name = "projectionDashboard")
    public ProjectionDashboard dashboard(String underlying, LocalDate expiryDate) {
        return ProjectionDashboard.builder()
                .oneMinute(toTimeframeProjection(latest("1m", underlying, expiryDate)))
                .threeMinute(toTimeframeProjection(latest("3m", underlying, expiryDate)))
                .fiveMinute(toTimeframeProjection(latest("5m", underlying, expiryDate)))
                .fifteenMinute(toTimeframeProjection(latest("15m", underlying, expiryDate)))
                .thirtyMinute(toTimeframeProjection(latest("30m", underlying, expiryDate)))
                .oneHour(toTimeframeProjection(latest("1h", underlying, expiryDate)))
                .build();
    }

    @CircuitBreaker(name = "projectionService", fallbackMethod = "trackAccuracyFallback")
    @RateLimiter(name = "projectionAccuracy")
    public AccuracySummary trackAccuracy(String timeframe, String underlying, LocalDate expiryDate) {
        String projectionTable = getRequiredTable(PROJECTION_TABLES, timeframe);

        List<MarketProjection> rows = repository.findRecentProjections(
                projectionTable,
                underlying,
                expiryDate,
                150
        );

        if (rows == null || rows.size() < 2) {
            return AccuracySummary.builder()
                    .timeframe(timeframe)
                    .sampleSize(0)
                    .accuracyPercent(0)
                    .build();
        }

        List<MarketProjection> ordered = new ArrayList<>(rows);
        ordered.sort(Comparator.comparing(MarketProjection::getTimestamp));

        long successCount = 0;
        long sampleCount = 0;

        for (int i = 0; i < ordered.size() - 1; i++) {
            MarketProjection p = ordered.get(i);
            MarketProjection next = ordered.get(i + 1);

            double predicted = p.getMarketProjectionScore();
            double actualMove = next.getSpotPrice() - p.getSpotPrice();

            if (predicted == 0 || actualMove == 0) {
                continue;
            }

            boolean success = Math.signum(predicted) == Math.signum(actualMove);
            sampleCount++;
            if (success) {
                successCount++;
            }

            repository.saveProjectionAccuracy(
                    timeframe,
                    underlying,
                    expiryDate,
                    p.getMarketRegime(),
                    p.getMarketProjectionScore(),
                    actualMove,
                    success
            );
        }

        double accuracy = sampleCount == 0 ? 0 : (successCount * 100.0) / sampleCount;

        return AccuracySummary.builder()
                .timeframe(timeframe)
                .sampleSize(sampleCount)
                .accuracyPercent(accuracy)
                .build();
    }

    public List<AccuracySummary> trackAccuracyAll(String underlying, LocalDate expiryDate) {
        return List.of(
                trackAccuracy("1m", underlying, expiryDate),
                trackAccuracy("3m", underlying, expiryDate),
                trackAccuracy("5m", underlying, expiryDate),
                trackAccuracy("15m", underlying, expiryDate),
                trackAccuracy("30m", underlying, expiryDate),
                trackAccuracy("1h", underlying, expiryDate)
        );
    }

    private String getRequiredTable(Map<String, String> tableMap, String timeframe) {
        String table = tableMap.get(timeframe);
        if (table == null) {
            throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
        }
        return table;
    }

    private TimeframeProjection toTimeframeProjection(MarketProjection p) {
        if (p == null) {
            return null;
        }

        return TimeframeProjection.builder()
                .timeframe(p.getTimeframe())
                .score(p.getMarketProjectionScore())
                .regime(p.getMarketRegime())
                .support(p.getSupportStrike())
                .resistance(p.getResistanceStrike())
                .build();
    }

    // ===== Fallback methods for Circuit Breaker =====

    public MarketProjection calculateAndStoreFallback(
            String timeframe,
            String underlying,
            LocalDate expiryDate,
            int limit,
            Throwable ex) {
        return MarketProjection.builder()
                .timeframe(timeframe)
                .underlying(underlying)
                .expiryDate(expiryDate)
                .marketProjectionScore(0)
                .marketRegime("SERVICE_UNAVAILABLE")
                .commentary("Service temporarily unavailable: " + ex.getMessage())
                .build();
    }

    public ProjectionDashboard dashboardFallback(
            String underlying,
            LocalDate expiryDate,
            Throwable ex) {
        return ProjectionDashboard.builder()
                .oneMinute(null)
                .threeMinute(null)
                .fiveMinute(null)
                .fifteenMinute(null)
                .thirtyMinute(null)
                .oneHour(null)
                .build();
    }

    public AccuracySummary trackAccuracyFallback(
            String timeframe,
            String underlying,
            LocalDate expiryDate,
            Throwable ex) {
        return AccuracySummary.builder()
                .timeframe(timeframe)
                .sampleSize(0)
                .accuracyPercent(0)
                .build();
    }
}


