package com.nse.analyser.marketData.dto;

import lombok.Data;

import java.util.List;

@Data
public class SymbolsAndExpiryDTO {
    private String symbol;
    private String underlying;
    private List<Long> expires;
}
