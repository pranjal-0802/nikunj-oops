package com.iitsm.domain.state;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import java.time.Instant;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident resolved by a technician and awaiting employee verification.
 * Permitted actions:
 * - verify (moves to VERIFIED)
 * - reject (reopens incident, increments reopenCount, returns to IN_PROGRESS)
 */
public class ResolvedState implements IncidentState {

    private static final Set<String> ALLOWED = Set.of("verify", "reject");

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.RESOLVED;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }

    @Override
    public void verify(Incident incident, String employeeId, Instant now) {
        incident.transitionTo(IncidentStatus.VERIFIED);
    }

    @Override
    public void reject(Incident incident, String employeeId, String reason, Instant now) {
        incident.incrementReopenCount();
        incident.transitionTo(IncidentStatus.IN_PROGRESS);
    }
}
