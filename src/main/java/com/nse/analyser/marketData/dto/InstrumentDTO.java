package com.nse.analyser.marketData.dto;


import lombok.Data;

@Data
public class InstrumentDTO  {
    private String symbol;

    private String fyToken;

    private String underlying;
}
