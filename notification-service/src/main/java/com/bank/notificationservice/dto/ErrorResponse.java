package com.bank.notificationservice.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(String message, Instant timestamp, Map<String, String> errors) {

    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, Instant.now(), Map.of());
    }

    public static ErrorResponse of(String message, Map<String, String> errors) {
        return new ErrorResponse(message, Instant.now(), errors);
    }
}

