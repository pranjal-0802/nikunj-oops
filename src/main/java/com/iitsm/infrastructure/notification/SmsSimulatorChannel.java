package com.iitsm.infrastructure.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pattern: Adapter.
 * Simulated SMS channel implementation adhering to the NotificationChannel interface.
 * Logs dispatches and maintains an in-memory buffer for testing and verification.
 */
@Component
public class SmsSimulatorChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(SmsSimulatorChannel.class);
    private static final String CHANNEL_NAME = "SMS";

    private final List<SmsRecord> sentRecords = new CopyOnWriteArrayList<>();

    public record SmsRecord(String recipient, String message) {}

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(String recipient, String subject, String message) {
        String fullBody = "[" + subject + "] " + message;
        sentRecords.add(new SmsRecord(recipient, fullBody));
        log.info("[SMS-SIMULATOR] Sent SMS to {}: {}", recipient, fullBody);
    }

    public List<SmsRecord> getSentRecords() {
        return Collections.unmodifiableList(sentRecords);
    }

    public void clear() {
        sentRecords.clear();
    }
}

