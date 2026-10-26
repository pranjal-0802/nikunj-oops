package com.iitsm.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain model representing an immutable audit log record for incident lifecycle events.
 */
public record AuditRecord(
        String id,
        Instant timestamp,
        String actorId,
        String action,
        String incidentId,
        String previousState,
        String newState,
        String description
) {
    public AuditRecord {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(actorId, "actorId cannot be null");
        Objects.requireNonNull(action, "action cannot be null");
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
    }

    // JavaBean getters for compatibility
    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getActorId() { return actorId; }
    public String getAction() { return action; }
    public String getIncidentId() { return incidentId; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getDescription() { return description; }
}

