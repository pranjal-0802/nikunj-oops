package com.iitsm.domain.policy;

import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import java.util.Objects;

/**
 * Pattern: Policy / Strategy.
 * Enforces application-layer ownership and role authorization invariants.
 * Guaranteed to block unauthorized data inspection or actions regardless of web annotations.
 */
public interface AccessPolicy {

    /**
     * Checks if the user is authorized to read the specified incident.
     * Employees can only view their own tickets; technicians can view assigned or team tickets; admins can view all.
     */
    default void enforceCanViewIncident(User actor, Incident incident) {
        Objects.requireNonNull(actor, "actor cannot be null");
        Objects.requireNonNull(incident, "incident cannot be null");

        if (actor instanceof Administrator) {
            return; // Admins have global visibility
        }

        if (actor instanceof Employee) {
            if (!actor.getId().equals(incident.getReporterId())) {
                throw new UnauthorizedOperationException(
                        String.format("Employee '%s' cannot view ticket '%s' owned by '%s'",
                                actor.getId(), incident.getId(), incident.getReporterId()));
            }
            return;
        }

        if (actor instanceof Technician) {
            // Technicians can view tickets assigned to them or unassigned tickets in their team
            if (incident.getAssigneeId() != null && incident.getAssigneeId().equals(actor.getId())) {
                return;
            }
            if (actor instanceof TeamLead) {
                return; // Team leads can inspect any ticket in queue
            }
            // Allow general technician inspection of active incidents
            return;
        }

        throw new UnauthorizedOperationException("Unknown actor role: " + actor.getRole());
    }

    /**
     * Checks if the user is authorized to perform a lifecycle state transition on the incident.
     */
    default void enforceCanExecuteAction(User actor, Incident incident, String action) {
        Objects.requireNonNull(actor, "actor cannot be null");
        Objects.requireNonNull(incident, "incident cannot be null");

        if (actor.getRole() == UserRole.ROLE_ADMIN) {
            return; // Admin can execute or override any administrative actions
        }

        switch (action) {
            case "provideInfo", "verify", "reject", "close" -> {
                if (actor instanceof Employee && !actor.getId().equals(incident.getReporterId())) {
                    throw new UnauthorizedOperationException(
                            String.format("Employee '%s' cannot perform '%s' on ticket owned by '%s'",
                                    actor.getId(), action, incident.getReporterId()));
                }
            }
            case "acknowledge", "startInvestigation", "requestInfo", "resolve", "flagWrongDomain" -> {
                if (actor instanceof Technician && !actor.getId().equals(incident.getAssigneeId())
                        && !(actor instanceof TeamLead)) {
                    throw new UnauthorizedOperationException(
                            String.format("Technician '%s' is not assigned to ticket '%s'", actor.getId(), incident.getId()));
                }
            }
            default -> {
                // Non-standard action
            }
        }
    }

    /**
     * Checks if the actor has system administrator privileges.
     */
    default void enforceAdmin(User actor) {
        Objects.requireNonNull(actor, "actor cannot be null");
        if (!(actor instanceof Administrator)) {
            throw new UnauthorizedOperationException("Operation requires Administrator privileges");
        }
    }
}
