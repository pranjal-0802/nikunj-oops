package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;

/**
 * OOP: Value Object representing an investigation entry or communication on an incident.
 */
public record InvestigationNote(
        String id,
        String authorId,
        String authorName,
        String noteText,
        Instant createdAt,
        boolean internalOnly) {

    public InvestigationNote {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "Note ID cannot be blank");
        }
        if (authorId == null || authorId.trim().isEmpty()) {
            throw new ValidationException("authorId", "Author ID cannot be blank");
        }
        if (noteText == null || noteText.trim().isEmpty()) {
            throw new ValidationException("noteText", "Note text cannot be blank");
        }
        if (createdAt == null) {
            throw new ValidationException("createdAt", "Created timestamp cannot be null");
        }
    }
}
