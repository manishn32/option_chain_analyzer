package com.nse.analyser.models.contractSnapShot;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuantitySummary {

    private Long totalBuyQuantity;

    private Long totalSellQuantity;
}