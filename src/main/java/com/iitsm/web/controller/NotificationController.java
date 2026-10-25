package com.iitsm.web.controller;

import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.notification.SseEmitterRegistry;
import com.iitsm.infrastructure.persistence.entity.NotificationJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataNotificationRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller exposing real-time SSE notification stream and persisted in-app notification queries.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final SseEmitterRegistry sseEmitterRegistry;
    private final SpringDataNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(
            SseEmitterRegistry sseEmitterRegistry,
            SpringDataNotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.sseEmitterRegistry = Objects.requireNonNull(sseEmitterRegistry, "sseEmitterRegistry cannot be null");
        this.notificationRepository = Objects.requireNonNull(notificationRepository, "notificationRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
    }

    private String resolveRecipientId(Authentication authentication) {
        if (authentication == null) {
            return "ANONYMOUS";
        }
        String name = authentication.getName();
        if (name.contains("@")) {
            return userRepository.findByEmail(new Email(name))
                    .map(User::getId)
                    .orElse(name);
        }
        return name;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        String recipientId = resolveRecipientId(authentication);
        return sseEmitterRegistry.register(recipientId);
    }

    @GetMapping
    public ResponseEntity<List<NotificationJpaEntity>> getMyNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String recipientId = resolveRecipientId(authentication);
        List<NotificationJpaEntity> notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") String id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setDelivered(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.noContent().build();
    }
}
