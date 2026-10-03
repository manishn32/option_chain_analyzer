package com.nse.analyser.config.collectionJob;

import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

@Component
public class NseRequestThrottle {

    private final Semaphore semaphore = new Semaphore(3, true);

    public <T> T execute(Callable<T> task) {

        try {
            semaphore.acquire();

            try {
                return task.call();
            } finally {
                Thread.sleep(500); // 1 second gap between requests
                semaphore.release();
            }

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}