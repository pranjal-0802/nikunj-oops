package com.iitsm.web.mapper;

import com.iitsm.domain.model.AuditRecord;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaTracker;
import com.iitsm.domain.model.User;
import com.iitsm.domain.state.IncidentStates;
import com.iitsm.domain.state.PublicStatusMapper;
import com.iitsm.web.dto.AdminIncidentView;
import com.iitsm.web.dto.EmployeeIncidentView;
import com.iitsm.web.dto.TechnicianIncidentView;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pattern: Mapper / Adapter.
 * Maps pure domain Incident aggregate roots to persona-specific DTO projections.
 * Strictly guarantees that Employee projections NEVER contain priority, SLA internals, or internal notes.
 */
public final class IncidentDtoMapper {

    private IncidentDtoMapper() {}

    public static EmployeeIncidentView toEmployeeView(
            Incident incident,
            String assigneeName,
            List<AuditRecord> auditRecords) {
        return toEmployeeView(incident, assigneeName, null, auditRecords);
    }

    public static EmployeeIncidentView toEmployeeView(
            Incident incident,
            String assigneeName,
            String assigneeEmail,
            List<AuditRecord> auditRecords) {
        if (incident == null) return null;

        String publicStatus = PublicStatusMapper.toPublicStatus(incident.getStatus()).name();

        String resolutionText = null;
        if (incident.getResolution() != null) {
            resolutionText = incident.getResolution().stepsTaken();
            if (incident.getResolution().rootCause() != null && !incident.getResolution().rootCause().isBlank()) {
                resolutionText = incident.getResolution().rootCause() + " - " + resolutionText;
            }
        }

        // Only include actions an employee is permitted to trigger
        Set<String> internalActions = IncidentStates.of(incident.getStatus()).allowedActions();
        Set<String> employeeActions = internalActions.stream()
                .filter(a -> Set.of("provideInfo", "verify", "reject", "close").contains(a))
                .collect(Collectors.toSet());

        List<EmployeeIncidentView.EmployeeAuditRecordView> safeAuditTrail = (auditRecords != null)
                ? auditRecords.stream()
                        .map(r -> new EmployeeIncidentView.EmployeeAuditRecordView(
                                r.getAction(),
                                r.getDescription(),
                                r.getTimestamp()))
                        .collect(Collectors.toList())
                : Collections.emptyList();

        return new EmployeeIncidentView(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                publicStatus,
                assigneeName,
                assigneeEmail,
                incident.getCreatedAt(),
                resolutionText,
                safeAuditTrail,
                employeeActions,
                incident.getReopenCount());
    }

    public static TechnicianIncidentView toTechnicianView(Incident incident, User reporter) {
        if (incident == null) return null;

        String reporterName = (reporter != null) ? reporter.getName() : "Unknown";
        String reporterDept = (reporter instanceof Employee e) ? e.getDepartment() : "General";

        SlaTracker sla = incident.getSlaTracker();

        List<TechnicianIncidentView.InvestigationNoteView> notes = incident.getNotes().stream()
                .map(n -> new TechnicianIncidentView.InvestigationNoteView(
                        n.id(),
                        n.authorId(),
                        n.authorName(),
                        n.noteText(),
                        n.createdAt(),
                        n.internalOnly()))
                .collect(Collectors.toList());

        TechnicianIncidentView.ResolutionView resView = null;
        if (incident.getResolution() != null) {
            resView = new TechnicianIncidentView.ResolutionView(
                    incident.getResolution().rootCause(),
                    incident.getResolution().stepsTaken(),
                    incident.getResolution().preventiveAction(),
                    incident.getResolution().confirmedCategory(),
                    incident.getResolution().resolvedBy(),
                    incident.getResolution().resolvedAt());
        }

        Set<String> actions = IncidentStates.of(incident.getStatus()).allowedActions();

        return new TechnicianIncidentView(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getReporterId(),
                reporterName,
                reporterDept,
                incident.getCategory(),
                incident.getServiceId(),
                incident.getStatus(),
                (incident.getPriorityDecision() != null) ? incident.getPriorityDecision().priority() : null,
                (sla != null) ? sla.getResponseDueAt() : null,
                (sla != null) ? sla.getResolutionDueAt() : null,
                (sla != null) ? sla.getStatus() : null,
                notes,
                resView,
                actions,
                incident.getReopenCount(),
                incident.getCreatedAt());
    }

