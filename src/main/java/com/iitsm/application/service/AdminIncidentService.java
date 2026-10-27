package com.iitsm.application.service;

import com.iitsm.application.dto.TriageQueueItemDto;
import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.exception.ServiceNotFoundException;
import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.ReassignmentRequest;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.policy.AccessPolicy;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.ReassignmentRequestRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.domain.strategy.assignment.CandidateFilter;
import com.iitsm.domain.strategy.classification.ClassificationResult;
import com.iitsm.domain.strategy.classification.IncidentClassifier;
import com.iitsm.domain.strategy.priority.CompositePriorityCalculator;
import com.iitsm.domain.strategy.priority.IncidentEvaluationContext;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing administrative incident commands: assignment, priority overrides,
 * reclassification, and unified triage queue handling.
 */
@Service
@Transactional
public class AdminIncidentService {

    private static final Logger log = LoggerFactory.getLogger(AdminIncidentService.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final ItServiceRepository itServiceRepository;
    private final ReassignmentRequestRepository reassignmentRequestRepository;
    private final DomainEventPublisher eventPublisher;
    private final AccessPolicy accessPolicy;
    private final IncidentClassifier incidentClassifier;
    private final CompositePriorityCalculator priorityCalculator;
    private final AssignmentStrategyRegistry assignmentStrategyRegistry;
    private final CandidateFilter candidateFilter;
    private final TechnicianAssignmentService technicianAssignmentService;
    private final Clock clock;

    public AdminIncidentService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            SlaPolicyRepository slaPolicyRepository,
            ItServiceRepository itServiceRepository,
            ReassignmentRequestRepository reassignmentRequestRepository,
            DomainEventPublisher eventPublisher,
            AccessPolicy accessPolicy,
            IncidentClassifier incidentClassifier,
            CompositePriorityCalculator priorityCalculator,
            AssignmentStrategyRegistry assignmentStrategyRegistry,
            CandidateFilter candidateFilter,
            TechnicianAssignmentService technicianAssignmentService,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
        this.slaPolicyRepository = Objects.requireNonNull(slaPolicyRepository, "slaPolicyRepository cannot be null");
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository, "itServiceRepository cannot be null");
        this.reassignmentRequestRepository = Objects.requireNonNull(reassignmentRequestRepository, "reassignmentRequestRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.accessPolicy = Objects.requireNonNull(accessPolicy, "accessPolicy cannot be null");
        this.incidentClassifier = Objects.requireNonNull(incidentClassifier, "incidentClassifier cannot be null");
        this.priorityCalculator = Objects.requireNonNull(priorityCalculator, "priorityCalculator cannot be null");
        this.assignmentStrategyRegistry = Objects.requireNonNull(assignmentStrategyRegistry, "assignmentStrategyRegistry cannot be null");
        this.candidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter cannot be null");
        this.technicianAssignmentService = Objects.requireNonNull(technicianAssignmentService, "technicianAssignmentService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Transactional(readOnly = true)
    public IncidentService.AdminStats getAdminStats() {
        long total = incidentRepository.count();
        long newCount = incidentRepository.countByStatusIn(List.of(IncidentStatus.NEW, IncidentStatus.CLASSIFIED));
        long assigned = incidentRepository.countByStatusIn(List.of(IncidentStatus.ASSIGNED, IncidentStatus.ACKNOWLEDGED));
        long inProgress = incidentRepository.countByStatusIn(List.of(IncidentStatus.IN_PROGRESS, IncidentStatus.PENDING_INFO));
        long resolved = incidentRepository.countByStatusIn(List.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED));
        long closed = incidentRepository.countByStatusIn(List.of(IncidentStatus.CLOSED));
        long critical = incidentRepository.countByPriority(Priority.P1);

        return new IncidentService.AdminStats(total, newCount, assigned, inProgress, resolved, closed, critical);
    }

    public Incident assign(String incidentId, String adminId, String technicianId) {
        User admin = loadUser(adminId);
        accessPolicy.enforceAdmin(admin);

        Incident incident = loadIncident(incidentId);
        Technician technician = (Technician) userRepository.findById(technicianId)
                .filter(u -> u instanceof Technician)
                .orElseThrow(() -> new ServiceNotFoundException("Technician not found: " + technicianId));

        Instant now = clock.instant();
        String previousAssigneeId = incident.getAssigneeId();
        if (incident.getStatus() == IncidentStatus.ASSIGNED || incident.getStatus() == IncidentStatus.ACKNOWLEDGED) {
            incident.flagWrongDomain(adminId, "Administrative reassignment", now);
            if (previousAssigneeId != null) {
                technicianRepository.findById(previousAssigneeId).ifPresent(prevTech -> {
                    prevTech.releaseIncident(incidentId);
                    technicianRepository.save(prevTech);
                });
            }
        }
        incident.setNeedsTriage(false);
        incident.clearExcludedTechnicians();
        incident.assign(technician.getId(), now);
        technician.assignIncident(incident.getId());
        technicianRepository.save(technician);
        Incident saved = incidentRepository.save(incident);

        // Resolve any pending reassignment requests for this incident
        List<ReassignmentRequest> pendingRequests = reassignmentRequestRepository.findByIncidentId(incidentId);
        for (ReassignmentRequest req : pendingRequests) {
            if (req.getStatus() == ReassignmentRequest.Status.PENDING) {
                req.approve(adminId, now);
                reassignmentRequestRepository.save(req);
            }
        }

        eventPublisher.publish(new DomainEvent.IncidentAssigned(
                UUID.randomUUID().toString(), now, incidentId, adminId, technicianId));
        return saved;
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

    public Incident reclassify(String incidentId, String adminId, IncidentCategory newCategory, String serviceId, String reason) {
        User admin = loadUser(adminId);
        accessPolicy.enforceAdmin(admin);

        Incident incident = loadIncident(incidentId);
        Instant now = clock.instant();

        ItService service = (serviceId != null) ? itServiceRepository.findById(serviceId).orElse(null) : null;
        User reporter = userRepository.findById(incident.getReporterId()).orElse(admin);

        int recentTickets = countRecentUnresolvedTicketsOnService(incident.getReporterId(), incident.getServiceId(), now);
        int linkedDuplicates = incident.getLinkedIncidentIds().size();
        IncidentEvaluationContext evalContext = new IncidentEvaluationContext(recentTickets, linkedDuplicates, now);

        PriorityDecision decision = priorityCalculator.calculate(incident, reporter, service, evalContext, now);
        SlaPolicy slaPolicy = slaPolicyRepository.findDefaultByPriority(decision.priority())
                .orElseGet(() -> SlaPolicy.defaultFor(decision.priority()));

        incident.setNeedsTriage(false);
        incident.clearExcludedTechnicians();
        incident.reclassify(newCategory, (service != null) ? service.getId() : serviceId, decision, slaPolicy, now);
        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentClassified(
                UUID.randomUUID().toString(), now, incidentId, adminId, newCategory, (service != null) ? service.getId() : serviceId));
        eventPublisher.publish(new DomainEvent.PriorityCalculated(
                UUID.randomUUID().toString(), now, incidentId, adminId, decision));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<TriageQueueItemDto> getUnifiedTriageQueue() {
        List<TriageQueueItemDto> items = new ArrayList<>();

        // 1. Low-confidence incidents needing human triage
        List<Incident> lowConfidenceIncidents = incidentRepository.findByNeedsTriageTrue();
        for (Incident inc : lowConfidenceIncidents) {
            String serviceName = null;
            if (inc.getServiceId() != null) {
                serviceName = itServiceRepository.findById(inc.getServiceId()).map(ItService::getName).orElse(null);
            }
            List<TriageQueueItemDto.CategorySuggestionDto> suggestions = new ArrayList<>();
            double confidence = 0.50;
            try {
                ClassificationResult pred = incidentClassifier.classify(inc.getTitle(), inc.getDescription() != null ? inc.getDescription() : "");
                if (pred != null) {
                    confidence = pred.confidence();
                    suggestions.add(new TriageQueueItemDto.CategorySuggestionDto(pred.category().name(), pred.confidence()));
                }
            } catch (Exception ignored) {}

            items.add(new TriageQueueItemDto(
                    "LOW_CONFIDENCE",
                    inc.getId(),
                    inc.getTitle(),
                    inc.getDescription(),
                    inc.getCategory() != null ? inc.getCategory().name() : null,
                    inc.getServiceId(),
                    serviceName,
                    confidence,
                    suggestions,
                    null,
                    "Automated intake flagged low classification confidence (< 0.60)",
                    inc.getCreatedAt()
            ));
        }

        // 2. Pending wrong-domain reassignment requests
        List<ReassignmentRequest> pendingRequests = reassignmentRequestRepository.findByStatus(ReassignmentRequest.Status.PENDING);
        for (ReassignmentRequest req : pendingRequests) {
            incidentRepository.findById(req.getIncidentId()).ifPresent(inc -> {
                String serviceName = null;
                if (inc.getServiceId() != null) {
                    serviceName = itServiceRepository.findById(inc.getServiceId()).map(ItService::getName).orElse(null);
                }
                List<TriageQueueItemDto.CategorySuggestionDto> suggestions = new ArrayList<>();
                double confidence = 0.50;
                try {
                    ClassificationResult pred = incidentClassifier.classify(inc.getTitle(), inc.getDescription() != null ? inc.getDescription() : "");
                    if (pred != null) {
                        confidence = pred.confidence();
                        suggestions.add(new TriageQueueItemDto.CategorySuggestionDto(pred.category().name(), pred.confidence()));
                    }
                } catch (Exception ignored) {}

                items.add(new TriageQueueItemDto(
                        "WRONG_DOMAIN",
                        inc.getId(),
                        inc.getTitle(),
                        inc.getDescription(),
                        inc.getCategory() != null ? inc.getCategory().name() : null,
                        inc.getServiceId(),
                        serviceName,
                        confidence,
                        suggestions,
                        req.getRequestingTechnicianId(),
                        req.getReason(),
                        req.getRequestedAt()
                ));
            });
        }

        return items;
    }

    public Incident acceptTriageSuggestion(String incidentId, String adminId, IncidentCategory newCategory, String serviceId) {
        User admin = loadUser(adminId);
        accessPolicy.enforceAdmin(admin);

        Incident incident = loadIncident(incidentId);
        Instant now = clock.instant();

        ItService service = (serviceId != null) ? itServiceRepository.findById(serviceId).orElse(null) : null;
        User reporter = userRepository.findById(incident.getReporterId()).orElse(admin);

        int recentTickets = countRecentUnresolvedTicketsOnService(incident.getReporterId(), incident.getServiceId(), now);
        int linkedDuplicates = incident.getLinkedIncidentIds().size();
        IncidentEvaluationContext evalContext = new IncidentEvaluationContext(recentTickets, linkedDuplicates, now);

        PriorityDecision decision = priorityCalculator.calculate(incident, reporter, service, evalContext, now);
        SlaPolicy slaPolicy = slaPolicyRepository.findDefaultByPriority(decision.priority())
                .orElseGet(() -> SlaPolicy.defaultFor(decision.priority()));

        incident.setNeedsTriage(false);
        incident.clearExcludedTechnicians();
        incident.reclassify(newCategory, (service != null) ? service.getId() : (incident.getServiceId() != null ? incident.getServiceId() : "SRV-GEN-001"), decision, slaPolicy, now);

        // Resolve any pending reassignment requests for this incident
        List<ReassignmentRequest> pendingRequests = reassignmentRequestRepository.findByIncidentId(incidentId);
        for (ReassignmentRequest req : pendingRequests) {
            if (req.getStatus() == ReassignmentRequest.Status.PENDING) {
                req.approve(adminId, now);
                reassignmentRequestRepository.save(req);
            }
        }

        // Attempt assignment
        List<Technician> availableTechs = technicianRepository.findAvailableTechnicians().stream()
                .filter(Technician::hasCapacity)
                .collect(Collectors.toList());
        String preferredTeamId = (service != null) ? service.getOwningTeamId() : null;
        List<Technician> candidates = candidateFilter.filterCandidates(availableTechs, preferredTeamId, Collections.emptySet())
                .stream().filter(Technician::hasCapacity).collect(Collectors.toList());
        if (candidates.isEmpty()) {
            candidates = availableTechs;
        }
        if (!candidates.isEmpty()) {
            AssignmentStrategy activeStrategy = assignmentStrategyRegistry.getActiveStrategy();
            try {
                Optional<String> assignedId = technicianAssignmentService.tryAssignTechnician(incident, candidates, activeStrategy, now);
                assignedId.ifPresent(techId -> incident.assign(techId, now));
            } catch (Exception e) {
                log.warn("Technician capacity contention for incident {}: {}", incident.getId(), e.getMessage());
            }
        }

        Incident saved = incidentRepository.save(incident);

        eventPublisher.publish(new DomainEvent.IncidentClassified(
                UUID.randomUUID().toString(), now, incidentId, adminId, newCategory, incident.getServiceId()));
        eventPublisher.publish(new DomainEvent.PriorityCalculated(
                UUID.randomUUID().toString(), now, incidentId, adminId, decision));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReassignmentRequest> getPendingReassignments() {
        return reassignmentRequestRepository.findByStatus(ReassignmentRequest.Status.PENDING);
    }

    private int countRecentUnresolvedTicketsOnService(String reporterId, String serviceId, Instant now) {
        if (reporterId == null || serviceId == null) {
            return 0;
        }
        Instant cutoff = now.minus(java.time.Duration.ofDays(30));
        return (int) incidentRepository.findByReporterId(reporterId).stream()
                .filter(i -> Objects.equals(i.getServiceId(), serviceId))
                .filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED)
                .filter(i -> i.getCreatedAt().isAfter(cutoff))
                .count();
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
