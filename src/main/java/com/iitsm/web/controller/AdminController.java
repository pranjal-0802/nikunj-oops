package com.iitsm.web.controller;

import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.SearchService;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.infrastructure.persistence.entity.ReassignmentRequestJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataAuditRecordRepository;
import com.iitsm.infrastructure.persistence.repository.SpringDataReassignmentRequestRepository;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.web.dto.AdminIncidentView;
import com.iitsm.web.dto.AssignRequest;
import com.iitsm.web.dto.FeedbackRequest;
import com.iitsm.web.dto.OverridePriorityRequest;
import com.iitsm.web.dto.ReclassifyRequest;
import com.iitsm.web.dto.StrategyInfoDto;
import com.iitsm.web.dto.SwitchStrategyRequest;
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
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SpringDataAuditRecordRepository auditRecordRepository;
    private final SpringDataReassignmentRequestRepository reassignmentRequestRepository;
    private final Clock clock;

    public AdminController(
            IncidentService incidentService,
            SearchService searchService,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            SpringDataAuditRecordRepository auditRecordRepository,
            SpringDataReassignmentRequestRepository reassignmentRequestRepository,
            Clock clock) {
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService cannot be null");
        this.searchService = Objects.requireNonNull(searchService, "searchService cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
        this.auditRecordRepository = Objects.requireNonNull(auditRecordRepository, "auditRecordRepository cannot be null");
        this.reassignmentRequestRepository = Objects.requireNonNull(reassignmentRequestRepository, "reassignmentRequestRepository cannot be null");
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

        List<Incident> searchResults = searchService.search(criteria);
        List<AdminIncidentView> views = searchResults.stream()
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

    @PostMapping("/incidents/{id}/override-priority")
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
    public ResponseEntity<List<ReassignmentRequestJpaEntity>> getTriageQueue() {
        List<ReassignmentRequestJpaEntity> pending = reassignmentRequestRepository.findByStatus(ReassignmentRequest.Status.PENDING);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/technicians")
    public ResponseEntity<List<Technician>> getTechnicianRoster() {
        List<Technician> roster = technicianRepository.findAll();
        return ResponseEntity.ok(roster);
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
        User reporter = userRepository.findById(incident.getReporterId()).orElse(null);
        User assignee = (incident.getAssigneeId() != null)
                ? userRepository.findById(incident.getAssigneeId()).orElse(null)
                : null;
        List<AuditRecordJpaEntity> auditRecords = auditRecordRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());
        return IncidentDtoMapper.toAdminView(incident, reporter, assignee, auditRecords);
    }
}

