package com.nse.analyser.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketProjection {

    private LocalDateTime timestamp;
    private String timeframe;
    private String underlying;
    private LocalDate expiryDate;

    private double spotPrice;

    private double supportStrike;
    private double supportStrength;

    private double resistanceStrike;
    private double resistanceStrength;

    private double bullScore;
    private double bearScore;

    private double smartMoneyFlow;

    private double dealerPressure;

    private double breakoutProbability;

    private double strikeMigrationScore;

    private double ivBias;

    private double marketProjectionScore;

    private String marketRegime;

    private String commentary;
}

