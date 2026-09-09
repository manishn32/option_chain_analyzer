package com.nse.analyser.config.collectionJob;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class CollectionExecutorConfig {

    @Bean("mongoCollectionExecutor")
    @Primary
    public ThreadPoolExecutor mongoCollectionExecutor() {

        return new ThreadPoolExecutor(
                20,
                40,
                3,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder()
                        .setNameFormat("mongo-%d")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("clickHouseCollectionExecutor")
    public ThreadPoolExecutor clickHouseCollectionExecutor() {

        return new ThreadPoolExecutor(
                10,
                20,
                3,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(5000),
                new ThreadFactoryBuilder()
                        .setNameFormat("clickhouse-%d")
                        .build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

//    @Bean("retryExecutor")
//    public ThreadPoolExecutor retryExecutor() {
//
//        return new ThreadPoolExecutor(
//                2,
//                5,
//                5,
//                TimeUnit.MINUTES,
//                new LinkedBlockingQueue<>(100)
//        );
//    }
}