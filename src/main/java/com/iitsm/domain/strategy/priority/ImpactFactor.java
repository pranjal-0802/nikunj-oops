package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;
import java.util.Locale;

/**
 * Pattern: Strategy.
 * Factor evaluating the scope of organizational impact and linked duplicate correlation (weight: 0.20).
 */
public class ImpactFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "Impact";
    public static final double WEIGHT = 0.20;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        String text = (incident.getTitle() + " " + incident.getDescription()).toLowerCase(Locale.ROOT);

        double baseScore;
        String scopeDesc;

        if (text.contains("company-wide") || text.contains("all users") || text.contains("entire organization")
                || text.contains("entire floor") || text.contains("trading desk") || text.contains("all branches")
                || text.contains("enterprise")) {
            baseScore = 1.0;
            scopeDesc = "Enterprise-wide / multi-departmental impact detected";
        } else if (text.contains("department") || text.contains("team") || text.contains("multiple users")
                || text.contains("accounting") || text.contains("branch office")) {
            baseScore = 0.60;
            scopeDesc = "Departmental or group-level impact detected";
        } else {
            baseScore = 0.20;
            scopeDesc = "Individual single-user workstation scope";
        }

        // Correlation boost: each duplicate ticket increases impact score by +0.10
        int duplicates = context != null ? context.linkedDuplicateCount() : 0;
        double duplicateBoost = Math.min(0.50, duplicates * 0.10);
        double rawScore = Math.min(1.0, baseScore + duplicateBoost);

        String explanation = String.format("%s (base: %.2f) + %d correlated duplicates (+%.2f) = %.2f",
                scopeDesc, baseScore, duplicates, duplicateBoost, rawScore);

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

