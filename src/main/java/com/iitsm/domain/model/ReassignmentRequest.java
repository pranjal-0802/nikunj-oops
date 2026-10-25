package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;
import java.util.Objects;

/**
 * OOP: Entity tracking technician wrong-domain / out-of-scope reassignment requests.
 */
public class ReassignmentRequest {

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }

    private final String id;
    private final String incidentId;
    private final String requestingTechnicianId;
    private final String reason;
    private Status status;
    private final Instant requestedAt;
    private Instant resolvedAt;
    private String reviewedByAdminId;

    public ReassignmentRequest(
            String id,
            String incidentId,
            String requestingTechnicianId,
            String reason,
            Instant requestedAt) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "ReassignmentRequest ID cannot be blank");
        }
        if (incidentId == null || incidentId.trim().isEmpty()) {
            throw new ValidationException("incidentId", "Incident ID cannot be blank");
        }
        if (requestingTechnicianId == null || requestingTechnicianId.trim().isEmpty()) {
            throw new ValidationException("requestingTechnicianId", "Technician ID cannot be blank");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new ValidationException("reason", "Reassignment reason must be provided");
        }
        this.id = id.trim();
        this.incidentId = incidentId.trim();
        this.requestingTechnicianId = requestingTechnicianId.trim();
        this.reason = reason.trim();
        this.status = Status.PENDING;
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt cannot be null");
    }

    public String getId() {
        return id;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public String getRequestingTechnicianId() {
        return requestingTechnicianId;
    }

    public String getReason() {
        return reason;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public String getReviewedByAdminId() {
        return reviewedByAdminId;
    }

    public void approve(String adminId, Instant now) {
        this.status = Status.APPROVED;
        this.reviewedByAdminId = adminId;
        this.resolvedAt = Objects.requireNonNull(now, "resolvedAt cannot be null");
    }

    public void reject(String adminId, Instant now) {
        this.status = Status.REJECTED;
        this.reviewedByAdminId = adminId;
        this.resolvedAt = Objects.requireNonNull(now, "resolvedAt cannot be null");
    }
}
