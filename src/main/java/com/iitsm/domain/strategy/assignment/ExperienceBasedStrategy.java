package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.util.Comparator;
import java.util.List;

/**
 * Pattern: Strategy.
 * Routes incidents to the technician with the highest years of domain experience.
 */
public class ExperienceBasedStrategy implements AssignmentStrategy {

    public static final String STRATEGY_NAME = "EXPERIENCE_BASED";

    @Override
    public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No available technicians found for experience-based assignment");
        }

        // Pick technician with highest experience, breaking ties with lower load
        Technician selected = candidates.stream()
                .max(Comparator.comparingInt(Technician::getExperienceYears)
                        .thenComparing(Comparator.comparingDouble(Technician::getLoadRatio).reversed()))
                .orElse(candidates.get(0));

        double score = Math.min(1.0, selected.getExperienceYears() / 15.0);
        String rationale = String.format("Selected for highest experience: %d years domain expertise (load: %.2f)",
                selected.getExperienceYears(), selected.getLoadRatio());

        return new AssignmentRecommendation(selected, score, STRATEGY_NAME, rationale);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public String getDescription() {
        return "Assigns tickets to the most experienced technicians to accelerate root cause analysis on difficult issues.";
    }
}

