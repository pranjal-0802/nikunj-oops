package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.util.Comparator;
import java.util.List;

/**
 * Pattern: Strategy.
 * Routes incidents to the technician with the lowest active load ratio to balance team workload.
 */
public class LoadBasedStrategy implements AssignmentStrategy {

    public static final String STRATEGY_NAME = "LOAD_BASED";

    @Override
    public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No available technicians found for load-based assignment");
        }

        // Pick technician with lowest load ratio, breaking ties with higher experience
        Technician selected = candidates.stream()
                .min(Comparator.comparingDouble(Technician::getLoadRatio)
                        .thenComparing(Comparator.comparingInt(Technician::getExperienceYears).reversed()))
                .orElse(candidates.get(0));

        double score = 1.0 - selected.getLoadRatio();
        String rationale = String.format("Selected for lowest workload: %d/%d active tickets (load ratio: %.2f)",
                selected.getActiveIncidentCount(), selected.getMaxCapacity(), selected.getLoadRatio());

        return new AssignmentRecommendation(selected, score, STRATEGY_NAME, rationale);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public String getDescription() {
        return "Balances workload across support teams by routing to the technician with the lowest load ratio.";
    }
}

