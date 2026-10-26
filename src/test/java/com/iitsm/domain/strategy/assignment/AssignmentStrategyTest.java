package com.iitsm.domain.strategy.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.Technician;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for all assignment strategies, candidate filtering, and Open/Closed Principle extensibility.
 */
class AssignmentStrategyTest {

    private Instant now;
    private Technician techSenior;
    private Technician techJuniorLoaded;
    private Technician techNetworkSpecialist;
    private Incident sampleIncident;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-26T10:00:00Z");

        // Senior generalist: 10 yrs exp, 1 active / 8 max (load: 0.125), broad skills
        techSenior = new Technician(
                "TECH-SENIOR", "Alice Senior", new Email("alice@test.com"), "hash",
                Set.of(new Skill("Java", IncidentCategory.APPLICATION), new Skill("Spring Boot", IncidentCategory.APPLICATION)),
                10, 8, "TEAM-APP"
        );
        techSenior.assignIncident("INC-EXISTING-1");

        // Junior loaded: 2 yrs exp, 4 active / 5 max (load: 0.80)
        techJuniorLoaded = new Technician(
                "TECH-JUNIOR", "Bob Junior", new Email("bob@test.com"), "hash",
                Set.of(new Skill("Java", IncidentCategory.APPLICATION)),
                2, 5, "TEAM-APP"
        );
        techJuniorLoaded.assignIncident("INC-1");
        techJuniorLoaded.assignIncident("INC-2");
        techJuniorLoaded.assignIncident("INC-3");
        techJuniorLoaded.assignIncident("INC-4");

        // Network specialist: 6 yrs exp, 0 active / 6 max (load: 0.0), network skills
        techNetworkSpecialist = new Technician(
                "TECH-NET", "Charlie Net", new Email("charlie@test.com"), "hash",
                Set.of(new Skill("Cisco Routing", IncidentCategory.NETWORK), new Skill("BGP", IncidentCategory.NETWORK)),
                6, 6, "TEAM-NET"
        );

        sampleIncident = new Incident("INC-100", "BGP route flap causing network drops", "Packet loss across Cisco router", "EMP-1", now);
        PriorityDecision decision = new PriorityDecision(Priority.P2, 0.60, List.of(), List.of(), now, null, null);
        sampleIncident.classify(IncidentCategory.NETWORK, "SRV-NET", decision, SlaPolicy.defaultFor(Priority.P2), now);
    }

    @Test
    @DisplayName("SkillBasedStrategy selects technician with matching skills")
    void testSkillBasedStrategy() {
        SkillBasedStrategy strategy = new SkillBasedStrategy();
        List<Technician> candidates = List.of(techSenior, techJuniorLoaded, techNetworkSpecialist);

        AssignmentRecommendation rec = strategy.recommend(sampleIncident, candidates);
        assertThat(rec.technician()).isEqualTo(techNetworkSpecialist);
        assertThat(rec.strategyName()).isEqualTo("SKILL_BASED");
    }

    @Test
    @DisplayName("LoadBasedStrategy selects technician with lowest active load ratio")
    void testLoadBasedStrategy() {
        LoadBasedStrategy strategy = new LoadBasedStrategy();
        List<Technician> candidates = List.of(techSenior, techJuniorLoaded, techNetworkSpecialist);

        AssignmentRecommendation rec = strategy.recommend(sampleIncident, candidates);
        // techNetworkSpecialist has load 0/6 = 0.0, techSenior has 1/8 = 0.125
        assertThat(rec.technician()).isEqualTo(techNetworkSpecialist);
        assertThat(rec.strategyName()).isEqualTo("LOAD_BASED");
    }

    @Test
    @DisplayName("ExperienceBasedStrategy selects technician with highest domain experience")
    void testExperienceBasedStrategy() {
        ExperienceBasedStrategy strategy = new ExperienceBasedStrategy();
        List<Technician> candidates = List.of(techSenior, techJuniorLoaded, techNetworkSpecialist);

        AssignmentRecommendation rec = strategy.recommend(sampleIncident, candidates);
        assertThat(rec.technician()).isEqualTo(techSenior); // 10 years experience
        assertThat(rec.strategyName()).isEqualTo("EXPERIENCE_BASED");
    }

    @Test
    @DisplayName("RoundRobinStrategy cyclically rotates assignments")
    void testRoundRobinStrategy() {
        RoundRobinStrategy strategy = new RoundRobinStrategy();
        List<Technician> candidates = List.of(techSenior, techNetworkSpecialist);

        AssignmentRecommendation rec1 = strategy.recommend(sampleIncident, candidates);
        AssignmentRecommendation rec2 = strategy.recommend(sampleIncident, candidates);
        AssignmentRecommendation rec3 = strategy.recommend(sampleIncident, candidates);

        assertThat(rec1.technician()).isNotEqualTo(rec2.technician());
        assertThat(rec3.technician()).isEqualTo(rec1.technician());
    }

    @Test
    @DisplayName("CandidateFilter excludes overloaded technician and previously rejecting technician")
    void testCandidateFilter() {
        CandidateFilter filter = new CandidateFilter();

        // Fill techJuniorLoaded to max capacity
        techJuniorLoaded.assignIncident("INC-5"); // 5/5 -> full
        assertThat(techJuniorLoaded.hasCapacity()).isFalse();

        List<Technician> filtered = filter.filterCandidates(
                List.of(techSenior, techJuniorLoaded, techNetworkSpecialist),
                "TEAM-APP",
                Set.of(techSenior.getId()) // Exclude techSenior
        );

        // techJuniorLoaded has no capacity, techSenior is excluded -> falls back to cross-team techNetworkSpecialist
        assertThat(filtered).containsExactly(techNetworkSpecialist);
    }

    @Test
    @DisplayName("Open/Closed Principle: Can register and route with custom strategy without modifying core classes")
    void testOpenClosedPrincipleCustomStrategy() {
        AssignmentStrategyRegistry registry = new AssignmentStrategyRegistry();

        // Define a custom strategy dynamically
        AssignmentStrategy customStrategy = new AssignmentStrategy() {
            @Override
            public AssignmentRecommendation recommend(Incident incident, List<Technician> candidates) {
                // Always picks last candidate in list
                Technician chosen = candidates.get(candidates.size() - 1);
                return new AssignmentRecommendation(chosen, 0.99, "CUSTOM_TEST_STRATEGY", "Custom test assignment");
            }

            @Override
            public String getStrategyName() {
                return "CUSTOM_TEST_STRATEGY";
            }

            @Override
            public String getDescription() {
                return "Custom strategy demonstrating Open/Closed Principle";
            }
        };

        registry.register(customStrategy);
        assertThat(registry.hasStrategy("CUSTOM_TEST_STRATEGY")).isTrue();

        registry.setActiveStrategy("CUSTOM_TEST_STRATEGY");
        AssignmentStrategy active = registry.getActiveStrategy();
        assertThat(active.getStrategyName()).isEqualTo("CUSTOM_TEST_STRATEGY");

        List<Technician> candidates = List.of(techSenior, techNetworkSpecialist);
        AssignmentRecommendation rec = active.recommend(sampleIncident, candidates);
        assertThat(rec.technician()).isEqualTo(techNetworkSpecialist);
    }
}

