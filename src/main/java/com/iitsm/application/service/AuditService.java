package com.iitsm.application.service;

import com.iitsm.domain.model.AuditRecord;
import com.iitsm.domain.repository.AuditRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service providing read and write access to incident audit trails.
 */
@Service
@Transactional(readOnly = true)
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository cannot be null");
    }

    public List<AuditRecord> getAuditTrailForIncident(String incidentId) {
        return auditRepository.findByIncidentIdOrderByTimestampAsc(incidentId);
    }

    public List<AuditRecord> getAllAuditRecords() {
        return auditRepository.findAll();
    }
}

