package com.iitsm.application.service;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.IncidentMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataIncidentRepository;
import com.iitsm.infrastructure.persistence.specification.IncidentSpecifications;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for executing dynamic multi-criteria searches over the incident directory.
 * Employs composable JPA Specifications supporting >= 5 distinct filter criteria.
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

    private final SpringDataIncidentRepository incidentRepository;

    public SearchService(SpringDataIncidentRepository incidentRepository) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
    }

    public List<Incident> search(IncidentSearchCriteria criteria) {
        Specification<IncidentJpaEntity> spec = buildSpecification(criteria);
        return incidentRepository.findAll(spec).stream()
                .map(IncidentMapper::toDomain)
                .collect(Collectors.toList());
    }

    public Page<Incident> searchPaged(IncidentSearchCriteria criteria, Pageable pageable) {
        Specification<IncidentJpaEntity> spec = buildSpecification(criteria);
        return incidentRepository.findAll(spec, pageable)
                .map(IncidentMapper::toDomain);
    }

    private Specification<IncidentJpaEntity> buildSpecification(IncidentSearchCriteria criteria) {
        if (criteria == null) {
            return Specification.where(null);
        }

        return Specification.where(IncidentSpecifications.hasStatus(criteria.status()))
                .and(IncidentSpecifications.hasPriority(criteria.priority()))
                .and(IncidentSpecifications.hasCategory(criteria.category()))
                .and(IncidentSpecifications.hasServiceId(criteria.serviceId()))
                .and(IncidentSpecifications.hasAssigneeId(criteria.assigneeId()))
                .and(IncidentSpecifications.hasReporterId(criteria.reporterId()))
                .and(IncidentSpecifications.containsText(criteria.query()))
                .and(IncidentSpecifications.createdBetween(criteria.fromDate(), criteria.toDate()));
    }
}

