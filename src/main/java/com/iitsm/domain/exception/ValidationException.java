package com.iitsm.domain.exception;

/**
 * OOP: Thrown when domain invariants or input validation boundaries are violated.
 */
public class ValidationException extends IitsmException {

    private final String field;

    public ValidationException(String field, String message) {
        super("ERR_VALIDATION_FAILED", String.format("Validation failed for field '%s': %s", field, message));
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
