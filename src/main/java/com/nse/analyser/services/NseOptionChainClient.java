package com.nse.analyser.services;

import com.nse.analyser.dtos.nseResponse.NseOptionChainResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;

import java.util.List;

public interface NseOptionChainClient {
    NseOptionChainResponse fetchOptionChainDataForIndices(String symbol,String expiry);
    NseOptionChainResponse fetchOptionChainDataForEquity(String symbol,String expiry);
    List<String> fetchExpirationDates(String symbol);
    List<String> fetchStrikePrices(String symbol);
    UnderLyingInfoResponse fetchUnderLyingInfo();
}
