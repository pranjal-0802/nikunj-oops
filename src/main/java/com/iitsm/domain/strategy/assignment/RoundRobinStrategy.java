package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pattern: Strategy.
 * Deterministic round-robin cyclic rotation among eligible candidates.
 */
public class RoundRobinStrategy implements AssignmentStrategy {

    public static final String STRATEGY_NAME = "ROUND_ROBIN";

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new TechnicianUnavailableException("No available technicians found for round-robin assignment");
        }

        int index = Math.abs(counter.getAndIncrement() % candidates.size());
        Technician selected = candidates.get(index);

        double score = 0.80;
        String rationale = String.format("Assigned via cyclic round-robin distribution (queue slot #%d of %d)",
                index + 1, candidates.size());

        return new AssignmentRecommendation(selected, score, STRATEGY_NAME, rationale);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }

    @Override
    public String getDescription() {
        return "Distributes tickets in equal, cyclic rotation among all available technicians.";
    }
}

