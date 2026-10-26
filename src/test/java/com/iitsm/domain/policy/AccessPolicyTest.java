package com.iitsm.domain.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests application-layer authorization invariants via AccessPolicy.
 */
class AccessPolicyTest {

    private AccessPolicy accessPolicy;
    private Employee employeeAlice;
    private Employee employeeBob;
    private Technician technicianCharlie;
    private Administrator adminDiana;
    private Incident aliceIncident;

    @BeforeEach
    void setUp() {
        accessPolicy = new AccessPolicy() {};
        employeeAlice = new Employee("EMP-ALICE", "Alice", new Email("alice@nimbus.com"), "hash", Designation.ASSOCIATE, "HR", 2);
        employeeBob = new Employee("EMP-BOB", "Bob", new Email("bob@nimbus.com"), "hash", Designation.ASSOCIATE, "Finance", 3);
        technicianCharlie = new Technician("TECH-CHARLIE", "Charlie", new Email("charlie@nimbus.com"), "hash", Set.of(), 5, 5, "TEAM-NET");
        adminDiana = new Administrator("ADMIN-DIANA", "Diana", new Email("diana@nimbus.com"), "hash", AdminLevel.SYSTEM_ADMIN);

        aliceIncident = new Incident("INC-100", "Alice Ticket", "Description with more than twenty characters.", employeeAlice.getId(), Instant.now());
    }

    @Test
    @DisplayName("Authorization: Employee viewing another employee's ticket throws UnauthorizedOperationException")
    void testEmployeeCannotViewAnotherEmployeesTicket() {
        // Alice can view her own ticket
        assertThatCode(() -> accessPolicy.enforceCanViewIncident(employeeAlice, aliceIncident))
                .doesNotThrowAnyException();

        // Bob attempting to view Alice's ticket is blocked
        assertThatThrownBy(() -> accessPolicy.enforceCanViewIncident(employeeBob, aliceIncident))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not authorized to view incident");
    }

    @Test
    @DisplayName("Authorization: Admin has global visibility to all incidents")
    void testAdminHasGlobalVisibility() {
        assertThatCode(() -> accessPolicy.enforceCanViewIncident(adminDiana, aliceIncident))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Authorization: Employee cannot perform technician action, and non-owner employee cannot perform close/verify")
    void testActionAuthorization() {
        // Bob attempting to verify Alice's ticket throws
        assertThatThrownBy(() -> accessPolicy.enforceCanExecuteAction(employeeBob, aliceIncident, "verify"))
                .isInstanceOf(UnauthorizedOperationException.class);

        // Charlie (not assigned) attempting to resolve throws
        assertThatThrownBy(() -> accessPolicy.enforceCanExecuteAction(technicianCharlie, aliceIncident, "resolve"))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("Authorization: Non-administrator attempting admin action throws UnauthorizedOperationException")
    void testEnforceAdmin() {
        assertThatThrownBy(() -> accessPolicy.enforceAdmin(employeeAlice))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("requires Administrator privileges");

        assertThatCode(() -> accessPolicy.enforceAdmin(adminDiana))
                .doesNotThrowAnyException();
    }
}

