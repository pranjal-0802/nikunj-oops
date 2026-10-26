package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.User;

/**
 * Pattern: Strategy.
 * Factor evaluating the reporting employee's organizational hierarchy level (weight: 0.15).
 */
public class ReporterDesignationFactor implements PriorityFactor {

    public static final String FACTOR_NAME = "ReporterDesignation";
    public static final double WEIGHT = 0.15;

    @Override
    public FactorContribution evaluate(Incident incident, User reporter, ItService service, IncidentEvaluationContext context) {
        double rawScore;
        String explanation;

        if (reporter instanceof Employee emp && emp.getDesignation() != null) {
            rawScore = emp.getDesignation().getPriorityWeight();
            explanation = String.format("Reporter designation '%s' contributes priority weight %.2f",
                    emp.getDesignation().getTitle(), rawScore);
        } else {
            rawScore = 0.30;
            explanation = "Non-employee or unassigned designation baseline applied (0.30)";
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

