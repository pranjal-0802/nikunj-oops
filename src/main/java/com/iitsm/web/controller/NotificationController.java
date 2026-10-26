package com.iitsm.web.controller;

import com.iitsm.application.service.NotificationService;
import com.iitsm.application.service.UserService;
import com.iitsm.domain.model.User;
import com.iitsm.web.dto.NotificationDto;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(
            NotificationService notificationService,
            UserService userService) {
        this.notificationService = Objects.requireNonNull(notificationService, "notificationService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
    }

    private String resolveRecipientId(Authentication authentication) {
        if (authentication == null) {
            return "ANONYMOUS";
        }
        String name = authentication.getName();
        if (name.contains("@")) {
            return userService.findByEmail(name)
                    .map(User::getId)
                    .orElse(name);
        }
        return name;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(Authentication authentication) {
        String recipientId = resolveRecipientId(authentication);
        return notificationService.subscribe(recipientId);
    }

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }
        String recipientId = resolveRecipientId(authentication);
        List<NotificationDto> notifications = notificationService.getNotificationsForUser(recipientId).stream()
                .map(NotificationDto::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable("id") String id) {
        notificationService.markDelivered(id);
        return ResponseEntity.noContent().build();
    }
}
