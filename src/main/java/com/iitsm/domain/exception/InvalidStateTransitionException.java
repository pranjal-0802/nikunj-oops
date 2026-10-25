package com.iitsm.domain.exception;

import com.iitsm.domain.model.IncidentStatus;
import java.util.Set;

/**
 * OOP: Thrown when an illegal state transition is attempted on an incident.
 * Encapsulates the origin state, attempted action, and the set of currently allowed actions.
 */
public class InvalidStateTransitionException extends IitsmException {

    private final IncidentStatus fromState;
    private final String attemptedAction;
    private final Set<String> allowedActions;

    public InvalidStateTransitionException(
            IncidentStatus fromState, String attemptedAction, Set<String> allowedActions) {
        super(
                "ERR_INVALID_STATE_TRANSITION",
                String.format(
                        "Action '%s' is not allowed when incident is in state '%s'. Allowed actions: %s",
                        attemptedAction, fromState, allowedActions));
        this.fromState = fromState;
        this.attemptedAction = attemptedAction;
        this.allowedActions = Set.copyOf(allowedActions);
    }

    public IncidentStatus getFromState() {
        return fromState;
    }

    public String getAttemptedAction() {
        return attemptedAction;
    }

    public Set<String> getAllowedActions() {
        return allowedActions;
    }
}
