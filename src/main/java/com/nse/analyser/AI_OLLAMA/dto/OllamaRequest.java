package com.nse.analyser.AI_OLLAMA.dto;

public record OllamaRequest(
        String model,
        String prompt,
        boolean stream
) {
}
