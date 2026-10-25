package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;

/**
 * OOP: Value Object representing a technical competency.
 * Immutable record ensuring encapsulation and value semantics.
 */
public record Skill(String name, IncidentCategory category) {

    public Skill {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("skillName", "Skill name must not be blank");
        }
        if (category == null) {
            throw new ValidationException("skillCategory", "Skill category must not be null");
        }
        name = name.trim();
    }
}

