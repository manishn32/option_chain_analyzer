package com.nse.analyser.services.schedulers;

import com.nse.analyser.dtos.nseResponse.NseOptionChainResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.services.NseOptionChainClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NseDataRetrievalService {

    private final NseOptionChainClient client;

    public NseOptionChainResponse fetchFromNse(InstrumentType instrumentType, String symbol, String expiry) {
        if(symbol !=null) symbol = symbol.toUpperCase();

        switch (instrumentType) {
            case EQUITY -> {
                return client.fetchOptionChainDataForEquity(symbol,expiry);
            }
            case INDEX -> {
                return client.fetchOptionChainDataForIndices(symbol,expiry);
            }
            default -> {
                throw new IllegalArgumentException("Invalid instrument type: " + instrumentType);
            }
        }
    }

    public List<String> fetchExpirationDates(String symbol) {
        return client.fetchExpirationDates(symbol);
    }

    public List<String> fetchStrikePrices(String symbol) {
        return client.fetchStrikePrices(symbol);
    }

    public UnderLyingInfoResponse findAllFnO_Contracts(){
        return client.fetchUnderLyingInfo();
    }
}