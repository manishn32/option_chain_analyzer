package com.nse.analyser.config.collectionJob;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorConfig {

    @Bean
    public ExecutorService collectionExecutor(){

        return Executors.newFixedThreadPool(
                5
        );
    }
}
