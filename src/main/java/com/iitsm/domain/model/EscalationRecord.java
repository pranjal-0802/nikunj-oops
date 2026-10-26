package com.iitsm.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * OOP: Value Object / Domain Record representing an administrative or automated incident escalation.
 */
public record EscalationRecord(
        String id,
        String incidentId,
        String fromRole,
        String toRole,
        String toUserId,
        String reason,
        Instant escalatedAt) {

    public EscalationRecord {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        Objects.requireNonNull(fromRole, "fromRole cannot be null");
        Objects.requireNonNull(toRole, "toRole cannot be null");
        Objects.requireNonNull(toUserId, "toUserId cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null");
        Objects.requireNonNull(escalatedAt, "escalatedAt cannot be null");
    }
}

