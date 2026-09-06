package com.nse.analyser.dtos.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeframeProjection {
    private String timeframe;
    private Double score;
    private String regime;
    private Double support;
    private Double resistance;
}

