package com.iitsm.application.service;

import com.iitsm.application.port.RealtimeNotificationPort;
import com.iitsm.domain.model.Notification;
import com.iitsm.domain.repository.NotificationRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Application service managing notification queries and real-time SSE subscriptions.
 */
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RealtimeNotificationPort realtimeNotificationPort;

    public NotificationService(
            NotificationRepository notificationRepository,
            RealtimeNotificationPort realtimeNotificationPort) {
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository cannot be null");
        this.realtimeNotificationPort = Objects.requireNonNull(realtimeNotificationPort, "realtimeNotificationPort cannot be null");
    }

    public SseEmitter subscribe(String recipientId) {
        return realtimeNotificationPort.register(recipientId);
    }

    public List<Notification> getNotificationsForUser(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    @Transactional
    public void markDelivered(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            Notification updated = new Notification(
                    n.getId(),
                    n.getRecipientId(),
                    n.getChannel(),
                    n.getSubject(),
                    n.getMessage(),
                    true,
                    n.getCreatedAt()
            );
            notificationRepository.save(updated);
        });
    }
}

