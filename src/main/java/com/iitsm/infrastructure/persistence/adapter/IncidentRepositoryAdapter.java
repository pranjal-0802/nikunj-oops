package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.IncidentMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataIncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Pattern: Adapter / Repository.
 * Adapts SpringDataIncidentRepository to the pure Java domain IncidentRepository port.
 * Translates DataAccessExceptions and optimistic locking collisions to PersistenceFailureException.
 */
@Component
public class IncidentRepositoryAdapter implements IncidentRepository {

    private final SpringDataIncidentRepository springDataRepository;

    public IncidentRepositoryAdapter(SpringDataIncidentRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public Incident save(Incident incident) {
        Objects.requireNonNull(incident, "incident cannot be null");
        try {
            IncidentJpaEntity jpaEntity = IncidentMapper.toJpaEntity(incident);
            IncidentJpaEntity saved = springDataRepository.save(jpaEntity);
            return IncidentMapper.toDomain(saved);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException | jakarta.persistence.OptimisticLockException ex) {
            throw new java.util.ConcurrentModificationException("Incident " + incident.getId() + " was modified concurrently by another transaction", ex);
        } catch (DataAccessException ex) {
            if (ex.getCause() instanceof jakarta.persistence.OptimisticLockException || ex instanceof org.springframework.orm.ObjectOptimisticLockingFailureException) {
                throw new java.util.ConcurrentModificationException("Incident " + incident.getId() + " was modified concurrently by another transaction", ex);
            }
            throw new PersistenceFailureException("Failed to save incident: " + incident.getId(), ex);
        }
    }

    @Override
    public Optional<Incident> findById(String id) {
        try {
            return springDataRepository.findById(id).map(IncidentMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve incident by ID: " + id, ex);
        }
    }

    @Override
    public List<Incident> findByReporterId(String reporterId) {
        try {
            return springDataRepository.findByReporterId(reporterId).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve incidents for reporter: " + reporterId, ex);
        }
    }

    @Override
    public List<Incident> findByAssigneeId(String assigneeId) {
        try {
            return springDataRepository.findByAssigneeId(assigneeId).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve incidents for assignee: " + assigneeId, ex);
        }
    }

    @Override
    public List<Incident> findByStatus(IncidentStatus status) {
        try {
            return springDataRepository.findByStatus(status).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve incidents by status: " + status, ex);
        }
    }

    @Override
    public List<Incident> findByCategory(IncidentCategory category) {
        try {
            return springDataRepository.findByCategory(category).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve incidents by category: " + category, ex);
        }
    }

    @Override
    public List<Incident> findOpenIncidentsByServiceAndCategorySince(
            String serviceId, IncidentCategory category, Instant since) {
        try {
            return springDataRepository.findOpenIncidentsByServiceAndCategorySince(serviceId, category, since).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to query open correlated incidents", ex);
        }
    }

    @Override
    public List<Incident> findOpenIncidentsSince(Instant since) {
        try {
            return springDataRepository.findOpenIncidentsSince(since).stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to query open incidents since " + since, ex);
        }
    }

    @Override
    public List<Incident> findActiveIncidents() {
        try {
            return springDataRepository.findActiveIncidents().stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to query active incidents", ex);
        }
    }

    @Override
    public List<Incident> findByNeedsTriageTrue() {
        try {
            return springDataRepository.findByNeedsTriageTrue().stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to retrieve triage incidents", ex);
        }
    }

    @Override
    public List<Incident> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(IncidentMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all incidents", ex);
        }
    }

    @Override
    public long count() {
        try {
            return springDataRepository.count();
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to count incidents", ex);
        }
    }

    @Override
    public long countByStatusIn(List<IncidentStatus> statuses) {
        try {
            return springDataRepository.countByStatusIn(statuses);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to count incidents by status in: " + statuses, ex);
        }
    }

    @Override
    public long countByPriority(com.iitsm.domain.model.Priority priority) {
        try {
            return springDataRepository.countByPriority(priority);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to count incidents by priority: " + priority, ex);
        }
    }
}

