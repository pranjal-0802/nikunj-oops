package com.iitsm.domain.state;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident acknowledged by the assigned technician.
 * Permitted actions: startInvestigation (moves to IN_PROGRESS), flagWrongDomain (returns to CLASSIFIED).
 */
public class AcknowledgedState implements IncidentState {

    private static final Set<String> ALLOWED = Set.of("startInvestigation", "flagWrongDomain");

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.ACKNOWLEDGED;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }

    @Override
    public void startInvestigation(Incident incident, String technicianId, Instant now) {
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
    }

    @Override
    public void flagWrongDomain(Incident incident, String technicianId, String reason, Instant now) {
        incident.clearAssignee();
        incident.transitionTo(IncidentStatus.CLASSIFIED);
    }
}

