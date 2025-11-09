package com.bank.notificationservice.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationRecord(
        UUID id,
        NotificationChannel channel,
        String recipient,
        String subject,
        String preview,
        String notificationType,
        Instant timestamp) {

    private static final int PREVIEW_LIMIT = 180;

    public static NotificationRecord email(String recipient, String subject, String body, String notificationType) {
        return new NotificationRecord(
                UUID.randomUUID(),
                NotificationChannel.EMAIL,
                recipient,
                subject,
                truncate(body),
                notificationType,
                Instant.now());
    }

    public static NotificationRecord sms(String recipient, String body, String notificationType) {
        return new NotificationRecord(
                UUID.randomUUID(),
                NotificationChannel.SMS,
                recipient,
                null,
                truncate(body),
                notificationType,
                Instant.now());
    }

    private static String truncate(String body) {
        if (body == null || body.length() <= PREVIEW_LIMIT) {
            return body;
        }
        return body.substring(0, PREVIEW_LIMIT - 3) + "...";
    }
}
