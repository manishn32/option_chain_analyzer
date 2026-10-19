package com.nse.analyser.marketData.dto.fyers;

import lombok.Data;

@Data
public class StrikeContainer {

    private OptionChainItemDTO ce;

    private OptionChainItemDTO pe;
}