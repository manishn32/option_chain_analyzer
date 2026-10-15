package com.nse.analyser.marketData.service;

import java.util.List;
import java.util.Map;

import com.nse.analyser.marketData.dto.InstrumentDTO;
import com.nse.analyser.marketData.constants.Constants;
import com.nse.analyser.marketData.constants.MarketDataAPIs;
import com.nse.analyser.marketData.dto.SymbolsAndExpiryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class FyersMarketDataClientImpl
        implements FyersMarketDataClient {

    private final RestClient marketDataRestClient;

    @Override
    public List<SymbolsAndExpiryDTO> getIntrumentAndExpiries(
            String exchange,
            List<String> symbols) {

        return marketDataRestClient
                .method(HttpMethod.POST)
                .uri(uriBuilder ->
                        uriBuilder
                                .path(Constants.MarketDataBaseUrl+ MarketDataAPIs.InstrumentAndExpires)
                                .queryParam("exchange", exchange)
                                .build())
                .body(symbols)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }
                );
    }

    @Override
    public List<InstrumentDTO> getFnoInstruments(String exchange,boolean index) {
        return marketDataRestClient
                .method(HttpMethod.GET)
                .uri(uriBuilder ->
                        uriBuilder
                                .path(Constants.MarketDataBaseUrl+ (index ? MarketDataAPIs.IndexSymbols: MarketDataAPIs.FnoInstruments))
                                .queryParam("exchange", exchange)
                                .build())
//                .body(symbols)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

}