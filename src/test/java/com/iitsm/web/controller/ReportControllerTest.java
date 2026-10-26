package com.iitsm.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Controller tests for management and SLA report REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class ReportControllerTest {

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
    @DisplayName("Admin can fetch workload distribution report")
    void testGetWorkloadReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/workload")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technicianLoads").isArray())
                .andExpect(jsonPath("$.teamLoads").isArray())
                .andExpect(jsonPath("$.averageLoadRatio").isNumber());
    }

    @Test
    @DisplayName("Admin can fetch service analysis report")
    void testGetServiceAnalysisReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/services")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceMetrics").isArray());
    }

    @Test
    @DisplayName("Admin can fetch SLA performance report")
    void testGetSlaPerformanceReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/sla")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallCompliancePercentage").isNumber())
                .andExpect(jsonPath("$.priorityCompliance").exists());
    }

    @Test
    @DisplayName("Admin can fetch MTTR and MTTResp report")
    void testGetMttrReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/mttr")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallMttrMinutes").isNumber())
                .andExpect(jsonPath("$.overallMttrespMinutes").isNumber());
    }

    @Test
    @DisplayName("Admin can fetch escalations report")
    void testGetEscalationsReport() throws Exception {
        mockMvc.perform(get("/api/admin/reports/escalations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEscalations").isNumber())
                .andExpect(jsonPath("$.recentEscalations").isArray());
    }

    @Test
    @DisplayName("Employee is forbidden from accessing administrative reports")
    void testEmployeeForbiddenFromReports() throws Exception {
        mockMvc.perform(get("/api/admin/reports/workload")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }
}

