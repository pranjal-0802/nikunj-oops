package com.iitsm.simulation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end integration tests for all 5 simulation scenarios (S1–S5).
 */
@SpringBootTest
@ActiveProfiles("dev")
class SimulationScenariosTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    @Autowired
    private com.iitsm.domain.repository.UserRepository userRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }
    }

    @Test
    @DisplayName("Scenario S1: Happy Path Lifecycle (Report -> Acknowledge -> Investigate -> Resolve -> Verify -> Close)")
    void testScenario1HappyPath() {
        SimulationResult result = simulationService.runScenario("S1");

        assertThat(result.success()).isTrue();
        assertThat(result.scenarioId()).isEqualTo("S1");
        assertThat(result.stepLogs()).isNotEmpty();
    }

    @Test
    @DisplayName("Scenario S2: P1 Critical Outage & Multi-Tier Escalations (75%, 90%, 100% breach)")
    void testScenario2CriticalOutageAndEscalation() {
        SimulationResult result = simulationService.runScenario("S2");

        assertThat(result.success()).isTrue();
        assertThat(result.scenarioId()).isEqualTo("S2");
        assertThat(result.stepLogs()).anyMatch(log -> log.contains("Escalated to:"));
    }

    @Test
    @DisplayName("Scenario S3: Wrong-Domain Flagging & Triage Administrative Reassignment")
    void testScenario3WrongDomainAndTriage() {
        SimulationResult result = simulationService.runScenario("S3");

        assertThat(result.success()).isTrue();
        assertThat(result.scenarioId()).isEqualTo("S3");
        assertThat(result.stepLogs()).anyMatch(log -> log.contains("reclassified ticket to DATABASE"));
    }

    @Test
    @DisplayName("Scenario S4: Duplicate Ticket Storm & Avalanche Priority Escalation")
    void testScenario4DuplicateStormAvalanche() {
        SimulationResult result = simulationService.runScenario("S4");

        assertThat(result.success()).isTrue();
        assertThat(result.scenarioId()).isEqualTo("S4");
        assertThat(result.stepLogs()).anyMatch(log -> log.contains("duplicates linked"));
    }

    @Test
    @DisplayName("Scenario S5: Rejection Loop & SLA Clock Pausing during PENDING_INFO")
    void testScenario5RejectionLoopAndSlaPausing() {
        SimulationResult result = simulationService.runScenario("S5");

        assertThat(result.success()).isTrue();
        assertThat(result.scenarioId()).isEqualTo("S5");
        assertThat(result.stepLogs()).anyMatch(log -> log.contains("Reopen Count: 1"));
    }

    @Test
    @DisplayName("Controls simulation clock speed, fast-forwarding, and status inspection")
    void testSimulationClockControls() {
        simulationService.setSpeedMultiplier(60L);
        SimulationStatusDto status = simulationService.getClockStatus();
        assertThat(status.speedMultiplier()).isEqualTo(60L);
        assertThat(status.isAccelerated()).isTrue();

        simulationService.advanceClock(Duration.ofHours(5));
        SimulationStatusDto advanced = simulationService.getClockStatus();
        assertThat(advanced.virtualTime()).isAfter(status.virtualTime());

        simulationService.resetClock();
        SimulationStatusDto reset = simulationService.getClockStatus();
        assertThat(reset.speedMultiplier()).isEqualTo(1L);
    }
}

