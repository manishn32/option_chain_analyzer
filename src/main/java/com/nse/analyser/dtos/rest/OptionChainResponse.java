package com.nse.analyser.dtos.rest;


public record OptionChainResponse(
        String status,
        String message,
        String symbol,
        Object data) {
}