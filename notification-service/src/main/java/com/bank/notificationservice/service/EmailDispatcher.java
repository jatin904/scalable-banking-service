package com.bank.notificationservice.service;

import com.bank.notificationservice.config.NotificationProperties;
import com.bank.notificationservice.support.EmailMessage;
import com.bank.notificationservice.support.NotificationDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailDispatcher(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    public void dispatch(EmailMessage message) {
        if (properties.mail().mockDelivery()) {
            log.info("Mock email delivery: to={} subject='{}' body={}", message.to(), message.subject(), message.body());
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            helper.setSubject(message.subject());
            helper.setTo(message.to());
            helper.setFrom(properties.mail().from());
            helper.setText(message.body(), false);
            mailSender.send(mimeMessage);
            log.info("Dispatched email notification to {}", message.to());
        } catch (MailException | MessagingException ex) {
            throw new NotificationDeliveryException("Failed to send notification email", ex);
        }
    }
}
