package com.iitsm.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import com.iitsm.domain.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryAdapterTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("UserRepositoryAdapter persists and hydrates Employee subtype correctly")
    void testSaveAndFindEmployee() {
        Employee emp = new Employee(
                "EMP-TEST-99",
                "Integration Tester",
                new Email("tester.integration@nimbusfinserve.com"),
                "$2a$12$e0MYzXyjpJS7Pd0RVvHwHeFj7bK2B3vQyO.",
                Designation.SENIOR_ASSOCIATE,
                "Engineering",
                5);

        userRepository.save(emp);

        Optional<User> found = userRepository.findById("EMP-TEST-99");
        assertThat(found).isPresent();
        assertThat(found.get()).isInstanceOf(Employee.class);

        Employee hydrated = (Employee) found.get();
        assertThat(hydrated.getName()).isEqualTo("Integration Tester");
        assertThat(hydrated.getDepartment()).isEqualTo("Engineering");
        assertThat(hydrated.getDesignation()).isEqualTo(Designation.SENIOR_ASSOCIATE);
        assertThat(hydrated.getSeniorityYears()).isEqualTo(5);
        assertThat(hydrated.getRole()).isEqualTo(UserRole.ROLE_EMPLOYEE);
    }

    @Test
    @DisplayName("UserRepositoryAdapter persists account lockout state")
    void testLockoutPersistence() {
        Employee emp = new Employee(
                "EMP-LOCK-01",
                "Locked User",
                new Email("locked.user@nimbusfinserve.com"),
                "hash",
                Designation.INTERN,
                "HR",
                1);

        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5);
        emp.recordFailedLogin(5); // 5th failure -> locked
        userRepository.save(emp);

        Optional<User> found = userRepository.findByEmail(new Email("locked.user@nimbusfinserve.com"));
        assertThat(found).isPresent();
        assertThat(found.get().isLocked()).isTrue();
        assertThat(found.get().getFailedLoginAttempts()).isEqualTo(5);
    }
}
