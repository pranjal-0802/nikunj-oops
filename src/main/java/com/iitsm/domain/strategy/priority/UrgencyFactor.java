package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;
import java.util.Locale;

/**
 * Pattern: Strategy.
 * Factor evaluating textual urgency cues and time sensitivity (weight: 0.15).
 */
public class UrgencyFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "Urgency";
    public static final double WEIGHT = 0.15;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        String text = (incident.getTitle() + " " + incident.getDescription()).toLowerCase(Locale.ROOT);

        double rawScore;
        String explanation;

        if (text.contains("outage") || text.contains("down") || text.contains("blocked")
                || text.contains("stopped") || text.contains("emergency") || text.contains("fatal")
                || text.contains("immediate") || text.contains("critical")) {
            rawScore = 0.95;
            explanation = "Critical operational stoppage keywords detected (outage/blocked/emergency)";
        } else if (text.contains("deadline") || text.contains("urgent") || text.contains("failing")
                || text.contains("broken") || text.contains("severe") || text.contains("asap")) {
            rawScore = 0.70;
            explanation = "High urgency cues detected (deadline/urgent/failing)";
        } else if (text.contains("slow") || text.contains("degraded") || text.contains("intermittent")
                || text.contains("warning") || text.contains("latency")) {
            rawScore = 0.40;
            explanation = "Moderate operational degradation detected (slow/intermittent)";
        } else if (text.contains("minor") || text.contains("cosmetic") || text.contains("request")
                || text.contains("inquiry") || text.contains("info")) {
            rawScore = 0.15;
            explanation = "Low time sensitivity / minor informational query";
        } else {
            rawScore = 0.30;
            explanation = "Standard operational urgency default";
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

