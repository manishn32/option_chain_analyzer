package com.nse.analyser.controller;

import com.nse.analyser.dtos.rest.AccuracySummary;
import com.nse.analyser.dtos.rest.ProjectionDashboard;
import com.nse.analyser.models.MarketProjection;
import com.nse.analyser.services.impl.MarketProjectionService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projections")
@RequiredArgsConstructor
public class ProjectionController {

    private final MarketProjectionService service;

    @PostMapping("/calculate")
    @RateLimiter(name = "projectionCalculate")
    public MarketProjection calculate(
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "NIFTY") String underlying,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate,
            @RequestParam(defaultValue = "1200") int limit
    ) {
        return service.calculateAndStore(timeframe, underlying, expiryDate, limit);
    }

    @GetMapping("/latest")
    @RateLimiter(name = "projectionLatest")
    public MarketProjection latest(
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "NIFTY") String underlying,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate
    ) {
        return service.latest(timeframe, underlying, expiryDate);
    }

    @GetMapping("/dashboard")
    @RateLimiter(name = "projectionDashboard")
    public ProjectionDashboard dashboard(
            @RequestParam(defaultValue = "NIFTY") String underlying,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate
    ) {
        return service.dashboard(underlying, expiryDate);
    }

    @PostMapping("/accuracy")
    @RateLimiter(name = "projectionAccuracy")
    public AccuracySummary accuracy(
            @RequestParam String timeframe,
            @RequestParam(defaultValue = "NIFTY") String underlying,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate
    ) {
        return service.trackAccuracy(timeframe, underlying, expiryDate);
    }

    @GetMapping("/accuracy/all")
    @RateLimiter(name = "projectionAccuracy")
    public List<AccuracySummary> accuracyAll(
            @RequestParam(defaultValue = "NIFTY") String underlying,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryDate
    ) {
        return service.trackAccuracyAll(underlying, expiryDate);
    }
}

