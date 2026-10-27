package com.iitsm.web.controller;

import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.UserService;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.User;
import com.iitsm.web.dto.AddNoteRequest;
import com.iitsm.web.dto.FlagWrongDomainRequest;
import com.iitsm.web.dto.ProvideInfoRequest;
import com.iitsm.web.dto.ResolveIncidentRequest;
import com.iitsm.web.dto.TechnicianIncidentView;
import com.iitsm.web.mapper.IncidentDtoMapper;
import jakarta.validation.Valid;
import java.time.Clock;
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
 * Controller exposing operational queue, dashboard, and investigation actions for IT Technicians.
 */
@RestController
@RequestMapping("/api/technician")
@PreAuthorize("hasAnyRole('TECHNICIAN', 'TEAM_LEAD', 'ADMIN')")
public class TechnicianController {

    private final IncidentService incidentService;
    private final UserService userService;
    private final Clock clock;

    public TechnicianController(
            IncidentService incidentService,
            UserService userService,
            Clock clock) {
        this.incidentService = Objects.requireNonNull(incidentService, "incidentService cannot be null");
        this.userService = Objects.requireNonNull(userService, "userService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @GetMapping("/queue")
    public ResponseEntity<List<TechnicianIncidentView>> getQueue(Authentication authentication) {
        String technicianId = authentication.getName();
        List<Incident> queue = incidentService.getTechnicianQueue(technicianId);
        List<TechnicianIncidentView> views = queue.stream()
                .map(this::mapToTechnicianView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @GetMapping("/team-queue")
    public ResponseEntity<List<TechnicianIncidentView>> getTeamQueue(Authentication authentication) {
        String actorId = authentication.getName();
        List<Incident> queue = incidentService.getTeamQueue(actorId);
        List<TechnicianIncidentView> views = queue.stream()
                .map(this::mapToTechnicianView)
                .collect(Collectors.toList());
        return ResponseEntity.ok(views);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<IncidentService.TechnicianDashboardData> getDashboard(Authentication authentication) {
        String technicianId = authentication.getName();
        IncidentService.TechnicianDashboardData data = incidentService.getTechnicianDashboard(technicianId);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/incidents/{id}")
    public ResponseEntity<TechnicianIncidentView> getIncidentDetails(
            @PathVariable("id") String id,
            Authentication authentication) {
        String actorId = authentication.getName();
        Incident incident = incidentService.getIncident(actorId, id);
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/acknowledge")
    public ResponseEntity<TechnicianIncidentView> acknowledge(
            @PathVariable("id") String id,
            Authentication authentication) {
        String technicianId = authentication.getName();
        Incident incident = incidentService.acknowledge(id, technicianId);
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/start-investigation")
    public ResponseEntity<TechnicianIncidentView> startInvestigation(
            @PathVariable("id") String id,
            Authentication authentication) {
        String technicianId = authentication.getName();
        Incident incident = incidentService.startInvestigation(id, technicianId);
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/notes")
    public ResponseEntity<TechnicianIncidentView> addNote(
            @PathVariable("id") String id,
            @Valid @RequestBody AddNoteRequest request,
            Authentication authentication) {
        String authorId = authentication.getName();
        String authorName = userService.findById(authorId).map(User::getName).orElse(authorId);
        Incident incident = incidentService.addNote(id, authorId, authorName, request.noteText(), request.internalOnly());
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/request-info")
    public ResponseEntity<TechnicianIncidentView> requestInfo(
            @PathVariable("id") String id,
            @Valid @RequestBody ProvideInfoRequest request,
            Authentication authentication) {
        String technicianId = authentication.getName();
        Incident incident = incidentService.requestInfo(id, technicianId, request.responseText());
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/resolve")
    public ResponseEntity<TechnicianIncidentView> resolve(
            @PathVariable("id") String id,
            @Valid @RequestBody ResolveIncidentRequest request,
            Authentication authentication) {
        String technicianId = authentication.getName();
        Resolution resolution = new Resolution(
                request.rootCause(),
                request.stepsTaken(),
                (request.preventiveAction() != null && !request.preventiveAction().isBlank())
                        ? request.preventiveAction()
                        : "Standard operational procedure applied",
                request.confirmedCategory().name(),
                technicianId,
                clock.instant());
        Incident incident = incidentService.resolve(id, technicianId, resolution);
        return ResponseEntity.ok(mapToTechnicianView(incident));
    }

    @PostMapping("/incidents/{id}/flag-wrong-domain")
    public ResponseEntity<Void> flagWrongDomain(
            @PathVariable("id") String id,
            @Valid @RequestBody FlagWrongDomainRequest request,
            Authentication authentication) {
        String technicianId = authentication.getName();
        incidentService.flagWrongDomain(id, technicianId, request.reason());
        return ResponseEntity.noContent().build();
    }

    private TechnicianIncidentView mapToTechnicianView(Incident incident) {
        User reporter = userService.findById(incident.getReporterId()).orElse(null);
        return IncidentDtoMapper.toTechnicianView(incident, reporter);
    }
}
