package com.iitsm.domain.model;

/**
 * OOP: Represents the internal lifecycle states of an Incident.
 * Mapped to behavior implementations via the State Pattern.
 */
public enum IncidentStatus {
    NEW("New"),
    CLASSIFIED("Classified"),
    ASSIGNED("Assigned"),
    ACKNOWLEDGED("Acknowledged"),
    IN_PROGRESS("In Progress"),
    PENDING_INFO("Pending Info"),
    RESOLVED("Resolved"),
    VERIFIED("Verified"),
    CLOSED("Closed");

    private final String displayName;

    IncidentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

