package com.nse.analyser.AI_OLLAMA.service;

import com.nse.analyser.AI_OLLAMA.dto.AnalysisResult;
import org.springframework.stereotype.Service;

@Service
public class PromptBuilder {

    public String build(AnalysisResult r) {

        return """
                You are an NSE Option Chain Commentary Engine.

                Use ONLY supplied data.

                Symbol=%s

                Spot=%s

                PCR=%s

                VIX=%s

                Support1=%s
                Support2=%s

                Resistance1=%s
                Resistance2=%s

                MaxPain=%s

                LongBuildUps=%s
                ShortBuildUps=%s
                ShortCovering=%s
                LongUnwinding=%s

                Sentiment=%s
                """
                .formatted(
                        r.symbol(),
                        r.spot(),
                        r.pcr(),
                        r.vix(),
                        r.support1(),
                        r.support2(),
                        r.resistance1(),
                        r.resistance2(),
                        r.maxPain(),
                        r.longBuildUps(),
                        r.shortBuildUps(),
                        r.shortCovering(),
                        r.longUnwinding(),
                        r.sentiment()
                );
    }
}