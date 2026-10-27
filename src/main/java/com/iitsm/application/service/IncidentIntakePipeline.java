package com.iitsm.application.service;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.domain.strategy.assignment.CandidateFilter;
import com.iitsm.domain.strategy.classification.ClassificationResult;
import com.iitsm.domain.strategy.classification.IncidentClassifier;
import com.iitsm.domain.strategy.classification.ServiceMatcher;
import com.iitsm.domain.strategy.priority.CompositePriorityCalculator;
import com.iitsm.domain.strategy.priority.IncidentEvaluationContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline coordinating incident intake, ML classification, service matching,
 * duplicate correlation, multi-factor priority determination, and technician assignment.
 */
@Service
@Transactional
public class IncidentIntakePipeline {

    private static final Logger log = LoggerFactory.getLogger(IncidentIntakePipeline.class);

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final TechnicianRepository technicianRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final ItServiceRepository itServiceRepository;
    private final DomainEventPublisher eventPublisher;
    private final IncidentFactory incidentFactory;
    private final IncidentClassifier incidentClassifier;
    private final ServiceMatcher serviceMatcher;
    private final CompositePriorityCalculator priorityCalculator;
    private final AssignmentStrategyRegistry assignmentStrategyRegistry;
    private final CandidateFilter candidateFilter;
    private final DuplicateDetectionService duplicateDetectionService;
    private final TechnicianAssignmentService technicianAssignmentService;
    private final Clock clock;

    @Value("${iitsm.classification.min-confidence:0.60}")
    private double minConfidence = 0.60;

