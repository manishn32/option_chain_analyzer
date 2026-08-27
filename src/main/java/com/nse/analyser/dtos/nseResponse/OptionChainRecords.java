package com.nse.analyser.dtos.nseResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionChainRecords {

    private List<OptionStrikeData> data;

    private String timestamp;

    private Double underlyingValue;

    private List<String> expiryDates;

    private List<String> strikePrices;
}