package com.bank.notificationservice.dto;

import com.bank.notificationservice.model.NotificationRecord;
import java.time.Instant;

public record NotificationLogEntry(
        String id,
        String channel,
        String recipient,
        String subject,
        String preview,
        String notificationType,
        Instant timestamp) {

    public static NotificationLogEntry from(NotificationRecord record) {
        return new NotificationLogEntry(
                record.id().toString(),
                record.channel().name(),
                record.recipient(),
                record.subject(),
                record.preview(),
                record.notificationType(),
                record.timestamp());
    }
}
