package com.nse.analyser.AI_OLLAMA.client;

import com.nse.analyser.AI_OLLAMA.config.OllamaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfiguration {

    @Bean(name = "aiClient")
    RestClient ollamaRestClient(
            OllamaProperties properties) {

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}