package com.bank.notificationservice.service;

import com.bank.notificationservice.config.NotificationProperties;
import com.bank.notificationservice.dto.AccountEventNotificationRequest;
import com.bank.notificationservice.dto.AccountStatusChangeNotificationRequest;
import com.bank.notificationservice.dto.HighValueTransactionNotificationRequest;
import com.bank.notificationservice.model.NotificationRecord;
import com.bank.notificationservice.repository.NotificationAuditRepository;
import com.bank.notificationservice.support.EmailMessage;
import com.bank.notificationservice.support.SmsMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String HIGH_VALUE_TYPE = "HIGH_VALUE_TRANSACTION";
    private static final String ACCOUNT_STATUS_TYPE = "ACCOUNT_STATUS_CHANGE";
    private static final String ACCOUNT_EVENT_TYPE = "ACCOUNT_EVENT";

    private final NotificationComposer composer;
    private final EmailDispatcher emailDispatcher;
    private final SmsDispatcher smsDispatcher;
    private final NotificationProperties properties;
    private final NotificationAuditRepository auditRepository;

    public NotificationService(NotificationComposer composer,
            EmailDispatcher emailDispatcher,
            SmsDispatcher smsDispatcher,
            NotificationProperties properties,
            NotificationAuditRepository auditRepository) {
        this.composer = composer;
        this.emailDispatcher = emailDispatcher;
        this.smsDispatcher = smsDispatcher;
        this.properties = properties;
        this.auditRepository = auditRepository;
    }

    public boolean handleHighValueTransaction(HighValueTransactionNotificationRequest request) {
        BigDecimal threshold = resolveThreshold(request);
        if (request.getAmount().compareTo(threshold) < 0) {
            log.info("Skipping high value alert for transaction below threshold: amount={} threshold={} account={}",
                    request.getAmount(), threshold, request.getAccountNumber());
            return false;
        }
        EmailMessage emailMessage = composer.composeHighValueTransaction(request, threshold);
        emailDispatcher.dispatch(emailMessage);
        auditRepository.save(NotificationRecord.email(emailMessage.to(), emailMessage.subject(), emailMessage.body(), HIGH_VALUE_TYPE));

        SmsMessage smsMessage = composer.composeHighValueTransactionSms(request);
        smsDispatcher.dispatch(smsMessage);
        auditRepository.save(NotificationRecord.sms(smsMessage.to(), smsMessage.body(), HIGH_VALUE_TYPE));
        return true;
    }

    public boolean handleAccountStatusChange(AccountStatusChangeNotificationRequest request) {
        EmailMessage emailMessage = composer.composeAccountStatusChange(request);
        emailDispatcher.dispatch(emailMessage);
        auditRepository.save(NotificationRecord.email(emailMessage.to(), emailMessage.subject(), emailMessage.body(), ACCOUNT_STATUS_TYPE));

        SmsMessage smsMessage = composer.composeAccountStatusChangeSms(request);
        smsDispatcher.dispatch(smsMessage);
        auditRepository.save(NotificationRecord.sms(smsMessage.to(), smsMessage.body(), HIGH_VALUE_TYPE));
        return true;
    }

    public boolean handleAccountEvent(AccountEventNotificationRequest request) {
        EmailMessage emailMessage = composer.composeAccountEvent(request);
        emailDispatcher.dispatch(emailMessage);
        auditRepository.save(NotificationRecord.email(emailMessage.to(), emailMessage.subject(), emailMessage.body(), ACCOUNT_EVENT_TYPE));

        SmsMessage smsMessage = composer.composeAccountEventSms(request);
        smsDispatcher.dispatch(smsMessage);
        auditRepository.save(NotificationRecord.sms(smsMessage.to(), smsMessage.body(), ACCOUNT_EVENT_TYPE));
        return true;
    }

    public List<NotificationRecord> fetchNotificationsForPastWeek() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return auditRepository.findSince(since);
    }

    private BigDecimal resolveThreshold(HighValueTransactionNotificationRequest request) {
        if (request.getThresholdOverride() != null && request.getThresholdOverride().signum() > 0) {
            return request.getThresholdOverride();
        }
        return properties.thresholds().highValueTransaction();
    }
}
