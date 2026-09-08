package com.nse.analyser.repositories.mongoRepositories.jobRepositories;

import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.models.jobStatus.CollectionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionStateRepository
        extends MongoRepository<CollectionState,String> {

    Optional<CollectionState> findBySymbolAndExpiryDateAndBusinessDateAndTimeOfDayAndDataBaseChoice(
            String symbol,
            String expiryDate,
            LocalDate businessDate,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice
    );

    long countBySymbolAndBusinessDateAndStatus(
            String symbol,
            LocalDate businessDate,
            CollectionStatus status
    );

    List<CollectionState> findByStatus(
            CollectionStatus status
    );

    Optional<CollectionState> findBySymbolAndExpiryDateAndDataBaseChoiceAndIntradayKey(String symbol, String expiry, DataBaseChoice dataBaseChoice, String intradayKey);
}
