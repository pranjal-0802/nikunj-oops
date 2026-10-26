package com.iitsm.domain.chain.escalation;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Multi-Tier Escalation Chain of Responsibility.
 */
class EscalationChainTest {

    private EscalationHandler chain;
    private Instant now;
    private EscalationContext context;

    @BeforeEach
    void setUp() {
        chain = EscalationChainBuilder.buildDefaultChain();
        now = Instant.parse("2026-10-26T14:00:00Z");
        context = new EscalationContext(
                now,
                "LEAD-NET-1",
                "ADMIN-SYS-1",
                "ADMIN-MGR-1",
                true // mission critical
        );
    }

    private Incident createIncident(Priority priority, String serviceId) {
        Incident incident = new Incident("INC-1", "Test outage", "Detailed description of the incident for escalation chain testing.", "EMP-1", now.minus(Duration.ofMinutes(40)));
        PriorityDecision decision = new PriorityDecision(priority, 0.80, List.of(), List.of(), now, null, null);
        incident.classify(IncidentCategory.NETWORK, serviceId, decision, SlaPolicy.defaultFor(priority), now);
        incident.assign("TECH-NET-1", now);
        return incident;
    }

    @Test
    @DisplayName("Tier 4: Catastrophic P1 breach on Mission-Critical service escalates to IT Manager")
    void testTier4ItManagerEscalation() {
        Incident incident = createIncident(Priority.P1, "SVC-CORE-DB");
        // 40 mins elapsed > 30 min P1 resolution SLA -> BREACHED
        incident.updateSlaStatus(now);

        Optional<EscalationAction> action = chain.handle(incident, context);

        assertThat(action).isPresent();
        assertThat(action.get().tier()).isEqualTo(4);
        assertThat(action.get().targetRole()).isEqualTo("IT_MANAGER");
        assertThat(action.get().targetUserId()).isEqualTo("ADMIN-MGR-1");
    }

    @Test
    @DisplayName("Tier 3: P2 breach on non-critical service escalates to System Admin")
    void testTier3SystemAdminEscalation() {
        EscalationContext nonCriticalContext = new EscalationContext(
                now, "LEAD-APP-1", "ADMIN-SYS-1", "ADMIN-MGR-1", false
        );
        Incident incident = new Incident("INC-2", "P2 app crash", "Detailed description of the incident for escalation chain testing.", "EMP-1", now.minus(Duration.ofHours(3)));
        PriorityDecision decision = new PriorityDecision(Priority.P2, 0.60, List.of(), List.of(), now, null, null);
        incident.classify(IncidentCategory.APPLICATION, "SVC-APP", decision, SlaPolicy.defaultFor(Priority.P2), now);
        incident.assign("TECH-APP-1", now);

        // 3 hours elapsed > 2 hour P2 resolution SLA -> BREACHED
        incident.updateSlaStatus(now);

        Optional<EscalationAction> action = chain.handle(incident, nonCriticalContext);

        assertThat(action).isPresent();
        assertThat(action.get().tier()).isEqualTo(3);
        assertThat(action.get().targetRole()).isEqualTo("ADMINISTRATOR");
        assertThat(action.get().targetUserId()).isEqualTo("ADMIN-SYS-1");
    }

    @Test
    @DisplayName("Tier 2: Incident at 90% SLA window escalates to Team Lead")
    void testTier2TeamLeadEscalation() {
        // P2 has 2h resolution (120m). 92% of 120m = 110m
        Incident incident = new Incident("INC-3", "P2 near warning", "Detailed description of the incident for escalation chain testing.", "EMP-1", now.minus(Duration.ofMinutes(110)));
        PriorityDecision decision = new PriorityDecision(Priority.P2, 0.60, List.of(), List.of(), now, null, null);
        incident.classify(IncidentCategory.NETWORK, "SVC-NET", decision, SlaPolicy.defaultFor(Priority.P2), now);
        incident.assign("TECH-NET-1", now);
        incident.acknowledge("TECH-NET-1", now.minus(Duration.ofMinutes(105)));

        incident.updateSlaStatus(now);

        Optional<EscalationAction> action = chain.handle(incident, context);

        assertThat(action).isPresent();
        assertThat(action.get().tier()).isEqualTo(2);
        assertThat(action.get().targetRole()).isEqualTo("TEAM_LEAD");
        assertThat(action.get().targetUserId()).isEqualTo("LEAD-NET-1");
    }

    @Test
    @DisplayName("Tier 1: Incident at 75% SLA window triggers Technician Reminder")
    void testTier1TechnicianReminder() {
        // P3 has 8h resolution (480m). 78% of 480m = 375m
        Incident incident = new Incident("INC-4", "P3 near at-risk", "Detailed description of the incident for escalation chain testing.", "EMP-1", now.minus(Duration.ofMinutes(375)));
        PriorityDecision decision = new PriorityDecision(Priority.P3, 0.40, List.of(), List.of(), now, null, null);
        incident.classify(IncidentCategory.NETWORK, "SVC-NET", decision, SlaPolicy.defaultFor(Priority.P3), now);
        incident.assign("TECH-NET-1", now);
        incident.acknowledge("TECH-NET-1", now.minus(Duration.ofMinutes(370)));

        incident.updateSlaStatus(now);

        Optional<EscalationAction> action = chain.handle(incident, context);

        assertThat(action).isPresent();
        assertThat(action.get().tier()).isEqualTo(1);
        assertThat(action.get().targetRole()).isEqualTo("TECHNICIAN");
        assertThat(action.get().targetUserId()).isEqualTo("TECH-NET-1");
    }

    @Test
    @DisplayName("Healthy ticket on track produces no escalation action")
    void testNoEscalationWhenOnTrack() {
        Incident incident = new Incident("INC-5", "Fresh ticket", "Detailed description of the incident for escalation chain testing.", "EMP-1", now.minus(Duration.ofMinutes(5)));
        PriorityDecision decision = new PriorityDecision(Priority.P3, 0.40, List.of(), List.of(), now, null, null);
        incident.classify(IncidentCategory.NETWORK, "SVC-NET", decision, SlaPolicy.defaultFor(Priority.P3), now);
        incident.assign("TECH-NET-1", now);

        incident.updateSlaStatus(now);

        Optional<EscalationAction> action = chain.handle(incident, context);
        assertThat(action).isEmpty();
    }
}

