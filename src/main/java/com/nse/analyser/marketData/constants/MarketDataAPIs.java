package com.nse.analyser.marketData.constants;

public class MarketDataAPIs {
    public static final String InstrumentAndExpires = "/intrumentMaster/instrumentsAndExpires";
    public static final String FnoInstruments = "/intrumentMaster/fno";
    public static final String IndexSymbols = "/intrumentMaster/index";
    public static final String OptionChain = "/option-chain?symbol=%s&strikeCount=%s&timestamp=%s&greeks=%s";
    public static final String DailyOption = "/option-chain";
    public static final String depth = "";
}
