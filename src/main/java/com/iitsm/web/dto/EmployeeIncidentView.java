package com.iitsm.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Persona DTO for Employee users.
 * STRICT PRIVACY INVARIANT: Priority, SLA metrics, and technician internal notes
 * are absent from this type definition by design (enforced by type system, not UI filter).
 */
public record EmployeeIncidentView(
        String id,
        String title,
        String description,
        String status, // Public 5-step stepper value: NEW, ASSIGNED, IN PROGRESS, RESOLVED, CLOSED
        String assigneeName,
        String assigneeEmail,
        Instant createdTime,
        String resolutionText,
        List<EmployeeAuditRecordView> auditTrail,
        Set<String> allowedActions,
        int reopenCount) {

    public record EmployeeAuditRecordView(
            String action,
            String description,
            Instant timestamp) {}
}

