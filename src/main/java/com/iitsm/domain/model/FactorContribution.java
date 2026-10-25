package com.iitsm.domain.model;

/**
 * OOP: Value Object encapsulating one component's contribution to priority calculation.
 * Ensures total algorithmic explainability for administrators and auditing.
 */
public record FactorContribution(
        String factorName,
        double rawScore,
        double weight,
        double weightedScore,
        String explanation) {

    public FactorContribution {
        if (factorName == null || factorName.trim().isEmpty()) {
            throw new IllegalArgumentException("factorName must not be blank");
        }
    }
}
