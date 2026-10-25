package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pattern: Registry / Open-Closed Principle.
 * Allows adding new categories at runtime via registration without modifying existing enums or code.
 */
public class CategoryRegistry {

    // ConcurrentHashMap chosen for thread-safe dynamic registry reads and runtime additions
    private final Map<String, String> registeredCategories = new ConcurrentHashMap<>();

    public CategoryRegistry() {
        // Register all built-in enum categories by default
        for (IncidentCategory category : IncidentCategory.values()) {
            register(category.name(), category.getDisplayName());
        }
    }

    public void register(String code, String displayName) {
        if (code == null || code.trim().isEmpty()) {
            throw new ValidationException("categoryCode", "Category code cannot be null or blank");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new ValidationException("displayName", "Display name cannot be null or blank");
        }
        registeredCategories.put(code.trim().toUpperCase(), displayName.trim());
    }

    public boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        return registeredCategories.containsKey(code.trim().toUpperCase());
    }

    public String getDisplayName(String code) {
        if (code == null) {
            return null;
        }
        return registeredCategories.get(code.trim().toUpperCase());
    }

    public Set<String> getAllCodes() {
        return Collections.unmodifiableSet(registeredCategories.keySet());
    }

    public Map<String, String> getAllCategories() {
        return Collections.unmodifiableMap(registeredCategories);
    }
}
