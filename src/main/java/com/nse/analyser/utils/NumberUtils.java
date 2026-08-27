package com.nse.analyser.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class NumberUtils {
//    public static double roundTo3Decimals(long value) {
//        return BigDecimal.valueOf(value)
//                .setScale(3, RoundingMode.HALF_UP)
//                .doubleValue();
//    }

    public static double roundTo3Decimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double roundTo2Decimals(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    // Add these helpers inside OptionMetricsService

    private static double safeDivide(double numerator, double denominator) {
        if (denominator == 0.0) {
            return 0.0;
        }
        return (numerator / denominator) * 100;
    }

    public static double safeDivideAndRound(double numerator, double denominator, int floatingDigits) {
        double value = safeDivide(numerator, denominator);
        return switch (floatingDigits) {
            case 2 -> NumberUtils.roundTo2Decimals(value);
            case 3 -> NumberUtils.roundTo3Decimals(value);
            default -> value;
        };
    }

    public static double safeDivide(long numerator, long denominator) {
        return denominator == 0
                ? 0.0
                : roundTo2Decimals((double) numerator / denominator);
    }
}
