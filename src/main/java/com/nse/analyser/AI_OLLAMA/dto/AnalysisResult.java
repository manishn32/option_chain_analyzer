package com.nse.analyser.AI_OLLAMA.dto;

public record AnalysisResult(

        String symbol,

        double spot,

        double pcr,

        double vix,

        long callOi,

        long putOi,

        double maxPain,

        int support1,

        int support2,

        int resistance1,

        int resistance2,

        int longBuildUps,

        int shortBuildUps,

        int shortCovering,

        int longUnwinding,

        String sentiment

) {
}