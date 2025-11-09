package com.bank.notificationservice.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(Mail mail, Thresholds thresholds, Sms sms) {

    private static final String DEFAULT_FROM_ADDRESS = "noreply@bank.example";
    private static final boolean DEFAULT_MAIL_MOCK_DELIVERY = true;
    private static final BigDecimal DEFAULT_THRESHOLD_AMOUNT = BigDecimal.valueOf(10_000L);
    private static final String DEFAULT_SMS_API_KEY = "demo-api-key";
    private static final String DEFAULT_SMS_BASE_URL = "https://2factor.in/API/V1";
    private static final boolean DEFAULT_SMS_MOCK_DELIVERY = true;
    private static final String DEFAULT_SMS_SENDER_ID = "TFCTOR";

    private static final Mail DEFAULT_MAIL = new Mail(DEFAULT_FROM_ADDRESS, DEFAULT_MAIL_MOCK_DELIVERY);
    private static final Thresholds DEFAULT_THRESHOLDS = new Thresholds(DEFAULT_THRESHOLD_AMOUNT);
    /* Twilio fields are optional and default to null. Keep existing SMS defaults for backwards compatibility. */
    private static final Sms DEFAULT_SMS = new Sms(DEFAULT_SMS_API_KEY, DEFAULT_SMS_BASE_URL, DEFAULT_SMS_SENDER_ID, DEFAULT_SMS_MOCK_DELIVERY, null, null, null);

    public NotificationProperties {
        mail = mail != null ? mail : DEFAULT_MAIL;
        thresholds = thresholds != null ? thresholds : DEFAULT_THRESHOLDS;
        sms = sms != null ? sms : DEFAULT_SMS;
    }

    public static record Mail(
            @Email @NotBlank String from,
            @DefaultValue("true") boolean mockDelivery) {

        public Mail {
            from = from != null ? from : DEFAULT_FROM_ADDRESS;
        }
    }

    public static record Thresholds(@Positive BigDecimal highValueTransaction) {

        public Thresholds {
            highValueTransaction = highValueTransaction != null ? highValueTransaction : DEFAULT_THRESHOLD_AMOUNT;
        }
    }

    public static record Sms(
            @NotBlank String apiKey,
            @NotBlank String baseUrl,
            @NotBlank String senderId,
            @DefaultValue("true") boolean mockDelivery,
            /* Twilio credentials - optional; when present the service will use Twilio to send SMS */
            String accountSid,
            String authToken,
            String fromNumber) {

        public Sms {
            apiKey = apiKey != null ? apiKey : DEFAULT_SMS_API_KEY;
            baseUrl = baseUrl != null ? baseUrl : DEFAULT_SMS_BASE_URL;
            senderId = senderId != null ? senderId : DEFAULT_SMS_SENDER_ID;
            /* accountSid, authToken and fromNumber intentionally left nullable - no defaults */
        }
    }
}
