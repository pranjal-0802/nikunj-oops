package com.iitsm.domain.event;

import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import java.time.Instant;

/**
 * Pattern: Domain Event / Observer.
 * Sealed interface defining all strongly-typed domain events in the I-ITSM event-driven spine.
 */
public sealed interface DomainEvent {

    String eventId();

    Instant occurredAt();

    String incidentId();

    String actorId();

    // 1. Incident Created
    record IncidentCreated(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String title,
            String description) implements DomainEvent {}

    // 2. Incident Classified
    record IncidentClassified(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            IncidentCategory category,
            String serviceId) implements DomainEvent {}

    // 3. Priority Calculated
    record PriorityCalculated(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            PriorityDecision decision) implements DomainEvent {}

    // 4. Priority Overridden
    record PriorityOverridden(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            Priority previousPriority,
            Priority newPriority,
            String reason) implements DomainEvent {}

    // 5. Duplicate Detected
    record DuplicateDetected(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String parentIncidentId,
            double similarityScore) implements DomainEvent {}

    // 6. Incident Assigned
    record IncidentAssigned(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String technicianId) implements DomainEvent {}

    // 7. Reassignment Requested
    record ReassignmentRequested(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String technicianId,
            String reason) implements DomainEvent {}

    // 8. Incident Acknowledged
    record IncidentAcknowledged(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId) implements DomainEvent {}

    // 9. Investigation Started
    record InvestigationStarted(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId) implements DomainEvent {}

    // 10. More Info Requested
    record MoreInfoRequested(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String message) implements DomainEvent {}

    // 11. Incident Resolved
    record IncidentResolved(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            Resolution resolution) implements DomainEvent {}

    // 12. Resolution Verified
    record ResolutionVerified(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId) implements DomainEvent {}

    // 13. Incident Reopened
    record IncidentReopened(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String rejectionReason,
            int reopenCount) implements DomainEvent {}

    // 14. Incident Closed
    record IncidentClosed(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            Feedback feedback) implements DomainEvent {}

    // 15. SLA Warning
    record SlaWarning(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String message) implements DomainEvent {}

    // 16. SLA Violated
    record SlaViolated(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String targetType,
            Instant breachedAt) implements DomainEvent {}

    // 17. Incident Escalated
    record IncidentEscalated(
            String eventId,
            Instant occurredAt,
            String incidentId,
            String actorId,
            String fromRole,
            String toRole,
            String toUserId,
            String escalationReason) implements DomainEvent {}
}

