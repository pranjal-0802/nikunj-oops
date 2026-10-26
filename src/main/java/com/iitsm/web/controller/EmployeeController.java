package com.iitsm.web.controller;

import com.iitsm.application.service.AuditService;
import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.UserService;
import com.iitsm.domain.model.AuditRecord;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.User;
import com.iitsm.web.dto.CreateIncidentRequest;
import com.iitsm.web.dto.EmployeeIncidentView;
import com.iitsm.web.dto.FeedbackRequest;
import com.iitsm.web.dto.ProvideInfoRequest;
import com.iitsm.web.dto.RejectRequest;
import com.iitsm.web.mapper.IncidentDtoMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing incident management endpoints for the Employee persona.
 * Guarantees that responses never contain internal priority, SLA trackers, or internal notes.
 */
@RestController
@RequestMapping("/api/employee/incidents")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeController {

    private final IncidentService incidentService;
    private final UserService userService;
    private final AuditService auditService;
    private final Clock clock;

    public EmployeeController(
            IncidentService incidentService,
            UserService userService,
            AuditService auditService,
            Clock clock) {
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
        this.auditService = Objects.requireNonNull(auditService, "auditService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @PostMapping
    public ResponseEntity<EmployeeIncidentView> raiseIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication) {
        String reporterId = authentication.getName();
        Incident incident = incidentService.createIncident(reporterId, request.title(), request.description());
        EmployeeIncidentView view = mapToEmployeeView(incident);
        return ResponseEntity.created(URI.create("/api/employee/incidents/" + incident.getId())).body(view);
    }

    @GetMapping
    public ResponseEntity<List<EmployeeIncidentView>> getMyTickets(Authentication authentication) {
        String reporterId = authentication.getName();
        List<Incident> tickets = incidentService.getMyTickets(reporterId);
        List<EmployeeIncidentView> views = tickets.stream()
                .map(this::mapToEmployeeView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeIncidentView> getTicketDetails(
            @PathVariable("id") String id,
            Authentication authentication) {
        String actorId = authentication.getName();
        Incident incident = incidentService.getIncident(actorId, id);
        return ResponseEntity.ok(mapToEmployeeView(incident));
    }

    @PostMapping("/{id}/provide-info")
    public ResponseEntity<EmployeeIncidentView> provideInfo(
            @PathVariable("id") String id,
            @Valid @RequestBody ProvideInfoRequest request,
            Authentication authentication) {
        String employeeId = authentication.getName();
        Incident updated = incidentService.provideInfo(id, employeeId, request.responseText());
        return ResponseEntity.ok(mapToEmployeeView(updated));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<EmployeeIncidentView> verifyResolution(
            @PathVariable("id") String id,
            Authentication authentication) {
        String employeeId = authentication.getName();
        Incident updated = incidentService.verify(id, employeeId);
        return ResponseEntity.ok(mapToEmployeeView(updated));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<EmployeeIncidentView> rejectResolution(
            @PathVariable("id") String id,
            @Valid @RequestBody RejectRequest request,
            Authentication authentication) {
        String employeeId = authentication.getName();
        Incident updated = incidentService.reject(id, employeeId, request.reason());
        return ResponseEntity.ok(mapToEmployeeView(updated));
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<EmployeeIncidentView> submitFeedback(
            @PathVariable("id") String id,
            @Valid @RequestBody FeedbackRequest request,
            Authentication authentication) {
        String employeeId = authentication.getName();
        Feedback feedback = new Feedback(request.rating(), request.comment(), clock.instant());
        Incident updated = incidentService.close(id, employeeId, feedback);
        return ResponseEntity.ok(mapToEmployeeView(updated));
    }

    private EmployeeIncidentView mapToEmployeeView(Incident incident) {
        String assigneeName = null;
        if (incident.getAssigneeId() != null) {
            assigneeName = userService.findById(incident.getAssigneeId())
                    .map(User::getName)
                    .orElse("Assigned Support");
        }
        List<AuditRecord> auditRecords = auditService.getAuditTrailForIncident(incident.getId());
        return IncidentDtoMapper.toEmployeeView(incident, assigneeName, auditRecords);
    }
}
