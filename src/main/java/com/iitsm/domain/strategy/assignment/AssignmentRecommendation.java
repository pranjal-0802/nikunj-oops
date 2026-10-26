package com.iitsm.domain.strategy.assignment;

import com.iitsm.domain.model.Technician;
import java.util.Objects;

/**
 * OOP: Value object encapsulating the result of an automated technician assignment recommendation.
 */
public record AssignmentRecommendation(
        Technician technician,
        double score,
        String strategyName,
        String rationale) {

    public AssignmentRecommendation {
        Objects.requireNonNull(technician, "technician cannot be null");
        Objects.requireNonNull(strategyName, "strategyName cannot be null");
        score = Math.max(0.0, Math.min(1.0, score));
    }
}

