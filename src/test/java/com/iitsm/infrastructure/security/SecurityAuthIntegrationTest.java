package com.iitsm.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iitsm.application.service.UserService;
import com.iitsm.infrastructure.persistence.CompanySeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CompanySeeder companySeeder;

    private String employeeToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        companySeeder.seedCompanyData();
        employeeToken = userService.authenticate("employee@nimbusfinserve.com", "Password123!").token();
        adminToken = userService.authenticate("admin@nimbusfinserve.com", "Password123!").token();
    }

    @Test
    @DisplayName("Security: Unauthenticated request returns consistent 401 JSON")
    void testUnauthenticatedReturns401Json() throws Exception {
        mockMvc.perform(get("/api/employee/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR_UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Security: Forbidden role request returns consistent 403 JSON")
    void testForbiddenRoleReturns403Json() throws Exception {
        // Employee attempting to access /api/admin/test
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ERR_FORBIDDEN"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("Security: Authorized role request succeeds with 200 OK")
    void testAuthorizedRequestSucceeds() throws Exception {
        mockMvc.perform(get("/api/admin/test")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
