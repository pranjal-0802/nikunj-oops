package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.repository.EscalationRecordRepository;
import com.iitsm.infrastructure.persistence.entity.EscalationRecordJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataEscalationRecordRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the pure domain EscalationRecordRepository port via SpringDataEscalationRecordRepository.
 */
@Component
public class EscalationRecordRepositoryAdapter implements EscalationRecordRepository {

    private final SpringDataEscalationRecordRepository springDataRepository;

    public EscalationRecordRepositoryAdapter(SpringDataEscalationRecordRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    private static EscalationRecord toDomain(EscalationRecordJpaEntity entity) {
        if (entity == null) return null;
        return new EscalationRecord(
                entity.getId(),
                entity.getIncidentId(),
                entity.getFromRole(),
                entity.getToRole(),
                entity.getToUserId(),
                entity.getReason(),
                entity.getEscalatedAt()
        );
    }

    private static EscalationRecordJpaEntity toJpaEntity(EscalationRecord domain) {
        if (domain == null) return null;
        EscalationRecordJpaEntity entity = new EscalationRecordJpaEntity();
        entity.setId(domain.id());
        entity.setIncidentId(domain.incidentId());
        entity.setFromRole(domain.fromRole());
        entity.setToRole(domain.toRole());
        entity.setToUserId(domain.toUserId());
        entity.setReason(domain.reason());
        entity.setEscalatedAt(domain.escalatedAt());
        return entity;
    }

    @Override
    public EscalationRecord save(EscalationRecord record) {
        Objects.requireNonNull(record, "record cannot be null");
        try {
            EscalationRecordJpaEntity entity = toJpaEntity(record);
            EscalationRecordJpaEntity saved = springDataRepository.save(entity);
            return toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save escalation record: " + record.id(), ex);
        }
    }

    @Override
    public Optional<EscalationRecord> findById(String id) {
        try {
            return springDataRepository.findById(id).map(EscalationRecordRepositoryAdapter::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find escalation record by id: " + id, ex);
        }
    }

    @Override
    public List<EscalationRecord> findByIncidentIdOrderByEscalatedAtDesc(String incidentId) {
        try {
            return springDataRepository.findByIncidentIdOrderByEscalatedAtDesc(incidentId).stream()
                    .map(EscalationRecordRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find escalation records for incident: " + incidentId, ex);
        }
    }

    @Override
    public List<EscalationRecord> findAllByOrderByEscalatedAtDesc() {
        try {
            return springDataRepository.findAllByOrderByEscalatedAtDesc().stream()
                    .map(EscalationRecordRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all escalation records", ex);
        }
    }

    @Override
    public List<EscalationRecord> findAll() {
        return findAllByOrderByEscalatedAtDesc();
    }
}

