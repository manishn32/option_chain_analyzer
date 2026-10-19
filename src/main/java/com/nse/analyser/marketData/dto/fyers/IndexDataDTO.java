package com.nse.analyser.marketData.dto.fyers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IndexDataDTO {

    private double ask;

    private double bid;

    private String description;

    @JsonProperty("ex_symbol")
    private String exSymbol;

    private String exchange;

    @JsonProperty("fyToken")
    private String fyToken;

    private double ltp;

    private double ltpch;

    private double ltpchp;

    @JsonProperty("option_type")
    private String optionType;

    @JsonProperty("strike_price")
    private double strikePrice;

    private String symbol;

    // only present in underlying record
    private Double fp;

    private Double fpch;

    private Double fpchp;
}

