package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.dtos.nseResponse.DataFillRecord;
import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.nse.analyser.utils.DateTimeFormatters.expiryDateFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionExecutorService {

    private final ExecutorService executorService;

    private final CollectionStateService stateService;

    private final NseDataCollectionJob job;

    public void execute(
            List<CollectionTask> tasks,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice
    ) {

        List<CompletableFuture<Void>> futures =
                tasks.stream()
                        .map(task ->
                                CompletableFuture.runAsync(
                                        () -> process(task,timeOfDay,dataBaseChoice),
                                        executorService
                                )
                        )
                        .toList();

        CompletableFuture.allOf(
                futures.toArray(
                        new CompletableFuture[0]
                )
        ).join();
    }

    private void process(
            CollectionTask task,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice
    ) {

        try {

            stateService.markStarted(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    task.getInstrumentType(),
                    timeOfDay,
                    dataBaseChoice,
                    task.getIntradayKey()
            );


            DataFillRecord records = dataBaseChoice == DataBaseChoice.MongoDB ?
                    job.fillData(task.getSymbol(), task.getExpiryDate(), task.getInstrumentType()) :
                    job.fillDataIntoClickhouseEveryMinute(task.getSymbol(), task.getExpiryDate(), task.getInstrumentType());

            stateService.markSuccess(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    records,
                    task.getTimeOfDay(),
                    dataBaseChoice,
                    task.getIntradayKey()
            );

        } catch(Exception exception){

            log.error(
                    "Failed Symbol: {} expiring {} with error {} run time flag {} to {}",
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    timeOfDay,
                    dataBaseChoice
            );

            stateService.markFailure(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    task.getTimeOfDay(),
                    dataBaseChoice
            );
        }
    }
}