package com.nse.analyser.utils;


import lombok.Builder;
import lombok.Value;
import org.apache.commons.math3.distribution.NormalDistribution;

public final class BlackScholes {

    private static final NormalDistribution N = new NormalDistribution();

    private BlackScholes() {
    }

    public static Result calculate(
            double spot,
            double strike,
            double iv,
            double riskFreeRate,
            double dividendYield,
            long dte) {

        double sigma = iv / 100.0;
        double r = riskFreeRate / 100.0;
        double q = dividendYield / 100.0;
        double t = dte / 252.0;

        if (t <= 0 || sigma <= 0) {
            throw new IllegalArgumentException("Invalid inputs");
        }

        double sqrtT = Math.sqrt(t);

        double d1 =
                (Math.log(spot / strike)
                        + (r - q + 0.5 * sigma * sigma) * t)
                        / (sigma * sqrtT);

        double d2 = d1 - sigma * sqrtT;

        double nd1 = N.cumulativeProbability(d1);
        double nd2 = N.cumulativeProbability(d2);

        double nMinusD1 = N.cumulativeProbability(-d1);
        double nMinusD2 = N.cumulativeProbability(-d2);

        double pdfD1 = N.density(d1);

        /*
         * PRICES
         */

        double callPrice =
                spot * Math.exp(-q * t) * nd1
                        - strike * Math.exp(-r * t) * nd2;

        double putPrice =
                strike * Math.exp(-r * t) * nMinusD2
                        - spot * Math.exp(-q * t) * nMinusD1;

        /*
         * DELTA
         */

        double callDelta =
                Math.exp(-q * t) * nd1;

        double putDelta =
                Math.exp(-q * t) * (nd1 - 1);

        /*
         * GAMMA
         */

        double gamma =
                Math.exp(-q * t) * pdfD1
                        / (spot * sigma * sqrtT);

        /*
         * VEGA
         *
         * Per 1% IV change
         */

        double vega =
                (spot * Math.exp(-q * t) * pdfD1 * sqrtT)
                        / 100.0;

        /*
         * THETA
         *
         * Per day
         */

        double callThetaAnnual =
                -(spot * pdfD1 * sigma * Math.exp(-q * t))
                        / (2 * sqrtT)
                        - r * strike * Math.exp(-r * t) * nd2
                        + q * spot * Math.exp(-q * t) * nd1;

        double putThetaAnnual =
                -(spot * pdfD1 * sigma * Math.exp(-q * t))
                        / (2 * sqrtT)
                        + r * strike * Math.exp(-r * t) * nMinusD2
                        - q * spot * Math.exp(-q * t) * nMinusD1;

        double callTheta =
                callThetaAnnual / 365.0;

        double putTheta =
                putThetaAnnual / 365.0;

        /*
         * RHO
         *
         * Per 1% rate change
         */

        double callRho =
                strike * t * Math.exp(-r * t) * nd2
                        / 100.0;

        double putRho =
                -strike * t * Math.exp(-r * t) * nMinusD2
                        / 100.0;

        return Result.builder()
                .callPrice(callPrice)
                .putPrice(putPrice)

                .callDelta(callDelta)
                .putDelta(putDelta)

                .gamma(gamma)

                .vega(vega)

                .callTheta(callTheta)
                .putTheta(putTheta)

                .callRho(callRho)
                .putRho(putRho)

                .d1(d1)
                .d2(d2)
                .build();
    }

    /**
     * Derive dividend yield from futures basis.
     */
    public static double deriveDividendYield(
            double spot,
            double future,
            double riskFreeRate,
            long dte) {

        double r = riskFreeRate / 100.0;
        double t = dte / 365.0;

        double q =
                r - (Math.log(future / spot) / t);

        return q * 100.0;
    }

    @Value
    @Builder
    public static class Result {

        double callPrice;
        double putPrice;

        double callDelta;
        double putDelta;

        double gamma;

        double vega;

        double callTheta;
        double putTheta;

        double callRho;
        double putRho;

        double d1;
        double d2;
    }
}