package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.repositories.mongoRepositories.jobRepositories.CollectionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionGuardService {

    private static final int MAX_RETRIES = 3;

    private final CollectionStateRepository repository;

    public boolean shouldCollect(
            String symbol,
            String expiry,
            TimeOfDay timeOfDay
    ) {

        Optional<CollectionState> existing =
                repository.findBySymbolAndExpiryDateAndBusinessDate(
                        symbol,
                        expiry,
                        LocalDate.now(),
                        timeOfDay
                );

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