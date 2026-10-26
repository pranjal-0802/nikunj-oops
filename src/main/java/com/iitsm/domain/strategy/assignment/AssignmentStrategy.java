package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.util.List;

/**
 * Pattern: Strategy.
 * Interface defining swappable technician assignment strategies.
 */
public interface AssignmentStrategy {

    /**
     * Recommends the optimal technician for the given incident among eligible candidates.
     */
    AssignmentRecommendation recommend(Incident incident, List<Technician> candidates);

    /**
     * Unique key identifying this assignment strategy.
     */
    String getStrategyName();

    /**
     * Human-readable description of how this strategy routes tickets.
     */
    String getDescription();
}

