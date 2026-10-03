package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.dtos.nseResponse.DataFillRecord;
import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.models.jobStatus.CollectionStatus;
import com.nse.analyser.repositories.mongoRepositories.jobRepositories.CollectionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.nse.analyser.utils.DateTimeFormatters.IST;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionStateService {

    private final CollectionStateRepository repository;

    public void markStarted(
            String symbol,
            String expiry,
            InstrumentType instrumentType,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            String intradayKey
    ) {

        CollectionState state = timeOfDay != TimeOfDay.DURING_DAY ?
                repository
                        .findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
                                symbol,
                                expiry,
                                LocalDate.now(IST),
                                timeOfDay,
                                dataBaseChoice
                        )
                        .orElse(CollectionState.builder()
                                .symbol(symbol)
                                .timeOfDay(timeOfDay)
                                .expiryDate(expiry)
                                .instrumentType(instrumentType)
                                .businessDate(LocalDate.now(IST))
                                .attemptCount(0)
                                .dataBaseChoice(dataBaseChoice)
                                .build())
                :
                repository.findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(
                        symbol,
                        expiry,
                        dataBaseChoice,
                        intradayKey
                ).orElse(CollectionState.builder()
                        .symbol(symbol)
                        .timeOfDay(timeOfDay)
                        .expiryDate(expiry)
                        .instrumentType(instrumentType)
                        .businessDate(LocalDate.now(IST))
                        .attemptCount(0)
                        .dataBaseChoice(dataBaseChoice)
                        .intradayKey(intradayKey)
                        .build())

                ;

        state.setAttemptCount(
                state.getAttemptCount() + 1
        );

        state.setStatus(
                CollectionStatus.IN_PROGRESS
        );

        state.setStartedAt(
                LocalDateTime.now(IST)
        );

        state.setLastAttemptAt(
                LocalDateTime.now(IST)
        );

        repository.save(state);
    }

    public void markSuccess(
            String symbol,
            String expiry,
            DataFillRecord records,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            String intradayKey,
            InstrumentType instrumentType
    ) {
        CollectionState state = timeOfDay != TimeOfDay.DURING_DAY ?
                repository
                        .findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
                                symbol,
                                expiry,
                                LocalDate.now(IST),
                                timeOfDay,
                                dataBaseChoice
                        )
                        .orElse(CollectionState.builder()
                                .symbol(symbol)
                                .timeOfDay(timeOfDay)
                                .expiryDate(expiry)
                                .instrumentType(instrumentType)
                                .businessDate(LocalDate.now(IST))
                                .attemptCount(0)
                                .dataBaseChoice(dataBaseChoice)
                                .build())
                :
                repository.findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(
                        symbol,
                        expiry,
                        dataBaseChoice,
                        intradayKey
                ).orElse(CollectionState.builder()
                        .symbol(symbol)
                        .timeOfDay(timeOfDay)
                        .expiryDate(expiry)
                        .instrumentType(instrumentType)
                        .businessDate(LocalDate.now(IST))
                        .attemptCount(0)
                        .dataBaseChoice(dataBaseChoice)
                        .intradayKey(intradayKey)
                        .build())

                ;

        if(state == null){
            log.error(
                    "No state found to mark success for symbol {} expiry {} timeOfDay {} database {} with state {}",
                    symbol,
                    expiry,
                    timeOfDay,
                    dataBaseChoice,
                    state
            );
            throw new RuntimeException("No state found to mark success");
        }
        state.setStatus(
                CollectionStatus.SUCCESS
        );

        state.setRecordsCollected((long)records.totalRecords());
        state.setNseTimestamp(records.nseTimestamp());
        state.setCompletedAt(
                LocalDateTime.now(IST)
        );

        repository.save(state);

//        if(timeOfDay != TimeOfDay.DURING_DAY){
//         repository
//                .findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
//                        symbol,
//                        expiry,
//                        LocalDate.now(IST),
//                        timeOfDay,
//                        dataBaseChoice
//                )
//                .ifPresent(state -> {
//
//                    state.setStatus(
//                            CollectionStatus.SUCCESS
//                    );
//
//                    state.setRecordsCollected((long)records.totalRecords());
//
//                    state.setNseTimestamp(records.nseTimestamp());
//
//                    state.setCompletedAt(
//                            LocalDateTime.now()
//                    );
//
//                    repository.save(state);
//                });
//
//        }else {
//            repository.findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(
//                    symbol,
//                    expiry,
//                    dataBaseChoice,
//                    intradayKey
//            )
//                    .ifPresent(state -> {
//
//                        state.setStatus(
//                                CollectionStatus.SUCCESS
//                        );
//
//                        state.setRecordsCollected((long)records.totalRecords());
//                        state.setNseTimestamp(records.nseTimestamp());
//                        state.setCompletedAt(
//                                LocalDateTime.now(IST)
//                        );
//
//                        repository.save(state);
//                    });
//        }
    }

    public void markFailure(
            String symbol,
            String expiry,
            Exception exception,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            String intradayKey,
            InstrumentType instrumentType
    ) {


        CollectionState state = timeOfDay != TimeOfDay.DURING_DAY ?
                repository
                        .findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
                                symbol,
                                expiry,
                                LocalDate.now(IST),
                                timeOfDay,
                                dataBaseChoice
                        )
                        .orElse(CollectionState.builder()
                                .symbol(symbol)
                                .timeOfDay(timeOfDay)
                                .expiryDate(expiry)
                                .instrumentType(instrumentType)
                                .businessDate(LocalDate.now(IST))
                                .attemptCount(0)
                                .dataBaseChoice(dataBaseChoice)
                                .build())
                :
                repository.findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(
                        symbol,
                        expiry,
                        dataBaseChoice,
                        intradayKey
                ).orElse(CollectionState.builder()
                        .symbol(symbol)
                        .timeOfDay(timeOfDay)
                        .expiryDate(expiry)
                        .instrumentType(instrumentType)
                        .businessDate(LocalDate.now(IST))
                        .attemptCount(0)
                        .dataBaseChoice(dataBaseChoice)
                        .intradayKey(intradayKey)
                        .build())

                ;

        state.setTimeOfDay(timeOfDay);

        state.setStatus(
                CollectionStatus.FAILED
        );

        state.setErrorMessage(
                exception.getMessage()
        );

        repository.save(state);

//        repository
//                .findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
//                        symbol,
//                        expiry,
//                        LocalDate.now(IST),
//                        timeOfDay,
//                        dataBaseChoice
//                )
//                .ifPresent(state -> {
//
//                    state.setTimeOfDay(timeOfDay);
//
//                    state.setStatus(
//                            CollectionStatus.FAILED
//                    );
//
//                    state.setErrorMessage(
//                            exception.getMessage()
//                    );
//
//                    repository.save(state);
//                });
    }
}