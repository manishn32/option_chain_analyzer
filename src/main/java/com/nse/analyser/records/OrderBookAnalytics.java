package com.nse.analyser.records;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.nse.analyser.utils.NumberUtils.roundTo3Decimals;

public record OrderBookAnalytics(
        double orderBookImbalance,
        double topOfBookImbalance,
        double imbalanceDivergence
) {

    public static OrderBookAnalytics calculate(
            double totalBuyQuantity,
            double totalSellQuantity,
            double buyQuantity1,
            double sellQuantity1
    ){
        double obi = calculateImbalance(totalBuyQuantity,totalSellQuantity);
        double tobi = calculateImbalance(buyQuantity1,sellQuantity1);
        double divergence = obi - tobi;
        return new OrderBookAnalytics(
                roundTo3Decimals(obi),
                roundTo3Decimals(tobi),
                roundTo3Decimals(divergence));
    }

    public static double calculateImbalance(double buyQuantity, double sellQuantity) {
        double denominator = buyQuantity + sellQuantity;

        if (denominator == 0) {
            return 0;
        }

        return (buyQuantity - sellQuantity) / denominator;
    }
}
