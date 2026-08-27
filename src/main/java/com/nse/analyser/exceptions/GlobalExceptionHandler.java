package com.nse.analyser.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NseSessionExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleNseSessionExpired(NseSessionExpiredException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "status", "ERROR",
                "errorCode", "NSE_SESSION_EXPIRED",
                "message", ex.getMessage() != null ? ex.getMessage() : "NSE session expired",
                "stackTrace", ex.getStackTrace(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(NseForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleNseForbidden(NseForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "ERROR",
                "errorCode", "NSE_FORBIDDEN",
                "message", ex.getMessage() != null ? ex.getMessage() : "NSE forbidden response",
                "stackTrace", ex.getStackTrace(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "status", "ERROR",
                "errorCode", "INVALID_REQUEST",
                "message", ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                "stackTrace", ex.getStackTrace(),
                "timestamp", Instant.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", "ERROR",
                "errorCode", "INTERNAL_SERVER_ERROR",
                "message", "Unexpected error while processing request",
                "stackTrace", ex.getStackTrace(),
                "timestamp", Instant.now().toString()
        ));
    }
}