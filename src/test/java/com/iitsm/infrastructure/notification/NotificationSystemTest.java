package com.iitsm.infrastructure.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.persistence.repository.SpringDataNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

class NotificationSystemTest {

    @Test
    @DisplayName("NotificationFactory constructs proper payload for IncidentAssigned")
    void testNotificationFactory() {
        NotificationFactory factory = new NotificationFactory();
        DomainEvent.IncidentAssigned assigned = new DomainEvent.IncidentAssigned(
                "EVT-01", Instant.now(), "INC-123", "SYSTEM", "TECH-007");

        Optional<NotificationFactory.NotificationPayload> payload = factory.createNotification(assigned, "TECH-007");
        assertThat(payload).isPresent();
        assertThat(payload.get().recipientUserId()).isEqualTo("TECH-007");
        assertThat(payload.get().subject()).contains("INC-123");
        assertThat(payload.get().body()).contains("assigned");
    }

    @Test
    @DisplayName("InAppChannel persists notification and broadcasts via SSE")
    void testInAppChannel() {
        SpringDataNotificationRepository repo = mock(SpringDataNotificationRepository.class);
        SseEmitterRegistry registry = new SseEmitterRegistry();
        Clock clock = Clock.systemUTC();

        when(repo.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        InAppChannel channel = new InAppChannel(repo, registry, clock);
        assertThat(channel.getChannelName()).isEqualTo("IN_APP");

        assertThatCode(() -> channel.send("USER-001", "Alert", "Test Message"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SmsSimulatorChannel maintains simulated SMS log")
    void testSmsSimulatorChannel() {
        SmsSimulatorChannel sms = new SmsSimulatorChannel();
        assertThat(sms.getChannelName()).isEqualTo("SMS");

        sms.send("EMP-001", "P1 Escalation", "Immediate action required");
        assertThat(sms.getSentRecords()).hasSize(1);
        assertThat(sms.getSentRecords().get(0).recipient()).isEqualTo("EMP-001");
        assertThat(sms.getSentRecords().get(0).message()).contains("Immediate action required");
    }

    @Test
    @DisplayName("EmailChannel gracefully handles missing JavaMailSender bean")
    void testEmailChannelMissingBean() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        EmailChannel emailChannel = new EmailChannel(provider, "no-reply@example.com");
        assertThatCode(() -> emailChannel.send("user@example.com", "Test", "Hello"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("NotificationRouter routes to multiple channels without throwing on channel error")
    void testNotificationRouterFaultIsolation() {
        SmsSimulatorChannel sms = new SmsSimulatorChannel();
        NotificationChannel faultyChannel = new NotificationChannel() {
            @Override
            public String getChannelName() { return "IN_APP"; }
            @Override
            public void send(String recipient, String subject, String message) {
                throw new RuntimeException("Simulated channel network failure");
            }
        };

        UserRepository userRepo = mock(UserRepository.class);
        User mockUser = mock(Employee.class);
        when(mockUser.getEmail()).thenReturn(new Email("user@domain.com"));
        when(userRepo.findById("EMP-001")).thenReturn(Optional.of(mockUser));

        NotificationRouter router = new NotificationRouter(List.of(faultyChannel, sms), userRepo);

        // Routing must catch the faulty channel failure and still dispatch to working channels
        assertThatCode(() -> router.route(new NotificationFactory.NotificationPayload("EMP-001", "Notice", "System maintenance")))
                .doesNotThrowAnyException();

        assertThat(sms.getSentRecords()).hasSize(1);
    }
}

