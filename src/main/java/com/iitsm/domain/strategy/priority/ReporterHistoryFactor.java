package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;

/**
 * Pattern: Strategy.
 * Factor evaluating reporter's historical ticket volume and chronic recurrence (weight: 0.10).
 */
public class ReporterHistoryFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "ReporterHistory";
    public static final double WEIGHT = 0.10;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        int recentCount = (context != null) ? context.reporterRecentIncidentCount() : 0;

        double rawScore;
        String explanation;

        if (recentCount >= 5) {
            rawScore = 0.90;
            explanation = String.format("Reporter has %d recent tickets (chronic recurring issues)", recentCount);
        } else if (recentCount >= 3) {
            rawScore = 0.60;
            explanation = String.format("Reporter has %d recent tickets (frequent requester)", recentCount);
        } else if (recentCount >= 1) {
            rawScore = 0.30;
            explanation = String.format("Reporter has %d prior recent ticket", recentCount);
        } else {
            rawScore = 0.10;
            explanation = "First or isolated ticket from this reporter";
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

