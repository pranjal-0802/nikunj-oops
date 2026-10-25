package com.iitsm.infrastructure.observer;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventListener;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataAuditRecordRepository;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pattern: Observer.
 * Synchronous domain event listener persisting immutable audit records in the same database transaction.
 */
@Component
public class AuditTrailObserver implements DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditTrailObserver.class);

    private final SpringDataAuditRecordRepository auditRepository;

    public AuditTrailObserver(SpringDataAuditRecordRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository cannot be null");
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onEvent(DomainEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        AuditRecordJpaEntity record = new AuditRecordJpaEntity();
        record.setId("AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        record.setIncidentId(event.incidentId() != null ? event.incidentId() : "N/A");
        record.setActorId(event.actorId() != null ? event.actorId() : "SYSTEM");
        record.setTimestamp(event.occurredAt());
        record.setAction(resolveActionName(event));
        record.setDescription(resolveDescription(event));

        resolveStateTransitions(event, record);

        auditRepository.save(record);
        log.debug("Audit record {} created for incident {}", record.getId(), record.getIncidentId());
    }

    private String resolveActionName(DomainEvent event) {
        return event.getClass().getSimpleName();
    }

    private void resolveStateTransitions(DomainEvent event, AuditRecordJpaEntity record) {
        switch (event) {
            case DomainEvent.IncidentCreated c -> {
                record.setPreviousState(null);
                record.setNewState("NEW");
            }
            case DomainEvent.IncidentClassified c -> {
                record.setPreviousState("NEW");
                record.setNewState("CLASSIFIED");
            }
            case DomainEvent.IncidentAssigned a -> {
                record.setPreviousState("CLASSIFIED");
                record.setNewState("ASSIGNED");
            }
            case DomainEvent.IncidentAcknowledged a -> {
                record.setPreviousState("ASSIGNED");
                record.setNewState("ACKNOWLEDGED");
            }
            case DomainEvent.InvestigationStarted i -> {
                record.setPreviousState("ACKNOWLEDGED");
                record.setNewState("IN_PROGRESS");
            }
            case DomainEvent.MoreInfoRequested m -> {
                record.setPreviousState("IN_PROGRESS");
                record.setNewState("PENDING_INFO");
            }
            case DomainEvent.IncidentResolved r -> {
                record.setPreviousState("IN_PROGRESS");
                record.setNewState("RESOLVED");
            }
            case DomainEvent.ResolutionVerified v -> {
                record.setPreviousState("RESOLVED");
                record.setNewState("VERIFIED");
            }
            case DomainEvent.IncidentReopened ro -> {
                record.setPreviousState("RESOLVED");
                record.setNewState("IN_PROGRESS");
            }
            case DomainEvent.IncidentClosed cl -> {
                record.setPreviousState("VERIFIED");
                record.setNewState("CLOSED");
            }
            case DomainEvent.ReassignmentRequested rr -> {
                record.setPreviousState("ASSIGNED");
                record.setNewState("CLASSIFIED");
            }
            default -> {
                // Other events (PriorityOverridden, SlaWarning, SlaViolated, IncidentEscalated)
            }
        }
    }

    private String resolveDescription(DomainEvent event) {
        return switch (event) {
            case DomainEvent.IncidentCreated c -> "Incident raised: " + c.title();
            case DomainEvent.IncidentClassified c -> "Classified under category " + c.category() + " (Service: " + c.serviceId() + ")";
            case DomainEvent.PriorityCalculated p -> "Calculated priority " + p.decision().priority() + " (Score: " + p.decision().compositeScore() + ")";
            case DomainEvent.PriorityOverridden p -> "Priority changed from " + p.previousPriority() + " to " + p.newPriority() + ". Reason: " + p.reason();
            case DomainEvent.DuplicateDetected d -> "Marked as duplicate of " + d.parentIncidentId() + " (Similarity: " + d.similarityScore() + ")";
            case DomainEvent.IncidentAssigned a -> "Assigned to technician " + a.technicianId();
            case DomainEvent.ReassignmentRequested r -> "Technician " + r.technicianId() + " requested reassignment: " + r.reason();
            case DomainEvent.IncidentAcknowledged a -> "Technician acknowledged incident";
            case DomainEvent.InvestigationStarted i -> "Technician began active investigation";
            case DomainEvent.MoreInfoRequested m -> "Information requested from reporter: " + m.message();
            case DomainEvent.IncidentResolved r -> "Resolved. Root cause: " + r.resolution().rootCause();
            case DomainEvent.ResolutionVerified v -> "Reporter verified resolution fix";
            case DomainEvent.IncidentReopened r -> "Resolution rejected. Reopened (#" + r.reopenCount() + "): " + r.rejectionReason();
            case DomainEvent.IncidentClosed c -> "Incident closed. Feedback: " + (c.feedback() != null ? c.feedback().rating() + "/5" : "None");
            case DomainEvent.SlaWarning s -> "SLA Warning: " + s.message();
            case DomainEvent.SlaViolated s -> "SLA Violated for target " + s.targetType() + " at " + s.breachedAt();
            case DomainEvent.IncidentEscalated e -> "Incident escalated from " + e.fromRole() + " to " + e.toRole() + " (" + e.toUserId() + "): " + e.escalationReason();
        };
    }
}
