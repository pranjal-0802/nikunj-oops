package com.iitsm.domain.repository;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for Incident entity persistence and querying. Pure Java, zero framework dependencies.
 */
public interface IncidentRepository {

    Incident save(Incident incident);

    Optional<Incident> findById(String id);

    List<Incident> findByReporterId(String reporterId);

    List<Incident> findByAssigneeId(String assigneeId);

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByCategory(IncidentCategory category);

    List<Incident> findOpenIncidentsByServiceAndCategorySince(
            String serviceId, IncidentCategory category, Instant since);

    List<Incident> findOpenIncidentsSince(Instant since);

    List<Incident> findActiveIncidents();

    List<Incident> findByNeedsTriageTrue();

    List<Incident> findAll();

    long count();

    long countByStatusIn(List<IncidentStatus> statuses);

    long countByPriority(Priority priority);
}

