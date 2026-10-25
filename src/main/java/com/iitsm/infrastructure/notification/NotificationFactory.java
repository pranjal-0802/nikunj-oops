package com.iitsm.infrastructure.notification;

import com.iitsm.domain.event.DomainEvent;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Pattern: Factory.
 * Constructs domain-specific notification payloads from sealed DomainEvent instances.
 */
@Component
public class NotificationFactory {

    public record NotificationPayload(String recipientUserId, String subject, String body) {}

    public Optional<NotificationPayload> createNotification(DomainEvent event, String targetUserId) {
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            return Optional.empty();
        }

        return switch (event) {
            case DomainEvent.IncidentCreated c -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Incident Created: " + c.incidentId(),
                    String.format("Ticket '%s' has been created with title: %s", c.incidentId(), c.title())));

            case DomainEvent.IncidentAssigned a -> Optional.of(new NotificationPayload(
                    a.technicianId(),
                    "Incident Assigned: " + a.incidentId(),
                    String.format("You have been assigned to ticket '%s'. Please review and acknowledge.", a.incidentId())));

            case DomainEvent.IncidentAcknowledged ack -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Incident Acknowledged: " + ack.incidentId(),
                    String.format("Ticket '%s' has been acknowledged by support.", ack.incidentId())));

            case DomainEvent.InvestigationStarted inv -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Investigation Underway: " + inv.incidentId(),
                    String.format("Investigation has officially started for ticket '%s'.", inv.incidentId())));

            case DomainEvent.MoreInfoRequested req -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Action Required: Information Requested for " + req.incidentId(),
                    String.format("Support requires further details: %s", req.message())));

            case DomainEvent.IncidentResolved res -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Incident Resolved: " + res.incidentId(),
                    String.format("Ticket '%s' resolved. Root cause: %s. Please verify the solution.",
                            res.incidentId(), res.resolution().rootCause())));

            case DomainEvent.ResolutionVerified ver -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Resolution Verified: " + ver.incidentId(),
                    String.format("The reporter has verified the fix for ticket '%s'.", ver.incidentId())));

            case DomainEvent.IncidentReopened re -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Incident Reopened: " + re.incidentId(),
                    String.format("Resolution rejected (Reopen #%d): %s", re.reopenCount(), re.rejectionReason())));

            case DomainEvent.IncidentClosed cls -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Incident Closed: " + cls.incidentId(),
                    String.format("Ticket '%s' has been formally closed. Thank you.", cls.incidentId())));

            case DomainEvent.ReassignmentRequested rr -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Reassignment Requested: " + rr.incidentId(),
                    String.format("Technician '%s' requested reassignment for '%s': %s",
                            rr.technicianId(), rr.incidentId(), rr.reason())));

            case DomainEvent.PriorityOverridden po -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "Priority Changed: " + po.incidentId(),
                    String.format("Priority updated to %s by admin '%s'. Reason: %s",
                            po.newPriority(), po.actorId(), po.reason())));

            case DomainEvent.SlaWarning sw -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "SLA Warning: " + sw.incidentId(),
                    sw.message()));

            case DomainEvent.SlaViolated sv -> Optional.of(new NotificationPayload(
                    targetUserId,
                    "SLA Breached: " + sv.incidentId(),
                    String.format("SLA breached on target '%s' at %s", sv.targetType(), sv.breachedAt())));

            case DomainEvent.IncidentEscalated ie -> Optional.of(new NotificationPayload(
                    ie.toUserId(),
                    "Incident Escalated: " + ie.incidentId(),
                    String.format("Escalated from %s to %s. Reason: %s", ie.fromRole(), ie.toRole(), ie.escalationReason())));

            default -> Optional.empty();
        };
    }
}

