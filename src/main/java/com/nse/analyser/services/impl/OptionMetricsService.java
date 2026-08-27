package com.nse.analyser.services.impl;


import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.enums.OptionType;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@AllArgsConstructor
public class OptionMetricsService {

    public static OptionRank determineRank(
            OptionType optionType,
            Double strikePrice,
            Double atmStrike) {

        if (Objects.equals(strikePrice, atmStrike)) {
            return OptionRank.ATM;
        }

        return switch (optionType) {
            case CE -> strikePrice < atmStrike
                    ? OptionRank.ITM
                    : OptionRank.OTM;

            case PE -> strikePrice > atmStrike
                    ? OptionRank.ITM
                    : OptionRank.OTM;
        };
    }

    public static Double determineAtmStrike(
            double spotPrice,
            List<Double> availableStrikes) {

        Objects.requireNonNull(availableStrikes, "availableStrikes");

        return availableStrikes.stream()
                .min(
                        Comparator
                                .comparingDouble(
                                        (Double strike) -> Math.abs(strike - spotPrice))
                                .thenComparing(Comparator.reverseOrder())
                )
                .orElseThrow(() ->
                        new IllegalArgumentException("No strikes available"));
    }

    public static OptionActivity determineOptionActivity(
            Double change,
            Long changeInOpenInterest) {

        if (change == null || changeInOpenInterest == null) {
            return OptionActivity.UNKNOWN;
        }

        if (change == 0 || changeInOpenInterest == 0) {
            return OptionActivity.UNKNOWN;
        }

//        if (change > 0 && changeInOpenInterest > 0) {
//            return OptionActivity.LONG_BUILDUP;
//        } else if (change < 0 && changeInOpenInterest > 0) {
//            return OptionActivity.SHORT_BUILDUP;
//        } else if (change < 0) {
//            return OptionActivity.LONG_UNWINDING;
//        } else {
//            return OptionActivity.SHORT_COVERING; // change > 0 && OI < 0
//        }
        if (change > 0) {
            return changeInOpenInterest > 0
                    ? OptionActivity.LONG_BUILDUP
                    : OptionActivity.SHORT_COVERING;
        }

        return changeInOpenInterest > 0
                ? OptionActivity.SHORT_BUILDUP
                : OptionActivity.LONG_UNWINDING;
    }
}
