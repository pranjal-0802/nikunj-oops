package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.ReassignmentRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "reassignment_requests")
public class ReassignmentRequestJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "incident_id", nullable = false, length = 64)
    private String incidentId;

    @Column(name = "requesting_technician_id", nullable = false, length = 64)
    private String requestingTechnicianId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReassignmentRequest.Status status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "reviewed_by_admin_id", length = 64)
    private String reviewedByAdminId;

    public ReassignmentRequestJpaEntity() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getRequestingTechnicianId() { return requestingTechnicianId; }
    public void setRequestingTechnicianId(String requestingTechnicianId) { this.requestingTechnicianId = requestingTechnicianId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ReassignmentRequest.Status getStatus() { return status; }
    public void setStatus(ReassignmentRequest.Status status) { this.status = status; }

    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getReviewedByAdminId() { return reviewedByAdminId; }
    public void setReviewedByAdminId(String reviewedByAdminId) { this.reviewedByAdminId = reviewedByAdminId; }
}
