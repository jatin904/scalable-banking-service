package com.bank.notificationservice.service;

import com.bank.notificationservice.config.NotificationProperties;
import com.bank.notificationservice.support.NotificationDeliveryException;
import com.bank.notificationservice.support.SmsMessage;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SmsDispatcher.class);

    private final NotificationProperties properties;

    public SmsDispatcher(NotificationProperties properties) {
        this.properties = properties;
        /*
         * We don't initialize Twilio here unless mockDelivery is disabled -
         * that prevents failures in environments where credentials are not provided.
         */
        if (!properties.sms().mockDelivery()) {
            String sid = properties.sms().accountSid();
            String token = properties.sms().authToken();
            if (sid != null && token != null) {
                Twilio.init(sid, token);
            }
        }
    }

    public void dispatch(SmsMessage message) {
        if (properties.sms().mockDelivery()) {
            log.info("Mock SMS delivery: to={} body='{}'", message.to(), message.body());
            return;
        }

        String sanitizedPhone = sanitize(message.to());

        String accountSid = properties.sms().accountSid();
        String authToken = properties.sms().authToken();
        String fromNumber = properties.sms().fromNumber();

        if (accountSid == null || authToken == null || fromNumber == null) {
            throw new NotificationDeliveryException("Twilio SMS delivery is enabled but Twilio credentials (accountSid/authToken/fromNumber) are not configured", null);
        }

        try {
            // Ensure Twilio is initialized in case it wasn't in constructor
            Twilio.init(accountSid, authToken);

            Message.creator(
                            new PhoneNumber(sanitizedPhone),
                            new PhoneNumber(fromNumber),
                            message.body())
                    .create();

            log.info("Dispatched SMS notification to {} via Twilio", message.to());
        } catch (ApiException ex) {
            throw new NotificationDeliveryException("Failed to send notification SMS via Twilio", ex);
        }
    }

    private String sanitize(String phone) {
        return phone.replaceAll("[^0-9+]", "");
    }
}
