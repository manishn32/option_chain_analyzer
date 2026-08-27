package com.nse.analyser.models.chainSnapShot;

import com.nse.analyser.enums.OptionActivity;
import com.nse.analyser.enums.OptionRank;
import com.nse.analyser.enums.OptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionLeg {

    private String underlying;

    private double underlyingValue;

    private Double LTP;


    /**
     * Total OI outstanding
     * */
    private Long openInterest;

    /**
     * Absolute change in OI
     * */
    private Long changeInOpenInterest;

    /** To Calculate OI Change Percent they would have done like below
     * Say from data we have,
     * current OI = 4,684
     * Change In OI = -357
     * Previous OI = Current OI - Change in OI
     * Percent change in OI = -7.0819%
     *
     * Is calculated as below,
     * previous OI = 4,684 - -(357)
     * previous OI = 4,684 + 357 = 5041
     *
     * Then:
     *
     * Precentage Change in OI = ( (current OI - previous OI) / previous OI ) * 100
     */
    private Double oiChangePercent;

    private Double impliedVolatility;

    private Long totalTradedVolume;

    private Double priceChange;

    private Double priceChangePercent;

    private Double strikePrice;

    private OptionRank optionRank;

    private Double obi;

    private Double oiConcentration;

    private Double volumeConcentration;

    private Long bidQuantity;

    private Long askQuantity;

    private OptionActivity optionActivity;

    private OptionType optionType;

    private LocalDate expiryDate;

    private LocalDateTime nseTimestamp;

    private Instant snapshotTimestamp;
}