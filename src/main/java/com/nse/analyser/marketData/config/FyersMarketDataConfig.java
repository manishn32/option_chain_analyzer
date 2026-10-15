package com.nse.analyser.marketData.config;

import com.nse.analyser.marketData.components.FyersMarketDataProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FyersMarketDataProperties.class)
public class FyersMarketDataConfig {
}
