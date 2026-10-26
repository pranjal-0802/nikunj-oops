package com.iitsm.domain.repository;

import com.iitsm.domain.model.Notification;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for notifications persistence.
 */
public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(String id);
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
    List<Notification> findAll();
}

