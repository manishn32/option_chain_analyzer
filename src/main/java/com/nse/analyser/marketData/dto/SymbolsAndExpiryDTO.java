package com.nse.analyser.marketData.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class SymbolsAndExpiryDTO {
    private String symbol;
    private String underlying;
    private List<LocalDate> expires;
}
