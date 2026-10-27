package com.iitsm.application.service;

import com.iitsm.application.dto.TriageQueueItemDto;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * High-level Application Service facade coordinating incident operations.
 * Delegates specialized domains to {@link IncidentIntakePipeline},
 * {@link IncidentLifecycleService}, and {@link AdminIncidentService}.
 */
@Service
@Transactional
public class IncidentService {

    public record TechnicianDashboardData(
            String technicianId,
            String technicianName,
            int activeLoad,
            int maxCapacity,
            double loadRatio,
            int assignedCount,
            int acknowledgedCount,
            int inProgressCount,
            int pendingInfoCount,
            int resolvedCount,
            int closedCount,
            int atRiskSlaCount) {}

    public record AdminStats(
            long totalIncidents,
            long newIncidents,
            long assignedIncidents,
            long inProgressIncidents,
            long resolvedIncidents,
            long closedIncidents,
            long criticalP1Incidents) {}

    private final IncidentIntakePipeline intakePipeline;
    private final IncidentLifecycleService lifecycleService;
    private final AdminIncidentService adminIncidentService;
    private final AssignmentStrategyRegistry assignmentStrategyRegistry;
    private final DuplicateDetectionService duplicateDetectionService;

    public IncidentService(
            IncidentIntakePipeline intakePipeline,
            IncidentLifecycleService lifecycleService,
            AdminIncidentService adminIncidentService,
            AssignmentStrategyRegistry assignmentStrategyRegistry,
            DuplicateDetectionService duplicateDetectionService) {
        this.intakePipeline = Objects.requireNonNull(intakePipeline, "intakePipeline cannot be null");
        this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService cannot be null");
        this.adminIncidentService = Objects.requireNonNull(adminIncidentService, "adminIncidentService cannot be null");
        this.assignmentStrategyRegistry = Objects.requireNonNull(assignmentStrategyRegistry, "assignmentStrategyRegistry cannot be null");
        this.duplicateDetectionService = Objects.requireNonNull(duplicateDetectionService, "duplicateDetectionService cannot be null");
    }

    public Incident createIncident(String reporterIdOrEmail, String title, String description) {
        return intakePipeline.processIntake(reporterIdOrEmail, title, description);
    }

    public Incident getIncident(String actorIdOrEmail, String incidentId) {
        return lifecycleService.getIncident(actorIdOrEmail, incidentId);
    }

    @Transactional(readOnly = true)
    public List<Incident> getMyTickets(String reporterIdOrEmail) {
        return lifecycleService.getMyTickets(reporterIdOrEmail);
    }

    @Transactional(readOnly = true)
    public List<Incident> getTechnicianQueue(String technicianIdOrEmail) {
        return lifecycleService.getTechnicianQueue(technicianIdOrEmail);
    }

    @Transactional(readOnly = true)
    public List<Incident> getTeamQueue(String actorIdOrEmail) {
        return lifecycleService.getTeamQueue(actorIdOrEmail);
    }

    @Transactional(readOnly = true)
    public TechnicianDashboardData getTechnicianDashboard(String technicianIdOrEmail) {
        return lifecycleService.getTechnicianDashboard(technicianIdOrEmail);
    }

    public String resolveOwningTeamId(Incident incident) {
        return lifecycleService.resolveOwningTeamId(incident);
    }

    public Incident acknowledge(String incidentId, String technicianId) {
        return lifecycleService.acknowledge(incidentId, technicianId);
    }

    public Incident startInvestigation(String incidentId, String technicianId) {
        return lifecycleService.startInvestigation(incidentId, technicianId);
    }

    public Incident addNote(String incidentId, String authorId, String authorName, String noteText, boolean internalOnly) {
        return lifecycleService.addNote(incidentId, authorId, authorName, noteText, internalOnly);
    }

    public Incident requestInfo(String incidentId, String technicianId, String message) {
        return lifecycleService.requestInfo(incidentId, technicianId, message);
    }

    public Incident provideInfo(String incidentId, String employeeId, String responseText) {
        return lifecycleService.provideInfo(incidentId, employeeId, responseText);
    }

    public Incident resolve(String incidentId, String technicianId, Resolution resolution) {
        return lifecycleService.resolve(incidentId, technicianId, resolution);
    }

    public Incident verify(String incidentId, String employeeId) {
        return lifecycleService.verify(incidentId, employeeId);
    }

    public Incident reject(String incidentId, String employeeId, String rejectionReason) {
        return lifecycleService.reject(incidentId, employeeId, rejectionReason);
    }

    public Incident close(String incidentId, String actorId, Feedback feedback) {
        return lifecycleService.close(incidentId, actorId, feedback);
    }

    public ReassignmentRequest flagWrongDomain(String incidentId, String technicianId, String reason) {
        return lifecycleService.flagWrongDomain(incidentId, technicianId, reason);
    }

    public ReassignmentRequest requestReassignment(String incidentId, String technicianId, String reason) {
        return lifecycleService.flagWrongDomain(incidentId, technicianId, reason);
    }

    @Transactional(readOnly = true)
    public AdminStats getAdminStats() {
        return adminIncidentService.getAdminStats();
    }

    public Incident assign(String incidentId, String adminId, String technicianId) {
        return adminIncidentService.assign(incidentId, adminId, technicianId);
    }

    public Incident overridePriority(String incidentId, String adminId, Priority newPriority, String reason) {
        return adminIncidentService.overridePriority(incidentId, adminId, newPriority, reason);
    }

    public Incident reclassify(String incidentId, String adminId, IncidentCategory newCategory, String serviceId, String reason) {
        return adminIncidentService.reclassify(incidentId, adminId, newCategory, serviceId, reason);
    }

    @Transactional(readOnly = true)
    public List<TriageQueueItemDto> getUnifiedTriageQueue() {
        return adminIncidentService.getUnifiedTriageQueue();
    }

    public Incident acceptTriageSuggestion(String incidentId, String adminId, IncidentCategory newCategory, String serviceId) {
        return adminIncidentService.acceptTriageSuggestion(incidentId, adminId, newCategory, serviceId);
    }

    @Transactional(readOnly = true)
    public List<ReassignmentRequest> getPendingReassignments() {
        return adminIncidentService.getPendingReassignments();
    }

    public AssignmentStrategyRegistry getAssignmentStrategyRegistry() {
        return assignmentStrategyRegistry;
    }

    public DuplicateDetectionService getDuplicateDetectionService() {
        return duplicateDetectionService;
    }
}
