package com.iitsm.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.application.service.IncidentService;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import com.iitsm.web.dto.ReclassifyRequest;
import com.iitsm.web.dto.SwitchStrategyRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Controller tests for administrative intelligence endpoints (assignment strategy switching, reclassification, duplicates).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class AdminIntelligenceEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    private User admin;
    private String token;
    private Incident incident;

    @BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }

        admin = userRepository.findById("ADMIN-SYS-01").orElseThrow();
        token = tokenProvider.generateToken(admin);

        incident = incidentService.createIncident(
                "EMP-0001",
                "Internal web portal 500 error",
                "Receiving HTTP 500 when accessing employee benefits portal.");
    }

    @Test
    @DisplayName("Admin can list available assignment strategies and switch active strategy live")
    void testAssignmentStrategyEndpoints() throws Exception {
        // 1. List strategies
        mockMvc.perform(get("/api/admin/assignment/strategies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").exists());

        // 2. Switch to SKILL_BASED
        SwitchStrategyRequest request = new SwitchStrategyRequest("SKILL_BASED");
        mockMvc.perform(put("/api/admin/assignment/strategies/active")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SKILL_BASED"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Admin can manually reclassify an incident from the triage queue")
    void testReclassifyEndpoint() throws Exception {
        ReclassifyRequest request = new ReclassifyRequest(
                IncidentCategory.DATABASE,
                "SVC-CORE-DB",
                "Identified root issue as database connection saturation rather than web application error"
        );

        mockMvc.perform(post("/api/admin/incidents/" + incident.getId() + "/reclassify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("DATABASE"))
                .andExpect(jsonPath("$.serviceId").value("SVC-CORE-DB"))
                .andExpect(jsonPath("$.priorityDecision").exists());
    }

    @Test
    @DisplayName("Admin can query linked duplicate incidents")
    void testQueryDuplicatesEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/incidents/" + incident.getId() + "/duplicates")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

