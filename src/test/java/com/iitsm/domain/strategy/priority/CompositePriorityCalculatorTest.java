package com.iitsm.domain.strategy.priority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CompositePriorityCalculator (6-factor evaluation and policy override rules).
 */
class CompositePriorityCalculatorTest {

    private CompositePriorityCalculator calculator;
    private Instant now;

    @BeforeEach
    void setUp() {
        calculator = new CompositePriorityCalculator();
        now = Instant.parse("2026-10-26T10:00:00Z");
    }

    private Employee createEmployee(Designation designation) {
        return new Employee("EMP-1", "Jane Doe", new Email("jane.doe@nimbusfinserve.com"), "hash",
                designation, "Engineering", 5);
    }

    private ItService createService(BusinessCriticality criticality) {
        return new ItService("SRV-1", "Core Banking", "TEAM-1", criticality, 5000, Set.of("banking", "transfer"));
    }

    private PriorityDecision dummyDecision(Priority p) {
        return new PriorityDecision(p, 0.50, java.util.List.of(), java.util.List.of(), now, null, null);
    }

    @Test
    @DisplayName("Calculates composite score summing all 6 factors with correct weights")
    void testFactorBreakdown() {
        Employee vp = createEmployee(Designation.VP_C_LEVEL);
        ItService criticalService = createService(BusinessCriticality.TIER_4_MISSION_CRITICAL);

        Incident incident = new Incident(
                "INC-1",
                "Core banking wire transfer outage blocked",
                "Enterprise company-wide failure affecting all users",
                vp.getId(),
                now.minus(Duration.ofDays(4)) // 4 days old
        );
        incident.classify(IncidentCategory.APPLICATION, criticalService.getId(), dummyDecision(Priority.P1), SlaPolicy.defaultFor(Priority.P1), now);

        IncidentEvaluationContext context = new IncidentEvaluationContext(6, 2, now);
        PriorityDecision decision = calculator.calculate(incident, vp, criticalService, context, now);

        assertThat(decision).isNotNull();
        assertThat(decision.factorContributions()).hasSize(6);

        // Sum of weighted scores must equal composite score
        double expectedScore = decision.factorContributions().stream()
                .mapToDouble(f -> f.weightedScore())
                .sum();
        assertThat(decision.compositeScore()).isCloseTo(expectedScore, within(0.0001));
        assertThat(decision.compositeScore()).isGreaterThanOrEqualTo(0.75);
        assertThat(decision.priority()).isEqualTo(Priority.P1);
    }

    @Test
    @DisplayName("Maps low-impact intern ticket to P4 priority")
    void testLowPriorityMapping() {
        Employee intern = createEmployee(Designation.INTERN);
        ItService lowService = createService(BusinessCriticality.TIER_1_INTERNAL_SUPPORT);

        Incident incident = new Incident(
                "INC-LOW",
                "Minor cosmetic request for documentation template",
                "Requesting info regarding internal onboarding checklist",
                intern.getId(),
                now // Just created (< 4 hours)
        );
        incident.classify(IncidentCategory.APPLICATION, lowService.getId(), dummyDecision(Priority.P4), SlaPolicy.defaultFor(Priority.P4), now);

        IncidentEvaluationContext context = new IncidentEvaluationContext(0, 0, now);
        PriorityDecision decision = calculator.calculate(incident, intern, lowService, context, now);

        assertThat(decision.compositeScore()).isLessThan(0.35);
        assertThat(decision.priority()).isEqualTo(Priority.P4);
    }

    @Test
    @DisplayName("Override Rule 1: Security category with breach terms forces at least P2")
    void testSecurityBreachOverrideRule() {
        Employee associate = createEmployee(Designation.ASSOCIATE);
        ItService lowService = createService(BusinessCriticality.TIER_1_INTERNAL_SUPPORT);

        Incident incident = new Incident(
                "INC-SEC",
                "Suspicious ransomware malware breach detected",
                "Workstation locked by unauthorized payload exfiltration",
                associate.getId(),
                now
        );
        incident.classify(IncidentCategory.SECURITY, lowService.getId(), dummyDecision(Priority.P2), SlaPolicy.defaultFor(Priority.P2), now);

        IncidentEvaluationContext context = new IncidentEvaluationContext(0, 0, now);
        PriorityDecision decision = calculator.calculate(incident, associate, lowService, context, now);

        assertThat(decision.priority().getRank()).isLessThanOrEqualTo(Priority.P2.getRank());
        assertThat(decision.rulesApplied()).anyMatch(r -> r.contains("RULE_SECURITY_BREACH"));
    }

    @Test
    @DisplayName("Override Rule 2: Tier 4 Mission Critical Service with outage terms forces P1")
    void testCriticalServiceOutageOverrideRule() {
        Employee associate = createEmployee(Designation.ASSOCIATE);
        ItService tier4 = createService(BusinessCriticality.TIER_4_MISSION_CRITICAL);

        Incident incident = new Incident(
                "INC-OUTAGE",
                "Payment gateway down outage",
                "Service is completely stopped and unavailable",
                associate.getId(),
                now
        );
        incident.classify(IncidentCategory.APPLICATION, tier4.getId(), dummyDecision(Priority.P1), SlaPolicy.defaultFor(Priority.P1), now);

        IncidentEvaluationContext context = new IncidentEvaluationContext(0, 0, now);
        PriorityDecision decision = calculator.calculate(incident, associate, tier4, context, now);

        assertThat(decision.priority()).isEqualTo(Priority.P1);
        assertThat(decision.rulesApplied()).anyMatch(r -> r.contains("RULE_CRITICAL_SERVICE_OUTAGE"));
    }

    @Test
    @DisplayName("Override Rule 3: 5+ linked duplicate tickets elevates priority by one level")
    void testDuplicateAvalancheOverrideRule() {
        Employee associate = createEmployee(Designation.ASSOCIATE);
        ItService tier2 = createService(BusinessCriticality.TIER_2_DEPARTMENTAL);

        Incident incident = new Incident(
                "INC-DUP",
                "General network latency on secondary Wi-Fi AP",
                "Slow speeds reported across floor 2",
                associate.getId(),
                now
        );
        incident.classify(IncidentCategory.NETWORK, tier2.getId(), dummyDecision(Priority.P3), SlaPolicy.defaultFor(Priority.P3), now);

        // 6 duplicates linked
        IncidentEvaluationContext context = new IncidentEvaluationContext(0, 6, now);
        PriorityDecision decision = calculator.calculate(incident, associate, tier2, context, now);

        assertThat(decision.rulesApplied()).anyMatch(r -> r.contains("RULE_DUPLICATE_AVALANCHE"));
    }
}

