package com.iitsm.application.service;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.exception.ServiceNotFoundException;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.model.AvailabilityStatus;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.IncidentUrgencyOrdering;
import com.iitsm.domain.model.InvestigationNote;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.policy.AccessPolicy;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.persistence.entity.ReassignmentRequestJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataReassignmentRequestRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core Application Service orchestrating the complete incident management lifecycle.
 * Dispatches domain events, enforces AccessPolicy, and ensures transactional atomicity.
 */
@Service
@Transactional
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);

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

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final ItServiceRepository itServiceRepository;
    private final SpringDataReassignmentRequestRepository reassignmentRequestRepository;
    private final DomainEventPublisher eventPublisher;
    private final IncidentFactory incidentFactory;
    private final AccessPolicy accessPolicy;
    private final Clock clock;

    public IncidentService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            SlaPolicyRepository slaPolicyRepository,
            ItServiceRepository itServiceRepository,
            SpringDataReassignmentRequestRepository reassignmentRequestRepository,
            DomainEventPublisher eventPublisher,
            IncidentFactory incidentFactory,
            AccessPolicy accessPolicy,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
        this.slaPolicyRepository = Objects.requireNonNull(slaPolicyRepository, "slaPolicyRepository cannot be null");
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository, "itServiceRepository cannot be null");
        this.reassignmentRequestRepository = Objects.requireNonNull(reassignmentRequestRepository, "reassignmentRequestRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.incidentFactory = Objects.requireNonNull(incidentFactory, "incidentFactory cannot be null");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public Incident createIncident(String reporterIdOrEmail, String title, String description) {
        User reporter = loadUser(reporterIdOrEmail);

        Incident incident = incidentFactory.createIncident(title, description, reporter.getId());
        Instant now = clock.instant();

        // 1. Initial placeholder classification (will be augmented by ML classifier in Phase 4)
        IncidentCategory category = IncidentCategory.APPLICATION;
        List<ItService> services = itServiceRepository.findAll();
        String serviceId = services.isEmpty() ? "SRV-DEFAULT" : services.get(0).getId();

        Priority priority = Priority.P3;
        PriorityDecision decision = new PriorityDecision(
                priority,
                0.40,
                Collections.emptyList(),
                List.of("PHASE3_DEFAULT_POLICY"),
                now,
                null,
                null);

        SlaPolicy slaPolicy = slaPolicyRepository.findDefaultByPriority(priority)
                .orElseGet(() -> new SlaPolicy("SLA-DEF-P3", priority, null, Duration.ofHours(1), Duration.ofHours(8), 0.80));

        incident.classify(category, serviceId, decision, slaPolicy, now);

        // 2. Initial assignment: round-robin / lowest load among available technicians
        List<Technician> availableTechs = technicianRepository.findAvailableTechnicians();
        if (availableTechs.isEmpty()) {
            availableTechs = technicianRepository.findAll();
        }

        if (!availableTechs.isEmpty()) {
            // Pick technician with lowest active count
            Technician assigned = availableTechs.stream()
                    .min(Comparator.comparingInt(t -> incidentRepository.findByAssigneeId(t.getId()).size()))
                    .orElse(availableTechs.get(0));
            incident.assign(assigned.getId(), now);
        }

        Incident saved = incidentRepository.save(incident);

        // Publish event spine
        eventPublisher.publish(new DomainEvent.IncidentCreated(
                UUID.randomUUID().toString(), now, saved.getId(), reporter.getId(), title, description));
        eventPublisher.publish(new DomainEvent.IncidentClassified(
                UUID.randomUUID().toString(), now, saved.getId(), "SYSTEM", category, serviceId));
        eventPublisher.publish(new DomainEvent.PriorityCalculated(
                UUID.randomUUID().toString(), now, saved.getId(), "SYSTEM", decision));

        if (saved.getAssigneeId() != null) {
            eventPublisher.publish(new DomainEvent.IncidentAssigned(
                    UUID.randomUUID().toString(), now, saved.getId(), "SYSTEM", saved.getAssigneeId()));
        }

        log.info("Incident {} created successfully for reporter {}", saved.getId(), reporter.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Incident getIncident(String actorId, String incidentId) {
        User actor = loadUser(actorId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanViewIncident(actor, incident);
        return incident;
    }

    @Transactional(readOnly = true)
    public List<Incident> getMyTickets(String reporterIdOrEmail) {
        User reporter = loadUser(reporterIdOrEmail);
        return incidentRepository.findByReporterId(reporter.getId()).stream()
                .sorted(Comparator.comparing(Incident::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Incident> getTechnicianQueue(String technicianIdOrEmail) {
        User technician = loadUser(technicianIdOrEmail);
        return incidentRepository.findByAssigneeId(technician.getId()).stream()
                .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                .sorted(IncidentUrgencyOrdering.INSTANCE)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TechnicianDashboardData getTechnicianDashboard(String technicianIdOrEmail) {
        User user = loadUser(technicianIdOrEmail);
        if (!(user instanceof Technician technician)) {
            throw new ServiceNotFoundException("Technician not found: " + technicianIdOrEmail);
        }

        List<Incident> assignedTickets = incidentRepository.findByAssigneeId(technician.getId());

        int assigned = 0, acknowledged = 0, inProgress = 0, pendingInfo = 0, resolved = 0, closed = 0, atRisk = 0;
        int activeLoad = 0;

        for (Incident inc : assignedTickets) {
            switch (inc.getStatus()) {
                case ASSIGNED -> { assigned++; activeLoad++; }
                case ACKNOWLEDGED -> { acknowledged++; activeLoad++; }
                case IN_PROGRESS -> { inProgress++; activeLoad++; }
                case PENDING_INFO -> { pendingInfo++; activeLoad++; }
                case RESOLVED -> resolved++;
                case CLOSED -> closed++;
                default -> {}
            }
            if (inc.getSlaTracker() != null) {
                SlaStatus sla = inc.getSlaTracker().getStatus();
                if (sla == SlaStatus.AT_RISK || sla == SlaStatus.WARNING) {
                    atRisk++;
                }
            }
        }

        double loadRatio = (double) activeLoad / (double) technician.getMaxCapacity();

        return new TechnicianDashboardData(
                technician.getId(),
                technician.getName(),
                activeLoad,
                technician.getMaxCapacity(),
                loadRatio,
                assigned,
                acknowledged,
                inProgress,
                pendingInfo,
                resolved,
                closed,
                atRisk);
    }

    @Transactional(readOnly = true)
    public AdminStats getAdminStats() {
        List<Incident> all = incidentRepository.findAll();
        long total = all.size();
        long newCount = all.stream().filter(i -> i.getStatus() == IncidentStatus.NEW || i.getStatus() == IncidentStatus.CLASSIFIED).count();
        long assigned = all.stream().filter(i -> i.getStatus() == IncidentStatus.ASSIGNED || i.getStatus() == IncidentStatus.ACKNOWLEDGED).count();
        long inProgress = all.stream().filter(i -> i.getStatus() == IncidentStatus.IN_PROGRESS || i.getStatus() == IncidentStatus.PENDING_INFO).count();
        long resolved = all.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.VERIFIED).count();
        long closed = all.stream().filter(i -> i.getStatus() == IncidentStatus.CLOSED).count();
        long critical = all.stream().filter(i -> i.getPriorityDecision() != null && i.getPriorityDecision().priority() == Priority.P1).count();

        return new AdminStats(total, newCount, assigned, inProgress, resolved, closed, critical);
    }

    public Incident acknowledge(String incidentId, String technicianId) {
        User actor = loadUser(technicianId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "acknowledge");

        Instant now = clock.instant();
        incident.acknowledge(technicianId, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentAcknowledged(
                UUID.randomUUID().toString(), now, incidentId, technicianId));
        return saved;
    }

    public Incident startInvestigation(String incidentId, String technicianId) {
        User actor = loadUser(technicianId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "startInvestigation");

        Instant now = clock.instant();
        incident.startInvestigation(technicianId, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.InvestigationStarted(
                UUID.randomUUID().toString(), now, incidentId, technicianId));
        return saved;
    }

    public Incident addNote(String incidentId, String authorId, String authorName, String noteText, boolean internalOnly) {
        User actor = loadUser(authorId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanViewIncident(actor, incident);

        Instant now = clock.instant();
        String noteId = "NOTE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        incident.addNote(new InvestigationNote(noteId, authorId, authorName, noteText, now, internalOnly));

        return incidentRepository.save(incident);
    }

    public Incident requestInfo(String incidentId, String technicianId, String message) {
        User actor = loadUser(technicianId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "requestInfo");

        Instant now = clock.instant();
        incident.requestInfo(technicianId, message, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.MoreInfoRequested(
                UUID.randomUUID().toString(), now, incidentId, technicianId, message));
        return saved;
    }

    public Incident provideInfo(String incidentId, String employeeId, String responseText) {
        User actor = loadUser(employeeId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "provideInfo");

        Instant now = clock.instant();
        incident.provideInfo(employeeId, responseText, now);
        Incident saved = incidentRepository.save(incident);

        return saved;
    }

    public Incident resolve(String incidentId, String technicianId, Resolution resolution) {
        User actor = loadUser(technicianId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "resolve");

        Instant now = clock.instant();
        incident.resolve(resolution, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentResolved(
                UUID.randomUUID().toString(), now, incidentId, technicianId, resolution));
        return saved;
    }

    public Incident verify(String incidentId, String employeeId) {
        User actor = loadUser(employeeId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "verify");

        Instant now = clock.instant();
        incident.verify(employeeId, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.ResolutionVerified(
                UUID.randomUUID().toString(), now, incidentId, employeeId));
        return saved;
    }

    public Incident reject(String incidentId, String employeeId, String rejectionReason) {
        User actor = loadUser(employeeId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "reject");

        Instant now = clock.instant();
        incident.reject(employeeId, rejectionReason, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentReopened(
                UUID.randomUUID().toString(), now, incidentId, employeeId, rejectionReason, incident.getReopenCount()));
        return saved;
    }

    public Incident close(String incidentId, String actorId, Feedback feedback) {
        User actor = loadUser(actorId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "close");

        Instant now = clock.instant();
        incident.close(actorId, feedback, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentClosed(
                UUID.randomUUID().toString(), now, incidentId, actorId, feedback));
        return saved;
    }

    public ReassignmentRequestJpaEntity flagWrongDomain(String incidentId, String technicianId, String reason) {
        User actor = loadUser(technicianId);
        Incident incident = loadIncident(incidentId);
        accessPolicy.enforceCanExecuteAction(actor, incident, "flagWrongDomain");

        Instant now = clock.instant();
        incident.flagWrongDomain(technicianId, reason, now);
        incidentRepository.save(incident);

        ReassignmentRequestJpaEntity req = new ReassignmentRequestJpaEntity();
        req.setId("REASSIGN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        req.setIncidentId(incidentId);
        req.setRequestingTechnicianId(technicianId);
        req.setReason(reason);
        req.setStatus(ReassignmentRequest.Status.PENDING);
        req.setRequestedAt(now);

        ReassignmentRequestJpaEntity savedReq = reassignmentRequestRepository.save(req);

        eventPublisher.publish(new DomainEvent.ReassignmentRequested(
                UUID.randomUUID().toString(), now, incidentId, technicianId, technicianId, reason));
        return savedReq;
    }

    public Incident overridePriority(String incidentId, String adminId, Priority newPriority, String reason) {
        User admin = loadUser(adminId);
        accessPolicy.enforceAdmin(admin);

        Incident incident = loadIncident(incidentId);
        Priority previousPriority = incident.getPriorityDecision() != null
                ? incident.getPriorityDecision().priority()
                : Priority.P3;

        Instant now = clock.instant();
        incident.overridePriority(newPriority, adminId, reason, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.PriorityOverridden(
                UUID.randomUUID().toString(), now, incidentId, adminId, previousPriority, newPriority, reason));
        return saved;
    }

    public Incident assign(String incidentId, String adminId, String technicianId) {
        User admin = loadUser(adminId);
        accessPolicy.enforceAdmin(admin);

        Incident incident = loadIncident(incidentId);
        Technician technician = (Technician) userRepository.findById(technicianId)
                .filter(u -> u instanceof Technician)
                .orElseThrow(() -> new ServiceNotFoundException("Technician not found: " + technicianId));

        Instant now = clock.instant();
        if (incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.ACKNOWLEDGED) {
            incident.flagWrongDomain(adminId, "Administrative reassignment", now);
        }
        incident.assign(technician.getId(), now);
        Incident saved = incidentRepository.save(incident);

        // Resolve any pending reassignment requests for this incident
        List<ReassignmentRequestJpaEntity> pendingRequests = reassignmentRequestRepository.findByIncidentId(incidentId);
        for (ReassignmentRequestJpaEntity req : pendingRequests) {
            if (req.getStatus() == ReassignmentRequest.Status.PENDING) {
                req.setStatus(ReassignmentRequest.Status.APPROVED);
                reassignmentRequestRepository.save(req);
            }
        }

        eventPublisher.publish(new DomainEvent.IncidentAssigned(
                UUID.randomUUID().toString(), now, incidentId, adminId, technicianId));
        return saved;
    }

    private User loadUser(String userIdOrEmail) {
        if (userIdOrEmail == null || userIdOrEmail.isBlank()) {
            throw new ValidationException("userId", "User identifier cannot be blank");
        }
        if (userIdOrEmail.contains("@")) {
            return userRepository.findByEmail(new com.iitsm.domain.model.Email(userIdOrEmail.trim()))
                    .orElseThrow(() -> new ValidationException("userId", "User not found with email: " + userIdOrEmail));
        }
        return userRepository.findById(userIdOrEmail.trim())
                .orElseThrow(() -> new ValidationException("userId", "User not found with ID: " + userIdOrEmail));
    }

    private Incident loadIncident(String incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ServiceNotFoundException("Incident not found: " + incidentId));
    }
}
