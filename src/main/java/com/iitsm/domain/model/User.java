package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Objects;

/**
 * OOP: Abstract base class for all authenticated system users.
 * Subclasses represent specific personas: Employee, Technician (and TeamLead), Administrator.
 */
public abstract class User {

    private final String id;
    private String name;
    private final Email email;
    private String passwordHash;
    private final UserRole role;
    private boolean active;
    private int failedLoginAttempts;
    private boolean locked;

    protected User(String id, String name, Email email, String passwordHash, UserRole role) {
        if (id == null || id.trim().isEmpty()) {
            throw new ValidationException("id", "User ID cannot be blank");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("name", "User name cannot be blank");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.email = Objects.requireNonNull(email, "Email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash must not be null");
        this.role = Objects.requireNonNull(role, "UserRole must not be null");
        this.active = true;
        this.failedLoginAttempts = 0;
        this.locked = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public boolean isLocked() {
        return locked;
    }

    public void updatePassword(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.trim().isEmpty()) {
            throw new ValidationException("password", "Password hash cannot be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    public void recordFailedLogin(int maxAllowedBeforeLock) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAllowedBeforeLock) {
            this.locked = true;
        }
    }

    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
        this.locked = false;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
