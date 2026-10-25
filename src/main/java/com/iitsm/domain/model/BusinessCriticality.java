package com.iitsm.domain.model;

/**
 * OOP: Business criticality tier (1-4) of an IT Service as specified in Section 7.1.
 */
public enum BusinessCriticality {
    TIER_4_MISSION_CRITICAL(4, 1.0, "Mission Critical"),
    TIER_3_BUSINESS_OPERATIONAL(3, 0.75, "Business Operational"),
    TIER_2_DEPARTMENTAL(2, 0.50, "Departmental"),
    TIER_1_INTERNAL_SUPPORT(1, 0.25, "Internal Support");

    private final int tier;
    private final double score;
    private final String description;

    BusinessCriticality(int tier, double score, String description) {
        this.tier = tier;
        this.score = score;
        this.description = description;
    }

    public int getTier() {
        return tier;
    }

    public double getScore() {
        return score;
    }

    public String getDescription() {
        return description;
    }

    public static BusinessCriticality fromTier(int tier) {
        for (BusinessCriticality bc : values()) {
            if (bc.tier == tier) {
                return bc;
            }
        }
        throw new IllegalArgumentException("Unknown business criticality tier: " + tier);
    }
}
