package com.iitsm.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.application.service.IncidentService;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import com.iitsm.web.dto.AssignRequest;
import com.iitsm.web.dto.FeedbackRequest;
import com.iitsm.web.dto.OverridePriorityRequest;
import java.time.Instant;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UserRepository userRepository;

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
                "Exchange server high CPU alert",
                "High load on mail routing component affecting delivery latency.");
    }

    @Test
    @DisplayName("Admin can view system statistics, directory, triage queue and technician roster")
    void testAdminInspectionEndpoints() throws Exception {
        // 1. Stats
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents").isNumber());

        // 2. Incident Search
        mockMvc.perform(get("/api/admin/incidents?query=Exchange")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 3. Triage Queue
        mockMvc.perform(get("/api/admin/triage-queue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 4. Technician Roster
        mockMvc.perform(get("/api/admin/technicians")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Admin can perform administrative controls: assign, override priority, close")
    void testAdminActionEndpoints() throws Exception {
        // 1. Get Details
        mockMvc.perform(get("/api/admin/incidents/" + incident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incident.getId()))
                .andExpect(jsonPath("$.priorityDecision").exists())
                .andExpect(jsonPath("$.slaTracker").exists());

        // 2. Assign
        AssignRequest assignReq = new AssignRequest("TECH-NET-1");
        mockMvc.perform(post("/api/admin/incidents/" + incident.getId() + "/assign")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value("TECH-NET-1"));

        // 3. Override Priority
        OverridePriorityRequest overrideReq = new OverridePriorityRequest(Priority.P1, "CEO escalates high priority outage");
        mockMvc.perform(post("/api/admin/incidents/" + incident.getId() + "/override-priority")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overrideReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.priorityDecision.priority").value("P1"));

        // 4. Advance through lifecycle so incident is in VERIFIED state before closing
        incidentService.acknowledge(incident.getId(), "TECH-NET-1");
        incidentService.startInvestigation(incident.getId(), "TECH-NET-1");
        incidentService.resolve(incident.getId(), "TECH-NET-1",
                new Resolution("Root cause identified and resolved", "Applied configuration fix", "Updated runbook", "INFRASTRUCTURE", "TECH-NET-1", Instant.now()));
        incidentService.verify(incident.getId(), "EMP-0001");

        // 5. Close
        FeedbackRequest closeReq = new FeedbackRequest(5, "Resolved by admin");
        mockMvc.perform(post("/api/admin/incidents/" + incident.getId() + "/close")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(closeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }
}
