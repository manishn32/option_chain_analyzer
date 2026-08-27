package com.nse.analyser.enums;

public enum InstrumentType {
    EQUITY("Equity"),
    FUTURES("Futures"),
    OPTIONS("Options"),
    CURRENCY("Currency"),
    COMMODITY("Commodity"),
    BOND("Bond"),
    ETF("Exchange Traded Fund"),
    MF("Mutual Fund"),
    CRYPTO("Cryptocurrency"),
    INDEX("Indices");

    private final String displayName;

    InstrumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}