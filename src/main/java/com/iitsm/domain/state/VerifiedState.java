package com.iitsm.domain.state;

import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident whose resolution has been verified by the reporter.
 * Permitted action: close (attaches optional feedback, moves to CLOSED).
 */
public class VerifiedState implements IncidentState {

    private static final Set<String> ALLOWED = Set.of("close");

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.VERIFIED;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }

    @Override
    public void close(Incident incident, String actorId, Feedback feedback, Instant now) {
        if (feedback != null) {
            incident.setFeedback(feedback);
        }
        incident.transitionTo(IncidentStatus.CLOSED);
    }
}

