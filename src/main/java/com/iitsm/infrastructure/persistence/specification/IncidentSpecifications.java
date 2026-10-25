package com.iitsm.infrastructure.persistence.specification;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

/**
 * Pattern: Specification.
 * Composable JPA criteria specifications for multi-criteria incident search.
 * Satisfies the requirement of >= 5 search/filter criteria.
 */
public final class IncidentSpecifications {

    private IncidentSpecifications() {}

    public static Specification<IncidentJpaEntity> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<IncidentJpaEntity> hasPriority(Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<IncidentJpaEntity> hasCategory(IncidentCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<IncidentJpaEntity> hasServiceId(String serviceId) {
        return (root, query, cb) -> (serviceId == null || serviceId.isBlank())
                ? null
                : cb.equal(root.get("serviceId"), serviceId.trim());
    }

    public static Specification<IncidentJpaEntity> hasAssigneeId(String assigneeId) {
        return (root, query, cb) -> (assigneeId == null || assigneeId.isBlank())
                ? null
                : cb.equal(root.get("assigneeId"), assigneeId.trim());
    }

    public static Specification<IncidentJpaEntity> hasReporterId(String reporterId) {
        return (root, query, cb) -> (reporterId == null || reporterId.isBlank())
                ? null
                : cb.equal(root.get("reporterId"), reporterId.trim());
    }

    public static Specification<IncidentJpaEntity> containsText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            String pattern = "%" + text.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern));
        };
    }

    public static Specification<IncidentJpaEntity> createdBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}

