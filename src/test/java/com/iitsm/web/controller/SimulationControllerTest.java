package com.iitsm.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for simulation engine endpoints and scenario execution.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    private String adminToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }

        User admin = userRepository.findById("ADMIN-SYS-01").orElseThrow();
        adminToken = tokenProvider.generateToken(admin);

        User employee = userRepository.findById("EMP-0001").orElseThrow();
        employeeToken = tokenProvider.generateToken(employee);
    }

    @Test
    @DisplayName("Admin can inspect virtual simulation clock status")
    void testGetClockStatus() throws Exception {
        mockMvc.perform(get("/api/simulation/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.virtualTime").exists())
                .andExpect(jsonPath("$.speedMultiplier").isNumber());
    }

    @Test
    @DisplayName("Admin can accelerate simulation clock speed")
    void testSetSpeedMultiplier() throws Exception {
        mockMvc.perform(post("/api/simulation/speed?multiplier=60")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speedMultiplier").value(60))
                .andExpect(jsonPath("$.isAccelerated").value(true));
    }

    @Test
    @DisplayName("Admin can fast-forward simulation time")
    void testAdvanceTime() throws Exception {
        mockMvc.perform(post("/api/simulation/advance?minutes=45")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.virtualTime").exists());
    }

    @Test
    @DisplayName("Admin can trigger Scenario S1 (Happy Path) execution")
    void testRunScenarioS1() throws Exception {
        mockMvc.perform(post("/api/simulation/run/S1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarioId").value("S1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stepLogs").isArray());
    }

    @Test
    @DisplayName("Employee is forbidden from accessing simulation engine")
    void testEmployeeForbiddenFromSimulation() throws Exception {
        mockMvc.perform(get("/api/simulation/status")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}

