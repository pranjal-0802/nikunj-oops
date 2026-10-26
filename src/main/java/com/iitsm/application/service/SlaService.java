package com.iitsm.application.service;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.model.SlaTracker;
import com.iitsm.domain.repository.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing SLA evaluations, violation detection, and triggering escalations.
 */
@Service
@Transactional
public class SlaService {

    private static final Logger log = LoggerFactory.getLogger(SlaService.class);

    private final IncidentRepository incidentRepository;
    private final DomainEventPublisher eventPublisher;
    private final EscalationService escalationService;
    private final Clock clock;

    public SlaService(
            IncidentRepository incidentRepository,
            DomainEventPublisher eventPublisher,
            EscalationService escalationService,
            Clock clock) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher cannot be null");
        this.escalationService = Objects.requireNonNull(escalationService, "escalationService cannot be null");
        this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    }

    /**
     * Evaluates the SLA state for a single incident.
     */
    public SlaEvaluationResult evaluateIncident(String incidentId) {
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        return evaluateIncidentInternal(incident);
    }

    /**
     * Evaluates all active (unresolved/unclosed) incidents in the system.
     */
    public List<SlaEvaluationResult> evaluateAllActiveIncidents() {
        List<Incident> all = incidentRepository.findAll();
        Instant now = clock.instant();
        List<SlaEvaluationResult> results = new ArrayList<>();

        for (Incident inc : all) {
            // Only evaluate active, non-terminal incidents
            if (inc.getStatus() != IncidentStatus.VERIFIED && inc.getStatus() != IncidentStatus.CLOSED) {
                results.add(evaluateIncidentInternal(inc));
            }
        }

        return results;
    }

    private SlaEvaluationResult evaluateIncidentInternal(Incident incident) {
        Instant now = clock.instant();
        SlaTracker tracker = incident.getSlaTracker();

        if (tracker == null) {
            return new SlaEvaluationResult(
                    incident.getId(),
                    SlaStatus.ON_TRACK,
                    SlaStatus.ON_TRACK,
                    0.0,
                    false,
                    Optional.empty()
            );
        }

        SlaStatus prevStatus = tracker.getStatus();
        incident.updateSlaStatus(now);
        SlaStatus currentStatus = tracker.getStatus();
        double riskScore = tracker.getRiskScore(now);
        boolean changed = (prevStatus != currentStatus);

        if (changed) {
            log.info("Incident {} SLA status transitioned from {} to {} (Risk Score: {})",
                    incident.getId(), prevStatus, currentStatus, riskScore);

            if (currentStatus == SlaStatus.AT_RISK || currentStatus == SlaStatus.WARNING) {
                DomainEvent.SlaWarning warningEvent = new DomainEvent.SlaWarning(
                        UUID.randomUUID().toString(),
                        now,
                        incident.getId(),
                        "SYSTEM_SLA_MONITOR",
                        "Incident " + incident.getId() + " reached SLA status " + currentStatus.getDescription()
                );
                eventPublisher.publish(warningEvent);
            } else if (currentStatus == SlaStatus.BREACHED) {
                DomainEvent.SlaViolated breachEvent = new DomainEvent.SlaViolated(
                        UUID.randomUUID().toString(),
                        now,
                        incident.getId(),
                        "SYSTEM_SLA_MONITOR",
                        "RESOLUTION_TARGET",
                        now
                );
                eventPublisher.publish(breachEvent);
            }

            incidentRepository.save(incident);
        }

        // Trigger escalation check
        Optional<EscalationRecord> escalation = escalationService.evaluateAndEscalate(incident.getId());

        return new SlaEvaluationResult(
                incident.getId(),
                prevStatus,
                currentStatus,
                riskScore,
                changed,
                escalation
        );
    }
}

