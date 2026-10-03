package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.InstrumentType;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.models.underlyingInfos.TradableFnO_Contracts;
import com.nse.analyser.services.schedulers.NseDataRetrievalService;
import com.nse.analyser.utils.DateTimeFormatters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionTaskStreamingService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final NseDataRetrievalService nseDataRetrievalService;
    private final CollectionGuardService guardService;

    public void streamCollectionTasks(
            TradableFnO_Contracts contracts,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            LocalDateTime session,
            Consumer<CollectionTask> consumer) {

        LocalDate today = LocalDate.now(IST);
        LocalDate maxDate = today.plusMonths(3);

        processSymbols(
                contracts.getIndices(),
                InstrumentType.INDEX,
                timeOfDay,
                dataBaseChoice,
                session,
                today,
                maxDate,
                consumer);

        processSymbols(
                contracts.getStocks(),
                InstrumentType.EQUITY,
                timeOfDay,
                dataBaseChoice,
                session,
                today,
                maxDate,
                consumer);
    }

    private void processSymbols(
            List<String> symbols,
            InstrumentType instrumentType,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            LocalDateTime session,
            LocalDate today,
            LocalDate maxDate,
            Consumer<CollectionTask> consumer) {

        for (String symbol : symbols) {

            try {

                List<String> expiries =
                        nseDataRetrievalService
                                .fetchExpirationDates(symbol);

                if (expiries == null || expiries.isEmpty()) {
                    continue;
                }

                log.info(
                        "Found {} expiries for {}",
                        expiries.size(),
                        symbol);

                for (String expiry : expiries) {

                    createTaskIfEligible(
                            symbol,
                            expiry,
                            instrumentType,
                            timeOfDay,
                            dataBaseChoice,
                            session,
                            today,
                            maxDate,
                            consumer);
                }

            } catch (Exception ex) {

                log.error(
                        "Failed retrieving expiries for symbol {}",
                        symbol,
                        ex);
            }
        }
    }

    private void createTaskIfEligible(
            String symbol,
            String expiry,
            InstrumentType instrumentType,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice,
            LocalDateTime session,
            LocalDate today,
            LocalDate maxDate,
            Consumer<CollectionTask> consumer) {

        try {

            LocalDate expiryDate =
                    LocalDate.parse(
                            expiry,
                            DateTimeFormatters.expiryDateFormatter);

            if (expiryDate.isBefore(today)) {
                return;
            }

            if (expiryDate.isAfter(maxDate)) {
                return;
            }

            String intradayKey =
                    symbol + "_"
                            + expiry + "_"
                            + dataBaseChoice.name() + "_"
                            + session;

            boolean shouldCollect =
                    guardService.shouldCollect(
                            symbol,
                            expiry,
                            timeOfDay,
                            dataBaseChoice,
                            intradayKey);

            if (!shouldCollect) {
                return;
            }

            CollectionTask task =
                    CollectionTask.builder()
                            .symbol(symbol)
                            .expiryDate(expiry)
                            .instrumentType(instrumentType)
                            .intradayKey(intradayKey)
                            .dataBaseChoice(dataBaseChoice)
                            .build();

            consumer.accept(task);

        } catch (Exception ex) {

            log.error(
                    "Failed creating task for symbol {} expiry {}",
                    symbol,
                    expiry,
                    ex);
        }
    }
}