package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.domain.repository.ReassignmentRequestRepository;
import com.iitsm.infrastructure.persistence.entity.ReassignmentRequestJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataReassignmentRequestRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing the pure domain ReassignmentRequestRepository port via SpringDataReassignmentRequestRepository.
 */
@Component
public class ReassignmentRequestRepositoryAdapter implements ReassignmentRequestRepository {

    private final SpringDataReassignmentRequestRepository springDataRepository;

    public ReassignmentRequestRepositoryAdapter(SpringDataReassignmentRequestRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    private static ReassignmentRequest toDomain(ReassignmentRequestJpaEntity entity) {
        if (entity == null) return null;
        return ReassignmentRequest.reconstitute(
                entity.getId(),
                entity.getIncidentId(),
                entity.getRequestingTechnicianId(),
                entity.getReason(),
                entity.getStatus(),
                entity.getRequestedAt(),
                entity.getResolvedAt(),
                entity.getReviewedByAdminId()
        );
    }

    private static ReassignmentRequestJpaEntity toJpaEntity(ReassignmentRequest domain) {
        if (domain == null) return null;
        ReassignmentRequestJpaEntity entity = new ReassignmentRequestJpaEntity();
        entity.setId(domain.getId());
        entity.setIncidentId(domain.getIncidentId());
        entity.setRequestingTechnicianId(domain.getRequestingTechnicianId());
        entity.setReason(domain.getReason());
        entity.setStatus(domain.getStatus());
        entity.setRequestedAt(domain.getRequestedAt());
        entity.setResolvedAt(domain.getResolvedAt());
        entity.setReviewedByAdminId(domain.getReviewedByAdminId());
        return entity;
    }

    @Override
    public ReassignmentRequest save(ReassignmentRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        try {
            ReassignmentRequestJpaEntity entity = toJpaEntity(request);
            ReassignmentRequestJpaEntity saved = springDataRepository.save(entity);
            return toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save reassignment request: " + request.getId(), ex);
        }
    }

    @Override
    public Optional<ReassignmentRequest> findById(String id) {
        try {
            return springDataRepository.findById(id).map(ReassignmentRequestRepositoryAdapter::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find reassignment request by id: " + id, ex);
        }
    }

    @Override
    public List<ReassignmentRequest> findByIncidentId(String incidentId) {
        try {
            return springDataRepository.findByIncidentId(incidentId).stream()
                    .map(ReassignmentRequestRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find reassignment requests for incident: " + incidentId, ex);
        }
    }

    @Override
    public List<ReassignmentRequest> findByStatus(ReassignmentRequest.Status status) {
        try {
            return springDataRepository.findByStatus(status).stream()
                    .map(ReassignmentRequestRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find reassignment requests by status: " + status, ex);
        }
    }

    @Override
    public List<ReassignmentRequest> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(ReassignmentRequestRepositoryAdapter::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all reassignment requests", ex);
        }
    }
}

