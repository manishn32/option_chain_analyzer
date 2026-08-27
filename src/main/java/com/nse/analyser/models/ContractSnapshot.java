package com.nse.analyser.models;

import com.nse.analyser.enums.OptionType;
import com.nse.analyser.models.contractSnapShot.MarketDepth;
import com.nse.analyser.models.contractSnapShot.QuantitySummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDate;

//@Document(collection = "Option_contract_snapshot")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractSnapshot {

    @Id
    private String id;

    private String identifier;

    private String underlying;

    private Double underlyingValue;

    private LocalDate expiryDate;

    private Integer strikePrice;

    private OptionType optionType;

    private Double lastPrice;

    private Double priceChange;

    private Double priceChangePercent;

    private Long openInterest;

    private Long changeInOpenInterest;

    private Double openInterestChangePercent;

    private Double impliedVolatility;

    private Long totalTradedVolume;

    private MarketDepth marketDepth;

    private QuantitySummary quantitySummary;

    private Instant snapshotTime;
}