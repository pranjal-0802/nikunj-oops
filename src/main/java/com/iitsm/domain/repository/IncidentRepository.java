package com.iitsm.domain.repository;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
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

    List<Incident> findOpenIncidentsByServiceAndCategorySince(
            String serviceId, IncidentCategory category, Instant since);

    List<Incident> findAll();

    long count();
}
