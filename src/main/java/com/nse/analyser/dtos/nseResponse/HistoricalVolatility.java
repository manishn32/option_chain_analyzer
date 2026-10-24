package com.nse.analyser.dtos.nseResponse;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HistoricalVolatility {

    private String date;
    private String symbol;

    // Underlying
    private double underlyingAnnualisedVolatility;

    // Futures
    private double futuresAnnualisedVolatility;

    // Applicable
    private double applicableDailyVolatility;
    private double applicableAnnualisedVolatility;
}