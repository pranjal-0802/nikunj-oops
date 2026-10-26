package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.AuditRecord;
import com.iitsm.domain.repository.AuditRepository;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataAuditRecordRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the pure domain AuditRepository port via SpringDataAuditRecordRepository.
 */
@Component
public class AuditRepositoryAdapter implements AuditRepository {

    private final SpringDataAuditRecordRepository springDataRepository;

    public AuditRepositoryAdapter(SpringDataAuditRecordRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    private static AuditRecord toDomain(AuditRecordJpaEntity entity) {
        if (entity == null) return null;
        return new AuditRecord(
                entity.getId(),
                entity.getTimestamp(),
                entity.getActorId(),
                entity.getAction(),
                entity.getIncidentId(),
                entity.getPreviousState(),
                entity.getNewState(),
                entity.getDescription()
        );
    }

    private static AuditRecordJpaEntity toJpaEntity(AuditRecord domain) {
        if (domain == null) return null;
        AuditRecordJpaEntity entity = new AuditRecordJpaEntity();
        entity.setId(domain.id());
        entity.setTimestamp(domain.timestamp());
        entity.setActorId(domain.actorId());
        entity.setAction(domain.action());
        entity.setIncidentId(domain.incidentId());
        entity.setPreviousState(domain.previousState());
        entity.setNewState(domain.newState());
        entity.setDescription(domain.description());
        return entity;
    }

    @Override
    public AuditRecord save(AuditRecord record) {
        Objects.requireNonNull(record, "record cannot be null");
        try {
            AuditRecordJpaEntity entity = toJpaEntity(record);
            AuditRecordJpaEntity saved = springDataRepository.save(entity);
            return toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save audit record: " + record.id(), ex);
        }
    }

    @Override
    public Optional<AuditRecord> findById(String id) {
        try {
            return springDataRepository.findById(id).map(AuditRepositoryAdapter::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find audit record by id: " + id, ex);
        }
    }

    @Override
    public List<AuditRecord> findByIncidentIdOrderByTimestampAsc(String incidentId) {
        try {
            return springDataRepository.findByIncidentIdOrderByTimestampAsc(incidentId).stream()
                    .map(AuditRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find audit records for incident: " + incidentId, ex);
        }
    }

    @Override
    public List<AuditRecord> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(AuditRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all audit records", ex);
        }
    }
}

