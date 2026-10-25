package com.iitsm.domain.state;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident assigned to a technician queue.
 * Permitted actions: acknowledge (moves to ACKNOWLEDGED), flagWrongDomain (returns to CLASSIFIED).
 */
public class AssignedState implements IncidentState {

    private static final Set<String> ALLOWED = Set.of("acknowledge", "flagWrongDomain");

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.ASSIGNED;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }

    @Override
    public void acknowledge(Incident incident, String technicianId, Instant now) {
        if (incident.getSlaTracker() != null) {
            incident.getSlaTracker().recordResponse(now);
        }
        incident.transitionTo(IncidentStatus.ACKNOWLEDGED);
    }

    @Override
    public void flagWrongDomain(Incident incident, String technicianId, String reason, Instant now) {
        incident.clearAssignee();
        incident.transitionTo(IncidentStatus.CLASSIFIED);
    }
}
