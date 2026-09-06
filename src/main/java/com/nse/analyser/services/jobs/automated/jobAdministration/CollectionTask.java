package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollectionTask {

    private String symbol;

    private String expiryDate;

    private InstrumentType instrumentType;

    private TimeOfDay timeOfDay;

}