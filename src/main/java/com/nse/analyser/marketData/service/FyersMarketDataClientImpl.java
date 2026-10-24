package com.nse.analyser.marketData.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import com.nse.analyser.marketData.dto.InstrumentDTO;
import com.nse.analyser.marketData.constants.Constants;
import com.nse.analyser.marketData.constants.MarketDataAPIs;
import com.nse.analyser.marketData.dto.SymbolsAndExpiryDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainResponseDTO;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
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

    private final Semaphore access = new Semaphore(2);

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

    @Override
    @Bulkhead(name = "fyers", type = Bulkhead.Type.SEMAPHORE)
    public OptionChainResponseDTO getOptionChain(String symbol, Integer strikeCount, Long timeStamp, Boolean greeks) {
//        MarketDataAPIs.OptionChain.formatted(symbol,strikeCount,timeStamp,greeks);
        return marketDataRestClient
                .method(HttpMethod.GET)
                .uri(uriBuilder ->
                        uriBuilder
                                .path(Constants.MarketDataBaseUrl+ MarketDataAPIs.DailyOption)
                                .queryParam("symbol", symbol)
                                .queryParam("strikeCount", strikeCount)
                                .queryParam("timestamp",timeStamp)
                                .queryParam("greeks",greeks)
                                .build())
//                .body(symbols)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
    private static String e(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

}