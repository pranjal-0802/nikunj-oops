package com.iitsm.domain.chain.escalation;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.SlaStatus;
import java.util.Optional;

/**
 * Pattern: Chain of Responsibility (Tier 2 Handler).
 * Handles incidents approaching warning threshold (90% elapsed) or failing response deadline,
 * alerting the owning support team lead.
 */
public class Tier2TeamLeadHandler extends AbstractEscalationHandler {

    @Override
    public Optional<EscalationAction> handle(Incident incident, EscalationContext context) {
        if (context.supportTeamLeadId() != null && incident.getSlaTracker() != null) {
            SlaStatus status = incident.getSlaTracker().getStatus();

            // Reached WARNING status (>= 90%)
            if (status == SlaStatus.WARNING) {
                return Optional.of(new EscalationAction(
                        2,
                        "TEAM_LEAD_INTERVENTION",
                        context.supportTeamLeadId(),
                        "TEAM_LEAD",
                        "Incident " + incident.getId() + " reached 90% SLA warning threshold; team lead action required"
                ));
            }

            // Missed initial response target without acknowledgment
            if (incident.getSlaTracker().getRespondedAt() == null
                    && context.currentInstant().isAfter(incident.getSlaTracker().getResponseDueAt())) {
                return Optional.of(new EscalationAction(
                        2,
                        "TEAM_LEAD_INTERVENTION",
                        context.supportTeamLeadId(),
                        "TEAM_LEAD",
                        "Incident " + incident.getId() + " missed initial response SLA; technician has not acknowledged"
                ));
            }
        }
        return forward(incident, context);
    }
}

