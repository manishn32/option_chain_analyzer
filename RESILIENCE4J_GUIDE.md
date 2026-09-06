# Resilience4j Configuration Guide

This document explains the resilience4j integration for the Market Projection module, providing circuit breaker, rate limiter, and retry capabilities for API resilience.

## Overview

Resilience4j protects the projection service from:
- **Circuit Breaker**: Prevents cascading failures by stopping calls when failure rate is high
- **Rate Limiter**: Controls request throughput to prevent overload
- **Retry**: Automatically retries failed operations

## Configuration (application.yml)

### Circuit Breaker

**projectionService** - Primary service CB:
- Triggers OPEN when ≥50% of 10 calls fail
- Waits 5 seconds before attempting HALF_OPEN
- Allows 3 test calls in HALF_OPEN state
- Records: `SQLException`, `DataIntegrityViolationException`

**projectionController** - API endpoint CB:
- Triggers OPEN when ≥60% of 5 calls fail
- Waits 3 seconds before attempting HALF_OPEN
- Looser thresholds for user-facing endpoints

### Rate Limiter

| Endpoint | Limit | Period | Use Case |
|----------|-------|--------|----------|
| `/calculate` | 10 req | 1 min | Heavy computation |
| `/latest` | 60 req | 1 min | Read-only, fast |
| `/dashboard` | 30 req | 1 min | Multi-timeframe aggregation |
| `/accuracy` | 5 req | 5 min | Heavy analysis |

### Retry

**projectionService** - Service-level retries:
- Max 3 attempts
- 1 second wait between attempts
- Retries: `SQLException`, `DataIntegrityViolationException`, `ConnectException`

**projectionRepository** - Repository-level retries:
- Max 2 attempts
- 500ms wait between attempts
- Retries: `SQLException`, `DataIntegrityViolationException`

## Implementation

### Service Layer (MarketProjectionService)

```java
@CircuitBreaker(name = "projectionService", fallbackMethod = "calculateAndStoreFallback")
@Retry(name = "projectionService")
public MarketProjection calculateAndStore(...) {
    // Service logic
}

// Fallback called when circuit is OPEN
public MarketProjection calculateAndStoreFallback(
        String timeframe,
        String underlying,
        LocalDate expiryDate,
        int limit,
        Throwable ex) {
    return MarketProjection.builder()
            .marketRegime("SERVICE_UNAVAILABLE")
            .commentary("Service temporarily unavailable: " + ex.getMessage())
            .build();
}
```

### Controller Layer (ProjectionController)

```java
@GetMapping("/latest")
@RateLimiter(name = "projectionLatest")
public MarketProjection latest(...) {
    return service.latest(...);
}
```

Rate limiter returns HTTP 429 (Too Many Requests) when limit exceeded.

## Monitoring & Health

### Health Indicator

Access via Spring Boot Actuator:
```
GET /actuator/health/circuitbreakers
GET /actuator/health/ratelimiters
```

### Logging

Enable debug logging for resilience4j events:
```yaml
logging:
  level:
    io.github.resilience4j: DEBUG
```

Events logged:
- CircuitBreaker state transitions (CLOSED → OPEN → HALF_OPEN)
- RateLimiter available permits
- Retry attempts

### Configuration Class

`Resilience4jConfig.java` registers event listeners for:
- CircuitBreaker creation/removal
- CircuitBreaker state & failure rate changes
- RateLimiter creation/removal
- RateLimiter available permits
- Retry events

## Testing

### Simulate Circuit Breaker

Trigger 5+ consecutive failures to OPEN circuit:
```bash
for i in {1..6}; do
  curl "http://localhost:8080/api/projections/latest?timeframe=1m&expiryDate=2026-09-10"
done
```

Circuit will return fallback response for subsequent calls.

### Simulate Rate Limiter

Exceed per-minute limit on `/latest` (60 req/min):
```bash
for i in {1..65}; do
  curl "http://localhost:8080/api/projections/latest?timeframe=1m&expiryDate=2026-09-10" &
done
```

Requests exceeding limit return HTTP 429.

### Test Retry

Repository retries 2x on SQL connection failure before giving up.

## Adjusting Configuration

Edit `src/main/resources/application.yml` under `resilience4j` section:

### Increase Circuit Breaker Sensitivity
```yaml
projectionService:
  failureRateThreshold: 40.0  # Trigger sooner
  slidingWindowSize: 20       # Look at more calls
```

### Increase Rate Limit
```yaml
projectionLatest:
  limitForPeriod: 120  # Double from 60
```

### Increase Retry Attempts
```yaml
projectionService:
  maxAttempts: 5       # More aggressive retry
  waitDuration: 2000   # 2 second wait
```

## Fallback Strategies

### calculateAndStoreFallback
Returns minimal projection with "SERVICE_UNAVAILABLE" regime.

### dashboardFallback
Returns empty dashboard (all timeframes null) with degraded UX.

### trackAccuracyFallback
Returns 0 samples, 0% accuracy (indicates data unavailable).

## Production Tuning

For high-traffic production:

1. **Increase rate limits** by 2-3x if legitimate load is higher
2. **Lower circuit breaker threshold** (40-50%) to fail fast on persistent issues
3. **Increase wait duration** before HALF_OPEN to allow backend recovery
4. **Reduce retry attempts** to fail faster and shed load
5. **Monitor via actuator** and adjust based on failure patterns

## Dependencies

- `resilience4j-spring-boot3` (2.0.3) - Spring Boot 3/4 integration
- `resilience4j-circuitbreaker` (2.0.3) - Circuit breaker
- `resilience4j-ratelimiter` (2.0.3) - Rate limiting
- `resilience4j-retry` (2.0.3) - Retry logic
- `resilience4j-spring-aspects` (2.0.3) - AOP aspect weaving

## References

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)

