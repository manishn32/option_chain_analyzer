package com.nse.analyser.AI_OLLAMA.controller;

import com.nse.analyser.AI_OLLAMA.dto.AnalysisResult;
import com.nse.analyser.AI_OLLAMA.service.CommentaryService;
import com.nse.analyser.AI_OLLAMA.service.OllamaClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai")
public class AiController {

    private final OllamaClient ollamaClient;
    private final CommentaryService commentaryService;

    @GetMapping("/test")
    public String test() {

        return ollamaClient.ask(
                "Explain Put Call Ratio in one sentence.");
    }

    @GetMapping("/commentary")
    public String commentary() {

        AnalysisResult result = getDummyResult();

        return commentaryService.generate(result);
    }

    private AnalysisResult getDummyResult() {
        return
                new AnalysisResult(
                        "NIFTY",
                        23061,
                        1.05,
                        14.42,
                        292062420,
                        307002605,
                        23050,
                        23000,
                        22900,
                        23100,
                        23200,
                        12,
                        28,
                        5,
                        8,
                        "NEUTRAL"
                );
    }

}