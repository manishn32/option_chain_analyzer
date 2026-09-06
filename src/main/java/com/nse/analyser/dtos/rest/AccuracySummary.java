package com.nse.analyser.dtos.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccuracySummary {
    private String timeframe;
    private long sampleSize;
    private double accuracyPercent;
}

