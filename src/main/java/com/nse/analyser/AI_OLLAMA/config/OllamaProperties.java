package com.nse.analyser.AI_OLLAMA.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@Data
@ConfigurationProperties(prefix = "ai.ollama")
public class OllamaProperties {

    private String baseUrl;
    private String model;
}
