package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;

/**
 * OOP: Structured resolution report submitted by a technician upon resolving an incident.
 * Invariants guarantee thoroughness before end-user verification.
 */
public record Resolution(
        String rootCause,
        String stepsTaken,
        String preventiveAction,
        String confirmedCategory,
        String resolvedBy,
        Instant resolvedAt) {

    public Resolution {
        if (rootCause == null || rootCause.trim().isEmpty()) {
            throw new ValidationException("rootCause", "Root cause description is required");
        }
        if (stepsTaken == null || stepsTaken.trim().isEmpty()) {
            throw new ValidationException("stepsTaken", "Steps taken description is required");
        }
        if (preventiveAction == null || preventiveAction.trim().isEmpty()) {
            throw new ValidationException("preventiveAction", "Preventive action description is required");
        }
        if (confirmedCategory == null || confirmedCategory.trim().isEmpty()) {
            throw new ValidationException("confirmedCategory", "Category confirmation is required");
        }
        if (resolvedBy == null || resolvedBy.trim().isEmpty()) {
            throw new ValidationException("resolvedBy", "Technician resolver ID is required");
        }
        if (resolvedAt == null) {
            throw new ValidationException("resolvedAt", "Resolution timestamp is required");
        }
        rootCause = rootCause.trim();
        stepsTaken = stepsTaken.trim();
        preventiveAction = preventiveAction.trim();
        confirmedCategory = confirmedCategory.trim();
        resolvedBy = resolvedBy.trim();
    }
}
