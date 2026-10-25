package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * OOP: Entity representing an IT Service catalog entry.
 * Encapsulates owning team, business criticality, user base, and keyword matchers.
 */
public class ItService {

    private final String id;
    private final String name;
    private String owningTeamId;
    private BusinessCriticality businessCriticality;
    private int userBase;
    private final Set<String> keywords;

    public ItService(
            String id,
            String name,
            String owningTeamId,
            BusinessCriticality businessCriticality,
            int userBase,
            Set<String> keywords) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Service ID cannot be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "Service name cannot be blank");
        }
        if (userBase < 0) {
            throw new ValidationException("userBase", "User base count cannot be negative");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.owningTeamId = owningTeamId;
        this.businessCriticality = Objects.requireNonNull(businessCriticality, "BusinessCriticality cannot be null");
        this.userBase = userBase;
        this.keywords = (keywords != null) ? new HashSet<>(keywords) : new HashSet<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwningTeamId() {
        return owningTeamId;
    }

    public BusinessCriticality getBusinessCriticality() {
        return businessCriticality;
    }

    public int getUserBase() {
        return userBase;
    }

    public Set<String> getKeywords() {
        return Collections.unmodifiableSet(keywords);
    }

    public void updateOwningTeam(String newOwningTeamId) {
        this.owningTeamId = newOwningTeamId;
    }

    public void updateBusinessCriticality(BusinessCriticality newCriticality) {
        this.businessCriticality = Objects.requireNonNull(newCriticality, "BusinessCriticality cannot be null");
    }

    public void updateUserBase(int newUserBase) {
        if (newUserBase < 0) {
            throw new ValidationException("userBase", "User base count cannot be negative");
        }
        this.userBase = newUserBase;
    }

    public void addKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            this.keywords.add(keyword.trim().toLowerCase());
        }
    }

    public boolean matchesKeyword(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String lowerText = text.toLowerCase();
        if (lowerText.contains(name.toLowerCase())) {
            return true;
        }
        return keywords.stream().anyMatch(lowerText::contains);
    }
}

