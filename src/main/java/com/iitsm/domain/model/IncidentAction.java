package com.iitsm.domain.model;

/**
 * Strongly-typed enumeration of all actionable operations in the incident lifecycle.
 * Replaces unstructured string identifiers with strict compile-time verification.
 */
public enum IncidentAction {
    VIEW,
    PROVIDE_INFO,
    VERIFY,
    REJECT,
    CLOSE,
    ACKNOWLEDGE,
    START_INVESTIGATION,
    REQUEST_INFO,
    RESOLVE,
    FLAG_WRONG_DOMAIN,
    REASSIGN,
    OVERRIDE_PRIORITY,
    CANCEL;

    public static IncidentAction fromString(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action name cannot be blank");
        }
        for (IncidentAction a : values()) {
            if (a.name().equalsIgnoreCase(action.trim())
                    || toCamelCase(a.name()).equalsIgnoreCase(action.trim())) {
                return a;
            }
        }
        throw new IllegalArgumentException("Unknown incident action: " + action);
    }

    private static String toCamelCase(String s) {
        String[] parts = s.split("_");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)))
                  .append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}

