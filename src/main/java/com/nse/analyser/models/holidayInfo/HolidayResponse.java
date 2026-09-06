package com.nse.analyser.models.holidayInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class HolidayResponse {

    @JsonProperty("CM")
    private List<Holiday> cm;
}