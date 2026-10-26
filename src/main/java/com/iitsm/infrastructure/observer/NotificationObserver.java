package com.iitsm.infrastructure.observer;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventListener;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.infrastructure.notification.NotificationFactory;
import com.iitsm.infrastructure.notification.NotificationRouter;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Pattern: Observer.
 * Asynchronous / decoupled domain event listener translating events into persona notifications.
 * Delivery failures are safely caught and isolated so business operations never roll back.
 */
@Component
public class NotificationObserver implements DomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationObserver.class);

    private final NotificationFactory notificationFactory;
    private final NotificationRouter notificationRouter;
    private final IncidentRepository incidentRepository;

    public NotificationObserver(
            NotificationFactory notificationFactory,
            NotificationRouter notificationRouter,
            IncidentRepository incidentRepository) {
        this.notificationFactory = Objects.requireNonNull(notificationFactory, "notificationFactory cannot be null");
        this.notificationRouter = Objects.requireNonNull(notificationRouter, "notificationRouter cannot be null");
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository cannot be null");
    }

    @Override
    public void onEvent(DomainEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        try {
            String recipientId = resolveRecipient(event);
            if (recipientId != null) {
                notificationFactory.createNotification(event, recipientId)
                        .ifPresent(notificationRouter::route);
            }
        } catch (Exception ex) {
            log.error("Failed to process event {} in NotificationObserver: {}", event.eventId(), ex.getMessage(), ex);
        }
    }

    private String resolveRecipient(DomainEvent event) {
        return switch (event) {
            case DomainEvent.IncidentCreated c -> c.actorId(); // Send confirmation to reporter
            case DomainEvent.IncidentAssigned a -> a.technicianId();
            case DomainEvent.IncidentAcknowledged ack -> resolveReporterId(ack.incidentId());
            case DomainEvent.InvestigationStarted inv -> resolveReporterId(inv.incidentId());
            case DomainEvent.MoreInfoRequested req -> resolveReporterId(req.incidentId());
            case DomainEvent.IncidentResolved res -> resolveReporterId(res.incidentId());
            case DomainEvent.ResolutionVerified ver -> resolveAssigneeId(ver.incidentId());
            case DomainEvent.IncidentReopened re -> resolveAssigneeId(re.incidentId());
            case DomainEvent.IncidentClosed cls -> resolveReporterId(cls.incidentId());
            case DomainEvent.ReassignmentRequested rr -> resolveAdminId(); // Admin triage
            case DomainEvent.PriorityOverridden po -> resolveAssigneeId(po.incidentId());
            case DomainEvent.SlaWarning sw -> resolveAssigneeId(sw.incidentId());
            case DomainEvent.SlaViolated sv -> resolveAssigneeId(sv.incidentId());
            case DomainEvent.IncidentEscalated ie -> ie.toUserId();
            default -> null;
        };
    }

    private String resolveReporterId(String incidentId) {
        if (incidentId == null) return null;
        return incidentRepository.findById(incidentId)
                .map(Incident::getReporterId)
                .orElse(null);
    }

    private String resolveAssigneeId(String incidentId) {
        if (incidentId == null) return null;
        return incidentRepository.findById(incidentId)
                .map(Incident::getAssigneeId)
                .orElse(null);
    }

    private String resolveAdminId() {
        return "ADMIN-SYS-01";
    }
}

