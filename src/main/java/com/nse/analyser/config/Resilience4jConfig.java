package com.nse.analyser.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Resilience4jConfig {

    public Resilience4jConfig(
            CircuitBreakerRegistry circuitBreakerRegistry,
            RateLimiterRegistry rateLimiterRegistry,
            RetryRegistry retryRegistry) {

        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> log.info("CircuitBreaker added: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> log.info("CircuitBreaker removed: {}", event.getRemovedEntry().getName()));

        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(event -> {

                    CircuitBreaker cb = event.getAddedEntry();

                    log.info("CircuitBreaker added: {}", cb.getName());

                    cb.getEventPublisher()
                            .onStateTransition(e ->
                                    log.info("CB [{}] state transition {}",
                                            cb.getName(),
                                            e.getStateTransition()))
                            .onFailureRateExceeded(e ->
                                    log.warn("CB [{}] failure rate exceeded",
                                            cb.getName()))
                            .onCallNotPermitted(e ->
                                    log.warn("CB [{}] call not permitted",
                                            cb.getName()));
                });
//        circuitBreakerRegistry.getEventPublisher()
//                .onEvent(event -> {
//                    CircuitBreaker cb = event.getCircuitBreaker();
//                    log.debug("CircuitBreaker [{}] state: {}, failureRate: {}%",
//                            cb.getName(),
//                            cb.getState(),
//                            String.format("%.2f", cb.getMetrics().getFailureRate()));
//                });

        rateLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> log.info("RateLimiter added: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> log.info("RateLimiter removed: {}", event.getRemovedEntry().getName()));

        rateLimiterRegistry.getEventPublisher()
                .onEntryAdded(event -> {

                    RateLimiter rl = event.getAddedEntry();

                    log.info("RateLimiter added: {}", rl.getName());

                    rl.getEventPublisher()
                            .onSuccess(e ->
                                    log.debug("RateLimiter [{}] success",
                                            rl.getName()))
                            .onFailure(e ->
                                    log.warn("RateLimiter [{}] denied request",
                                            rl.getName()));
                });
//        rateLimiterRegistry.getEventPublisher()
//                .onEvent(event -> {
//                    RateLimiter rl = event.getRateLimiter();
//                    log.debug("RateLimiter [{}] available permits: {}",
//                            rl.getName(),
//                            rl.getMetrics().getAvailablePermissions());
//                });

        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> log.info("Retry added: {}", event.getAddedEntry().getName()))
                .onEntryRemoved(event -> log.info("Retry removed: {}", event.getRemovedEntry().getName()));


        retryRegistry.getEventPublisher()
                .onEntryAdded(event -> {

                    Retry retry = event.getAddedEntry();

                    log.info("Retry added: {}", retry.getName());

                    retry.getEventPublisher()
                            .onRetry(e ->
                                    log.info("Retry [{}] attempt {}",
                                            retry.getName(),
                                            e.getNumberOfRetryAttempts()))
                            .onSuccess(e ->
                                    log.info("Retry [{}] success",
                                            retry.getName()))
                            .onError(e ->
                                    log.error("Retry [{}]",retry.getName()));
                });
//        retryRegistry.getEventPublisher()
//                .onEvent(event -> log.debug("Retry [{}] event: {}", event.getRetry().getName(), event.getEventType()));

    }
}

