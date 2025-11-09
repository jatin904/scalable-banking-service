package com.bank.notificationservice.dto;

import java.time.Instant;

public record NotificationResponse(String message, Instant timestamp) {

    public static NotificationResponse of(String message) {
        return new NotificationResponse(message, Instant.now());
    }
}

