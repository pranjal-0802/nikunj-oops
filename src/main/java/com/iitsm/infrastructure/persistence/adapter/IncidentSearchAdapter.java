package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.application.port.IncidentSearchPort;
import com.iitsm.domain.model.Incident;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.IncidentMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataIncidentRepository;
import com.iitsm.infrastructure.persistence.specification.IncidentSpecifications;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Adapter implementing IncidentSearchPort via JPA Specifications and SpringDataIncidentRepository.
 */
@Component
public class IncidentSearchAdapter implements IncidentSearchPort {

    private final SpringDataIncidentRepository incidentRepository;

    public IncidentSearchAdapter(SpringDataIncidentRepository incidentRepository) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
    }

    @Override
    public List<Incident> search(SearchCriteria criteria) {
        Specification<IncidentJpaEntity> spec = buildSpecification(criteria);
        return incidentRepository.findAll(spec).stream()
                .map(IncidentMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Incident> searchPaged(SearchCriteria criteria, Pageable pageable) {
        Specification<IncidentJpaEntity> spec = buildSpecification(criteria);
        return incidentRepository.findAll(spec, pageable)
                .map(IncidentMapper::toDomain);
    }

    private Specification<IncidentJpaEntity> buildSpecification(SearchCriteria criteria) {
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

