package com.nse.analyser.marketData.dto.fyers;


import lombok.Data;

@Data
public class GreeksDto {
    private double delta;

    private double gamma;

    private double theta;

    private double vega;

    private double iv;
}
