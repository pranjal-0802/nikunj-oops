package com.iitsm.web.dto;

import com.iitsm.domain.model.AvailabilityStatus;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.Technician;
import java.util.Set;

/**
 * Safe summary projection for technicians in administrative rosters.
 * Guarantees no sensitive security fields (e.g. passwordHash) are ever serialized to JSON.
 */
public record TechnicianSummaryDto(
        String id,
        String name,
        String email,
        String role,
        boolean active,
        boolean locked,
        int experienceYears,
        int maxCapacity,
        int activeIncidentCount,
        double loadRatio,
        AvailabilityStatus availabilityStatus,
        String supportTeamId,
        Set<Skill> skills,
        boolean availableForAssignment
) {
    public static TechnicianSummaryDto fromDomain(Technician t) {
        if (t == null) return null;
        return new TechnicianSummaryDto(
                t.getId(),
                t.getName(),
                t.getEmail().value(),
                t.getRole().name(),
                t.isActive(),
                t.isLocked(),
                t.getExperienceYears(),
                t.getMaxCapacity(),
                t.getActiveIncidentIds().size(),
                t.getLoadRatio(),
                t.getAvailabilityStatus(),
                t.getSupportTeamId(),
                t.getSkills(),
                t.isAvailableForAssignment()
        );
    }
}

