package com.iitsm.infrastructure.notification;

import com.iitsm.domain.exception.NotificationFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Pattern: Adapter.
 * Adapts Spring Mail (JavaMailSender) to the NotificationChannel interface.
 * Dispatches HTML/text emails to user email addresses via Mailpit or SMTP.
 */
@Component
public class EmailChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);
    private static final String CHANNEL_NAME = "EMAIL";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String fromAddress;

    public EmailChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${iitsm.mail.from:no-reply@nimbusfinserve.com}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(String recipientEmail, String subject, String message) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("JavaMailSender bean unavailable. Email to '{}' simulated: [{}] {}", recipientEmail, subject, message);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(recipientEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email notification sent to '{}' with subject: {}", recipientEmail, subject);
        } catch (MailException ex) {
            log.warn("Failed to deliver email to '{}': {}", recipientEmail, ex.getMessage());
            throw new NotificationFailureException(CHANNEL_NAME, recipientEmail, ex.getMessage(), ex);
        }
    }
}

