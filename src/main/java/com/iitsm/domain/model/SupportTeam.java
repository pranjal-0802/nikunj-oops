package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OOP: Aggregation entity representing a functional support team.
 * Models aggregation (SupportTeam o-- Technician) where technicians can exist independently.
 */
public class SupportTeam {

    private final String id;
    private String name;
    private final Set<IncidentCategory> handledCategories;
    private String teamLeadId;
    private final Set<String> memberIds;

    public SupportTeam(
            String id,
            String name,
            Set<IncidentCategory> handledCategories,
            String teamLeadId) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Team ID cannot be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Team name cannot be blank");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.handledCategories = (handledCategories != null) ? new HashSet<>(handledCategories) : new HashSet<>();
        this.teamLeadId = teamLeadId;
        this.memberIds = new HashSet<>();
        if (teamLeadId != null) {
            this.memberIds.add(teamLeadId);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<IncidentCategory> getHandledCategories() {
        return Collections.unmodifiableSet(handledCategories);
    }

    public String getTeamLeadId() {
        return teamLeadId;
    }

    public Set<String> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public void updateName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new ValidationException("name", "Team name cannot be blank");
        }
        this.name = newName.trim();
    }

    public void setTeamLead(String newTeamLeadId) {
        this.teamLeadId = Objects.requireNonNull(newTeamLeadId, "Team lead ID cannot be null");
        this.memberIds.add(newTeamLeadId);
    }

    public void addMember(String technicianId) {
        Objects.requireNonNull(technicianId, "Technician ID cannot be null");
        this.memberIds.add(technicianId);
    }

    public void removeMember(String technicianId) {
        if (technicianId != null && !technicianId.equals(this.teamLeadId)) {
            this.memberIds.remove(technicianId);
        }
    }

    public boolean handlesCategory(IncidentCategory category) {
        return handledCategories.contains(category);
    }
}

