package com.nse.analyser.marketData.service;

import com.nse.analyser.marketData.dto.InstrumentDTO;
import com.nse.analyser.marketData.dto.SymbolsAndExpiryDTO;
import com.nse.analyser.marketData.dto.fyers.OptionChainResponseDTO;

import java.util.List;
import java.util.Map;

public interface FyersMarketDataClient {
    List<SymbolsAndExpiryDTO> getIntrumentAndExpiries(
            String exchange,
            List<String> symbols
    );

    List<InstrumentDTO> getFnoInstruments(String exchange,boolean index);

    OptionChainResponseDTO getOptionChain(String symbol, Integer strikeCount, Long timeStamp, Boolean greeks);
}
