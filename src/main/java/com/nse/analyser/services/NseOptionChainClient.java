package com.nse.analyser.services;

import com.nse.analyser.dtos.nseResponse.NseOptionChainResponse;
import com.nse.analyser.dtos.nseUnderLyingInfoDTOs.UnderLyingInfoResponse;
import com.nse.analyser.models.holidayInfo.Holiday;

import java.util.List;

public interface NseOptionChainClient {
    NseOptionChainResponse fetchOptionChainDataForIndices(String symbol,String expiry);
    NseOptionChainResponse fetchOptionChainDataForEquity(String symbol,String expiry);
    List<String> fetchExpirationDates(String symbol);
    List<String> fetchStrikePrices(String symbol);
    UnderLyingInfoResponse fetchUnderLyingInfo();
    List<Holiday> fetchNseHolidays();

}
