package com.nse.analyser.models.contractSnapShot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDepth {

    private Double bestBidPrice;

    private Long bestBidQuantity;

    private Double bestAskPrice;

    private Long bestAskQuantity;
}