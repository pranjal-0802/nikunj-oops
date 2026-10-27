package com.iitsm.application.service;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.strategy.assignment.AssignmentRecommendation;
import com.iitsm.domain.strategy.assignment.AssignmentStrategy;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.domain.strategy.assignment.CandidateFilter;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled background dispatcher that periodically scans for unassigned CLASSIFIED incidents
 * and assigns them as technician capacity becomes available.
 */
@Service
public class UnassignedIncidentDispatcher {

    private static final Logger log = LoggerFactory.getLogger(UnassignedIncidentDispatcher.class);

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final ItServiceRepository itServiceRepository;
    private final CandidateFilter candidateFilter;
    private final AssignmentStrategyRegistry strategyRegistry;
    private final TechnicianAssignmentService technicianAssignmentService;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public UnassignedIncidentDispatcher(
            IncidentRepository incidentRepository,
            TechnicianRepository technicianRepository,
            ItServiceRepository itServiceRepository,
            CandidateFilter candidateFilter,
            AssignmentStrategyRegistry strategyRegistry,
            TechnicianAssignmentService technicianAssignmentService,
            DomainEventPublisher eventPublisher,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.technicianRepository = Objects.requireNonNull(technicianRepository, "technicianRepository cannot be null");
        this.itServiceRepository = Objects.requireNonNull(itServiceRepository, "itServiceRepository cannot be null");
        this.candidateFilter = Objects.requireNonNull(candidateFilter, "candidateFilter cannot be null");
        this.strategyRegistry = Objects.requireNonNull(strategyRegistry, "strategyRegistry cannot be null");
        this.technicianAssignmentService = Objects.requireNonNull(technicianAssignmentService, "technicianAssignmentService cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    @Transactional
    public int dispatchWaitingIncidents() {
        List<Incident> unassigned = incidentRepository.findByStatus(IncidentStatus.CLASSIFIED).stream()
                .filter(i -> i.getAssigneeId() == null)
                .filter(i -> !i.isNeedsTriage()) // Skip tickets requiring human triage
                .sorted(Comparator.comparing(Incident::getPriority, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        if (unassigned.isEmpty()) {
            return 0;
        }

        int assignedCount = 0;
        Instant now = clock.instant();

        for (Incident incident : unassigned) {
            String serviceId = incident.getServiceId();
            String preferredTeamId = null;
            if (serviceId != null) {
                preferredTeamId = itServiceRepository.findById(serviceId)
                        .map(ItService::getOwningTeamId)
                        .orElse(null);
            }

            List<Technician> available = technicianRepository.findAvailableTechnicians().stream()
                    .filter(Technician::hasCapacity)
                    .collect(Collectors.toList());

            if (available.isEmpty()) {
                continue; // Still no capacity anywhere
            }

            java.util.Set<String> exclusions = incident.getExcludedTechnicianIds();
            List<Technician> candidates = candidateFilter.filterCandidates(available, preferredTeamId, exclusions)
                    .stream().filter(Technician::hasCapacity).collect(Collectors.toList());

            if (candidates.isEmpty() && (exclusions == null || exclusions.isEmpty())) {
                candidates = available;
            }

            if (!candidates.isEmpty()) {
                AssignmentStrategy activeStrategy = strategyRegistry.getActiveStrategy();
                java.util.Optional<String> assignedId = technicianAssignmentService.tryAssignTechnician(incident, candidates, activeStrategy, now);
                if (assignedId.isPresent()) {
                    incident.assign(assignedId.get(), now);
                    incidentRepository.save(incident);

                    eventPublisher.publish(new DomainEvent.IncidentAssigned(
                            UUID.randomUUID().toString(), now, incident.getId(), "DISPATCHER", assignedId.get()));

                    log.info("Dispatcher assigned waiting incident {} to technician {}", incident.getId(), assignedId.get());
                    assignedCount++;
                }
            }
        }
        return assignedCount;
    }
}

