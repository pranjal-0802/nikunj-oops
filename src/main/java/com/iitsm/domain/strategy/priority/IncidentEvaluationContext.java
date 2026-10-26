package com.iitsm.domain.strategy.priority;

import java.time.Instant;
import java.util.Objects;

/**
 * OOP: Contextual parameters provided during multi-factor priority evaluation.
 */
public record IncidentEvaluationContext(
        int reporterRecentIncidentCount,
        int linkedDuplicateCount,
        Instant evaluationTime) {

    public IncidentEvaluationContext {
        Objects.requireNonNull(evaluationTime, "evaluationTime cannot be null");
        reporterRecentIncidentCount = Math.max(0, reporterRecentIncidentCount);
        linkedDuplicateCount = Math.max(0, linkedDuplicateCount);
    }

    public static IncidentEvaluationContext of(Instant now) {
        return new IncidentEvaluationContext(0, 0, now);
    }
}

