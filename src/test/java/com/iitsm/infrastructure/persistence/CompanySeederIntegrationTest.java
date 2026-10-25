package com.iitsm.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.application.service.UserService;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test verifying that the CompanySeeder loads Nimbus FinServe data
 * deterministically and that all demo accounts authenticate successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class CompanySeederIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private SupportTeamRepository supportTeamRepository;

    @Autowired
    private ItServiceRepository itServiceRepository;

    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    @Autowired
    private CompanySeeder companySeeder;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("Exit Criteria: Seeder creates ~300 employees, 6 teams, ~20 technicians, 14 services")
    void testCompanyDataSeededAccurately() {
        // Explicitly invoke seeder in test profile
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }

        // 1. Verify Employees & Users count
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(320);

        // 2. Verify 6 Support Teams
        assertThat(supportTeamRepository.findAll()).hasSize(6);

        // 3. Verify Technicians (6 Team Leads + 18 Technicians = 24 total)
        assertThat(technicianRepository.findAll()).hasSize(24);

        // 4. Verify 14 IT Services
        assertThat(itServiceRepository.findAll()).hasSize(14);

        // 5. Verify 4 default SLA Policies
        assertThat(slaPolicyRepository.findAll()).hasSize(4);

        // 6. Verify each demo account authenticates and yields a valid JWT token
        String[] demoEmails = {
                "employee@nimbusfinserve.com",
                "tech.network@nimbusfinserve.com",
                "lead.network@nimbusfinserve.com",
                "admin@nimbusfinserve.com",
                "manager@nimbusfinserve.com"
        };

        for (String email : demoEmails) {
            UserService.AuthResult auth = userService.authenticate(email, "Password123!");
            assertThat(auth).isNotNull();
            assertThat(auth.token()).isNotBlank();
            assertThat(auth.user().getEmail().value()).isEqualTo(email);
        }
    }
}
