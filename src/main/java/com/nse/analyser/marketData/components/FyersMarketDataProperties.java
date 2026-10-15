package com.nse.analyser.marketData.components;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-data")
public record FyersMarketDataProperties(
        String host,
        String internalApiKey
) {
}