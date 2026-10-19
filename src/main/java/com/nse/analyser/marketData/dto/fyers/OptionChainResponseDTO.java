package com.nse.analyser.marketData.dto.fyers;

import lombok.Data;

@Data
public class OptionChainResponseDTO {

    private int code;

    private OptionChainDataDTO data;

    private String message;

    private String s;
}