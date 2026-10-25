package com.iitsm.domain.model;

/**
 * OOP: Employee organisational hierarchy level and its priority weight contribution.
 * Policy note: Designation-based priority influence is capped and weighted according to Section 7.1.
 */
public enum Designation {
    INTERN("Intern", 0.15),
    ASSOCIATE("Associate", 0.30),
    SENIOR_ASSOCIATE("Senior Associate", 0.45),
    MANAGER("Manager", 0.60),
    SENIOR_MANAGER("Senior Manager", 0.70),
    DIRECTOR("Director", 0.85),
    VP_C_LEVEL("VP / C-Level", 1.00);

    private final String title;
    private final double priorityWeight;

    Designation(String title, double priorityWeight) {
        this.title = title;
        this.priorityWeight = priorityWeight;
    }

    public String getTitle() {
        return title;
    }

    public double getPriorityWeight() {
        return priorityWeight;
    }
}

