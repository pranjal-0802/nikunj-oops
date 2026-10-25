package com.iitsm.domain.model;

/**
 * OOP: Administrative authorization tiers as required in Section 5.
 */
public enum AdminLevel {
    SYSTEM_ADMIN("System Administrator"),
    IT_MANAGER("IT Operations Manager");

    private final String title;

    AdminLevel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}

