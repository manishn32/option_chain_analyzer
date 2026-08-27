package com.nse.analyser.services.impl;


import com.nse.analyser.constants.NseConstants;
import com.nse.analyser.exceptions.NseForbiddenException;
import com.nse.analyser.exceptions.NseSessionExpiredException;
import com.nse.analyser.services.NseRestClient;
import com.nse.analyser.services.NseSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NseRestClientImpl implements NseRestClient {

    private final NseSessionManager sessionManager;

    public <T> T get(
            String url,
            Class<T> responseType) {

        try {

            if (sessionManager.expired()) {
                log.info("Session expired");
                sessionManager.refreshSession();
            }

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

        } catch (Exception ex) {

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
}