package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;

/**
 * OOP: Value Object representing end-user feedback on resolved & closed tickets.
 * Rating must be an integer between 1 and 5.
 */
public record Feedback(
        int rating,
        String comment,
        Instant submittedAt) {

    public Feedback {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("rating", "Feedback rating must be an integer between 1 and 5");
        }
        if (submittedAt == null) {
            throw new ValidationException("submittedAt", "Submission timestamp cannot be null");
        }
        comment = (comment != null) ? comment.trim() : "";
    }
}
