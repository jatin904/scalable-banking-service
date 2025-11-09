package com.bank.notificationservice.repository;

import com.bank.notificationservice.model.NotificationRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryNotificationAuditRepository implements NotificationAuditRepository {

    private final Deque<NotificationRecord> records = new ConcurrentLinkedDeque<>();

    @Override
    public void save(NotificationRecord record) {
        if (record != null) {
            records.add(record);
        }
    }

    @Override
    public List<NotificationRecord> findSince(Instant since) {
        return records.stream()
                .filter(record -> since == null || !record.timestamp().isBefore(since))
                .sorted(Comparator.comparing(NotificationRecord::timestamp).reversed())
                .collect(Collectors.toList());
    }
}
