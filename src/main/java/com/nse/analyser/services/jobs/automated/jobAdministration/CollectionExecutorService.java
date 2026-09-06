package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.enums.TimeOfDay;
import com.nse.analyser.services.jobs.NseDataCollectionJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionExecutorService {

    private final ExecutorService executorService;

    private final CollectionStateService stateService;

    private final NseDataCollectionJob job;

    public void execute(
            List<CollectionTask> tasks,
            TimeOfDay timeOfDay
    ) {

        List<CompletableFuture<Void>> futures =
                tasks.stream()
                        .map(task ->
                                CompletableFuture.runAsync(
                                        () -> process(task,timeOfDay),
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
            TimeOfDay timeOfDay
    ) {

        try {

            stateService.markStarted(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    task.getInstrumentType(),
                    timeOfDay
            );

            long records =
                    job.fillData(
                            task.getSymbol(),
                            task.getExpiryDate(),
                            task.getInstrumentType()
                    );

            stateService.markSuccess(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    records,
                    task.getTimeOfDay()
            );

        } catch(Exception exception){

            log.error(
                    "Failed {} {}",
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    timeOfDay
            );

            stateService.markFailure(
                    task.getSymbol(),
                    task.getExpiryDate(),
                    exception,
                    task.getTimeOfDay()
            );
        }
    }
}