package com.iitsm.domain.chain.escalation;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaStatus;
import java.util.Optional;

/**
 * Pattern: Chain of Responsibility (Tier 3 Handler).
 * Handles general SLA breaches on P1/P2 tickets or incidents stuck unassigned in triage.
 */
public class Tier3SystemAdminHandler extends AbstractEscalationHandler {

    @Override
    public Optional<EscalationAction> handle(Incident incident, EscalationContext context) {
        if (incident.getSlaTracker() != null) {
            SlaStatus status = incident.getSlaTracker().getStatus();
            Priority priority = incident.getPriority();
            boolean isHighPriority = (priority == Priority.P1 || priority == Priority.P2);

            if (status == SlaStatus.BREACHED && (isHighPriority || incident.getAssigneeId() == null)) {
                return Optional.of(new EscalationAction(
                        3,
                        "SYSTEM_ADMIN_ALERT",
                        context.systemAdminId(),
                        "ADMINISTRATOR",
                        "SLA breached on " + priority + " ticket " + incident.getId() + "; administrator intervention required"
                ));
            }
        }

        // Triage stagnation: ticket in CLASSIFIED state without assignee
        if (incident.getStatus() == IncidentStatus.CLASSIFIED && incident.getAssigneeId() == null) {
            if (incident.getSlaTracker() != null && incident.getSlaTracker().getStatus() != SlaStatus.ON_TRACK) {
                return Optional.of(new EscalationAction(
                        3,
                        "SYSTEM_ADMIN_ALERT",
                        context.systemAdminId(),
                        "ADMINISTRATOR",
                        "Unassigned ticket " + incident.getId() + " stagnant in triage queue"
                ));
            }
        }

        return forward(incident, context);
    }
}

