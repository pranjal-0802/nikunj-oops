package com.iitsm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "escalation_records")
public class EscalationRecordJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "from_role", nullable = false, length = 32)
    private String fromRole;

    @Column(name = "to_role", nullable = false, length = 32)
    private String toRole;

    @Column(name = "to_user_id", nullable = false, length = 64)
    private String toUserId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "escalated_at", nullable = false)
    private Instant escalatedAt;

    public EscalationRecordJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getFromRole() { return fromRole; }
    public void setFromRole(String fromRole) { this.fromRole = fromRole; }

    public String getToRole() { return toRole; }
    public void setToRole(String toRole) { this.toRole = toRole; }

    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getEscalatedAt() { return escalatedAt; }
    public void setEscalatedAt(Instant escalatedAt) { this.escalatedAt = escalatedAt; }
}
