package com.iitsm.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resolutions")
public class ResolutionJpaEntity {

    @Id
    @Column(name = "incident_id", length = 64)
    private String incidentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "incident_id")
    private IncidentJpaEntity incident;

    @Column(name = "root_cause", nullable = false, columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "steps_taken", nullable = false, columnDefinition = "TEXT")
    private String stepsTaken;

    @Column(name = "preventive_action", nullable = false, columnDefinition = "TEXT")
    private String preventiveAction;

    @Column(name = "confirmed_category", nullable = false, length = 32)
    private String confirmedCategory;

    @Column(name = "resolved_by", nullable = false, length = 64)
    private String resolvedBy;

    @Column(name = "resolved_at", nullable = false)
    private Instant resolvedAt;

    public ResolutionJpaEntity() {}

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public IncidentJpaEntity getIncident() { return incident; }
    public void setIncident(IncidentJpaEntity incident) { this.incident = incident; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getStepsTaken() { return stepsTaken; }
    public void setStepsTaken(String stepsTaken) { this.stepsTaken = stepsTaken; }

    public String getPreventiveAction() { return preventiveAction; }
    public void setPreventiveAction(String preventiveAction) { this.preventiveAction = preventiveAction; }

    public String getConfirmedCategory() { return confirmedCategory; }
    public void setConfirmedCategory(String confirmedCategory) { this.confirmedCategory = confirmedCategory; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
