package com.iitsm.application.port;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Port for managing server-sent event (SSE) notification streams.
 */
public interface RealtimeNotificationPort {
    SseEmitter register(String recipientId);
    void sendNotification(String recipientId, String eventName, Object data);
}

