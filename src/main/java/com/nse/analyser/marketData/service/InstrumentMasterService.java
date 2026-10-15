package com.nse.analyser.marketData.service;

import java.util.List;
import java.util.Map;

import com.nse.analyser.marketData.dto.SymbolsAndExpiryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class InstrumentMasterService {

    private final FyersMarketDataClient marketDataClient;

    public List<SymbolsAndExpiryDTO> loadIndexExpiries() {

        return marketDataClient.getIntrumentAndExpiries(
                "NSE",
                List.of(
                        "NIFTY",
                        "BANKNIFTY",
                        "FINNIFTY",
                        "MIDCPNIFTY"
                )
        );
    }
}