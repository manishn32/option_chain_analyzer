package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.dtos.nseResponse.DataFillRecord;
import com.nse.analyser.enums.DataBaseChoice;
import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.MDC;

import static com.nse.analyser.utils.DateTimeFormatters.expiryDateFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionExecutorService {

    @Qualifier("mongoCollectionExecutor")
    private final ThreadPoolExecutor mongoExecutor;
    @Qualifier("clickHouseCollectionExecutor")
    private final ThreadPoolExecutor clickHouseExecutor;
    private final CollectionStateService stateService;
    private final NseDataCollectionJob job;

    public void execute_advanced(
            List<CollectionTask> tasks,
            TimeOfDay timeOfDay,
            DataBaseChoice databaseChoice
    ) {

        ExecutorService executor =
                databaseChoice == DataBaseChoice.MongoDB
                        ? mongoExecutor
                        : clickHouseExecutor;

        tasks.forEach(task ->
                CompletableFuture.runAsync(
                        () -> process(
                                task,
                                timeOfDay,
                                databaseChoice
                        ),
                        executor
                ).exceptionally(ex->{
                    log.error("Unexpected async error", ex);
                    return null;
                })
        );
    }
    public void execute(
            List<CollectionTask> tasks,
            TimeOfDay timeOfDay,
            DataBaseChoice databaseChoice
    ) {

        ExecutorService executor =
                databaseChoice == DataBaseChoice.MongoDB
                        ? mongoExecutor
                        : clickHouseExecutor;

        log.info(
                "Submitting {} tasks to {} executor",
                tasks.size(),
                databaseChoice
        );

//        tasks.forEach(task ->
//                CompletableFuture.runAsync(
//                        () -> process(task, timeOfDay, databaseChoice),
//                        executor
//                ).exceptionally(ex -> {
//                    log.error("Unexpected async error", ex);
//                    return null;
//                })
//        );

        tasks.forEach(task ->
                executor.submit(
                        () -> process(
                                task,
                                timeOfDay,
                                databaseChoice
                        )
                )
        );

    }

//    public void execute_old_next_level_thing(
//            List<CollectionTask> tasks,
//            TimeOfDay timeOfDay,
//            DataBaseChoice dataBaseChoice
//    ) {
//
//        List<CompletableFuture<Void>> futures =
//                tasks.stream()
//                        .map(task ->
//                                CompletableFuture.runAsync(
//                                        () -> process(task,timeOfDay,dataBaseChoice),
//                                        executorService
//                                )
//                        )
//                        .toList();
//
//        CompletableFuture.allOf(
//                futures.toArray(
//                        new CompletableFuture[0]
//                )
//        ).join();
//    }

    private void process(
            CollectionTask task,
            TimeOfDay timeOfDay,
            DataBaseChoice dataBaseChoice
    ) {

        try {
            MDC.put("symbol", task.getSymbol());
            MDC.put("db", dataBaseChoice.name());
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

//            log.error(
//                    "Failed Symbol: {} expiring {} with error {} run time flag {} to {}",
//                    task.getSymbol(),
//                    task.getExpiryDate(),
//                    exception,
//                    timeOfDay,
//                    dataBaseChoice
//            );
            log.error(
                    "Failed Symbol {} Expiry {} TimeOfDay {} Database {}",
                    task.getSymbol(),
                    task.getExpiryDate(),
                    timeOfDay,
                    dataBaseChoice,
                    exception
            );

            stateService.markFailure(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    task.getTimeOfDay(),
                    dataBaseChoice
            );
        }finally {
            MDC.clear();
        }
    }
}