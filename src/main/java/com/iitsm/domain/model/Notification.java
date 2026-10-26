package com.iitsm.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing a dispatched or persisted notification.
 */
public record Notification(
        String id,
        String recipientId,
        String channel,
        String subject,
        String message,
        boolean delivered,
        Instant createdAt
) {
    public Notification {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(recipientId, "recipientId cannot be null");
        Objects.requireNonNull(channel, "channel cannot be null");
        Objects.requireNonNull(subject, "subject cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public String getId() { return id; }
    public String getRecipientId() { return recipientId; }
    public String getChannel() { return channel; }
    public String getSubject() { return subject; }
    public String getMessage() { return message; }
    public boolean isDelivered() { return delivered; }
    public Instant getCreatedAt() { return createdAt; }
}

