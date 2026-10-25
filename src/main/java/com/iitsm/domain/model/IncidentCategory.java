package com.iitsm.domain.model;

/**
 * OOP: Standard IT incident categories built into the system.
 */
public enum IncidentCategory {
    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    NETWORK("Network"),
    DATABASE("Database"),
    CLOUD("Cloud"),
    SECURITY("Security"),
    AUTHENTICATION("Authentication"),
    APPLICATION("Application"),
    STORAGE("Storage");

    private final String displayName;

    IncidentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
