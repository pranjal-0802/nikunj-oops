package com.iitsm.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * OOP: Value Object encapsulating a calculated priority with full factor transparency and audit trail.
 */
public record PriorityDecision(
        Priority priority,
        double compositeScore,
        List<FactorContribution> factorContributions,
        List<String> rulesApplied,
        Instant calculatedAt,
        String overriddenBy,
        String overrideReason) {

    public PriorityDecision {
        if (priority == null) {
            throw new IllegalArgumentException("priority must not be null");
        }
        if (calculatedAt == null) {
            throw new IllegalArgumentException("calculatedAt must not be null");
        }
        factorContributions = (factorContributions != null)
                ? List.copyOf(factorContributions)
                : Collections.emptyList();
        rulesApplied = (rulesApplied != null)
                ? List.copyOf(rulesApplied)
                : Collections.emptyList();
    }

    public boolean isOverridden() {
        return overriddenBy != null && !overriddenBy.trim().isEmpty();
    }

    public PriorityDecision withOverride(Priority newPriority, String adminId, String reason, Instant now) {
        return new PriorityDecision(
                newPriority,
                this.compositeScore,
                this.factorContributions,
                this.rulesApplied,
                now,
                adminId,
                reason);
    }
}
