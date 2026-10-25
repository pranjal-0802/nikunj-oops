package com.iitsm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.TechnicianOverloadedException;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OOP: Inheritance, encapsulation, and boundary tests for the User class hierarchy.
 */
class UserHierarchyTest {

    @Test
    @DisplayName("Inheritance: User subtypes demonstrate real is-a polymorphism")
    void testInheritanceHierarchy() {
        Employee emp = new Employee("E1", "Alice", new Email("alice@company.com"), "hash",
                Designation.MANAGER, "Finance", 5);
        Technician tech = new Technician("T1", "Bob", new Email("bob@company.com"), "hash",
                Set.of(), 4, 3, "TEAM-NET");
        TeamLead lead = new TeamLead("L1", "Charlie", new Email("charlie@company.com"), "hash",
                Set.of(), 8, 5, "TEAM-NET");
        Administrator admin = new Administrator("A1", "Diana", new Email("diana@company.com"), "hash",
                AdminLevel.SYSTEM_ADMIN);

        assertThat(emp).isInstanceOf(User.class);
        assertThat(tech).isInstanceOf(User.class);
        assertThat(lead).isInstanceOf(Technician.class);
        assertThat(lead).isInstanceOf(User.class);
        assertThat(admin).isInstanceOf(User.class);

        assertThat(emp.getRole()).isEqualTo(UserRole.ROLE_EMPLOYEE);
        assertThat(tech.getRole()).isEqualTo(UserRole.ROLE_TECHNICIAN);
        assertThat(lead.getRole()).isEqualTo(UserRole.ROLE_TEAM_LEAD);
        assertThat(admin.getRole()).isEqualTo(UserRole.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Boundary: 5th vs 6th failed login locks the account")
    void testAccountLockoutBoundary() {
        Employee emp = new Employee("E1", "Alice", new Email("alice@company.com"), "hash",
                Designation.ASSOCIATE, "HR", 2);

        // 1 to 4 failed attempts: not locked
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        assertThat(emp.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(emp.isLocked()).isFalse();

        // 5th failed attempt: exactly locks account
        emp.recordFailedLogin(5);
        assertThat(emp.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(emp.isLocked()).isTrue();

        // Further attempt maintains lock
        emp.recordFailedLogin(5);
        assertThat(emp.getFailedLoginAttempts()).isEqualTo(6);
        assertThat(emp.isLocked()).isTrue();

        // Reset restores access
        emp.resetFailedLogins();
        assertThat(emp.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(emp.isLocked()).isFalse();
    }

    @Test
    @DisplayName("Boundary: Technician assignment at exact maxCapacity vs capacity + 1")
    void testTechnicianCapacityBoundary() {
        Technician tech = new Technician("T1", "Bob", new Email("bob@company.com"), "hash",
                Set.of(new Skill("Linux", IncidentCategory.APPLICATION)), 3, 2, "TEAM-APP");

        assertThat(tech.getMaxCapacity()).isEqualTo(2);
        assertThat(tech.getActiveIncidentCount()).isEqualTo(0);
        assertThat(tech.hasCapacity()).isTrue();
        assertThat(tech.isAvailableForAssignment()).isTrue();

        // 1st assignment
        tech.assignIncident("INC-1");
        assertThat(tech.getActiveIncidentCount()).isEqualTo(1);
        assertThat(tech.hasCapacity()).isTrue();
        assertThat(tech.getLoadRatio()).isEqualTo(0.5);

        // 2nd assignment: reaches exact maxCapacity
        tech.assignIncident("INC-2");
        assertThat(tech.getActiveIncidentCount()).isEqualTo(2);
        assertThat(tech.hasCapacity()).isFalse();
        assertThat(tech.getLoadRatio()).isEqualTo(1.0);
        assertThat(tech.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
        assertThat(tech.isAvailableForAssignment()).isFalse();

        // 3rd assignment: exceeds capacity -> throws TechnicianOverloadedException
        assertThatThrownBy(() -> tech.assignIncident("INC-3"))
                .isInstanceOf(TechnicianOverloadedException.class)
                .satisfies(ex -> {
                    TechnicianOverloadedException toe = (TechnicianOverloadedException) ex;
                    assertThat(toe.getErrorCode()).isEqualTo("ERR_TECHNICIAN_OVERLOADED");
                });

        // Release restores capacity
        tech.releaseIncident("INC-1");
        assertThat(tech.getActiveIncidentCount()).isEqualTo(1);
        assertThat(tech.hasCapacity()).isTrue();
        assertThat(tech.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.ONLINE);
        assertThat(tech.isAvailableForAssignment()).isTrue();
    }

    @Test
    @DisplayName("Encapsulation: Technician skills cannot be modified externally")
    void testTechnicianSkillsEncapsulation() {
        Skill skill = new Skill("Postgres", IncidentCategory.DATABASE);
        Technician tech = new Technician("T1", "Bob", new Email("bob@company.com"), "hash",
                Set.of(skill), 5, 5, "TEAM-DB");

        assertThat(tech.hasSkill("Postgres")).isTrue();
        assertThat(tech.hasCategorySkill(IncidentCategory.DATABASE)).isTrue();
        assertThat(tech.hasCategorySkill(IncidentCategory.HARDWARE)).isFalse();

        assertThatThrownBy(() -> tech.getSkills().add(new Skill("C++", IncidentCategory.SOFTWARE)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

