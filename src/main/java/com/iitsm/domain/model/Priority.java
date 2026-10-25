package com.iitsm.domain.model;

/**
 * OOP: Priority levels for incidents, ordered by urgency and impact.
 * Natural ordering ranks P1 (highest urgency) before P4 (lowest urgency).
 */
public enum Priority {
    P1("P1 - Critical", 1, 0.75),
    P2("P2 - High", 2, 0.55),
    P3("P3 - Medium", 3, 0.35),
    P4("P4 - Low", 4, 0.00);

    private final String label;
    private final int rank;
    private final double minThresholdScore;

    Priority(String label, int rank, double minThresholdScore) {
        this.label = label;
        this.rank = rank;
        this.minThresholdScore = minThresholdScore;
    }

    public String getLabel() {
        return label;
    }

    public int getRank() {
        return rank;
    }

    public double getMinThresholdScore() {
        return minThresholdScore;
    }

    /**
     * Resolves the Priority tier based on a normalized 0.0 - 1.0 composite score.
     */
    public static Priority fromScore(double score) {
        if (score >= P1.minThresholdScore) {
            return P1;
        } else if (score >= P2.minThresholdScore) {
            return P2;
        } else if (score >= P3.minThresholdScore) {
            return P3;
        } else {
            return P4;
        }
    }
}
