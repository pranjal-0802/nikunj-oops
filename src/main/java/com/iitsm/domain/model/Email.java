package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.regex.Pattern;

/**
 * OOP: Value Object representing an email address with invariant enforcement.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$");

    public Email {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("email", "Email address cannot be null or blank");
        }
        value = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new ValidationException("email", "Invalid email format: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
