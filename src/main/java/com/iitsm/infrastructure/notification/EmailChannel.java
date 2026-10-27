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
    private final String overrideRecipient;

    public EmailChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${iitsm.mail.from:no-reply@nimbusfinserve.com}") String fromAddress) {
        this(mailSenderProvider, fromAddress, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EmailChannel(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${iitsm.mail.from:no-reply@nimbusfinserve.com}") String fromAddress,
            @Value("${iitsm.mail.override-recipient:${REAL_EMAIL_OVERRIDE:}}") String overrideRecipient) {
        this.mailSenderProvider = mailSenderProvider;
        this.fromAddress = fromAddress;
        this.overrideRecipient = overrideRecipient;
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
            String targetEmail = (overrideRecipient != null && !overrideRecipient.trim().isEmpty())
                    ? overrideRecipient.trim()
                    : recipientEmail;
            String formattedSubject = (overrideRecipient != null && !overrideRecipient.trim().isEmpty())
                    ? String.format("[I-ITSM -> %s] %s", recipientEmail, subject)
                    : subject;

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromAddress);
            mailMessage.setTo(targetEmail);
            mailMessage.setSubject(formattedSubject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email notification delivered to '{}' (target: '{}') with subject: {}",
                    targetEmail, recipientEmail, formattedSubject);
        } catch (MailException ex) {
            log.warn("Failed to deliver email to '{}': {}", recipientEmail, ex.getMessage());
            throw new NotificationFailureException(CHANNEL_NAME, recipientEmail, ex.getMessage(), ex);
        }
    }
}

