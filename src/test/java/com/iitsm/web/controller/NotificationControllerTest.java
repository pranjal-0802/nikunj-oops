package com.iitsm.web.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.persistence.entity.NotificationJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataNotificationRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpringDataNotificationRepository notificationRepository;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    private User employee;
    private String token;

    @BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }

        employee = userRepository.findById("EMP-0001").orElseThrow();
        token = tokenProvider.generateToken(employee);

        notificationRepository.deleteAll();

        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId("NOTIF-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setRecipientId(employee.getId());
        entity.setChannel("IN_APP");
        entity.setSubject("Test Subject");
        entity.setMessage("Test Message");
        entity.setDelivered(false);
        entity.setCreatedAt(Instant.now());
        notificationRepository.save(entity);
    }

    @Test
    @DisplayName("User can query in-app notifications and mark notification as read")
    void testQueryAndMarkRead() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].subject").value("Test Subject"));

        NotificationJpaEntity notif = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(employee.getId()).get(0);

        mockMvc.perform(post("/api/notifications/" + notif.getId() + "/read")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("User can connect to SSE notification stream")
    void testNotificationSseStream() throws Exception {
        mockMvc.perform(get("/api/notifications/stream")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
