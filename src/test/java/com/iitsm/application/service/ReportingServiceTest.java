package com.iitsm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.web.dto.EscalationReportDto;
import com.iitsm.web.dto.MttrReportDto;
import com.iitsm.web.dto.ServiceAnalysisReportDto;
import com.iitsm.web.dto.SlaPerformanceReportDto;
import com.iitsm.web.dto.WorkloadReportDto;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ReportingService (workload, service analysis, SLA compliance, MTTR).
 */
class ReportingServiceTest {

    private IncidentRepository incidentRepository;
    private TechnicianRepository technicianRepository;
    private SupportTeamRepository supportTeamRepository;
    private ItServiceRepository itServiceRepository;
    private EscalationService escalationService;
    private ReportingService reportingService;

    private Instant now;

    @BeforeEach
    void setUp() {
        incidentRepository = mock(IncidentRepository.class);
        technicianRepository = mock(TechnicianRepository.class);
        supportTeamRepository = mock(SupportTeamRepository.class);
        itServiceRepository = mock(ItServiceRepository.class);
        escalationService = mock(EscalationService.class);

        now = Instant.parse("2026-10-26T15:00:00Z");
        reportingService = new ReportingService(
                incidentRepository, technicianRepository, supportTeamRepository,
                itServiceRepository, escalationService
        );
    }

    @Test
    @DisplayName("Generates accurate WorkloadReportDto across technicians and support teams")
    void testWorkloadReport() {
        Technician tech1 = new Technician(
                "TECH-1", "Alice", new Email("alice@test.com"), "hash",
                Set.of(new Skill("Java", IncidentCategory.APPLICATION)),
                5, 4, "TEAM-APP"
        );
        tech1.assignIncident("INC-1");
        tech1.assignIncident("INC-2"); // 2 active / 4 cap = 0.50

        Technician tech2 = new Technician(
                "TECH-2", "Bob", new Email("bob@test.com"), "hash",
                Set.of(new Skill("Postgres", IncidentCategory.DATABASE)),
                8, 5, "TEAM-DB"
        );
        tech2.assignIncident("INC-3"); // 1 active / 5 cap = 0.20

        SupportTeam teamApp = new SupportTeam("TEAM-APP", "Application Support", Set.of(IncidentCategory.APPLICATION), "TECH-1");
        SupportTeam teamDb = new SupportTeam("TEAM-DB", "Database Support", Set.of(IncidentCategory.DATABASE), "TECH-2");

        when(technicianRepository.findAll()).thenReturn(List.of(tech1, tech2));
        when(supportTeamRepository.findAll()).thenReturn(List.of(teamApp, teamDb));
        when(incidentRepository.findAll()).thenReturn(List.of());

        WorkloadReportDto report = reportingService.generateWorkloadReport();

        assertThat(report.technicianLoads()).hasSize(2);
        assertThat(report.technicianLoads().get(0).loadRatio()).isEqualTo(0.50);
        assertThat(report.technicianLoads().get(1).loadRatio()).isEqualTo(0.20);
        assertThat(report.averageLoadRatio()).isEqualTo(0.35);

        assertThat(report.teamLoads()).hasSize(2);
        assertThat(report.teamLoads().get(0).totalActiveIncidents()).isEqualTo(2);
        assertThat(report.teamLoads().get(1).totalActiveIncidents()).isEqualTo(1);
    }

    @Test
    @DisplayName("Generates accurate SLA performance metrics and compliance percentage")
    void testSlaPerformanceReport() {
        Incident metInc = new Incident("INC-MET", "Met ticket", "Detailed report description for met incident.", "EMP-1", now.minus(Duration.ofHours(2)));
        PriorityDecision d1 = new PriorityDecision(Priority.P2, 0.5, List.of(), List.of(), now, null, null);
        metInc.classify(IncidentCategory.APPLICATION, "SVC-1", d1, SlaPolicy.defaultFor(Priority.P2), now);
        metInc.assign("TECH-1", now.minus(Duration.ofHours(2)));
        metInc.acknowledge("TECH-1", now.minus(Duration.ofHours(2)));
        metInc.startInvestigation("TECH-1", now.minus(Duration.ofHours(2)));
        metInc.resolve(new Resolution("Fixed", "Restarted", "Monitoring", "APPLICATION", "TECH-1", now.minus(Duration.ofHours(1))), now.minus(Duration.ofHours(1)));

        Incident breachInc = new Incident("INC-BREACH", "Breached ticket", "Detailed report description for breached incident.", "EMP-2", now.minus(Duration.ofHours(5)));
        PriorityDecision d2 = new PriorityDecision(Priority.P2, 0.5, List.of(), List.of(), now, null, null);
        breachInc.classify(IncidentCategory.APPLICATION, "SVC-1", d2, SlaPolicy.defaultFor(Priority.P2), now);
        breachInc.assign("TECH-1", now);
        breachInc.updateSlaStatus(now); // 5 hours > 2 hours -> BREACHED

        when(incidentRepository.findAll()).thenReturn(List.of(metInc, breachInc));

        SlaPerformanceReportDto report = reportingService.generateSlaPerformanceReport();

        assertThat(report.totalEvaluated()).isEqualTo(2);
        assertThat(report.metCount()).isEqualTo(1);
        assertThat(report.breachedCount()).isEqualTo(1);
        assertThat(report.overallCompliancePercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Calculates MTTR and MTTResp accurately")
    void testMttrReport() {
        Instant created = now.minus(Duration.ofMinutes(120));
        Incident inc = new Incident("INC-1", "Resolved ticket", "Detailed report description for resolved incident.", "EMP-1", created);
        PriorityDecision decision = new PriorityDecision(Priority.P3, 0.4, List.of(), List.of(), created, null, null);
        inc.classify(IncidentCategory.APPLICATION, "SVC-1", decision, SlaPolicy.defaultFor(Priority.P3), created);
        inc.assign("TECH-1", created);

        // Responded in 20 minutes
        inc.acknowledge("TECH-1", created.plus(Duration.ofMinutes(20)));
        inc.startInvestigation("TECH-1", created.plus(Duration.ofMinutes(25)));

        // Resolved in 60 minutes
        inc.resolve(new Resolution("Bug fix", "Patched code", "Unit test", "APPLICATION", "TECH-1", created.plus(Duration.ofMinutes(60))), created.plus(Duration.ofMinutes(60)));

        when(incidentRepository.findAll()).thenReturn(List.of(inc));

        MttrReportDto report = reportingService.generateMttrReport();

        assertThat(report.overallMttrMinutes()).isEqualTo(60.0);
        assertThat(report.overallMttrespMinutes()).isEqualTo(20.0);
        assertThat(report.mttrByCategoryMinutes()).containsEntry("APPLICATION", 60.0);
    }

    @Test
    @DisplayName("Compiles escalation summary report correctly")
    void testEscalationReport() {
        EscalationRecord esc1 = new EscalationRecord("E-1", "INC-1", "SYSTEM", "TEAM_LEAD", "LEAD-1", "Warning", now);
        EscalationRecord esc2 = new EscalationRecord("E-2", "INC-2", "SYSTEM", "IT_MANAGER", "MGR-1", "Breach", now);

        when(escalationService.getAllEscalations()).thenReturn(List.of(esc1, esc2));

        EscalationReportDto report = reportingService.generateEscalationReport();

        assertThat(report.totalEscalations()).isEqualTo(2);
        assertThat(report.escalationsByTier()).containsEntry("TEAM_LEAD", 1L);
        assertThat(report.escalationsByTier()).containsEntry("IT_MANAGER", 1L);
    }
}
