package com.nse.analyser.models.chainSnapShot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StrikeSnapshot {

    private Double strikePrice;

    private OptionLeg ce;

    private OptionLeg pe;
}
