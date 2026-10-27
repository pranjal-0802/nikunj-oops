package com.iitsm.web.dto;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Persona DTO for Technician console.
 * Includes operational details, priority, SLA deadline tracking, investigation notes, and state-driven actions.
 */
public record TechnicianIncidentView(
        String id,
        String title,
        String description,
        String reporterId,
        String reporterName,
        String reporterDepartment,
        String assigneeId,
        String assigneeName,
        IncidentCategory category,
        String serviceId,
        IncidentStatus status,
        Priority priority,
        Instant responseDueAt,
        Instant resolutionDueAt,
        SlaStatus slaStatus,
        List<InvestigationNoteView> notes,
        ResolutionView resolution,
        Set<String> allowedActions,
        int reopenCount,
        Instant createdAt) {

    public record InvestigationNoteView(
            String id,
            String authorId,
            String authorName,
            String noteText,
            Instant createdAt,
            boolean internalOnly) {}

    public record ResolutionView(
            String rootCause,
            String stepsTaken,
            String preventiveAction,
            String confirmedCategory,
            String resolvedBy,
            Instant resolvedAt) {}
}
