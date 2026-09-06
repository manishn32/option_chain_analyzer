package com.nse.analyser.models.holidayInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "HolidayList")
public class HolidayList {
    private String year;
    private List<Holiday> holidays;
}
