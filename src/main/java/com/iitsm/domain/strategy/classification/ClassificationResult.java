package com.iitsm.domain.strategy.classification;

import com.iitsm.domain.model.IncidentCategory;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * OOP: Value object representing the outcome of an automated incident classification.
 */
public record ClassificationResult(
        IncidentCategory category,
        double confidence,
        String classifierName,
        List<String> rationale,
        boolean requiresTriage) {

    public static final double TRIAGE_THRESHOLD = 0.60;

    public ClassificationResult {
        Objects.requireNonNull(category, "category cannot be null");
        Objects.requireNonNull(classifierName, "classifierName cannot be null");
        rationale = (rationale != null) ? List.copyOf(rationale) : Collections.emptyList();
        confidence = Math.max(0.0, Math.min(1.0, confidence));
    }

    public static ClassificationResult of(
            IncidentCategory category,
            double confidence,
            String classifierName,
            List<String> rationale) {
        boolean triage = confidence < TRIAGE_THRESHOLD;
        return new ClassificationResult(category, confidence, classifierName, rationale, triage);
    }
}

