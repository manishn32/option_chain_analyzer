package com.nse.analyser.marketData.dto.fyers;

import lombok.Data;

import java.util.List;

@Data
public class OptionChainDataDTO {

    private long callOi;

    private long putOi;

    private IndexDataDTO indiavixData;

    private List<ExpiryDTO> expiryData;

    private List<OptionChainItemDTO> optionsChain;
}