package com.nse.analyser.services.impl;


import com.nse.analyser.config.collectionJob.NseRequestThrottle;
import com.nse.analyser.constants.NseConstants;
import com.nse.analyser.exceptions.NseForbiddenException;
import com.nse.analyser.exceptions.NseSessionExpiredException;
import com.nse.analyser.services.NseRestClient;
import com.nse.analyser.services.NseSessionManager;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class NseRestClientImpl implements NseRestClient {
    private final NseRequestThrottle nseRequestThrottle;
    private final NseSessionManager sessionManager;
//    @Override
    @RateLimiter(name = "nseApi")
    @CircuitBreaker(name = "nseApi", fallbackMethod = "fallback")
    public <T> T get_wasWorkingGreat(
            String url,
            Class<T> responseType) {

        try {

            ensureSession();

            return execute(
                    url,
                    responseType);

        } catch (NseSessionExpiredException ex) {

            sessionManager.refreshSession();

            return execute(
                    url,
                    responseType);
        }
    }
        @Override
        @RateLimiter(name = "nseApi")
        @CircuitBreaker(name = "nseApi", fallbackMethod = "fallback")
        @Retry(name = "nseApi")
        public <T> T get(String url, Class<T> responseType) {

            return nseRequestThrottle.execute(() -> {

                try {

                    ensureSession();

                    return execute(url, responseType);

                } catch (NseSessionExpiredException ex) {

                    sessionManager.refreshSession();

                    return execute(url, responseType);
                }
            });
        }


    private void ensureSession() {
        if(sessionManager.expired()){
            log.info("Nse Session expired, refreshing");
            sessionManager.refreshSession();
        }
    }

    private <T> T execute(
            String url,
            Class<T> responseType) {

        try {

            return sessionManager
                    .getClient()
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(responseType);

        }
        catch (RestClientResponseException ex){
            if(ex.getStatusCode().value() == 401){
                throw new NseSessionExpiredException();
            }else if(ex.getStatusCode().value() == 403){
                throw new NseForbiddenException("403 response",ex.getResponseBodyAsString());
            }
            throw ex;
            }
        catch (Exception ex) {

            String message =
                    ex.getMessage() == null
                            ? ""
                            : ex.getMessage();
            log.error("Error while executing request to NSE API: {}", message, ex);
            if (message.contains("401")) {

                throw new NseSessionExpiredException();
            }else if(message.contains("403")){
                throw new NseForbiddenException("403 response",ex.getMessage());
            }
            throw ex;
        }
    }

    private <T> T fallback(String url, Class<T> responseType, Throwable ex) {
        log.error("NSE circuit breaker Fallback triggered for url: {} due to exception: {}", url, ex.getMessage());
        throw new RuntimeException("NSE API is currently unavailable. Please try again later.", ex);
    }
}