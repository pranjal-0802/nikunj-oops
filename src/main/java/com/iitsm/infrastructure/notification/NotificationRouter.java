package com.iitsm.infrastructure.notification;

import com.iitsm.domain.exception.NotificationFailureException;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pattern: Strategy / Router.
 * Routes notification payloads across multiple delivery channels (In-App, Email, SMS).
 * Enforces fault isolation: channel delivery failures are caught, logged, and never roll back transactions.
 */
@Component
public class NotificationRouter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final List<NotificationChannel> channels;
    private final UserRepository userRepository;

    public NotificationRouter(List<NotificationChannel> channels, UserRepository userRepository) {
        this.channels = Objects.requireNonNull(channels, "channels cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
    }

    public void route(NotificationFactory.NotificationPayload payload) {
        if (payload == null || payload.recipientUserId() == null) {
            return;
        }

        String recipientId = payload.recipientUserId();
        String recipientEmail = null;

        // Try looking up user's email
        Optional<User> userOpt = userRepository.findById(recipientId);
        if (userOpt.isPresent() && userOpt.get().getEmail() != null) {
            recipientEmail = userOpt.get().getEmail().value();
        } else if (recipientId.contains("@")) {
            recipientEmail = recipientId;
        }

        for (NotificationChannel channel : channels) {
            try {
                if ("IN_APP".equalsIgnoreCase(channel.getChannelName())) {
                    channel.send(recipientId, payload.subject(), payload.body());
                } else if ("EMAIL".equalsIgnoreCase(channel.getChannelName()) && recipientEmail != null) {
                    channel.send(recipientEmail, payload.subject(), payload.body());
                } else if ("SMS".equalsIgnoreCase(channel.getChannelName())) {
                    channel.send(recipientId, payload.subject(), payload.body());
                }
            } catch (NotificationFailureException nfe) {
                log.warn("Isolated notification failure on channel '{}' for recipient '{}': {}",
                        channel.getChannelName(), recipientId, nfe.getMessage());
            } catch (Exception ex) {
                log.error("Unexpected error on notification channel '{}' for recipient '{}': {}",
                        channel.getChannelName(), recipientId, ex.getMessage(), ex);
            }
        }
    }
}

