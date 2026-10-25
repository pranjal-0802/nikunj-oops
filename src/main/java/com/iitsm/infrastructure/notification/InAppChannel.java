package com.iitsm.infrastructure.notification;

import com.iitsm.domain.exception.NotificationFailureException;
import com.iitsm.infrastructure.persistence.entity.NotificationJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataNotificationRepository;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pattern: Adapter.
 * Adapts internal notifications to in-app persistence and real-time SSE push.
 */
@Component
public class InAppChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(InAppChannel.class);
    private static final String CHANNEL_NAME = "IN_APP";

    private final SpringDataNotificationRepository notificationRepository;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final Clock clock;

    public InAppChannel(
            SpringDataNotificationRepository notificationRepository,
            SseEmitterRegistry sseEmitterRegistry,
            Clock clock) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository cannot be null");
        this.sseEmitterRegistry = Objects.requireNonNull(sseEmitterRegistry, "sseEmitterRegistry cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Override
    public String getChannelName() {
        return CHANNEL_NAME;
    }

    @Override
    public void send(String recipient, String subject, String message) {
        try {
            NotificationJpaEntity entity = new NotificationJpaEntity();
            entity.setId("NOTIF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            entity.setRecipientId(recipient);
            entity.setChannel(CHANNEL_NAME);
            entity.setSubject(subject);
            entity.setMessage(message);
            entity.setDelivered(true);
            entity.setCreatedAt(clock.instant());

            NotificationJpaEntity saved = notificationRepository.save(entity);

            // Push to connected SSE clients
            sseEmitterRegistry.sendToUser(recipient, saved);
            log.debug("In-app notification {} dispatched to {}", saved.getId(), recipient);
        } catch (Exception ex) {
            log.error("Failed to dispatch in-app notification to {}: {}", recipient, ex.getMessage());
            throw new NotificationFailureException(CHANNEL_NAME, recipient, "Failed to persist or push in-app notification", ex);
        }
    }
}

