package com.bank.notificationservice.service;

import com.bank.notificationservice.dto.AccountEventNotificationRequest;
import com.bank.notificationservice.dto.AccountStatusChangeNotificationRequest;
import com.bank.notificationservice.dto.HighValueTransactionNotificationRequest;
import com.bank.notificationservice.model.AccountEventType;
import com.bank.notificationservice.support.EmailMessage;
import com.bank.notificationservice.support.SmsMessage;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class NotificationComposer {

    private static final int SMS_MAX_LENGTH = 140;
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter TRANSACTION_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a").withZone(DEFAULT_ZONE);

    public EmailMessage composeHighValueTransaction(HighValueTransactionNotificationRequest request, BigDecimal threshold) {
        String subject = "High value " + request.getTxnType() + " alert for account " + request.getAccountNumber();
        String formattedAmount = formatCurrency(request.getAmount(), request.getCurrency());
        String formattedThreshold = threshold != null ? formatCurrency(threshold, request.getCurrency()) : null;

        StringBuilder body = new StringBuilder()
                .append("Hi ").append(request.getCustomerName()).append(",\n\n")
                .append("We detected a high value ").append(request.getTxnType().toLowerCase(Locale.ENGLISH))
                .append(" on your account ").append(request.getAccountNumber()).append(".\n")
                .append("Amount: ").append(formattedAmount).append("\n");

        if (formattedThreshold != null) {
            body.append("Notification threshold: ").append(formattedThreshold).append("\n");
        }

        if (request.getCounterparty() != null && !request.getCounterparty().isBlank()) {
            body.append("Counterparty: ").append(request.getCounterparty()).append("\n");
        }
        if (request.getReference() != null && !request.getReference().isBlank()) {
            body.append("Reference: ").append(request.getReference()).append("\n");
        }

        body.append("\nIf you did not authorize this transaction, please contact support immediately.\n\n")
                .append("Regards,\n")
                .append("Banking Alerts Team");

        return new EmailMessage(request.getCustomerEmail(), subject, body.toString());
    }

    public EmailMessage composeAccountStatusChange(AccountStatusChangeNotificationRequest request) {
        String subject = "Account status updated for account " + request.getAccountNumber();
        StringBuilder body = new StringBuilder()
                .append("Hi ").append(request.getCustomerName()).append(",\n\n")
                .append("There has been an update to the status of your account ").append(request.getAccountNumber()).append(".\n")
                .append("Previous status: ").append(request.getPreviousStatus()).append("\n")
                .append("Current status: ").append(request.getCurrentStatus()).append("\n");

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            body.append("Additional details: ").append(request.getRemarks()).append("\n");
        }

        body.append("\nIf you were not expecting this update, please reach out to customer support.\n\n")
                .append("Regards,\n")
                .append("Banking Alerts Team");

        return new EmailMessage(request.getCustomerEmail(), subject, body.toString());
    }

    public EmailMessage composeAccountEvent(AccountEventNotificationRequest request) {
        String subject = eventSubject(request.getEventType(), request.getAccountNumber());
        StringBuilder body = new StringBuilder()
                .append("Hi ").append(request.getCustomerName()).append(",\n\n")
                .append(eventMessage(request)).append("\n");

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            body.append("Details: ").append(request.getDescription()).append("\n\n");
        } else {
            body.append("\n");
        }

        body.append("If you have any questions, please contact customer care.\n\n")
                .append("Regards,\n")
                .append("Banking Alerts Team");

        return new EmailMessage(request.getCustomerEmail(), subject, body.toString());
    }

    public SmsMessage composeHighValueTransactionSms(HighValueTransactionNotificationRequest request) {
        String amount = formatAmountForSms(request.getAmount());
        String transactionTime = formatTransactionTime(request.getTransactionTime());
        String body = "Dear %s, your account was debited with ₹%s on %s."
                .formatted(request.getCustomerName(), amount, transactionTime);
        return new SmsMessage(request.getCustomerPhone(), trimSms(body));
    }

    public SmsMessage composeAccountStatusChangeSms(AccountStatusChangeNotificationRequest request) {
        StringBuilder body = new StringBuilder()
                .append("Dear ")
                .append(request.getCustomerName())
                .append(", your account ")
                .append(request.getAccountNumber())
                .append(" status changed from ")
                .append(request.getPreviousStatus())
                .append(" to ")
                .append(request.getCurrentStatus())
                .append(".");

        if (request.getRemarks() != null && !request.getRemarks().isBlank()) {
            body.append(" Details: ").append(request.getRemarks());
        }

        body.append(" Regards, Banking Alerts Team");

        return new SmsMessage(request.getCustomerPhone(), trimSms(body.toString()));
    }

    public SmsMessage composeAccountEventSms(AccountEventNotificationRequest request) {
        StringBuilder body = new StringBuilder()
                .append("Dear ")
                .append(request.getCustomerName())
                .append(", ")
                .append(eventMessage(request))
                .append(".");

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            body.append(" Details: ").append(request.getDescription());
        }

        body.append(" Regards, Banking Alerts Team");

        return new SmsMessage(request.getCustomerPhone(), trimSms(body.toString()));
    }

    private String formatCurrency(BigDecimal value, String currencyCode) {
        try {
            NumberFormat customFormatter = NumberFormat.getCurrencyInstance(Locale.US);
            customFormatter.setCurrency(java.util.Currency.getInstance(currencyCode));
            return customFormatter.format(value);
        } catch (Exception ignored) {
            NumberFormat fallback = NumberFormat.getCurrencyInstance(Locale.US);
            return fallback.format(value);
        }
    }

    private String eventSubject(AccountEventType type, String accountNumber) {
        return switch (type) {
            case ACCOUNT_NUMBER_UPDATED -> "Account number updated for account " + accountNumber;
            case CONTACT_INFORMATION_UPDATED -> "Contact information updated";
            case DOCUMENT_UPDATED -> "Documents updated for your account";
            case LOAN_TAKEN -> "Loan disbursal confirmation";
            case LOAN_CLEARED -> "Congratulations! Your loan is closed";
            case BILL_CLEARED -> "Bill payment confirmation";
        };
    }

    private String eventMessage(AccountEventNotificationRequest request) {
        return switch (request.getEventType()) {
            case ACCOUNT_NUMBER_UPDATED ->
                    "Your account number was recently updated for account " + request.getAccountNumber() + ".";
            case CONTACT_INFORMATION_UPDATED ->
                    "Your contact information linked to account " + request.getAccountNumber() + " was updated.";
            case DOCUMENT_UPDATED ->
                    "New documents were added or existing documents were updated for your account.";
            case LOAN_TAKEN ->
                    "We have processed your new loan request successfully.";
            case LOAN_CLEARED ->
                    "We have received full repayment of your loan associated with account " + request.getAccountNumber() + ".";
            case BILL_CLEARED ->
                    "Your recent bill has been cleared for account " + request.getAccountNumber() + ".";
        };
    }

    private String formatAmountForSms(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ENGLISH);
        DecimalFormat formatter = new DecimalFormat("##,##,##0.00", symbols);
        return formatter.format(amount);
    }

    private String formatTransactionTime(Instant timestamp) {
        return TRANSACTION_TIME_FORMATTER.format(timestamp != null ? timestamp : Instant.now());
    }

    private String trimSms(String value) {
        if (value.length() <= SMS_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, SMS_MAX_LENGTH - 3) + "...";
    }
}
