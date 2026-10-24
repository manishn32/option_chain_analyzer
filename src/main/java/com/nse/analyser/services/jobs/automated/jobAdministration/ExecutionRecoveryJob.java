package com.nse.analyser.services.jobs.automated.jobAdministration;

import com.nse.analyser.models.jobStatus.CollectionState;
import com.nse.analyser.models.jobStatus.CollectionStatus;
import com.nse.analyser.repositories.mongoRepositories.jobRepositories.CollectionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionRecoveryJob {

    private final CollectionStateRepository repository;

    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void recoverBrokenExecutions() {

        List<CollectionState> runningStates =
                repository.findByStatus(
                        CollectionStatus.IN_PROGRESS
                );

        for(CollectionState state : runningStates){

            state.setStatus(
                    CollectionStatus.FAILED
            );

            state.setErrorMessage(
                    "Application Restarted"
            );

            repository.save(state);
        }

        log.info(
                "Recovered {} executions",
                runningStates.size()
        );
    }
}