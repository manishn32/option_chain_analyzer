package com.nse.analyser.dtos.nseUnderLyingInfoDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnderlyingItem {
    private String symbol;
    private String underlying;
    private Integer serialNumber;
}