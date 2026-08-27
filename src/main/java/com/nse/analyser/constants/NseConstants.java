package com.nse.analyser.constants;

public class NseConstants {
    public static final String NSE_TOKEN_REFRESHER = "https://www.nseindia.com/api/option-chain-v3?type=Indices&symbol=NIFTY";
    public static final String NSE_REFERRER = "https://www.nseindia.com/option-chain";
    public static final String NSE_HOME = "https://www.nseindia.com";
//    public static final String NSE_OPTIONS_CHAIN_URL = "/api/option-chain-indices-v3?symbol=%s&expiry=%s";
    public static final String NSE_OPTIONS_CHAIN_INDICES_URL = "/api/option-chain-v3?type=Indices&symbol=%s&expiry=%s";
    public static final String NSE_OPTIONS_CHAIN_EQUITY_URL = "/api/option-chain-v3?type=Equity&symbol=%s&expiry=%s";
    public static final String DEFAULT_DATE_EXPIRY_SEARCH = "01-Jan-1999";
    public static final String NSE_UNDERLYING_INFORMATION = "/api/underlying-information";
}
