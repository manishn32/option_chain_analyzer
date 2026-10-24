package com.nse.analyser.marketData.dto.fyers;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OptionChainItemDTO {

    /**
     * The Optionchain API provides important information about options trading, specifically focusing on strike prices, which are predetermined prices at which an option contract can be exercised. This API offers data for both Call (CE) and Put (PE) options, along with values for index, IndiaVIX (Volatility Index) and available expiry dates.
     * When using the API, you can specify the number of strike prices you're interested in. For example, if you request a strike count of 2, the API will return data for:
     * At-The-Money (ATM) strike: The closest strike price to the current market price.
     * Out-of-The-Money (OTM) strikes: Strike prices higher than the current market price for Calls (CE) and lower than the current market price for Puts (PE). The API will return data for 2 CE and 2 PE OTM strikes.
     * In-The-Money (ITM) strikes: Strike prices lower than the current market price for Calls (CE) and higher than the current market price for Puts (PE). The API will return data for 2 CE and 2 PE ITM strikes.
     * Set greeks to 1 to receive delta, gamma, theta, vega and iv. Each strike then carries them in a nested greeks object.
     * The first optionsChain entry is the underlying itself (strike_price -1, empty option_type), with its future price in fp, fpch and fpchp.
     * expiryData lists the available expiries with date (DD-MM-YYYY), epoch expiry and expiry_flag. India VIX is returned in indiavixData.
     * callOi and putOi give total open interest for each side. For each strike, oi, oich, oichp and prev_oi show open interest and its change.
     *
     */


    private double ask;

    private double bid;

    @JsonProperty("fyToken")
    private String fyToken;

    private double ltp;

    private double ltpch;

    private double ltpchp;

    private long oi;

    private long oich;

    private double oichp;

    @JsonProperty("prev_oi")
    private long prevOi;

    @JsonProperty("option_type")
    private String optionType;

    @JsonProperty("strike_price")
    private double strikePrice;

    private String symbol;

    private long volume;

    private String exchange;

    private String description;

    @JsonProperty("ex_symbol")
    private String exSymbol;

    private Double fp;

    private Double fpch;

    private Double fpchp;

    private GreeksDto greeks;
}