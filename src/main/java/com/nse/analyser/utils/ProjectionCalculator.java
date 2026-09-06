package com.nse.analyser.utils;

import com.nse.analyser.enums.MarketRegime;
import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.OptionWall;
import com.nse.analyser.models.chainSnapShot.OptionLeg;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ProjectionCalculator {

    private ProjectionCalculator() {
    }

    public static double bullScore(List<OptionLeg> snapshots) {
        return snapshots.stream()
                .mapToDouble(snapshot -> {
                    OptionActivity activity = snapshot.getOptionActivity();
                    if (activity == null) {
                        return 0;
                    }
                    return switch (activity) {
                        case LONG_BUILDUP -> snapshot.getChangeInOpenInterest();
                        case SHORT_COVERING -> snapshot.getChangeInOpenInterest() * 0.75;
                        default -> 0;
                    };
                })
                .sum();
    }

    public static double bearScore(List<OptionLeg> snapshots) {
        return snapshots.stream()
                .mapToDouble(snapshot -> {
                    OptionActivity activity = snapshot.getOptionActivity();
                    if (activity == null) {
                        return 0;
                    }
                    return switch (activity) {
                        case SHORT_BUILDUP -> snapshot.getChangeInOpenInterest();
                        case LONG_UNWINDING -> Math.abs(snapshot.getChangeInOpenInterest());
                        default -> 0;
                    };
                })
                .sum();
    }

    public static double findSupport(List<OptionLeg> snapshots) {
        return snapshots.stream()
                .filter(s -> s.getOptionType() == OptionType.PE)
                .collect(Collectors.groupingBy(OptionLeg::getStrikePrice))
                .entrySet()
                .stream()
                .max(Comparator.comparingDouble(entry -> {
                    double oi = entry.getValue().stream().mapToDouble(OptionLeg::getOpenInterest).sum();
                    double changeOi = entry.getValue().stream().mapToDouble(OptionLeg::getChangeInOpenInterest).sum();
                    return oi * 0.7 + changeOi * 0.3;
                }))
                .map(Map.Entry::getKey)
                .orElse(0.0);
    }

    public static double findResistance(List<OptionLeg> snapshots) {
        return snapshots.stream()
                .filter(s -> s.getOptionType() == OptionType.CE)
                .collect(Collectors.groupingBy(OptionLeg::getStrikePrice))
                .entrySet()
                .stream()
                .max(Comparator.comparingDouble(entry -> {
                    double oi = entry.getValue().stream().mapToDouble(OptionLeg::getOpenInterest).sum();
                    double changeOi = entry.getValue().stream().mapToDouble(OptionLeg::getChangeInOpenInterest).sum();
                    return oi * 0.7 + changeOi * 0.3;
                }))
                .map(Map.Entry::getKey)
                .orElse(0.0);
    }

    public static double strength(List<OptionLeg> snapshots, double strike, OptionType type) {
        double score = snapshots.stream()
                .filter(s -> strike == s.getStrikePrice())
                .filter(s -> s.getOptionType() == type)
                .mapToDouble(snapshot -> snapshot.getOpenInterest() * 0.5
                        + snapshot.getChangeInOpenInterest() * 0.3
                        + snapshot.getTotalTradedVolume() * 0.2)
                .sum();

        return Math.min(score / 1000.0, 100);
    }

    public static double dealerPressure(List<OptionLeg> snapshots) {
        double buyPressure = snapshots.stream().mapToDouble(OptionLeg::getBidQuantity).sum();
        double sellPressure = snapshots.stream().mapToDouble(OptionLeg::getAskQuantity).sum();

        if ((buyPressure + sellPressure) == 0) {
            return 0;
        }

        return ((buyPressure - sellPressure) / (buyPressure + sellPressure)) * 100;
    }

    public static double breakoutProbability(double dealerPressure, double bullScore, double bearScore) {
        double strength = Math.abs(bullScore - bearScore);
        double probability = strength * 0.6 + Math.abs(dealerPressure) * 0.4;
        return Math.min(probability, 100);
    }

    public static double smartMoneyFlow(List<OptionLeg> snapshots) {
        return snapshots.stream()
                .mapToDouble(snapshot -> {
                    OptionActivity activity = snapshot.getOptionActivity();
                    if (activity == null) {
                        return 0;
                    }

                    double weight = snapshot.getOpenInterest() * snapshot.getTotalTradedVolume();

                    return switch (activity) {
                        case LONG_BUILDUP -> weight;
                        case SHORT_COVERING -> weight * 0.75;
                        case SHORT_BUILDUP -> -weight;
                        case LONG_UNWINDING -> -(weight * 0.75);
                        default -> 0;
                    };
                })
                .sum();
    }

    public static double ivBias(List<OptionLeg> snapshots) {
        double callIv = snapshots.stream()
                .filter(x -> x.getOptionType() == OptionType.CE)
                .mapToDouble(OptionLeg::getImpliedVolatility)
                .average()
                .orElse(0);

        double putIv = snapshots.stream()
                .filter(x -> x.getOptionType() == OptionType.PE)
                .mapToDouble(OptionLeg::getImpliedVolatility)
                .average()
                .orElse(0);

        return putIv - callIv;
    }

    public static OptionWall strongestCallWall(List<OptionLeg> snapshots) {
        return wallByType(snapshots, OptionType.CE, "CALL");
    }

    public static OptionWall strongestPutWall(List<OptionLeg> snapshots) {
        return wallByType(snapshots, OptionType.PE, "PUT");
    }

    public static double calculateMigrationScore(
            double currentSupport,
            double previousSupport,
            double currentResistance,
            double previousResistance) {

        double score = 0;

        if (currentSupport > previousSupport) {
            score += 50;
        }
        if (currentResistance > previousResistance) {
            score += 50;
        }
        if (currentSupport < previousSupport) {
            score -= 50;
        }
        if (currentResistance < previousResistance) {
            score -= 50;
        }

        return score;
    }

    public static double computeProjectionScore(
            double bullScore,
            double bearScore,
            double supportStrength,
            double resistanceStrength,
            double smartMoneyFlow,
            double migrationScore,
            double dealerPressure,
            double ivBias) {

        double score = ((bullScore - bearScore) * 0.25)
                + (supportStrength * 0.20)
                - (resistanceStrength * 0.15)
                + (smartMoneyFlow * 0.15)
                + (migrationScore * 0.10)
                + (dealerPressure * 0.10)
                + (ivBias * 0.05);

        return Math.max(-100, Math.min(100, score));
    }

    public static MarketRegime regime(double projectionScore, double breakoutProbability) {
        if (breakoutProbability > 80) {
            return MarketRegime.BREAKOUT;
        }
        if (projectionScore >= 75) {
            return MarketRegime.STRONG_BULLISH;
        }
        if (projectionScore >= 40) {
            return MarketRegime.BULLISH;
        }
        if (projectionScore <= -75) {
            return MarketRegime.STRONG_BEARISH;
        }
        if (projectionScore <= -40) {
            return MarketRegime.BEARISH;
        }
        return MarketRegime.NEUTRAL;
    }

    public static String generateCommentary(
            double support,
            double resistance,
            double score,
            MarketRegime regime,
            double breakoutProbability) {

        return """
                Support: %.2f
                Resistance: %.2f
                Projection Score: %.2f
                Breakout Probability: %.2f%%
                Current Regime: %s
                """.formatted(support, resistance, score, breakoutProbability, regime.name());
    }

    private static OptionWall wallByType(List<OptionLeg> snapshots, OptionType optionType, String wallType) {
        return snapshots.stream()
                .filter(x -> x.getOptionType() == optionType)
                .collect(Collectors.groupingBy(OptionLeg::getStrikePrice))
                .entrySet()
                .stream()
                .map(entry -> {
                    double score = entry.getValue()
                            .stream()
                            .mapToDouble(x -> x.getOpenInterest() + x.getChangeInOpenInterest() + x.getTotalTradedVolume())
                            .sum();

                    return OptionWall.builder()
                            .strike(entry.getKey())
                            .score(score)
                            .type(wallType)
                            .build();
                })
                .max(Comparator.comparing(OptionWall::getScore))
                .orElse(null);
    }
}