    public static AdminIncidentView toAdminView(
            Incident incident,
            User reporter,
            User assignee,
            List<AuditRecord> auditRecords) {
        if (incident == null) return null;

        String reporterName = (reporter != null) ? reporter.getName() : "Unknown";
        String reporterDept = (reporter instanceof Employee e) ? e.getDepartment() : "General";
        String assigneeName = (assignee != null) ? assignee.getName() : null;

        // Priority Decision View
        AdminIncidentView.PriorityDecisionView priorityView = null;
        if (incident.getPriorityDecision() != null) {
            PriorityDecision pd = incident.getPriorityDecision();
            List<AdminIncidentView.FactorContributionView> factors = pd.factorContributions().stream()
                    .map(f -> new AdminIncidentView.FactorContributionView(
                            f.factorName(),
                            f.rawScore(),
                            f.weight(),
                            f.weightedScore(),
                            f.explanation()))
                    .collect(Collectors.toList());

            priorityView = new AdminIncidentView.PriorityDecisionView(
                    pd.priority(),
                    pd.compositeScore(),
                    factors,
                    pd.rulesApplied(),
                    pd.calculatedAt(),
                    pd.overriddenBy(),
                    pd.overrideReason());
        }

        // SLA Tracker View
        AdminIncidentView.SlaTrackerView slaView = null;
        if (incident.getSlaTracker() != null) {
            SlaTracker s = incident.getSlaTracker();
            slaView = new AdminIncidentView.SlaTrackerView(
                    s.getResponseDueAt(),
                    s.getResolutionDueAt(),
                    s.getRespondedAt(),
                    s.getResolvedAt(),
                    s.getPausedDuration().toSeconds(),
                    s.getStatus());
        }

        // Notes
        List<TechnicianIncidentView.InvestigationNoteView> notes = incident.getNotes().stream()
                .map(n -> new TechnicianIncidentView.InvestigationNoteView(
                        n.id(),
                        n.authorId(),
                        n.authorName(),
                        n.noteText(),
                        n.createdAt(),
                        n.internalOnly()))
                .collect(Collectors.toList());

        // Resolution
        TechnicianIncidentView.ResolutionView resView = null;
        if (incident.getResolution() != null) {
            resView = new TechnicianIncidentView.ResolutionView(
                    incident.getResolution().rootCause(),
                    incident.getResolution().stepsTaken(),
                    incident.getResolution().preventiveAction(),
                    incident.getResolution().confirmedCategory(),
                    incident.getResolution().resolvedBy(),
                    incident.getResolution().resolvedAt());
        }

        // Feedback
        AdminIncidentView.FeedbackView fbView = null;
        if (incident.getFeedback() != null) {
            fbView = new AdminIncidentView.FeedbackView(
                    incident.getFeedback().rating(),
                    incident.getFeedback().comment(),
                    incident.getFeedback().submittedAt());
        }

        // Audit Trail
        List<AdminIncidentView.AdminAuditRecordView> adminAuditTrail = (auditRecords != null)
                ? auditRecords.stream()
                        .map(r -> new AdminIncidentView.AdminAuditRecordView(
                                r.getId(),
                                r.getTimestamp(),
                                r.getActorId(),
                                r.getAction(),
                                r.getPreviousState(),
                                r.getNewState(),
                                r.getDescription()))
                        .collect(Collectors.toList())
                : Collections.emptyList();

        Set<String> actions = IncidentStates.of(incident.getStatus()).allowedActions();

        return new AdminIncidentView(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getReporterId(),
                reporterName,
                reporterDept,
                incident.getCategory(),
                incident.getServiceId(),
                incident.getStatus(),
                priorityView,
                incident.getAssigneeId(),
                assigneeName,
                slaView,
                notes,
                resView,
                fbView,
                adminAuditTrail,
                incident.getLinkedIncidentIds(),
                actions,
                incident.getReopenCount(),
                incident.getCreatedAt(),
                incident.getVersion());
    }
}
