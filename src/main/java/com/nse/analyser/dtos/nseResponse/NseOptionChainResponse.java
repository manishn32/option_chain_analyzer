package com.nse.analyser.dtos.nseResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NseOptionChainResponse {

    private OptionChainRecords records;

    private OptionChainFiltered filtered;
}