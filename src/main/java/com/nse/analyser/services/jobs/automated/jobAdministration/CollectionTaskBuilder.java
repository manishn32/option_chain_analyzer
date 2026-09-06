package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CollectionTaskBuilder {

    private final CollectionGuardService guardService;

    public List<CollectionTask> buildTasks(
            Map<InstrumentType,
                    Map<String,List<String>>> metadata,
            TimeOfDay timeOfDay
    ) {

        List<CollectionTask> tasks =
                new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusMonths(3);

        for(var instrumentEntry : metadata.entrySet()) {

            InstrumentType type =
                    instrumentEntry.getKey();

            for(var symbolEntry :
                    instrumentEntry.getValue().entrySet()) {

                String symbol =
                        symbolEntry.getKey();

                for(String expiry :
                        symbolEntry.getValue()) {

                    try {

                        LocalDate expiryDate =
                                LocalDate.parse(
                                        expiry,
                                        DateTimeFormatters.expiryDateFormatter
                                );

                        if(expiryDate.isBefore(today)){
                            continue;
                        }

                        if(expiryDate.isAfter(maxDate)){
                            continue;
                        }

                        if(guardService.shouldCollect(
                                symbol,
                                expiry,
                                timeOfDay
                        )) {

                            tasks.add(
                                    CollectionTask.builder()
                                            .symbol(symbol)
                                            .expiryDate(expiry)
                                            .instrumentType(type)
                                            .build()
                            );
                        }

                    } catch (Exception ignored) {
                    }
                }
            }


        }

        return tasks;
    }
}