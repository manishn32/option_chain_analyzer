package com.nse.analyser.models.underlyingInfos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradableFnO_Contracts {
    private List<String> indices;
    private List<String> stocks;
}
