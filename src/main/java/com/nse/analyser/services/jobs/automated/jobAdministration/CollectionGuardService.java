package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.repositories.mongoRepositories.jobRepositories.CollectionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionGuardService {

    private static final int MAX_RETRIES = 3;

    private final CollectionStateRepository repository;

    public boolean shouldCollect(
            String symbol,
            String expiry,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            String intradayKey
    ) {

//        if(timeOfDay == TimeOfDay.DURING_DAY && dataBaseChoice == DataBaseChoice.MongoDB){
//            return true;
//        }
//
//        if(dataBaseChoice == DataBaseChoice.ClickHouse){
//            return true;
//        }
        Optional<CollectionState> existing = timeOfDay != TimeOfDay.DURING_DAY ?
                repository.findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
                        symbol,
                        expiry,
                        LocalDate.now(IST),
                        timeOfDay,
                        dataBaseChoice
                )
                : repository.findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(
                symbol,
                expiry,
                dataBaseChoice,
                intradayKey
        )
                ;

        if(existing.isEmpty()){
            return true;
        }

        CollectionState state = existing.get();

        return switch (state.getStatus()) {
            case SUCCESS, SKIPPED, IN_PROGRESS -> false;
            case FAILED -> state.getAttemptCount() < MAX_RETRIES;
            case PENDING -> true;
            //            default -> false;
        };
    }
}