package com.iitsm.domain.model;

import java.util.Objects;

/**
 * OOP: Administrative persona with system-wide oversight, assignment override, and operational control.
 */
public class Administrator extends User {

    private final AdminLevel adminLevel;

    public Administrator(
            String id,
            String name,
            Email email,
            String passwordHash,
            AdminLevel adminLevel) {
        super(id, name, email, passwordHash, UserRole.ROLE_ADMIN);
        this.adminLevel = Objects.requireNonNull(adminLevel, "AdminLevel cannot be null");
    }

    public AdminLevel getAdminLevel() {
        return adminLevel;
    }

    public boolean isSystemAdmin() {
        return adminLevel == AdminLevel.SYSTEM_ADMIN;
    }

    public boolean isItManager() {
        return adminLevel == AdminLevel.IT_MANAGER;
    }

    @Override
    public boolean canViewIncident(Incident incident, String owningTeamId) {
        return true; // Global administrative visibility
    }

    @Override
    public boolean canExecuteAction(Incident incident, IncidentAction action, String owningTeamId) {
        return true; // Global administrative operational authority
    }
}

