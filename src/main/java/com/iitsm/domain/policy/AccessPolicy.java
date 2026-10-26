package com.iitsm.domain.policy;

import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentAction;
import com.iitsm.domain.model.User;
import java.util.Objects;

/**
 * Pattern: Policy / Strategy.
 * Enforces polymorphic application-layer ownership and role authorization invariants.
 * Uses OOP dynamic dispatch on User subclasses rather than instanceof/switch anti-patterns.
 */
public interface AccessPolicy {

    default void enforceCanViewIncident(User actor, Incident incident) {
        enforceCanViewIncident(actor, incident, null);
    }

    default void enforceCanViewIncident(User actor, Incident incident, String owningTeamId) {
        Objects.requireNonNull(actor, "actor cannot be null");
        Objects.requireNonNull(incident, "incident cannot be null");

        if (!actor.canViewIncident(incident, owningTeamId)) {
            throw new UnauthorizedOperationException(
                    String.format("User '%s' (%s) is not authorized to view incident '%s'",
                            actor.getId(), actor.getRole(), incident.getId()));
        }
    }

    default void enforceCanExecuteAction(User actor, Incident incident, String action) {
        enforceCanExecuteAction(actor, incident, IncidentAction.fromString(action), null);
    }

    default void enforceCanExecuteAction(User actor, Incident incident, IncidentAction action) {
        enforceCanExecuteAction(actor, incident, action, null);
    }

    default void enforceCanExecuteAction(User actor, Incident incident, IncidentAction action, String owningTeamId) {
        Objects.requireNonNull(actor, "actor cannot be null");
        Objects.requireNonNull(incident, "incident cannot be null");
        Objects.requireNonNull(action, "action cannot be null");

        if (!actor.canExecuteAction(incident, action, owningTeamId)) {
            throw new UnauthorizedOperationException(
                    String.format("User '%s' (%s) is not authorized to execute '%s' on incident '%s'",
                            actor.getId(), actor.getRole(), action, incident.getId()));
        }
    }

    default void enforceAdmin(User actor) {
        Objects.requireNonNull(actor, "actor cannot be null");
        if (!(actor instanceof Administrator)) {
            throw new UnauthorizedOperationException("Operation requires Administrator privileges");
        }
    }
}
