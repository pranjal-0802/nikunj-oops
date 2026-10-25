package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.infrastructure.persistence.entity.NotificationJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationRepository extends JpaRepository<NotificationJpaEntity, String> {

    List<NotificationJpaEntity> findByRecipientIdOrderByCreatedAtDesc(String recipientId);
}

