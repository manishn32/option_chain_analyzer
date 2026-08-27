package com.nse.analyser.dtos.nseResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionStrikeData {

    private String expiryDates;

    private Double strikePrice;

    @JsonProperty("CE")
    private OptionContract ce;

    @JsonProperty("PE")
    private OptionContract pe;
}
