package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;

/**
 * Pattern: Strategy.
 * Factor evaluating the business criticality of the affected IT Service (weight: 0.30).
 */
public class ServiceCriticalityFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "ServiceCriticality";
    public static final double WEIGHT = 0.30;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        double rawScore;
        String explanation;

        if (service == null) {
            rawScore = 0.30;
            explanation = "No specific IT service identified; default baseline assigned (0.30)";
        } else {
            rawScore = service.getBusinessCriticality().getScore();
            explanation = String.format("Service '%s' has criticality %s (tier: %d, score: %.2f)",
                    service.getName(), service.getBusinessCriticality().getDescription(),
                    service.getBusinessCriticality().getTier(), rawScore);
        }

        double weighted = rawScore * WEIGHT;
        return new FactorContribution(FACTOR_NAME, rawScore, WEIGHT, weighted, explanation);
    }

    @Override
    public double getWeight() {
        return WEIGHT;
    }

    @Override
    public String getFactorName() {
        return FACTOR_NAME;
    }
}
