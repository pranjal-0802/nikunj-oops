package com.iitsm.domain.model;

/**
 * OOP: Impact level of an incident on organizational business operations.
 */
public enum Impact {
    HIGH(3, 1.0),
    MEDIUM(2, 0.6),
    LOW(1, 0.3);

    private final int level;
    private final double normalizedWeight;

    Impact(int level, double normalizedWeight) {
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

