package com.iitsm.application.service;

import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Pattern: Factory.
 * Centralizes construction and invariant enforcement for User hierarchy subtypes.
 */
@Component
public class UserFactory {

    public Employee createEmployee(
            String id,
            String name,
            String email,
            String passwordHash,
            Designation designation,
            String department,
            int seniorityYears) {
        return new Employee(
                id,
                name,
                new Email(email),
                passwordHash,
                designation != null ? designation : Designation.ASSOCIATE,
                department,
                seniorityYears);
    }

    public Technician createTechnician(
            String id,
            String name,
            String email,
            String passwordHash,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {
        return new Technician(
                id,
                name,
                new Email(email),
                passwordHash,
                skills,
                experienceYears,
                maxCapacity > 0 ? maxCapacity : 5,
                supportTeamId);
    }

    public TeamLead createTeamLead(
            String id,
            String name,
            String email,
            String passwordHash,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {
        return new TeamLead(
                id,
                name,
                new Email(email),
                passwordHash,
                skills,
                experienceYears,
                maxCapacity > 0 ? maxCapacity : 5,
                supportTeamId);
    }

    public Administrator createAdministrator(
            String id,
            String name,
            String email,
            String passwordHash,
            AdminLevel adminLevel) {
        return new Administrator(
                id,
                name,
                new Email(email),
                passwordHash,
                adminLevel != null ? adminLevel : AdminLevel.SYSTEM_ADMIN);
    }
}

