package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for persisting and retrieving audit records.
 */
@Repository
public interface SpringDataAuditRecordRepository extends JpaRepository<AuditRecordJpaEntity, String> {

    List<AuditRecordJpaEntity> findByIncidentIdOrderByTimestampAsc(String incidentId);
}

