package com.iitsm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.DomainEventPublisher;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.repository.IncidentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for SlaService (SLA status progression, event publishing, and escalation invocation).
 */
class SlaServiceTest {

    private IncidentRepository incidentRepository;
    private DomainEventPublisher eventPublisher;
    private EscalationService escalationService;
    private Clock clock;
    private Instant now;
    private SlaService slaService;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        eventPublisher = mock(DomainEventPublisher.class);
        escalationService = mock(EscalationService.class);
        now = Instant.parse("2026-10-26T12:00:00Z");
        clock = Clock.fixed(now, ZoneOffset.UTC);

        slaService = new SlaService(incidentRepository, eventPublisher, escalationService, clock);
    }

    private Incident createIncident(Priority priority, Duration age) {
        Instant created = now.minus(age);
        Incident incident = new Incident("INC-1", "Test ticket", "Detailed incident description for SLA service testing.", "EMP-1", created);
        PriorityDecision decision = new PriorityDecision(priority, 0.50, List.of(), List.of(), created, null, null);
        incident.classify(IncidentCategory.APPLICATION, "SVC-APP", decision, SlaPolicy.defaultFor(priority), created);
        incident.assign("TECH-1", created);
        incident.acknowledge("TECH-1", created);
        return incident;
    }

    @Test
    @DisplayName("Detects 75% target window transition and publishes SlaWarning")
    void testAtRiskTransition() {
        // P1 has 30m resolution SLA. 23m = ~76%
        Incident incident = createIncident(Priority.P1, Duration.ofMinutes(23));
        when(incidentRepository.findById("INC-1")).thenReturn(Optional.of(incident));

        SlaEvaluationResult result = slaService.evaluateIncident("INC-1");

        assertThat(result.currentStatus()).isEqualTo(SlaStatus.AT_RISK);
        assertThat(result.statusChanged()).isTrue();
        assertThat(result.riskScore()).isGreaterThanOrEqualTo(0.75);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DomainEvent.SlaWarning.class);
    }

    @Test
    @DisplayName("Detects resolution deadline breach and publishes SlaViolated")
    void testBreachTransition() {
        // P1 has 30m resolution SLA. 35m = breached
        Incident incident = createIncident(Priority.P1, Duration.ofMinutes(35));
        when(incidentRepository.findById("INC-1")).thenReturn(Optional.of(incident));

        SlaEvaluationResult result = slaService.evaluateIncident("INC-1");

        assertThat(result.currentStatus()).isEqualTo(SlaStatus.BREACHED);
        assertThat(result.riskScore()).isEqualTo(1.0);

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DomainEvent.SlaViolated.class);
    }

    @Test
    @DisplayName("Pausing SLA freezes status during PENDING_INFO")
    void testPausedSlaDoesNotProgress() {
        Incident incident = createIncident(Priority.P1, Duration.ofMinutes(10));
        incident.getSlaTracker().pauseClock(now.minus(Duration.ofMinutes(5)));
        when(incidentRepository.findById("INC-1")).thenReturn(Optional.of(incident));

        SlaEvaluationResult result = slaService.evaluateIncident("INC-1");

        assertThat(incident.getSlaTracker().isPaused()).isTrue();
        assertThat(result.currentStatus()).isEqualTo(SlaStatus.ON_TRACK);
    }
}

