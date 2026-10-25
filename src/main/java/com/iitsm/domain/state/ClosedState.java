package com.iitsm.domain.state;

import com.iitsm.domain.model.IncidentStatus;
import java.util.Collections;
import java.util.Set;

/**
 * Pattern: State.
 * Represents an incident permanently closed in the system.
 * Terminal state: strictly no further state transitions permitted.
 */
public class ClosedState implements IncidentState {

    private static final Set<String> ALLOWED = Collections.emptySet();

    @Override
    public IncidentStatus getStatus() {
        return IncidentStatus.CLOSED;
    }

    @Override
    public Set<String> allowedActions() {
        return ALLOWED;
    }
}

