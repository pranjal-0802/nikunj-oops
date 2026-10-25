package com.iitsm.domain.model;

/**
 * OOP: Urgency level reflecting how swiftly business operations require resolution.
 */
public enum Urgency {
    HIGH(3, 1.0),
    MEDIUM(2, 0.6),
    LOW(1, 0.3);

    private final int level;
    private final double normalizedWeight;

    Urgency(int level, double normalizedWeight) {
        this.level = level;
        this.normalizedWeight = normalizedWeight;
    }

    public int getLevel() {
        return level;
    }

    public double getNormalizedWeight() {
        return normalizedWeight;
    }
}
