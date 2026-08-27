package com.nse.analyser.services.impl;

import com.nse.analyser.constants.BrowserHeaders;
import com.nse.analyser.services.NseSessionManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;

import static com.nse.analyser.constants.NseConstants.NSE_REFERRER;
import static com.nse.analyser.constants.NseConstants.NSE_TOKEN_REFRESHER;

@Slf4j
@Getter
@Service
public class NseSessionManagerImpl implements NseSessionManager {

    private final CookieStore cookieStore =
            new BasicCookieStore();

    private RestClient restClient;

    private volatile Instant sessionCreationTime;

    public synchronized void initializeSession() {
            log.info("Initializing NSE session");
        try {

            PoolingHttpClientConnectionManager connectionManager =
                    new PoolingHttpClientConnectionManager();

            connectionManager.setMaxTotal(50);
            connectionManager.setDefaultMaxPerRoute(20);

            HttpClient httpClient =
                    HttpClients.custom()
                            .setConnectionManager(connectionManager)
                            .setDefaultCookieStore(cookieStore)
                            .build();

            HttpComponentsClientHttpRequestFactory factory =
                    new HttpComponentsClientHttpRequestFactory(httpClient);

            this.restClient =
                    RestClient.builder()
                            .requestFactory(factory)
                            .defaultHeader(
                                    HttpHeaders.USER_AGENT,
                                    BrowserHeaders.USER_AGENT)
                            .defaultHeader(
                                    HttpHeaders.ACCEPT,
                                    BrowserHeaders.ACCEPT)
                            .defaultHeader(
                                    HttpHeaders.ACCEPT_LANGUAGE,
                                    BrowserHeaders.ACCEPT_LANGUAGE)
                            .defaultHeader("priority",
                                    BrowserHeaders.PRIORITY)
                            .defaultHeader(
                                    HttpHeaders.REFERER,
                                    NSE_REFERRER)
                            .defaultHeader(
                                    "sec-ch-ua",
                                    BrowserHeaders.SEC_CH_UA)
                            .defaultHeader(
                                   "sec-ch-ua-mobile",
                                    BrowserHeaders.sec_ch_ua_mobile
                            )
                            .defaultHeader(
                                    "sec-ch-ua-platform",
                                    BrowserHeaders.sec_ch_ua_platform)
                            .defaultHeader(
                                    "sec-fetch-dest",
                                    BrowserHeaders.sec_fetch_dest
                            )
                            .defaultHeader(
                                    "sec-fetch-mode",
                                    BrowserHeaders.sec_fetch_mode
                            )
                            .defaultHeader(
                                    "sec-fetch-site",
                                    BrowserHeaders.sec_fetch_site
                            )
//                            .defaultHeader(HttpHeaders.ACCEPT_ENCODING,
//                                    BrowserHeaders.ACCEPT_ENCODING)
                            .build();

            bootstrap();

            sessionCreationTime = Instant.now();

            log.info(
                    "NSE session initialized. Cookies={}",
                    cookieStore.getCookies().size());

        } catch (Exception e) {
            log.error(
                    "Error initializing NSE session: {}",
                    e.getMessage(),
                    e);
            throw new IllegalStateException(
                    "Could not initialize NSE session",
                    e);
        }
    }

    @Override
    public RestClient getClient() {
        return restClient;
    }

    private void bootstrap() {

        restClient.get()
                .uri(NSE_TOKEN_REFRESHER)
                .retrieve()
                .toBodilessEntity();
    }

    public synchronized void refreshSession() {
        log.info("Refreshing session");
        cookieStore.clear();

        initializeSession();
    }

    public boolean expired() {

        if (sessionCreationTime == null) {
            return true;
        }

       return  Duration
                .between(
                        sessionCreationTime,
                        Instant.now())
                .toMinutes() >= 20;
    }
}
