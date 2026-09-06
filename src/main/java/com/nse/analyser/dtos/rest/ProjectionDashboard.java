package com.nse.analyser.dtos.rest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectionDashboard {
    private TimeframeProjection oneMinute;
    private TimeframeProjection threeMinute;
    private TimeframeProjection fiveMinute;
    private TimeframeProjection fifteenMinute;
    private TimeframeProjection thirtyMinute;
    private TimeframeProjection oneHour;
}

