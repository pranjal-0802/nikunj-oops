package com.iitsm.web.controller;

import com.iitsm.application.dto.TriageQueueItemDto;
import com.iitsm.application.service.AuditService;
import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.SearchService;
import com.iitsm.application.service.UserService;
import com.iitsm.domain.model.AuditRecord;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.User;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.web.dto.AdminIncidentView;
import com.iitsm.web.dto.AssignRequest;
import com.iitsm.web.dto.FeedbackRequest;
import com.iitsm.web.dto.OverridePriorityRequest;
import com.iitsm.web.dto.ReassignmentRequestDto;
import com.iitsm.web.dto.ReclassifyRequest;
import com.iitsm.web.dto.StrategyInfoDto;
import com.iitsm.web.dto.SwitchStrategyRequest;
import com.iitsm.web.dto.TechnicianSummaryDto;
import com.iitsm.web.mapper.IncidentDtoMapper;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing command centre endpoints, directory search, and administrative controls.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IncidentService incidentService;
    private final SearchService searchService;
    private final UserService userService;
    private final AuditService auditService;
    private final Clock clock;

    public AdminController(
            IncidentService incidentService,
            SearchService searchService,
            UserService userService,
            AuditService auditService,
            Clock clock) {
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService cannot be null");
        this.searchService = Objects.requireNonNull(searchService, "searchService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @GetMapping("/stats")
    public ResponseEntity<IncidentService.AdminStats> getStats() {
        return ResponseEntity.ok(incidentService.getAdminStats());
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<AdminIncidentView>> searchIncidents(
            @RequestParam(name = "status", required = false) IncidentStatus status,
            @RequestParam(name = "priority", required = false) Priority priority,
            @RequestParam(name = "category", required = false) IncidentCategory category,
            @RequestParam(name = "serviceId", required = false) String serviceId,
            @RequestParam(name = "assigneeId", required = false) String assigneeId,
            @RequestParam(name = "reporterId", required = false) String reporterId,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate) {

        SearchService.IncidentSearchCriteria criteria = new SearchService.IncidentSearchCriteria(
                status, priority, category, serviceId, assigneeId, reporterId, query, fromDate, toDate);

        List<Incident> results = searchService.search(criteria);
        List<AdminIncidentView> views = results.stream()
                .map(this::mapToAdminView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<AdminIncidentView> getIncidentDetails(
            @PathVariable("id") String id,
            Authentication authentication) {
        String adminId = authentication.getName();
        Incident incident = incidentService.getIncident(adminId, id);
        return ResponseEntity.ok(mapToAdminView(incident));
    }

    @PostMapping("/incidents/{id}/assign")
    public ResponseEntity<AdminIncidentView> assignIncident(
            @PathVariable("id") String id,
            @Valid @RequestBody AssignRequest request,
            Authentication authentication) {
        String adminId = authentication.getName();
        Incident updated = incidentService.assign(id, adminId, request.technicianId());
        return ResponseEntity.ok(mapToAdminView(updated));
    }

    @PostMapping({"/incidents/{id}/override-priority", "/incidents/{id}/priority-override"})
    public ResponseEntity<AdminIncidentView> overridePriority(
            @PathVariable("id") String id,
            @Valid @RequestBody OverridePriorityRequest request,
            Authentication authentication) {
        String adminId = authentication.getName();
        Incident updated = incidentService.overridePriority(id, adminId, request.priority(), request.reason());
        return ResponseEntity.ok(mapToAdminView(updated));
    }

    @PostMapping("/incidents/{id}/close")
    public ResponseEntity<AdminIncidentView> closeIncident(
            @PathVariable("id") String id,
            @RequestBody(required = false) FeedbackRequest request,
            Authentication authentication) {
        String adminId = authentication.getName();
        Feedback feedback = null;
        if (request != null && request.rating() > 0) {
            feedback = new Feedback(request.rating(), request.comment(), clock.instant());
        }
        Incident updated = incidentService.close(id, adminId, feedback);
        return ResponseEntity.ok(mapToAdminView(updated));
    }

    @GetMapping("/triage-queue")
    public ResponseEntity<List<TriageQueueItemDto>> getTriageQueue() {
        return ResponseEntity.ok(incidentService.getUnifiedTriageQueue());
    }

    public record AcceptTriageSuggestionRequest(String category, String serviceId) {}

    @PostMapping("/triage/{id}/accept-suggestion")
    public ResponseEntity<AdminIncidentView> acceptTriageSuggestion(
            @PathVariable("id") String id,
            @RequestBody(required = false) AcceptTriageSuggestionRequest request,
            Authentication authentication) {
        String adminId = authentication.getName();
        IncidentCategory cat = (request != null && request.category() != null)
                ? IncidentCategory.valueOf(request.category().trim().toUpperCase())
                : IncidentCategory.SOFTWARE;
        String srv = (request != null) ? request.serviceId() : null;
        Incident updated = incidentService.acceptTriageSuggestion(id, adminId, cat, srv);
        return ResponseEntity.ok(mapToAdminView(updated));
    }

    @GetMapping("/technicians")
    public ResponseEntity<List<TechnicianSummaryDto>> getTechnicianRoster() {
        List<TechnicianSummaryDto> roster = userService.getAllTechnicians().stream()
                .map(TechnicianSummaryDto::fromDomain)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roster);
    }

    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<Void> unlockUser(@PathVariable String userId) {
        userService.unlockUser(userId);
        return ResponseEntity.ok().build();
    }

    public record UpdateDesignationRequest(String designation) {}

    @PutMapping("/users/{userId}/designation")
    public ResponseEntity<Void> updateDesignation(
            @PathVariable String userId,
            @RequestBody UpdateDesignationRequest request) {
        if (request != null && request.designation() != null) {
            Designation des = Designation.valueOf(request.designation().trim().toUpperCase());
            userService.updateEmployeeDesignation(userId, des);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/assignment/strategies")
    public ResponseEntity<List<StrategyInfoDto>> getAssignmentStrategies() {
        AssignmentStrategyRegistry registry = incidentService.getAssignmentStrategyRegistry();
        String activeName = registry.getActiveStrategyName();
        List<StrategyInfoDto> dtos = registry.getAllStrategies().stream()
                .map(s -> new StrategyInfoDto(s.getStrategyName(), s.getDescription(), s.getStrategyName().equalsIgnoreCase(activeName)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/assignment/strategies/active")
    public ResponseEntity<StrategyInfoDto> setActiveAssignmentStrategy(
            @Valid @RequestBody SwitchStrategyRequest request) {
        AssignmentStrategyRegistry registry = incidentService.getAssignmentStrategyRegistry();
        registry.setActiveStrategy(request.strategyName());
        AssignmentStrategy active = registry.getActiveStrategy();
        return ResponseEntity.ok(new StrategyInfoDto(active.getStrategyName(), active.getDescription(), true));
    }

    @PostMapping("/incidents/{id}/reclassify")
    public ResponseEntity<AdminIncidentView> reclassifyIncident(
            @PathVariable("id") String id,
            @Valid @RequestBody ReclassifyRequest request,
            Authentication authentication) {
        String adminId = authentication.getName();
        Incident updated = incidentService.reclassify(id, adminId, request.category(), request.serviceId(), request.reason());
        return ResponseEntity.ok(mapToAdminView(updated));
    }

    @GetMapping("/incidents/{id}/duplicates")
    public ResponseEntity<List<AdminIncidentView>> getIncidentDuplicates(@PathVariable("id") String id) {
        List<Incident> duplicates = incidentService.getDuplicateDetectionService().findLinkedDuplicates(id);
        List<AdminIncidentView> views = duplicates.stream()
                .map(this::mapToAdminView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    private AdminIncidentView mapToAdminView(Incident incident) {
        User reporter = userService.findById(incident.getReporterId()).orElse(null);
        User assignee = (incident.getAssigneeId() != null)
                ? userService.findById(incident.getAssigneeId()).orElse(null)
                : null;
        List<AuditRecord> auditRecords = auditService.getAuditTrailForIncident(incident.getId());
        return IncidentDtoMapper.toAdminView(incident, reporter, assignee, auditRecords);
    }
}
