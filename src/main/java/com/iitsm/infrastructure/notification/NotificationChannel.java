package com.iitsm.infrastructure.notification;

/**
 * Pattern: Adapter / Strategy.
 * Target interface for swappable multi-channel notification dispatchers.
 */
public interface NotificationChannel {

    String getChannelName();

    void send(String recipient, String subject, String message);
}

