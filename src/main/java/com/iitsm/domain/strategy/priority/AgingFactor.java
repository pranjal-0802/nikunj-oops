package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;
import java.time.Duration;
import java.time.Instant;

/**
 * Pattern: Strategy.
 * Factor evaluating ticket aging and duration unaddressed (weight: 0.10).
 */
public class AgingFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "Aging";
    public static final double WEIGHT = 0.10;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        Instant evaluationTime = (context != null && context.evaluationTime() != null)
                ? context.evaluationTime()
                : Instant.now();

        Duration age = Duration.between(incident.getCreatedAt(), evaluationTime);
        if (age.isNegative()) {
            age = Duration.ZERO;
        }

        double rawScore;
        String explanation;

        if (age.toDays() >= 7) {
            rawScore = 1.0;
            explanation = String.format("Ticket open for %d days (critical aging >= 7d)", age.toDays());
        } else if (age.toDays() >= 3) {
            rawScore = 0.75;
            explanation = String.format("Ticket open for %d days (significant aging >= 3d)", age.toDays());
        } else if (age.toDays() >= 1) {
            rawScore = 0.50;
            explanation = String.format("Ticket open for %d hours (moderate aging >= 24h)", age.toHours());
        } else if (age.toHours() >= 4) {
            rawScore = 0.25;
            explanation = String.format("Ticket open for %d hours (initial aging >= 4h)", age.toHours());
        } else {
            rawScore = 0.10;
            explanation = "Newly created ticket (< 4 hours old)";
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

