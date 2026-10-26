package com.iitsm.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.iitsm.application.service.IncidentService;
import java.util.ConcurrentModificationException;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.domain.strategy.classification.ServiceMatcher;
import com.iitsm.domain.strategy.priority.IncidentEvaluationContext;
import com.iitsm.domain.strategy.priority.ReporterHistoryFactor;
import com.iitsm.infrastructure.notification.EmailChannel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Regression test suite reproducing confirmed correctness & behavioral findings F1–F8 and A4.
 * All tests are tagged with @Tag("regression") and must fail against the current unpatched code.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Tag("regression")
public class CorrectnessRegressionTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private ItServiceRepository itServiceRepository;

    @Autowired
    private EmailChannel emailChannel;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    @Autowired
    private com.iitsm.domain.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM technician_active_incidents");
        greenMail.reset();
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM technician_active_incidents");
    }

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("no-reply@nimbusfinserve.com", "password"))
            .withPerMethodLifecycle(false);

    @Test
    @DisplayName("F1: When all technicians are at capacity, ticket creation must leave incident CLASSIFIED unassigned instead of failing")
    void F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned() {
        // Finding F1: CandidateFilter drops full techs; IncidentService re-picks all techs and throws TechnicianOverloadedException
        List<Technician> allTechs = technicianRepository.findAll();
        for (Technician tech : allTechs) {
            new java.util.HashSet<>(tech.getActiveIncidentIds()).forEach(tech::releaseIncident);
            for (int i = 0; i < tech.getMaxCapacity(); i++) {
                tech.assignIncident("DUMMY-SLOT-" + i);
            }
            technicianRepository.save(tech);
        }

        try {
            // Creating a ticket when no technician has capacity must succeed, remaining CLASSIFIED and unassigned
            Incident created = incidentService.createIncident(
                    "employee@nimbusfinserve.com",
                    "Core banking latency under peak load",
                    "Database queries timing out during settlement processing"
            );

            assertThat(created).isNotNull();
            assertThat(created.getStatus())
                    .as("Ticket must be saved in CLASSIFIED state when no technicians are available")
                    .isEqualTo(IncidentStatus.CLASSIFIED);
            assertThat(created.getAssigneeId())
                    .as("Ticket must remain unassigned until capacity is freed")
                    .isNull();
        } finally {
            // Reset technician capacities
            for (Technician tech : technicianRepository.findAll()) {
                new java.util.HashSet<>(tech.getActiveIncidentIds()).forEach(tech::releaseIncident);
                tech.updateAvailability(com.iitsm.domain.model.AvailabilityStatus.ONLINE);
                technicianRepository.save(tech);
            }
        }
    }

    @Test
    @DisplayName("F2: Vague ticket text with no matching keywords must return Optional.empty(), not an arbitrary first service")
    void F2_unmatchedServiceDoesNotPickArbitraryService() {
        // Finding F2: ServiceMatcher.findBestMatch ends with availableServices.stream().findFirst()
        ServiceMatcher matcher = new ServiceMatcher();
        List<ItService> services = itServiceRepository.findAll();
        assertThat(services).isNotEmpty();

        Optional<ItService> match = matcher.findBestMatch("not working", "system is not working properly at all", services);

        assertThat(match)
                .as("Vague ticket with zero keyword overlap must return empty Optional, not an arbitrary first service")
                .isEmpty();
    }

    @Test
    @DisplayName("F3: Linking 5 duplicates must elevate the parent ticket's priority to P1 via RULE_DUPLICATE_AVALANCHE")
    void F3_linkingFiveDuplicatesElevatesParentPriorityToP1ViaAvalanche() {
        // Finding F3: createIncident passes linkedDuplicates size of new ticket (1), parent is never recalculated
        String uniqueMarker = "DuplicateAvalancheCluster-" + System.currentTimeMillis();
        String title = "Flickering monitor display " + uniqueMarker;
        String desc = "Monitor briefly flickers every 30 minutes in office 4B " + uniqueMarker;

        // 1. Create root parent ticket (initially normal priority, not P1)
        Incident parent = incidentService.createIncident("employee@nimbusfinserve.com", title, desc);
        assertThat(parent.getPriority())
                .as("Initial parent ticket priority must not be P1")
                .isNotEqualTo(Priority.P1);

        // 2. Create 5 duplicate tickets within the duplicate recency window
        for (int i = 0; i < 5; i++) {
            incidentService.createIncident("employee@nimbusfinserve.com", title, desc);
        }

        // Parent priority should now have been recomputed with cluster size >= 5, elevating to P1 via RULE_DUPLICATE_AVALANCHE
        Incident updatedParent = incidentRepository.findById(parent.getId()).orElseThrow();
        assertThat(updatedParent.getPriority())
                .as("Parent ticket priority must elevate to P1 when 5 duplicates are correlated to its cluster")
                .isEqualTo(Priority.P1);
    }

    @Test
    @DisplayName("F4: Duplicate incidents must NOT consume technician capacity or be assigned independently")
    void F4_duplicateIncidentDoesNotConsumeTechnicianCapacity() {
        // Finding F4: Duplicates are assigned independently and consume technician capacity slots
        String uniqueMarker = "CapacityTestCluster-" + System.currentTimeMillis();
        String title = "Workstation VPN Failure " + uniqueMarker;
        String desc = "VPN client cannot establish tunnel to primary auth gateway " + uniqueMarker;

        Incident parent = incidentService.createIncident("employee@nimbusfinserve.com", title, desc);
        assertThat(parent.getAssigneeId()).isNotNull();

        int totalActiveBefore = technicianRepository.findAll().stream()
                .mapToInt(t -> t.getActiveIncidentIds().size()).sum();

        // Create duplicate incident
        Incident duplicate = incidentService.createIncident("employee@nimbusfinserve.com", title, desc);

        int totalActiveAfter = technicianRepository.findAll().stream()
                .mapToInt(t -> t.getActiveIncidentIds().size()).sum();

        assertThat(totalActiveAfter)
                .as("Duplicate incident must not increment technician active capacity workload across the roster")
                .isEqualTo(totalActiveBefore);
    }

    @Test
    @DisplayName("F5: Low-confidence classification must route ticket to triage queue unassigned")
    void F5_lowConfidenceClassificationLeavesIncidentInTriageQueue() {
        // Finding F5: Classification confidence is computed but never checked; low-confidence tickets are assigned immediately
        String ambiguousTitle = "Something strange happened";
        String ambiguousDesc = "The screen turned blue and then rebooted unexpectedly without any logs or error codes.";

        Incident created = incidentService.createIncident("employee@nimbusfinserve.com", ambiguousTitle, ambiguousDesc);

        // When classifier confidence is below min-confidence threshold (0.60), ticket should remain unassigned in CLASSIFIED state
        assertThat(created.getAssigneeId())
                .as("Low-confidence incident must not be auto-assigned; must be routed to admin triage queue")
                .isNull();
        assertThat(created.getStatus())
                .as("Low-confidence incident must stay in CLASSIFIED state")
                .isEqualTo(IncidentStatus.CLASSIFIED);
    }

    @Test
    @DisplayName("F6: Reporter history factor must evaluate only recent tickets on same service within 30 days")
    void F6_reporterHistoryCountsOnlyRecentUnresolvedTicketsOnService() {
        // Finding F6: IncidentService line 161 does findByReporterId().size(), boosting chronic count from all-time history across all services
        String baselineReporter = "employee.baseline." + System.currentTimeMillis() + "@nimbusfinserve.com";
        userRepository.save(new com.iitsm.domain.model.Employee(
                "EMP-BASE-" + System.currentTimeMillis(),
                "Baseline Reporter",
                new com.iitsm.domain.model.Email(baselineReporter),
                "hash",
                com.iitsm.domain.model.Designation.ASSOCIATE,
                "Operations",
                1
        ));

        String chronicReporter = "employee.chronic." + System.currentTimeMillis() + "@nimbusfinserve.com";
        userRepository.save(new com.iitsm.domain.model.Employee(
                "EMP-CHRONIC-" + System.currentTimeMillis(),
                "Chronic Reporter",
                new com.iitsm.domain.model.Email(chronicReporter),
                "hash",
                com.iitsm.domain.model.Designation.ASSOCIATE,
                "Operations",
                1
        ));

        // Create 5 old tickets on hardware service for chronic reporter
        for (int i = 0; i < 5; i++) {
            incidentService.createIncident(
                    chronicReporter,
                    "Old hardware ticket " + i + " " + System.currentTimeMillis(),
                    "Keyboard replacement needed for developer desk on floor 2"
            );
        }

        // Now both reporters file a ticket on a brand new service (Core Banking) where neither has any prior tickets
        Incident baselineInc = incidentService.createIncident(
                baselineReporter,
                "Core banking latency under peak load " + System.currentTimeMillis(),
                "Database queries timing out during settlement processing on core banking ledger"
        );
        Incident chronicInc = incidentService.createIncident(
                chronicReporter,
                "Core banking latency under peak load " + System.currentTimeMillis(),
                "Database queries timing out during settlement processing on core banking ledger"
        );

        // In a correct implementation, history factor only considers recent tickets on the SAME service.
        // Thus both incidents should receive the exact same composite score.
        // In the unpatched code, chronicInc is penalized +0.08 based on unrelated hardware tickets.
        assertThat(chronicInc.getPriorityDecision().compositeScore())
                .as("Old tickets on other services must not increase priority composite score for this service")
                .isEqualTo(baselineInc.getPriorityDecision().compositeScore());
    }

    @Test
    @DisplayName("F7: Concurrent ticket creation must not overload a technician with only one slot remaining")
    void F7_concurrentTicketCreationDoesNotOverloadTechnician() throws Exception {
        // Finding F7: Technician lacks @Version, allowing race condition in concurrent assignment
        // Fill all technicians except one with 1 slot
        List<Technician> allTechs = technicianRepository.findAll();
        for (Technician t : allTechs) {
            new java.util.HashSet<>(t.getActiveIncidentIds()).forEach(t::releaseIncident);
            for (int i = 0; i < t.getMaxCapacity(); i++) {
                t.assignIncident("DUMMY-FILL-" + i);
            }
            technicianRepository.save(t);
        }

        // Free exactly 1 slot on the first tech
        Technician targetTech = technicianRepository.findById(allTechs.getFirst().getId()).orElseThrow();
        targetTech.releaseIncident("DUMMY-FILL-0");
        technicianRepository.save(targetTech);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> threadError = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.List<Incident> createdList = new java.util.concurrent.CopyOnWriteArrayList<>();

        Runnable task = () -> {
            try {
                latch.await();
                Incident inc = incidentService.createIncident(
                        "employee@nimbusfinserve.com",
                        "Concurrent High Priority Issue " + java.util.UUID.randomUUID(),
                        "Critical production gateway connection failure requiring immediate dispatch"
                );
                createdList.add(inc);
            } catch (Throwable t) {
                threadError.set(t);
            }
        };

        try {
            executor.submit(task);
            executor.submit(task);
            latch.countDown();
            executor.shutdown();
            executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

            // Under unpatched code, either an exception is thrown (e.g. TechnicianOverloadedException)
            // or the technician is over-assigned (> maxCapacity).
            // A correct implementation never throws an exception to the user and leaves the 2nd incident unassigned.
            assertThat(threadError.get())
                    .as("Concurrent ticket creation must never throw an error to the user")
                    .isNull();
            assertThat(createdList).hasSize(2);

            long assignedCount = createdList.stream().filter(inc -> inc.getAssigneeId() != null).count();
            long unassignedCount = createdList.stream().filter(inc -> inc.getAssigneeId() == null).count();

            assertThat(assignedCount)
                    .as("Exactly one incident can claim the single remaining slot")
                    .isEqualTo(1);
            assertThat(unassignedCount)
                    .as("The competing incident must gracefully remain unassigned in CLASSIFIED state")
                    .isEqualTo(1);
        } finally {
            // Reset
            for (Technician t : technicianRepository.findAll()) {
                new java.util.HashSet<>(t.getActiveIncidentIds()).forEach(t::releaseIncident);
                t.updateAvailability(com.iitsm.domain.model.AvailabilityStatus.ONLINE);
                technicianRepository.save(t);
            }
        }
    }

    @Test
    @DisplayName("A4: Optimistic lock failure on concurrent update must throw ConcurrentModificationException, not PersistenceFailureException")
    void A4_concurrentAcknowledgmentThrowsConcurrentModificationException() {
        // Finding A4: IncidentRepositoryAdapter catches DataAccessException and throws generic PersistenceFailureException
        Incident inc = incidentService.createIncident(
                "employee@nimbusfinserve.com",
                "Core network switch port malfunction in rack 2",
                "Network packet loss and timeout on internal switch VLAN 10 gateway"
        );

        // Stale incident with outdated version
        Incident copy1 = incidentRepository.findById(inc.getId()).orElseThrow();
        Incident copy2 = incidentRepository.findById(inc.getId()).orElseThrow();

        // Copy 1 updates and persists, incrementing the version in database
        copy1.acknowledge("tech.network@nimbusfinserve.com", Instant.now());
        incidentRepository.save(copy1);

        // Copy 2 attempts save with stale version
        copy2.acknowledge("tech.network@nimbusfinserve.com", Instant.now());
        assertThatThrownBy(() -> incidentRepository.save(copy2))
                .as("Stale optimistic lock update must throw ConcurrentModificationException for HTTP 409 mapping")
                .isInstanceOf(ConcurrentModificationException.class);
    }

    @Test
    @DisplayName("F8: EmailChannel must send real SMTP messages via configured JavaMailSender")
    void F8_emailChannelSendsRealSmtpMessage() {
        // Finding F8: JavaMailSender bean is unavailable in default run, so EmailChannel only logs simulation text
        emailChannel.send("tester@nimbusfinserve.com", "Incident Assigned: INC-100", "You have been assigned ticket INC-100.");

        // GreenMail should receive 1 message
        assertThat(greenMail.getReceivedMessages())
                .as("EmailChannel must send an actual SMTP message when email notification is triggered")
                .hasSize(1);
    }
}
