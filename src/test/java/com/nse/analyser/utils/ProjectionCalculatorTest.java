package com.nse.analyser.utils;

import com.nse.analyser.enums.MarketRegime;
import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.OptionWall;
import com.nse.analyser.models.chainSnapShot.OptionLeg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectionCalculatorTest {

    @Test
    void calculatesCoreSignals() {
        List<OptionLeg> rows = List.of(
                leg(24000, OptionType.PE, OptionActivity.LONG_BUILDUP, 2000L, 300L, 1000L, 120L, 100L, 18.0),
                leg(24100, OptionType.PE, OptionActivity.SHORT_COVERING, 1500L, 200L, 900L, 110L, 90L, 19.0),
                leg(24500, OptionType.CE, OptionActivity.SHORT_BUILDUP, 2500L, 400L, 1100L, 95L, 130L, 16.0),
                leg(24600, OptionType.CE, OptionActivity.LONG_UNWINDING, 1800L, -250L, 950L, 85L, 140L, 15.0)
        );

        double bull = ProjectionCalculator.bullScore(rows);
        double bear = ProjectionCalculator.bearScore(rows);
        double support = ProjectionCalculator.findSupport(rows);
        double resistance = ProjectionCalculator.findResistance(rows);
        double dealerPressure = ProjectionCalculator.dealerPressure(rows);
        double breakout = ProjectionCalculator.breakoutProbability(dealerPressure, bull, bear);
        double smartMoney = ProjectionCalculator.smartMoneyFlow(rows);
        double ivBias = ProjectionCalculator.ivBias(rows);

        assertTrue(bull > 0);
        assertTrue(bear > 0);
        assertEquals(24000.0, support);
        assertEquals(24500.0, resistance);
        assertTrue(breakout >= 0);
        assertTrue(smartMoney != 0);
        assertTrue(ivBias > 0);

        double score = ProjectionCalculator.computeProjectionScore(
                bull,
                bear,
                ProjectionCalculator.strength(rows, support, OptionType.PE),
                ProjectionCalculator.strength(rows, resistance, OptionType.CE),
                smartMoney,
                ProjectionCalculator.calculateMigrationScore(24100, 24000, 24600, 24500),
                dealerPressure,
                ivBias
        );

        MarketRegime regime = ProjectionCalculator.regime(score, breakout);
        String text = ProjectionCalculator.generateCommentary(support, resistance, score, regime, breakout);

        OptionWall callWall = ProjectionCalculator.strongestCallWall(rows);
        OptionWall putWall = ProjectionCalculator.strongestPutWall(rows);

        assertNotNull(regime);
        assertNotNull(text);
        assertNotNull(callWall);
        assertNotNull(putWall);
    }

    private static OptionLeg leg(
            double strike,
            OptionType type,
            OptionActivity activity,
            long oi,
            long chgOi,
            long volume,
            long bidQty,
            long askQty,
            double iv) {

        return OptionLeg.builder()
                .underlying("NIFTY")
//                .underlyingValue(24200)
                .strikePrice(strike)
                .optionType(type)
                .optionActivity(activity)
                .openInterest(oi)
                .changeInOpenInterest(chgOi)
                .totalTradedVolume(volume)
                .bidQuantity(bidQty)
                .askQuantity(askQty)
                .impliedVolatility(iv)
                .build();
    }
}

