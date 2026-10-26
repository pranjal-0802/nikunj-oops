package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;

/**
 * Pattern: Strategy.
 * Represents an individual algorithmic factor contributing to incident priority scoring.
 */
public interface PriorityFactor {

    /**
     * Evaluates this factor's contribution to the overall priority score.
     */
    FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context);

    /**
     * Relative weight of this factor in the composite calculation (summing to 1.0).
     */
    double getWeight();

    /**
     * Human-readable factor name.
     */
    String getFactorName();
}