    public IncidentIntakePipeline(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            TechnicianRepository technicianRepository,
            SlaPolicyRepository slaPolicyRepository,
            ItServiceRepository itServiceRepository,
            DomainEventPublisher eventPublisher,
            IncidentFactory incidentFactory,
            IncidentClassifier incidentClassifier,
            ServiceMatcher serviceMatcher,
            CompositePriorityCalculator priorityCalculator,
            AssignmentStrategyRegistry assignmentStrategyRegistry,
            CandidateFilter candidateFilter,
            DuplicateDetectionService duplicateDetectionService,
            TechnicianAssignmentService technicianAssignmentService,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
        this.slaPolicyRepository = Objects.requireNonNull(slaPolicyRepository, "slaPolicyRepository cannot be null");
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository, "itServiceRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.incidentFactory = Objects.requireNonNull(incidentFactory, "incidentFactory cannot be null");
        this.incidentClassifier = Objects.requireNonNull(incidentClassifier, "incidentClassifier cannot be null");
        this.serviceMatcher = Objects.requireNonNull(serviceMatcher, "serviceMatcher cannot be null");
        this.priorityCalculator = Objects.requireNonNull(priorityCalculator, "priorityCalculator cannot be null");
        this.assignmentStrategyRegistry = Objects.requireNonNull(assignmentStrategyRegistry, "assignmentStrategyRegistry cannot be null");
        this.candidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter cannot be null");
        this.duplicateDetectionService = Objects.requireNonNull(duplicateDetectionService, "duplicateDetectionService cannot be null");
        this.technicianAssignmentService = Objects.requireNonNull(technicianAssignmentService, "technicianAssignmentService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    public Incident processIntake(String reporterIdOrEmail, String title, String description) {
        User reporter = loadUser(reporterIdOrEmail);

        Incident incident = incidentFactory.createIncident(title, description, reporter.getId());
        Instant now = clock.instant();

        // Step 1: Natural Language & ML Automated Classification
        ClassificationResult classResult = incidentClassifier.classify(title, description);
        IncidentCategory category = classResult.category();

        // Step 2: IT Service Identification
        List<ItService> services = itServiceRepository.findAll();
        Optional<ItService> matchedService = serviceMatcher.findBestMatch(title, description, services);
        ItService service = matchedService.orElseGet(() ->
                itServiceRepository.findById("SRV-GEN-001")
                        .or(() -> services.stream().filter(s -> s.getName().toLowerCase().contains("general") || s.getName().toLowerCase().contains("unclassified")).findFirst())
                        .orElse(null));
        String serviceId = (service != null) ? service.getId() : (services.isEmpty() ? null : services.getFirst().getId());
        incident.setCategory(category);
        incident.setServiceId(serviceId);

        // Step 3: Duplicate Detection and Correlation
        Optional<DuplicateDetectionService.DuplicateMatch> duplicateMatch = duplicateDetectionService.checkAndLinkDuplicate(incident);
        boolean isDuplicate = duplicateMatch.isPresent();

        // Step 4: Multi-Factor Priority Calculation (6 factors + Policy Override Rules)
        int recentTickets = countRecentUnresolvedTicketsOnService(reporter.getId(), serviceId, now);
        int linkedDuplicates = isDuplicate ? 0 : incident.getLinkedIncidentIds().size();
        IncidentEvaluationContext evalContext = new IncidentEvaluationContext(recentTickets, linkedDuplicates, now);

        PriorityDecision decision = priorityCalculator.calculate(incident, reporter, service, evalContext, now);
        Priority priority = decision.priority();

        // Step 5: Attach SLA Policy matching calculated priority
        SlaPolicy slaPolicy = slaPolicyRepository.findDefaultByPriority(priority)
                .orElseGet(() -> SlaPolicy.defaultFor(priority));

        incident.classify(category, serviceId, decision, slaPolicy, now);

        // Step 6: Assignment or Cluster Linkage
        if (isDuplicate) {
            Incident rootParent = incidentRepository.findById(duplicateMatch.get().parentIncidentId()).orElse(null);
            if (rootParent != null) {
                if (rootParent.getAssigneeId() != null) {
                    incident.assign(rootParent.getAssigneeId(), now);
                }
                // Recalculate parent ticket priority with updated cluster size
                User parentReporter = loadUser(rootParent.getReporterId());
                ItService parentService = rootParent.getServiceId() != null
                        ? itServiceRepository.findById(rootParent.getServiceId()).orElse(null)
                        : null;
                int parentRecent = countRecentUnresolvedTicketsOnService(parentReporter.getId(), rootParent.getServiceId(), now);
                int parentDuplicates = rootParent.getLinkedIncidentIds().size();
                IncidentEvaluationContext parentContext = new IncidentEvaluationContext(parentRecent, parentDuplicates, now);

                PriorityDecision parentDecision = priorityCalculator.calculate(rootParent, parentReporter, parentService, parentContext, now);
                SlaPolicy parentSla = slaPolicyRepository.findDefaultByPriority(parentDecision.priority())
                        .orElseGet(() -> SlaPolicy.defaultFor(parentDecision.priority()));
                rootParent.updatePriorityDecision(parentDecision, parentSla, now);
                incidentRepository.save(rootParent);

                eventPublisher.publish(new DomainEvent.PriorityCalculated(
                        UUID.randomUUID().toString(), now, rootParent.getId(), "SYSTEM", parentDecision));
            }
        } else if (classResult.confidence() >= minConfidence) {
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
        } else {
            incident.setNeedsTriage(true);
            log.info("Incident {} classification confidence {} is below threshold {}; leaving unassigned in triage queue",
                    incident.getId(), classResult.confidence(), minConfidence);
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

        log.info("Incident {} created with category {}, priority {} ({}), assigned to {}",
                saved.getId(), category, priority, decision.compositeScore(), saved.getAssigneeId());
        return saved;
    }

    private int countRecentUnresolvedTicketsOnService(String reporterId, String serviceId, Instant now) {
        if (reporterId == null || serviceId == null) {
            return 0;
        }
        Instant cutoff = now.minus(java.time.Duration.ofDays(30));
        return (int) incidentRepository.findByReporterId(reporterId).stream()
                .filter(i -> Objects.equals(i.getServiceId(), serviceId))
                .filter(i -> i.getStatus() != com.iitsm.domain.model.IncidentStatus.RESOLVED && i.getStatus() != com.iitsm.domain.model.IncidentStatus.CLOSED)
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
}

