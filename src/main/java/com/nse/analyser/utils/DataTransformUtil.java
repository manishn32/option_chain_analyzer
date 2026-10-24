package com.nse.analyser.utils;

import com.nse.analyser.dtos.nseResponse.Range;
import com.nse.analyser.services.jobs.automated.DailyJobs;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

public class DataTransformUtil {
    public static List<Double> convertToIntegerList(List<String> strikePrices) {
        return strikePrices.stream().map(Double::parseDouble).toList();
    }

    public static Range calculatePriceRange(
            double spot,
            double iv,
            long dte) {

        double move =
                spot
                        * (iv / 100.0)
                        * Math.sqrt(dte / 365.0);

        return Range.builder()
                .expectedMove(NumberUtils.roundTo2Decimals(move))
                .lowerRange(NumberUtils.roundTo2Decimals(spot - move))
                .upperRange(NumberUtils.roundTo2Decimals(spot + move))
                .build();
    }

    public static Range calculatePriceRangeStraddle(double spot, double move){
        return Range.builder()
                .expectedMove(NumberUtils.roundTo2Decimals(move))
                .lowerRange(NumberUtils.roundTo2Decimals(spot - move))
                .upperRange(NumberUtils.roundTo2Decimals(spot + move))
                .build();
    }

    public static long calculateTradingDaysToExpiry(
            long todayEpochSec,
            long expiryEpochSec,
            String exchange) {
        Set<LocalDate> holidays = DailyJobs.holidayDates;
        LocalDate today = Instant.ofEpochSecond(todayEpochSec)
                .atZone(IST)
                .toLocalDate();

        LocalDate expiry = Instant.ofEpochSecond(expiryEpochSec)
                .atZone(IST)
                .toLocalDate();

        if (!"NSE".equalsIgnoreCase(exchange)) {
            return ChronoUnit.DAYS.between(today, expiry);
        }

        long tradingDays = 0;

        for (LocalDate date = today.plusDays(1);
             !date.isAfter(expiry);
             date = date.plusDays(1)) {

            DayOfWeek day = date.getDayOfWeek();

            boolean weekend =
                    day == DayOfWeek.SATURDAY
                            || day == DayOfWeek.SUNDAY;

            boolean holiday = holidays.contains(date);

            if (!weekend && !holiday) {
                tradingDays++;
            }
        }

        return tradingDays;
    }

    public static long calculateDaysToExpiry(
            long todayEpochSec,
            long expiryEpochSec,
            String exchange) {
        LocalDate today = Instant.ofEpochSecond(todayEpochSec)
                .atZone(IST)
                .toLocalDate();

        LocalDate expiry = Instant.ofEpochSecond(expiryEpochSec)
                .atZone(IST)
                .toLocalDate();

        if (!"NSE".equalsIgnoreCase(exchange)) {
            return ChronoUnit.DAYS.between(today, expiry);
        }

        long tradingDays = 0;

        for (LocalDate date = today.plusDays(1);
             !date.isAfter(expiry);
             date = date.plusDays(1)) {

            DayOfWeek day = date.getDayOfWeek();

            boolean weekend =
                    day == DayOfWeek.SATURDAY
                            || day == DayOfWeek.SUNDAY;

            if (!weekend) {
                tradingDays++;
            }
        }

        return tradingDays;
    }
}
