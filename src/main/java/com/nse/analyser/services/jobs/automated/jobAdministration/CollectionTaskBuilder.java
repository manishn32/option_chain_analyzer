package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Service
@RequiredArgsConstructor
public class CollectionTaskBuilder {

    private final CollectionGuardService guardService;

    public List<CollectionTask> buildTasks(
            Map<InstrumentType,
                    Map<String,List<Long>>> metadata,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            LocalDateTime session
    ) {

        List<CollectionTask> tasks =
                new ArrayList<>();

        LocalDate today = LocalDate.now(IST);
        LocalDate maxDate = today.plusMonths(3);

        for(var instrumentEntry : metadata.entrySet()) {

            InstrumentType type =
                    instrumentEntry.getKey();

            for(var symbolEntry :
                    instrumentEntry.getValue().entrySet()) {

                String symbol =
                        symbolEntry.getKey();

                for(Long expiry :
                        symbolEntry.getValue()) {

                    try {
                        String intraDayKey = symbol + "_" + expiry + "_" + dataBaseChoice.name() + "_" + session.toString();
                        LocalDate expiryDate = LocalDate.from(Instant.ofEpochSecond(expiry).atZone(DateTimeFormatters.IST))
//                                LocalDate.parse(
//                                        expiry,
//                                        DateTimeFormatters.expiryDateFormatter
//                                )
                                ;

                        if(expiryDate.isBefore(today)){
                            continue;
                        }

                        if(expiryDate.isAfter(maxDate)){
                            continue;
                        }

                        if(guardService.shouldCollect(
                                symbol,
                                expiry,
                                timeOfDay,
                                dataBaseChoice,
                                intraDayKey)
                        ) {

                            tasks.add(
                                    CollectionTask.builder()
                                            .symbol(symbol)
                                            .expiryDate(expiry)
                                            .instrumentType(type)
                                            .dataBaseChoice(dataBaseChoice)
                                            .intradayKey(intraDayKey)
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