package com.iitsm.application.service;

import com.iitsm.application.port.IncidentSearchPort;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for executing dynamic multi-criteria searches over the incident directory.
 * Pure application layer: delegates to IncidentSearchPort adapter.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    public record IncidentSearchCriteria(
            IncidentStatus status,
            Priority priority,
            IncidentCategory category,
            String serviceId,
            String assigneeId,
            String reporterId,
            String query,
            Instant fromDate,
            Instant toDate) {}

    private final IncidentSearchPort incidentSearchPort;

    public SearchService(IncidentSearchPort incidentSearchPort) {
        this.incidentSearchPort = Objects.requireNonNull(incidentSearchPort, "incidentSearchPort cannot be null");
    }

    private static IncidentSearchPort.SearchCriteria toPortCriteria(IncidentSearchCriteria c) {
        if (c == null) {
            return null;
        }
        return new IncidentSearchPort.SearchCriteria(
                c.status(),
                c.priority(),
                c.category(),
                c.serviceId(),
                c.assigneeId(),
                c.reporterId(),
                c.query(),
                c.fromDate(),
                c.toDate()
        );
    }

    public List<Incident> search(IncidentSearchCriteria criteria) {
        return incidentSearchPort.search(toPortCriteria(criteria));
    }

    public Page<Incident> searchPaged(IncidentSearchCriteria criteria, Pageable pageable) {
        return incidentSearchPort.searchPaged(toPortCriteria(criteria), pageable);
    }
}
