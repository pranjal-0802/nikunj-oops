package com.iitsm.domain.model;

import com.iitsm.domain.exception.TechnicianOverloadedException;
import com.iitsm.domain.exception.ValidationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OOP: Concrete user subclass representing an IT support technician.
 * Encapsulates skills, capacity constraints, availability, and active assignments.
 */
public class Technician extends User {

    private final Set<Skill> skills;
    private final int experienceYears;
    private final int maxCapacity;
    private AvailabilityStatus availabilityStatus;
    private String supportTeamId;
    // Set of active incident IDs assigned to this technician
    private final Set<String> activeIncidentIds;

    public Technician(
            String id,
            String name,
            Email email,
            String passwordHash,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {
        this(id, name, email, passwordHash, UserRole.ROLE_TECHNICIAN, skills, experienceYears, maxCapacity, supportTeamId);
    }

    protected Technician(
            String id,
            String name,
            Email email,
            String passwordHash,
            UserRole role,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {
        super(id, name, email, passwordHash, role);
        if (experienceYears < 0) {
            throw new ValidationException("experienceYears", "Experience years cannot be negative");
        }
        if (maxCapacity <= 0) {
            throw new ValidationException("maxCapacity", "Maximum capacity must be strictly positive");
        }
        this.skills = (skills != null) ? new HashSet<>(skills) : new HashSet<>();
        this.experienceYears = experienceYears;
        this.maxCapacity = maxCapacity;
        this.availabilityStatus = AvailabilityStatus.ONLINE;
        this.supportTeamId = supportTeamId;
        this.activeIncidentIds = new HashSet<>();
    }

    public Set<Skill> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public String getSupportTeamId() {
        return supportTeamId;
    }

    public Set<String> getActiveIncidentIds() {
        return Collections.unmodifiableSet(activeIncidentIds);
    }

    public int getActiveIncidentCount() {
        return activeIncidentIds.size();
    }

    public double getLoadRatio() {
        return (double) activeIncidentIds.size() / (double) maxCapacity;
    }

    public boolean hasCapacity() {
        return activeIncidentIds.size() < maxCapacity;
    }

    public boolean isAvailableForAssignment() {
        return availabilityStatus != AvailabilityStatus.OFFLINE && hasCapacity();
    }

    public void assignIncident(String incidentId) {
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        if (activeIncidentIds.size() >= maxCapacity) {
            throw new TechnicianOverloadedException(getId(), activeIncidentIds.size(), maxCapacity);
        }
        activeIncidentIds.add(incidentId);
        if (activeIncidentIds.size() >= maxCapacity) {
            this.availabilityStatus = AvailabilityStatus.BUSY;
        }
    }

    public void releaseIncident(String incidentId) {
        Objects.requireNonNull(incidentId, "incidentId cannot be null");
        activeIncidentIds.remove(incidentId);
        if (activeIncidentIds.size() < maxCapacity && this.availabilityStatus == AvailabilityStatus.BUSY) {
            this.availabilityStatus = AvailabilityStatus.ONLINE;
        }
    }

    public void updateAvailability(AvailabilityStatus newStatus) {
        this.availabilityStatus = Objects.requireNonNull(newStatus, "availabilityStatus cannot be null");
    }

    public void assignTeam(String supportTeamId) {
        this.supportTeamId = supportTeamId;
    }

    public void addSkill(Skill skill) {
        Objects.requireNonNull(skill, "skill cannot be null");
        this.skills.add(skill);
    }

    public boolean hasSkill(String skillName) {
        if (skillName == null) {
            return false;
        }
        return skills.stream().anyMatch(s -> s.name().equalsIgnoreCase(skillName.trim()));
    }

    public boolean hasCategorySkill(IncidentCategory category) {
        if (category == null) {
            return false;
        }
        return skills.stream().anyMatch(s -> s.category() == category);
    }

    @Override
    public boolean canViewIncident(Incident incident, String owningTeamId) {
        if (incident == null) return false;
        if (getId().equals(incident.getAssigneeId())) {
            return true;
        }
        // Technicians can view unassigned incidents in their owning team queue
        if (incident.getAssigneeId() == null && supportTeamId != null && supportTeamId.equals(owningTeamId)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canExecuteAction(Incident incident, IncidentAction action, String owningTeamId) {
        if (incident == null || action == null) return false;
        if (action == IncidentAction.VIEW) {
            return canViewIncident(incident, owningTeamId);
        }
        // Technician must be assigned to the ticket to execute technician actions
        if (!getId().equals(incident.getAssigneeId())) {
            return false;
        }
        return action == IncidentAction.ACKNOWLEDGE
                || action == IncidentAction.START_INVESTIGATION
                || action == IncidentAction.REQUEST_INFO
                || action == IncidentAction.RESOLVE
                || action == IncidentAction.FLAG_WRONG_DOMAIN
                || action == IncidentAction.REASSIGN;
    }
}

