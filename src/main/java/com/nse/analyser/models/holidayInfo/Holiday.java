package com.nse.analyser.models.holidayInfo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.Data;

import java.time.LocalDate;

@Data
public class Holiday {

    private String tradingDate;
    private String weekDay;
    private String description;

    @JsonProperty("Sr_no")
    private Integer srNo;

    public LocalDate getDate() {
        return LocalDate.parse(
                tradingDate,
                DateTimeFormatters.expiryDateFormatter
        );
    }
}