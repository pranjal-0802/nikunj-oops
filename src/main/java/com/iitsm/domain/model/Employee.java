package com.iitsm.domain.model;

import com.iitsm.domain.exception.ValidationException;
import java.util.Objects;

/**
 * OOP: Concrete user subclass representing an employee who can raise and verify incidents.
 */
public class Employee extends User {

    private Designation designation;
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

    public void setDesignation(Designation designation) {
        this.designation = Objects.requireNonNull(designation, "Designation cannot be null");
    }

    public String getDepartment() {
        return department;
    }

    public int getSeniorityYears() {
        return seniorityYears;
    }

    @Override
    public boolean canViewIncident(Incident incident, String owningTeamId) {
        if (incident == null) return false;
        return getId().equals(incident.getReporterId());
    }

    @Override
    public boolean canExecuteAction(Incident incident, IncidentAction action, String owningTeamId) {
        if (incident == null || action == null) return false;
        if (!getId().equals(incident.getReporterId())) {
            return false;
        }
        return action == IncidentAction.PROVIDE_INFO
                || action == IncidentAction.VERIFY
                || action == IncidentAction.REJECT
                || action == IncidentAction.CLOSE
                || action == IncidentAction.VIEW;
    }
}

