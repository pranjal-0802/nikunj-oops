package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;

/**
 * OOP: Immutable Value Object holding response and resolution deadlines.
 */
public record DeadlinePair(Instant responseDueAt, Instant resolutionDueAt) {

    public DeadlinePair {
        if (responseDueAt == null) {
            throw new ValidationException("responseDueAt", "Response due time must not be null");
        }
        if (resolutionDueAt == null) {
            throw new ValidationException("resolutionDueAt", "Resolution due time must not be null");
        }
        if (resolutionDueAt.isBefore(responseDueAt)) {
            throw new ValidationException(
                    "resolutionDueAt", "Resolution deadline cannot precede response deadline");
        }
    }
}
