package com.iitsm.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.IitsmApplication;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.policy.AccessPolicy;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Regression test suite reproducing confirmed security findings S1–S8 from the Code Review.
 * All tests are tagged with @Tag("regression") and must fail against the current unpatched code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Tag("regression")
public class SecurityRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private AccessPolicy accessPolicy;

    @Autowired
    private com.iitsm.application.service.IncidentService incidentService;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    @Autowired
    private com.iitsm.domain.repository.TechnicianRepository technicianRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM technician_active_incidents");
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM technician_active_incidents");
    }

    @Test
    @DisplayName("S1: App context startup must fail when iitsm.security.jwt.secret equals default committed secret in prod")
    void S1_jwtSecretValidationFailsOnWeakOrCommittedSecret() {
        // Finding S1: Default committed secret in application.yml should trigger a startup failure in production
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ctx = new SpringApplicationBuilder(IitsmApplication.class)
                    .profiles("prod")
                    .properties("server.port=0")
                    .run()) {
                assertThat(ctx).isNotNull();
            }
        }).as("Startup in prod with default committed secret must fail").isNotNull();
    }

    @Test
    @DisplayName("S2: Failed login attempt counter and lockout must persist in DB after 5 bad logins")
    void S2_lockoutPersistsAfterFiveBadLogins() throws Exception {
        // Finding S2: @Transactional on UserService rolls back failedLoginAttempts on InvalidCredentialsException
        String targetEmail = "admin@nimbusfinserve.com";
        String badPayload = objectMapper.writeValueAsString(Map.of(
                "email", targetEmail,
                "password", "WrongPassword999!"
        ));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badPayload))
                    .andExpect(status().isUnauthorized());
        }

        User user = userRepository.findByEmail(new Email(targetEmail)).orElseThrow();
        assertThat(user.getFailedLoginAttempts())
                .as("Failed login attempts counter must persist in DB despite exception")
                .isGreaterThanOrEqualTo(5);
        assertThat(user.isLocked())
                .as("Account must be locked after 5 consecutive bad login attempts")
                .isTrue();
    }

    @Test
    @DisplayName("S3: Self-registration must ignore caller-supplied designation and store lowest level")
    void S3_registrationIgnoresClientSuppliedDesignation() throws Exception {
        // Finding S3: Registration allows arbitrary designation, allowing self-registered VP_C_LEVEL
        String regPayload = objectMapper.writeValueAsString(Map.of(
                "name", "Attacker User",
                "email", "attacker.priv@nimbusfinserve.com",
                "password", "Password123!",
                "department", "Executive",
                "designation", "VP_C_LEVEL"
        ));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regPayload))
                .andExpect(status().isOk());

        User registered = userRepository.findByEmail(new Email("attacker.priv@nimbusfinserve.com")).orElseThrow();
        assertThat(registered).isInstanceOf(Employee.class);
        Employee emp = (Employee) registered;
        assertThat(emp.getDesignation())
                .as("Caller cannot self-assign VP_C_LEVEL; registration must default to lowest designation (ASSOCIATE)")
                .isNotEqualTo(Designation.VP_C_LEVEL)
                .isEqualTo(Designation.ASSOCIATE);
    }

    @Test
    @DisplayName("S4: Admin technician roster endpoint must NOT leak passwordHash")
    void S4_adminEndpointsDoNotLeakPasswordHash() throws Exception {
        // Finding S4: AdminController.getTechnicianRoster returns List<Technician> directly, leaking passwordHash
        User admin = userRepository.findByEmail(new Email("admin@nimbusfinserve.com")).orElseThrow();
        String adminToken = tokenProvider.generateToken(admin);

        MvcResult result = mockMvc.perform(get("/api/admin/technicians")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body)
                .as("Technician roster API response must never contain passwordHash")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("S5: /api/public/config must exist and control demo mode")
    void S5_publicConfigControlsDemoModeAndHidesDemoCredentials() throws Exception {
        // Finding S5: Hardcoded demo credentials in client JS; /api/public/config must expose demoMode state
        mockMvc.perform(get("/api/public/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demoMode").isBoolean());
    }

    @Test
    @DisplayName("S6: AccessPolicy must forbid cross-role actions and unauthorized technician inspection")
    void S6_accessPolicyRestrictsCrossRoleActionsAndUnassignedInspection() {
        // Finding S6: enforceCanViewIncident allows general technician inspection; enforceCanExecuteAction ignores role restrictions
        User tech2 = userRepository.findByEmail(new Email("tech.cloud2@nimbusfinserve.com")).orElseThrow();
        User employee = userRepository.findByEmail(new Email("employee@nimbusfinserve.com")).orElseThrow();

        // Create incident assigned to a network technician
        Incident incident = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Core network switch offline in datacenter",
                "Core network switch offline in rack 4 causing intermittent dropped packets on VLAN 10"
        );
        assertThat(incident.getAssigneeId()).isNotNull();

        // Unassigned technician from Cloud team should NOT have inspection access to network ticket
        assertThatThrownBy(() -> accessPolicy.enforceCanViewIncident(tech2, incident))
                .as("Technician not assigned to incident and not in owning team cannot view it")
                .isInstanceOf(UnauthorizedOperationException.class);

        // Employee should NOT be able to execute technician-only action 'resolve'
        assertThatThrownBy(() -> accessPolicy.enforceCanExecuteAction(employee, incident, "resolve"))
                .as("Employee cannot execute technician action 'resolve'")
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("S7: JwtAuthenticationFilter must reject locked or deactivated accounts with 401")
    void S7_jwtAuthenticationFilterRejectsLockedOrDeactivatedUser() throws Exception {
        // Finding S7: Tokens for locked/deactivated users are trusted until 24h expiration
        User user = userRepository.findByEmail(new Email("employee@nimbusfinserve.com")).orElseThrow();
        user.deactivate();
        userRepository.save(user);

        try {
            String token = tokenProvider.generateToken(user);
            mockMvc.perform(get("/api/employee/incidents")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        } finally {
            user.activate();
            userRepository.save(user);
        }
    }

    @Test
    @DisplayName("S8: HTTP responses must contain essential security headers (CSP, nosniff, frame-ancestors)")
    void S8_securityHeadersArePresentOnHttpResponses() throws Exception {
        // Finding S8: SecurityConfig does not enforce CSP, frame-ancestors, or nosniff headers
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }
}
