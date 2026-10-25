package com.iitsm.web.controller;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.application.service.IncidentService;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import com.iitsm.web.dto.CreateIncidentRequest;
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
@Transactional
class EmployeePrivacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private com.iitsm.domain.repository.UserRepository userRepository;

    @Test
    @DisplayName("Employee ticket responses strictly omit priority, SLA internals, and internal notes")
    void testEmployeeEndpointStrictPrivacyInvariant() throws Exception {
        // Use seeded employee EMP-0001
        com.iitsm.domain.model.User employee = userRepository.findById("EMP-0001").orElseThrow();
        String token = tokenProvider.generateToken(employee);

        CreateIncidentRequest request = new CreateIncidentRequest(
                "Unable to access GitLab repository",
                "SSH key authentication fails with Permission Denied (publickey) since morning.");

        mockMvc.perform(post("/api/employee/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Unable to access GitLab repository"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                // STRICT PRIVACY VERIFICATION: Priority and SLA fields must not exist in response JSON
                .andExpect(jsonPath("$", not(hasKey("priority"))))
                .andExpect(jsonPath("$", not(hasKey("priorityDecision"))))
                .andExpect(jsonPath("$", not(hasKey("compositeScore"))))
                .andExpect(jsonPath("$", not(hasKey("slaTracker"))))
                .andExpect(jsonPath("$", not(hasKey("responseDueAt"))))
                .andExpect(jsonPath("$", not(hasKey("resolutionDueAt"))))
                .andExpect(jsonPath("$", not(hasKey("slaStatus"))))
                .andExpect(jsonPath("$", not(hasKey("notes"))));
    }

    @Test
    @DisplayName("Employee cannot inspect tickets owned by another employee (enforces AccessPolicy 403)")
    void testEmployeeAccessPolicyBoundary() throws Exception {
        // Create an incident owned by EMP-0001
        Incident incident = incidentService.createIncident(
                "EMP-0001",
                "Database connection timeout on query",
                "Postgres report query timed out after 30 seconds wait.");

        // Employee 2 attempting to view Employee 1's incident
        com.iitsm.domain.model.User employee2 = userRepository.findById("EMP-0002").orElseThrow();
        String token2 = tokenProvider.generateToken(employee2);

        mockMvc.perform(get("/api/employee/incidents/" + incident.getId())
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ERR_UNAUTHORIZED_OPERATION"));
    }
}
