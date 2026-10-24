package com.nse.analyser.dtos.nseResponse;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Range {
    private Double expectedMove;
    private Double lowerRange;
    private Double upperRange;

}
