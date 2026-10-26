package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * OOP: Candidate technician filter applying operational constraints before strategy scoring:
 * capacity check, online availability, exclusion of rejecting technician, and owning-team prioritization.
 */
public class CandidateFilter {

    public CandidateFilter() {
    }

    /**
     * Filters available candidates enforcing capacity, availability, and rejector exclusion.
     * Prioritizes technicians belonging to preferredTeamId, falling back to cross-team if none available.
     */
    public List<Technician> filterCandidates(
            List<Technician> allTechnicians,
            String preferredTeamId,
            Set<String> excludedTechnicianIds) {
        if (allTechnicians == null || allTechnicians.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> exclusions = (excludedTechnicianIds != null) ? excludedTechnicianIds : Collections.emptySet();

        List<Technician> teamCandidates = new ArrayList<>();
        List<Technician> crossTeamCandidates = new ArrayList<>();

        for (Technician tech : allTechnicians) {
            // Must have capacity and not be excluded
            if (!tech.hasCapacity() || exclusions.contains(tech.getId())) {
                continue;
            }

            if (preferredTeamId != null && preferredTeamId.equalsIgnoreCase(tech.getSupportTeamId())) {
                teamCandidates.add(tech);
            } else {
                crossTeamCandidates.add(tech);
            }
        }

        // Return same-team candidates if available; otherwise cross-team fallback
        if (!teamCandidates.isEmpty()) {
            return Collections.unmodifiableList(teamCandidates);
        }
        return Collections.unmodifiableList(crossTeamCandidates);
    }
}

