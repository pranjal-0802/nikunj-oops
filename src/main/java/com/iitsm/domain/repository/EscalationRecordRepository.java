package com.iitsm.domain.repository;

import com.iitsm.domain.model.EscalationRecord;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for persisting and querying incident escalation records.
 */
public interface EscalationRecordRepository {
    EscalationRecord save(EscalationRecord record);
    Optional<EscalationRecord> findById(String id);
    List<EscalationRecord> findByIncidentIdOrderByEscalatedAtDesc(String incidentId);
    List<EscalationRecord> findAllByOrderByEscalatedAtDesc();
    List<EscalationRecord> findAll();
}

