package com.nse.analyser.dtos.nseResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.records.OrderBookAnalytics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptionContract {

    private Double PChange;

    private Double buyPrice1;

    private Long buyQuantity1;

    private Double change;

    private Long changeinOpenInterest;

    private String expiryDate;

    private String identifier;

    private Double impliedVolatility;

    private Double lastPrice;

    private Long openInterest;

    private String optionType;

    private Double pChange;

    private Double pchangeinOpenInterest;

    private Double sellPrice1;

    private Long sellQuantity1;

    private Double strikePrice;

    private Long totalBuyQuantity;

    private Long totalSellQuantity;

    private Long totalTradedVolume;

    private String underlying;

    private Double underlyingValue;

//    private OptionRank optionRank;
//
//    private OrderBookAnalytics orderBookAnalytics;
//
//    private Double oiConcentration;
//
//    private Double volumeConcentration;

}
