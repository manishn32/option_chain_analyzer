package com.nse.analyser.AI_OLLAMA.service;

import com.nse.analyser.AI_OLLAMA.config.OllamaProperties;
import com.nse.analyser.AI_OLLAMA.dto.OllamaRequest;
import com.nse.analyser.AI_OLLAMA.dto.OllamaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service

public class OllamaClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(@Qualifier(value = "aiClient") RestClient restClient, OllamaProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public String ask(String prompt) {

        OllamaRequest request =
                new OllamaRequest(
                        properties.getModel(),
                        prompt,
                        false
                );

        OllamaResponse response =
                restClient.post()
                        .uri("/api/generate")
                        .body(request)
                        .retrieve()
                        .body(OllamaResponse.class);

        return response.response();
    }
}