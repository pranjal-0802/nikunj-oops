package com.iitsm.domain.model;

/**
 * OOP: Compliance status of an incident with respect to its Service Level Agreement.
 */
public enum SlaStatus {
    ON_TRACK("On Track"),
    AT_RISK("At Risk - Exceeded 75% Target Window"),
    WARNING("Warning - Exceeded 90% Target Window"),
    BREACHED("Breached - SLA Violated"),
    MET("Met - Resolved Within Target");

    private final String description;

    SlaStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBreached() {
        return this == BREACHED;
    }

    public boolean isTerminal() {
        return this == MET || this == BREACHED;
    }
}

