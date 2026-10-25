package com.iitsm.domain.model;

import java.util.Set;

/**
 * OOP: Specialised Technician with team leadership and escalation privileges.
 * Inherits all Technician capabilities and represents real is-a hierarchy.
 */
public class TeamLead extends Technician {

    public TeamLead(
            String id,
            String name,
            Email email,
            String passwordHash,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {
        super(id, name, email, passwordHash, UserRole.ROLE_TEAM_LEAD, skills, experienceYears, maxCapacity, supportTeamId);
    }
}

