package com.iitsm.domain.strategy.priority;

import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Pattern: Composite / Strategy.
 * Calculates composite incident priority using a 6-factor weighted evaluation model
 * and applies policy override rules (Security Breach, Critical Service Outage, Duplicate Avalanche).
 */
public class CompositePriorityCalculator {

    public static final double P1_THRESHOLD = 0.75;
    public static final double P2_THRESHOLD = 0.55;
    public static final double P3_THRESHOLD = 0.35;

    private final List<PriorityFactor> factors;

    public CompositePriorityCalculator() {
        this(List.of(
                new ServiceCriticalityFactor(),
                new ImpactFactor(),
                new UrgencyFactor(),
                new ReporterDesignationFactor(),
                new AgingFactor(),
                new ReporterHistoryFactor()
        ));
    }

    public CompositePriorityCalculator(List<PriorityFactor> factors) {
        this.factors = List.copyOf(Objects.requireNonNull(factors, "factors cannot be null"));
    }

    /**
     * Calculates priority decision with full factor breakdown and rule explanations.
     */
    public PriorityDecision calculate(
            Incident incident,
            User reporter,
            ItService service,
            IncidentEvaluationContext context,
            Instant calculationTime) {
        Objects.requireNonNull(incident, "incident cannot be null");
        Objects.requireNonNull(calculationTime, "calculationTime cannot be null");

        List<FactorContribution> contributions = new ArrayList<>(factors.size());
        double compositeScore = 0.0;

        for (PriorityFactor factor : factors) {
            FactorContribution contribution = factor.evaluate(incident, reporter, service, context);
            contributions.add(contribution);
            compositeScore += contribution.weightedScore();
        }

        // Clamp composite score between 0.0 and 1.0, rounded to 4 decimal places
        compositeScore = Math.round(Math.max(0.0, Math.min(1.0, compositeScore)) * 10000.0) / 10000.0;

        // 1. Threshold mapping
        Priority basePriority;
        if (compositeScore >= P1_THRESHOLD) {
            basePriority = Priority.P1;
        } else if (compositeScore >= P2_THRESHOLD) {
            basePriority = Priority.P2;
        } else if (compositeScore >= P3_THRESHOLD) {
            basePriority = Priority.P3;
        } else {
            basePriority = Priority.P4;
        }

        // 2. Evaluate Policy Override Rules Chain
        Priority finalPriority = basePriority;
        List<String> appliedRules = new ArrayList<>();
        String fullText = (incident.getTitle() + " " + incident.getDescription()).toLowerCase(Locale.ROOT);

        // Rule 1: Security Breach -> At least P2
        if (incident.getCategory() == IncidentCategory.SECURITY) {
            if (fullText.contains("breach") || fullText.contains("compromised") || fullText.contains("ransomware")
                    || fullText.contains("unauthorized") || fullText.contains("exfiltration") || fullText.contains("malware")) {
                if (finalPriority.getRank() > Priority.P2.getRank()) {
                    finalPriority = Priority.P2;
                    appliedRules.add("RULE_SECURITY_BREACH: Elevated to at least P2 due to confirmed security breach indicators");
                }
            }
        }

        // Rule 2: Critical Service (Tier 4) + Outage -> Forced P1
        if (service != null && service.getBusinessCriticality() == BusinessCriticality.TIER_4_MISSION_CRITICAL) {
            if (fullText.contains("down") || fullText.contains("outage") || fullText.contains("unavailable")
                    || fullText.contains("crash") || fullText.contains("stopped") || fullText.contains("deadlock")) {
                if (finalPriority != Priority.P1) {
                    finalPriority = Priority.P1;
                    appliedRules.add("RULE_CRITICAL_SERVICE_OUTAGE: Forced P1 due to Critical Tier-4 Mission Critical Service outage");
                }
            }
        }

        // Rule 3: 5+ Linked Duplicates -> Bump one priority level
        int duplicateCount = (context != null) ? context.linkedDuplicateCount() : 0;
        if (duplicateCount >= 5) {
            Priority elevated = bumpPriority(finalPriority);
            if (elevated != finalPriority) {
                appliedRules.add(String.format("RULE_DUPLICATE_AVALANCHE: Elevated from %s to %s due to %d correlated duplicate tickets",
                        finalPriority, elevated, duplicateCount));
                finalPriority = elevated;
            }
        }

        return new PriorityDecision(
                finalPriority,
                compositeScore,
                contributions,
                appliedRules,
                calculationTime,
                null,
                null
        );
    }

    private Priority bumpPriority(Priority current) {
        return switch (current) {
            case P4 -> Priority.P3;
            case P3 -> Priority.P2;
            case P2 -> Priority.P1;
            case P1 -> Priority.P1;
        };
    }
}
