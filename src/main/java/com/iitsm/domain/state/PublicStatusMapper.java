package com.iitsm.domain.state;

import com.iitsm.domain.model.IncidentStatus;
import java.util.Objects;

/**
 * Pattern: Mapper / Adapter.
 * Collapses the 9 detailed internal incident states into the employee persona's 5-step stepper.
 * Guaranteed to never leak internal lifecycle transitions to standard employees.
 */
public final class PublicStatusMapper {

    public enum PublicStatus {
        NEW("New", 1),
        ASSIGNED("Assigned", 2),
        IN_PROGRESS("In Progress", 3),
        RESOLVED("Resolved", 4),
        CLOSED("Closed", 5);

        private final String displayName;
        private final int stepNumber;

        PublicStatus(String displayName, int stepNumber) {
            this.displayName = displayName;
            this.stepNumber = stepNumber;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getStepNumber() {
            return stepNumber;
        }
    }

    private PublicStatusMapper() {
        // Utility class
    }

    /**
     * Maps an internal IncidentStatus to the public 5-step representation.
     */
    public static PublicStatus toPublicStatus(IncidentStatus internalStatus) {
        Objects.requireNonNull(internalStatus, "internalStatus cannot be null");
        return switch (internalStatus) {
            case NEW, CLASSIFIED -> PublicStatus.NEW;
            case ASSIGNED, ACKNOWLEDGED -> PublicStatus.ASSIGNED;
            case IN_PROGRESS, PENDING_INFO -> PublicStatus.IN_PROGRESS;
            case RESOLVED, VERIFIED -> PublicStatus.RESOLVED;
            case CLOSED -> PublicStatus.CLOSED;
        };
    }
}
