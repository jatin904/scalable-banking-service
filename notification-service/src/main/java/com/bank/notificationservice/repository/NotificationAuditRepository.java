package com.bank.notificationservice.repository;

import com.bank.notificationservice.model.NotificationRecord;
import java.time.Instant;
import java.util.List;

public interface NotificationAuditRepository {

    void save(NotificationRecord record);

    List<NotificationRecord> findSince(Instant since);
}
