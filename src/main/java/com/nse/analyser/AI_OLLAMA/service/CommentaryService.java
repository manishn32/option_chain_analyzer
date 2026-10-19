package com.nse.analyser.AI_OLLAMA.service;

import com.nse.analyser.AI_OLLAMA.dto.AnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentaryService {

    private final PromptBuilder promptBuilder;
    private final OllamaClient ollamaClient;

    public String generate(
            AnalysisResult result) {

        return ollamaClient.ask(
                promptBuilder.build(result));
    }
}