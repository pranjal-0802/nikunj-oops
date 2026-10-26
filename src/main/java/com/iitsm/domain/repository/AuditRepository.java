package com.iitsm.domain.repository;

import com.iitsm.domain.model.AuditRecord;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for persisting and querying incident audit records.
 */
public interface AuditRepository {
    AuditRecord save(AuditRecord record);
    Optional<AuditRecord> findById(String id);
    List<AuditRecord> findByIncidentIdOrderByTimestampAsc(String incidentId);
    List<AuditRecord> findAll();
}

