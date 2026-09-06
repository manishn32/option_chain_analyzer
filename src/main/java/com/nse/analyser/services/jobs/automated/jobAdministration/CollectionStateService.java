package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.models.jobStatus.CollectionStatus;
import com.nse.analyser.repositories.mongoRepositories.jobRepositories.CollectionStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CollectionStateService {

    private final CollectionStateRepository repository;

    public void markStarted(
            String symbol,
            String expiry,
            InstrumentType instrumentType,
            TimeOfDay timeOfDay
    ) {

        CollectionState state =
                repository
                        .findBySymbolAndExpiryDateAndBusinessDate(
                                symbol,
                                expiry,
                                LocalDate.now(),
                                timeOfDay
                        )
                        .orElse(CollectionState.builder()
                                .symbol(symbol)
                                .timeOfDay(timeOfDay)
                                .expiryDate(expiry)
                                .instrumentType(instrumentType)
                                .businessDate(LocalDate.now())
                                .attemptCount(0)
                                .build());

        state.setAttemptCount(
                state.getAttemptCount() + 1
        );

        state.setStatus(
                CollectionStatus.IN_PROGRESS
        );

        state.setStartedAt(
                LocalDateTime.now()
        );

        state.setLastAttemptAt(
                LocalDateTime.now()
        );

        repository.save(state);
    }

    public void markSuccess(
            String symbol,
            String expiry,
            long records,
            TimeOfDay timeOfDay
    ) {

        repository
                .findBySymbolAndExpiryDateAndBusinessDate(
                        symbol,
                        expiry,
                        LocalDate.now(),
                        timeOfDay
                )
                .ifPresent(state -> {

                    state.setStatus(
                            CollectionStatus.SUCCESS
                    );

                    state.setRecordsCollected(records);

                    state.setCompletedAt(
                            LocalDateTime.now()
                    );

                    repository.save(state);
                });
    }

    public void markFailure(
            String symbol,
            String expiry,
            Exception exception,
            TimeOfDay timeOfDay
    ) {

        repository
                .findBySymbolAndExpiryDateAndBusinessDate(
                        symbol,
                        expiry,
                        LocalDate.now(),
                        timeOfDay
                )
                .ifPresent(state -> {

                    state.setTimeOfDay(timeOfDay);

                    state.setStatus(
                            CollectionStatus.FAILED
                    );

                    state.setErrorMessage(
                            exception.getMessage()
                    );

                    repository.save(state);
                });
    }
}