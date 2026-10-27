package com.iitsm.infrastructure.persistence.mapper;

import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import com.iitsm.infrastructure.persistence.entity.TechnicianSkillEmbeddable;
import com.iitsm.infrastructure.persistence.entity.UserJpaEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Maps between pure domain User models and UserJpaEntity persistence models.
 */
public final class UserMapper {

    private UserMapper() {}

    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;

        Email email = new Email(entity.getEmail());

        if (entity.getRole() == UserRole.ROLE_EMPLOYEE) {
            Employee employee = new Employee(
                    entity.getId(),
                    entity.getName(),
                    email,
                    entity.getPasswordHash(),
                    entity.getDesignation(),
                    entity.getDepartment(),
                    entity.getSeniorityYears() != null ? entity.getSeniorityYears() : 0);
            applyCommonState(employee, entity);
            return employee;
        } else if (entity.getRole() == UserRole.ROLE_TEAM_LEAD) {
            Set<Skill> skills = entity.getSkills().stream()
                    .map(s -> new Skill(s.getSkillName(), s.getCategory()))
                    .collect(Collectors.toSet());
            TeamLead lead = new TeamLead(
                    entity.getId(),
                    entity.getName(),
                    email,
                    entity.getPasswordHash(),
                    skills,
                    entity.getExperienceYears() != null ? entity.getExperienceYears() : 0,
                    entity.getMaxCapacity() != null ? entity.getMaxCapacity() : 5,
                    entity.getSupportTeamId());
            if (entity.getAvailabilityStatus() != null) {
                lead.updateAvailability(entity.getAvailabilityStatus());
            }
            if (entity.getActiveIncidentIds() != null) {
                entity.getActiveIncidentIds().forEach(lead::assignIncident);
            }
            applyCommonState(lead, entity);
            return lead;
        } else if (entity.getRole() == UserRole.ROLE_TECHNICIAN) {
            Set<Skill> skills = entity.getSkills().stream()
                    .map(s -> new Skill(s.getSkillName(), s.getCategory()))
                    .collect(Collectors.toSet());
            Technician tech = new Technician(
                    entity.getId(),
                    entity.getName(),
                    email,
                    entity.getPasswordHash(),
                    skills,
                    entity.getExperienceYears() != null ? entity.getExperienceYears() : 0,
                    entity.getMaxCapacity() != null ? entity.getMaxCapacity() : 5,
                    entity.getSupportTeamId());
            if (entity.getAvailabilityStatus() != null) {
                tech.updateAvailability(entity.getAvailabilityStatus());
            }
            if (entity.getActiveIncidentIds() != null) {
                entity.getActiveIncidentIds().forEach(tech::assignIncident);
            }
            applyCommonState(tech, entity);
            return tech;
        } else if (entity.getRole() == UserRole.ROLE_ADMIN) {
            Administrator admin = new Administrator(
                    entity.getId(),
                    entity.getName(),
                    email,
                    entity.getPasswordHash(),
                    entity.getAdminLevel());
            applyCommonState(admin, entity);
            return admin;
        }

        throw new IllegalArgumentException("Unknown user role: " + entity.getRole());
    }

    private static void applyCommonState(User user, UserJpaEntity entity) {
        if (!entity.isActive()) {
            user.deactivate();
        }
        user.setFailedLoginAttempts(entity.getFailedLoginAttempts());
        user.setLocked(entity.isLocked());
        user.setTokenVersion(entity.getTokenVersion());
        user.setLockedUntil(entity.getLockedUntil());
        user.setVersion(entity.getVersion());
    }

    public static UserJpaEntity toJpaEntity(User domain) {
        if (domain == null) return null;

        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail().value());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        entity.setActive(domain.isActive());
        entity.setFailedLoginAttempts(domain.getFailedLoginAttempts());
        entity.setLocked(domain.isLocked());
        entity.setTokenVersion(domain.getTokenVersion());
        entity.setLockedUntil(domain.getLockedUntil());
        entity.setVersion(domain.getVersion());

        if (domain instanceof Employee emp) {
            entity.setUserType("EMPLOYEE");
            entity.setDesignation(emp.getDesignation());
            entity.setDepartment(emp.getDepartment());
            entity.setSeniorityYears(emp.getSeniorityYears());
        } else if (domain instanceof Technician tech) {
            entity.setUserType(domain instanceof TeamLead ? "TEAM_LEAD" : "TECHNICIAN");
            entity.setExperienceYears(tech.getExperienceYears());
            entity.setMaxCapacity(tech.getMaxCapacity());
            entity.setAvailabilityStatus(tech.getAvailabilityStatus());
            entity.setSupportTeamId(tech.getSupportTeamId());

            Set<TechnicianSkillEmbeddable> skillEmbeddables = tech.getSkills().stream()
                    .map(s -> new TechnicianSkillEmbeddable(s.name(), s.category()))
                    .collect(Collectors.toSet());
            entity.setSkills(skillEmbeddables);
            entity.setActiveIncidentIds(new HashSet<>(tech.getActiveIncidentIds()));
        } else if (domain instanceof Administrator admin) {
            entity.setUserType("ADMINISTRATOR");
            entity.setAdminLevel(admin.getAdminLevel());
        }

        return entity;
    }
}
