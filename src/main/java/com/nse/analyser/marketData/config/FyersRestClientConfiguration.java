package com.nse.analyser.marketData.config;

import com.nse.analyser.marketData.components.FyersMarketDataProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class FyersRestClientConfiguration {

    private final FyersMarketDataProperties properties;

    @Bean
    public RestClient marketDataRestClient() {

        var requestFactory =
                ClientHttpRequestFactoryBuilder.simple()
//                        .connectTimeout(Duration.ofSeconds(5))
//                        .readTimeout(Duration.ofSeconds(30))
                        .build();

        return RestClient.builder()
                .baseUrl(properties.host())
                .defaultHeader(
                        "X-Internal-Api-Key",
                        properties.internalApiKey())
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }
}
