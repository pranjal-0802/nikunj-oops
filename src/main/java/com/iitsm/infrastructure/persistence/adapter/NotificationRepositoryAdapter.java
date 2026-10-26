package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.Notification;
import com.iitsm.domain.repository.NotificationRepository;
import com.iitsm.infrastructure.persistence.entity.NotificationJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataNotificationRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the pure domain NotificationRepository port via SpringDataNotificationRepository.
 */
@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository springDataRepository;

    public NotificationRepositoryAdapter(SpringDataNotificationRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    private static Notification toDomain(NotificationJpaEntity entity) {
        if (entity == null) return null;
        return new Notification(
                entity.getId(),
                entity.getRecipientId(),
                entity.getChannel(),
                entity.getSubject(),
                entity.getMessage(),
                entity.isDelivered(),
                entity.getCreatedAt()
        );
    }

    private static NotificationJpaEntity toJpaEntity(Notification domain) {
        if (domain == null) return null;
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(domain.getId());
        entity.setRecipientId(domain.getRecipientId());
        entity.setChannel(domain.getChannel());
        entity.setSubject(domain.getSubject());
        entity.setMessage(domain.getMessage());
        entity.setDelivered(domain.isDelivered());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    @Override
    public Notification save(Notification notification) {
        Objects.requireNonNull(notification, "notification cannot be null");
        try {
            NotificationJpaEntity entity = toJpaEntity(notification);
            NotificationJpaEntity saved = springDataRepository.save(entity);
            return toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save notification: " + notification.getId(), ex);
        }
    }

    @Override
    public Optional<Notification> findById(String id) {
        try {
            return springDataRepository.findById(id).map(NotificationRepositoryAdapter::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find notification by id: " + id, ex);
        }
    }

    @Override
    public List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId) {
        try {
            return springDataRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                    .map(NotificationRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find notifications for recipient: " + recipientId, ex);
        }
    }

    @Override
    public List<Notification> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(NotificationRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all notifications", ex);
        }
    }
}

