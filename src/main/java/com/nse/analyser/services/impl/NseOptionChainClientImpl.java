package com.nse.analyser.services.impl;

import com.nse.analyser.dtos.nseResponse.NseOptionChainResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.services.NseOptionChainClient;
import com.nse.analyser.services.NseRestClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.nse.analyser.constants.NseConstants.*;

@Service
@AllArgsConstructor
@Slf4j
public class NseOptionChainClientImpl implements NseOptionChainClient {

    private final NseRestClient client;

    @Override
    public NseOptionChainResponse fetchOptionChainDataForIndices(String symbol, String expiry) {
        // Implementation to fetch option chain data from NSE API
        String url = NSE_HOME + NSE_OPTIONS_CHAIN_INDICES_URL;
        log.info("Fetching option chain data for indices for symbol: {} and expiry: {}", symbol, expiry);
        log.debug("Fetching option chain data for indices for symbol: {} and expiry: {} with URL: {}", symbol, expiry,url);
        return client.get(
                url.formatted(symbol,expiry),
                NseOptionChainResponse.class);
    }

    @Override
    public NseOptionChainResponse fetchOptionChainDataForEquity(String symbol, String expiry) {
        String url = NSE_HOME + NSE_OPTIONS_CHAIN_EQUITY_URL;
        log.info("Fetching option chain data for equity for symbol: {} and expiry: {}", symbol, expiry);
        log.debug("Fetching option chain data for equity for symbol: {} and expiry: {} with URL: {}", symbol, expiry,url);
        return client.get(
                url.formatted(symbol,expiry),
                NseOptionChainResponse.class);
    }

    @Override
    public List<String> fetchExpirationDates(String symbol) {
        log.info("Fetching expiration dates for symbol: {}", symbol);
        String url = NSE_HOME + NSE_OPTIONS_CHAIN_EQUITY_URL;
        log.debug("Fetching expiration dates for symbol: {} with URL: {}", symbol,url);
        NseOptionChainResponse nseOptionChainResponse = client.get(
                url.formatted(symbol, DEFAULT_DATE_EXPIRY_SEARCH),
                NseOptionChainResponse.class);
        log.info("Completed fetching expiry dates for {}",symbol);
        log.debug("Fetched expiry dates for symbol: {} with response: {}", symbol, nseOptionChainResponse);
        return nseOptionChainResponse.getRecords() != null && nseOptionChainResponse.getRecords().getExpiryDates() !=null ? nseOptionChainResponse.getRecords().getExpiryDates() : Collections.emptyList();
    }

    @Override
    public List<String> fetchStrikePrices(String symbol) {
        log.info("Fetching strike prices for symbol: {}", symbol);
        String url = NSE_HOME + NSE_OPTIONS_CHAIN_EQUITY_URL;
        log.debug("Fetching strike prices for symbol: {} with URL: {}", symbol,url);
        NseOptionChainResponse nseOptionChainResponse = client.get(
                url.formatted(symbol, DEFAULT_DATE_EXPIRY_SEARCH),
                NseOptionChainResponse.class);
        log.info("Completed fetching strike prices for {}",symbol);
        log.debug("Fetched strike prices for symbol: {} with response: {}", symbol, nseOptionChainResponse);
        return nseOptionChainResponse.getRecords() != null && nseOptionChainResponse.getRecords().getStrikePrices() !=null ? nseOptionChainResponse.getRecords().getStrikePrices() : Collections.emptyList();
    }

    @Override
    public UnderLyingInfoResponse fetchUnderLyingInfo() {
        log.info("Fetching tradable FnO contracts from NSE");
        return client.get(
                NSE_HOME + NSE_UNDERLYING_INFORMATION,
                UnderLyingInfoResponse.class);
    }
}
