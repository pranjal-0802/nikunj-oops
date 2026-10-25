package com.iitsm.domain.state;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Resolution;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident actively under technician investigation.
 * Permitted actions: requestInfo (pauses SLA, moves to PENDING_INFO), resolve (moves to RESOLVED).
 */
public class InProgressState implements IncidentState {

    private static final Set<String> ALLOWED = Set.of("requestInfo", "resolve");

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.IN_PROGRESS;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }

    @Override
    public void requestInfo(Incident incident, String technicianId, String message, Instant now) {
        if (incident.getSlaTracker() != null) {
            incident.getSlaTracker().pauseClock(now);
        }
        incident.transitionTo(IncidentStatus.PENDING_INFO);
    }

    @Override
    public void resolve(Incident incident, Resolution resolution, Instant now) {
        Objects.requireNonNull(resolution, "resolution report cannot be null");
        incident.setResolution(resolution);
        if (incident.getSlaTracker() != null) {
            incident.getSlaTracker().recordResolution(now);
        }
        incident.transitionTo(IncidentStatus.RESOLVED);
    }
}
