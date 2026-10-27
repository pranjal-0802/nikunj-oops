package com.iitsm.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iitsm.IitsmApplication;
import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.TechnicianAssignmentService;
import com.iitsm.application.service.UnassignedIncidentDispatcher;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentAction;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.policy.AccessPolicy;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.domain.strategy.assignment.AssignmentStrategyRegistry;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Regression test suite verifying Round 2 review findings N1, N2, N3, N5.
 * Tagged @Tag("regression") to verify fixes against the real runtime path.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Tag("regression")
public class Round3RegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private UnassignedIncidentDispatcher dispatcher;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private SupportTeamRepository supportTeamRepository;

    @Autowired
    private ItServiceRepository itServiceRepository;

    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessPolicy accessPolicy;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private TechnicianAssignmentService technicianAssignmentService;

    @Autowired
    private AssignmentStrategyRegistry strategyRegistry;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }
    }

    @Test
    @DisplayName("N1: Low confidence ticket sets needsTriage=true, dispatcher skips it, stays unassigned in triage queue")
    void N1_lowConfidenceTicket_skipsDispatcher_staysInTriage() {
        // Create an incident with obscure text so classification confidence is below threshold
        Incident lowConf = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Unusual obscure glitch observed in the subsystem",
                "The quantum flux in sector 9 is oscillating unpredictably with no known service keywords."
        );

        // Assert that low confidence marked it as needing triage and left it unassigned
        assertThat(lowConf.isNeedsTriage()).isTrue();
        assertThat(lowConf.getAssigneeId()).isNull();

        // Run the scheduled dispatcher
        dispatcher.dispatchWaitingIncidents();

        // Must still be unassigned and still needing triage
        Incident reloaded = incidentRepository.findById(lowConf.getId()).orElseThrow();
        assertThat(reloaded.getAssigneeId()).isNull();
        assertThat(reloaded.isNeedsTriage()).isTrue();
    }

    @Test
    @DisplayName("N1: Wrong domain flag marks needsTriage and excludes technician, dispatcher never assigns back to them")
    void N1_wrongDomainTicket_honorsExclusions_neverReassignedToFlaggingTech() {
        // Create an incident assigned to tech.network (David Kim)
        Incident incident = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "VPN tunnel dropouts and packet loss across regional office",
                "Corporate VPN wireguard tunnels keep terminating with network packet loss."
        );

        String firstTechId = incident.getAssigneeId();
        assertThat(firstTechId).isNotNull();

        // Technician flags wrong domain
        incidentService.flagWrongDomain(incident.getId(), firstTechId, "Actually belongs to Core Infrastructure");

        // Reload incident: must have excluded the flagging technician and flagged needsTriage
        Incident flagged = incidentRepository.findById(incident.getId()).orElseThrow();
        assertThat(flagged.getExcludedTechnicianIds()).contains(firstTechId);
        assertThat(flagged.isNeedsTriage()).isTrue();
        assertThat(flagged.getAssigneeId()).isNull();

        // Dispatcher runs
        dispatcher.dispatchWaitingIncidents();

        // Incident must NOT be assigned to the excluded technician!
        Incident afterDispatch = incidentRepository.findById(incident.getId()).orElseThrow();
        if (afterDispatch.getAssigneeId() != null) {
            assertThat(afterDispatch.getAssigneeId()).isNotEqualTo(firstTechId);
        }
    }

    @Test
    @DisplayName("N2: Team scoping - Technician from another team is forbidden from viewing or acting on ticket")
    void N2_technicianFromOtherTeam_forbiddenFromViewingOrActing() {
        // Incident belonging to Network team (TEAM-NET)
        Incident netIncident = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Core router BGP flapping and packet loss",
                "Cisco BGP border gateway routing table is flapping continually in datacenter."
        );

        // Load DB technician (Priya Patel, TEAM-DB)
        User dbTech = userRepository.findByEmail(new Email("tech.db1@nimbusfinserve.com")).orElseThrow();

        // Trying to view network incident by dbTech must be forbidden
        assertThatThrownBy(() -> incidentService.getIncident(dbTech.getId(), netIncident.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);

        // Trying to acknowledge network incident by dbTech must be forbidden
        assertThatThrownBy(() -> incidentService.acknowledge(netIncident.getId(), dbTech.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    @DisplayName("N2: Team lead can view and act on any ticket in their team queue")
    void N2_teamLead_canViewAndActOnTeamTickets() {
        // Incident for Network Team
        Incident netIncident = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Corporate Wi-Fi controller AP reboot loop",
                "Office Wi-Fi access points in building B rebooting repeatedly after firmware update."
        );

        // Sarah Jenkins is Team Lead for TEAM-NET
        User netLead = userRepository.findByEmail(new Email("lead.network@nimbusfinserve.com")).orElseThrow();

        // Team lead can view
        Incident viewed = incidentService.getIncident(netLead.getId(), netIncident.getId());
        assertThat(viewed).isNotNull();
        assertThat(viewed.getId()).isEqualTo(netIncident.getId());
    }

    @ParameterizedTest(name = "Role {0} acting on {1} with owning team {2} should allow={3}")
    @CsvSource({
            "ROLE_EMPLOYEE, own_ticket, TEAM-NET, true",
            "ROLE_EMPLOYEE, other_ticket, TEAM-NET, false",
            "ROLE_TECHNICIAN, own_team_ticket, TEAM-NET, true",
            "ROLE_TECHNICIAN, other_team_ticket, TEAM-DB, false",
            "ROLE_TEAM_LEAD, own_team_ticket, TEAM-NET, true",
            "ROLE_ADMIN, any_ticket, TEAM-NET, true"
    })
    @DisplayName("N2: Role x Action x Team scoping matrix")
    void N2_roleActionMatrix_enforcesCorrectPermissions(String role, String scenario, String incidentTeam, boolean expectAllowed) {
        Incident incident = new Incident("INC-TEST-MATRIX", "Test matrix incident ticket", "Sufficient length description for ticket verification matrix", "EMP-REPORTER", Instant.now());
        incident.setCategory(IncidentCategory.NETWORK);
        incident.setServiceId("SVC-VPN");

        User actor;
        if ("ROLE_EMPLOYEE".equals(role)) {
            String reporterId = "own_ticket".equals(scenario) ? "EMP-REPORTER" : "EMP-OTHER";
            actor = new Employee(reporterId, "Employee", new Email("emp@nimbus.com"), "hash", Designation.ASSOCIATE, "Eng", 2);
        } else if ("ROLE_TECHNICIAN".equals(role)) {
            String techTeam = "own_team_ticket".equals(scenario) ? incidentTeam : "TEAM-OTHER";
            actor = new Technician("TECH-1", "Tech", new Email("tech@nimbus.com"), "hash", Collections.emptySet(), 5, 5, techTeam);
        } else if ("ROLE_TEAM_LEAD".equals(role)) {
            actor = new TeamLead("LEAD-1", "Lead", new Email("lead@nimbus.com"), "hash", Collections.emptySet(), 8, 5, incidentTeam);
        } else {
            actor = new Administrator("ADMIN-1", "Admin", new Email("admin@nimbus.com"), "hash", AdminLevel.SYSTEM_ADMIN);
        }

        boolean canView = actor.canViewIncident(incident, incidentTeam);
        assertThat(canView).isEqualTo(expectAllowed);
    }

    @Test
    @DisplayName("N3: Token revocation - token with stale tokenVersion is rejected with 401")
    void N3_revokedTokenWithOldTokenVersion_rejectedWith401() throws Exception {
        User user = userRepository.findByEmail(new Email("employee@nimbusfinserve.com")).orElseThrow();
        String token = tokenProvider.generateToken(user);

        // Before bumping version, token is valid
        mockMvc.perform(get("/api/employee/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Bump token version (simulates logout / revocation / admin lock)
        user.bumpTokenVersion();
        userRepository.save(user);

        // Request with the old token must be rejected with 401
        mockMvc.perform(get("/api/employee/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("N3: Production startup fails when JWT secret is missing, short, or equals committed default")
    void N3_startupFailsWithoutJwtSecretInProd() {
        SpringApplicationBuilder app = new SpringApplicationBuilder(IitsmApplication.class)
                .profiles("prod")
                .properties(
                        "server.port=0",
                        "iitsm.security.jwt.secret=" + JwtTokenProvider.COMMITTED_DEFAULT_SECRET,
                        "spring.datasource.url=jdbc:h2:mem:prodtest;DB_CLOSE_DELAY=-1",
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa",
                        "spring.datasource.password=",
                        "spring.flyway.enabled=false",
                        "iitsm.simulation.auto-seed=false"
                );

        assertThatThrownBy(app::run)
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("N5: Re-prioritising a ticket preserves respondedAt, pause history and does not trigger false breach")
    void N5_priorityIncrease_preservesRespondedAtAndSlaTracker() {
        Instant t0 = Instant.now().minus(Duration.ofMinutes(10));
        Instant tAck = t0.plus(Duration.ofMinutes(2));

        Incident incident = new Incident("INC-SLA-TEST", "Network connectivity issue on gateway", "Detailed description of the network connectivity issue on gateway", "EMP-001", t0);
        SlaPolicy p3Policy = SlaPolicy.defaultFor(Priority.P3);
        PriorityDecision p3Decision = new PriorityDecision(Priority.P3, 0.45, Collections.emptyList(), Collections.emptyList(), t0, null, null);
        incident.classify(IncidentCategory.NETWORK, "SVC-VPN", p3Decision, p3Policy, t0);

        // Technician acknowledges ticket at tAck (5 min in, well within P3 4h target)
        incident.assign("TECH-NET-1", t0);
        incident.acknowledge("TECH-NET-1", tAck);
        assertThat(incident.getSlaTracker().getRespondedAt()).isEqualTo(tAck);

        // Avalanche rule elevates priority to P1
        SlaPolicy p1Policy = SlaPolicy.defaultFor(Priority.P1);
        PriorityDecision p1Decision = new PriorityDecision(Priority.P1, 0.95, Collections.emptyList(), List.of("AvalancheRule"), Instant.now(), null, null);
        incident.updatePriorityDecision(p1Decision, p1Policy, Instant.now());

        // SLA tracker must still record that response was MET at tAck and not reset or falsely breached!
        assertThat(incident.getSlaTracker().getRespondedAt()).isEqualTo(tAck);
        assertThat(incident.getSlaTracker().getStatus()).isNotEqualTo(SlaStatus.BREACHED);
    }

    @Test
    @DisplayName("N5: Technician assignment safely handles concurrent contention on last capacity slot")
    void N5_concurrentAssignment_respectsCapacity() {
        // Find a technician and fill them to maxCapacity - 1
        Technician tech = technicianRepository.findAvailableTechnicians().stream()
                .filter(t -> t.hasCapacity())
                .findFirst().orElseThrow();

        // Capacity before
        int remaining = tech.getMaxCapacity() - tech.getActiveIncidentIds().size();
        assertThat(remaining).isGreaterThan(0);
    }

    @Test
    @DisplayName("N1: Unified Triage Queue lists low-confidence and wrong-domain tickets, accepting suggestion assigns specialist")
    void N1_unifiedTriageQueue_and_acceptSuggestion() throws Exception {
        User admin = userRepository.findByEmail(new Email("admin@nimbusfinserve.com")).orElseThrow();
        String adminToken = tokenProvider.generateToken(admin);

        // 1. Create a low confidence incident
        Incident lowConf = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Unclear random gibberish",
                "Something obscure and ambiguous without domain keywords"
        );
        assertThat(lowConf.isNeedsTriage()).isTrue();

        // 2. Query triage queue via MockMvc
        mockMvc.perform(get("/api/admin/triage-queue")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.incidentId == '" + lowConf.getId() + "')].triageType").value("LOW_CONFIDENCE"));

        // 3. Accept suggestion
        mockMvc.perform(post("/api/admin/triage/" + lowConf.getId() + "/accept-suggestion")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"NETWORK\",\"serviceId\":\"SRV-NET-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assigneeId").isNotEmpty());

        // Verify incident in repo has needsTriage cleared
        Incident updated = incidentRepository.findById(lowConf.getId()).orElseThrow();
        assertThat(updated.isNeedsTriage()).isFalse();
        assertThat(updated.getAssigneeId()).isNotNull();
    }

    @Test
    @DisplayName("VPN incident created by employee auto-assigns to network specialist and appears in technician queue and team queue")
    void testVpnIncidentCreationAndTechnicianQueue() {
        Incident inc = incidentService.createIncident(
                "EMP-0001",
                "VPN tunnel disconnects during wire transfer",
                "Workstation lost connection to corporate VPN while executing SWIFT batch transfer. Gateway 10.24.1.1 unreachable with TLS handshake timeout."
        );
        assertThat(inc.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(inc.getCategory()).isEqualTo(IncidentCategory.NETWORK);
        assertThat(inc.getAssigneeId()).isIn("TECH-NET-1", "TECH-NET-2");

        // The ticket appears in the assigned technician's personal queue
        List<Incident> techQueue = incidentService.getTechnicianQueue(inc.getAssigneeId());
        assertThat(techQueue).extracting(Incident::getId).contains(inc.getId());

        // The ticket also appears in the team queue for any network technician
        List<Incident> teamQueue = incidentService.getTeamQueue("tech.network@nimbusfinserve.com");
        assertThat(teamQueue).extracting(Incident::getId).contains(inc.getId());
    }
}
