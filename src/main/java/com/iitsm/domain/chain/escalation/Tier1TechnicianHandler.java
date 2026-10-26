package com.iitsm.domain.chain.escalation;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.SlaStatus;
import java.util.Optional;

/**
 * Pattern: Chain of Responsibility (Tier 1 Handler).
 * Handles incidents entering AT_RISK status (75% elapsed), nudging the assigned technician.
 */
public class Tier1TechnicianHandler extends AbstractEscalationHandler {

    @Override
    public Optional<EscalationAction> handle(Incident incident, EscalationContext context) {
        if (incident.getAssigneeId() != null && incident.getSlaTracker() != null) {
            SlaStatus status = incident.getSlaTracker().getStatus();

            if (status == SlaStatus.AT_RISK) {
                return Optional.of(new EscalationAction(
                        1,
                        "TECHNICIAN_REMINDER",
                        incident.getAssigneeId(),
                        "TECHNICIAN",
                        "Incident " + incident.getId() + " reached 75% SLA target window; urgent resolution needed"
                ));
            }
        }
        return forward(incident, context);
    }
}

