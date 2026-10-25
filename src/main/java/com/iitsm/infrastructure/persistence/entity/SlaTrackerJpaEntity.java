package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.SlaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "sla_trackers")
public class SlaTrackerJpaEntity {

    @Id
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "incident_id")
    private IncidentJpaEntity incident;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "response_due_at", nullable = false)
    private Instant responseDueAt;

    @Column(name = "resolution_due_at", nullable = false)
    private Instant resolutionDueAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "paused_duration_seconds", nullable = false)
    private long pausedDurationSeconds;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SlaStatus status;

    public SlaTrackerJpaEntity() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public IncidentJpaEntity getIncident() { return incident; }
    public void setIncident(IncidentJpaEntity incident) { this.incident = incident; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getResponseDueAt() { return responseDueAt; }
    public void setResponseDueAt(Instant responseDueAt) { this.responseDueAt = responseDueAt; }

    public Instant getResolutionDueAt() { return resolutionDueAt; }
    public void setResolutionDueAt(Instant resolutionDueAt) { this.resolutionDueAt = resolutionDueAt; }

    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public long getPausedDurationSeconds() { return pausedDurationSeconds; }
    public void setPausedDurationSeconds(long pausedDurationSeconds) { this.pausedDurationSeconds = pausedDurationSeconds; }

    public Instant getPausedAt() { return pausedAt; }
    public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }

    public SlaStatus getStatus() { return status; }
    public void setStatus(SlaStatus status) { this.status = status; }
}
