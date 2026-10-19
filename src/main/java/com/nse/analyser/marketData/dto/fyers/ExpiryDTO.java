package com.nse.analyser.marketData.dto.fyers;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExpiryDTO {

    private String date;

    private String expiry;

    @JsonProperty("expiry_flag")
    private String expiryFlag;
}