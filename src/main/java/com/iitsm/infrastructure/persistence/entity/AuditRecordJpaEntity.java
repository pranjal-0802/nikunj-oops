package com.iitsm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_records")
public class AuditRecordJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "actor_id", nullable = false, length = 64)
    private String actorId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "previous_state", length = 32)
    private String previousState;

    @Column(name = "new_state", length = 32)
    private String newState;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    public AuditRecordJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getPreviousState() { return previousState; }
    public void setPreviousState(String previousState) { this.previousState = previousState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
