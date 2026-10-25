package com.iitsm.web.dto;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Persona DTO for Administrator command centre.
 * Full visibility including priority calculation explainability, SLA internals, and complete audit history.
 */
public record AdminIncidentView(
        String id,
        String title,
        String description,
        String reporterId,
        String reporterName,
        String reporterDepartment,
        IncidentCategory category,
        String serviceId,
        IncidentStatus status,
        PriorityDecisionView priorityDecision,
        String assigneeId,
        String assigneeName,
        SlaTrackerView slaTracker,
        List<TechnicianIncidentView.InvestigationNoteView> notes,
        TechnicianIncidentView.ResolutionView resolution,
        FeedbackView feedback,
        List<AdminAuditRecordView> auditTrail,
        Set<String> linkedIncidentIds,
        Set<String> allowedActions,
        int reopenCount,
        Instant createdAt,
        Long version) {

    public record PriorityDecisionView(
            Priority priority,
            double compositeScore,
            List<FactorContributionView> factorContributions,
            List<String> rulesApplied,
            Instant calculatedAt,
            String overriddenBy,
            String overrideReason) {}

    public record FactorContributionView(
            String factorName,
            double rawScore,
            double weight,
            double weightedScore,
            String explanation) {}

    public record SlaTrackerView(
            Instant responseDueAt,
            Instant resolutionDueAt,
            Instant respondedAt,
            Instant resolvedAt,
            long pausedDurationSeconds,
            SlaStatus status) {}

    public record FeedbackView(
            int rating,
            String comment,
            Instant submittedAt) {}

    public record AdminAuditRecordView(
            String id,
            Instant timestamp,
            String actorId,
            String action,
            String previousState,
            String newState,
            String description) {}
}
