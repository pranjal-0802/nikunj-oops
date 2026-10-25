package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Objects;

/**
 * OOP: Concrete user subclass representing an employee who can raise and verify incidents.
 */
public class Employee extends User {

    private final Designation designation;
    private final String department;
    private final int seniorityYears;

    public Employee(
            String id,
            String name,
            Email email,
            String passwordHash,
            Designation designation,
            String department,
            int seniorityYears) {
        super(id, name, email, passwordHash, UserRole.ROLE_EMPLOYEE);
        this.designation = Objects.requireNonNull(designation, "Designation cannot be null");
        if (department == null || department.trim().isEmpty()) {
            throw new ValidationException("department", "Department cannot be blank");
        }
        if (seniorityYears < 0) {
            throw new ValidationException("seniorityYears", "Seniority years cannot be negative");
        }
        this.department = department.trim();
        this.seniorityYears = seniorityYears;
    }

    public Designation getDesignation() {
        return designation;
    }

    public String getDepartment() {
        return department;
    }

    public int getSeniorityYears() {
        return seniorityYears;
    }
}
