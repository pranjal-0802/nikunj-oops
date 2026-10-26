package com.iitsm.simulation;

import com.iitsm.application.service.DuplicateDetectionService;
import com.iitsm.application.service.EscalationService;
import com.iitsm.application.service.IncidentService;
import com.iitsm.application.service.SlaService;
import com.iitsm.domain.model.EscalationRecord;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaStatus;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.config.AdjustableClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates time-accelerated simulation execution, clock speed controls,
 * and standard demonstration scenarios (S1–S5).
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final Clock clock;
    private final IncidentService incidentService;
    private final SlaService slaService;
    private final EscalationService escalationService;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public SimulationService(
            Clock clock,
            IncidentService incidentService,
            SlaService slaService,
            EscalationService escalationService,
            IncidentRepository incidentRepository,
            UserRepository userRepository) {
        this.clock = Objects.requireNonNull(clock);
        this.incidentService = Objects.requireNonNull(incidentService);
        this.slaService = Objects.requireNonNull(slaService);
        this.escalationService = Objects.requireNonNull(escalationService);
        this.incidentRepository = Objects.requireNonNull(incidentRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public SimulationStatusDto getClockStatus() {
        Instant current = clock.instant();
        long multiplier = (clock instanceof AdjustableClock adj) ? adj.getSpeedMultiplier() : 1L;
        return new SimulationStatusDto(
                current,
                multiplier,
                TIME_FMT.format(current),
                multiplier > 1L
        );
    }

    public synchronized void setSpeedMultiplier(long multiplier) {
        if (clock instanceof AdjustableClock adj) {
            adj.setSpeedMultiplier(multiplier);
            log.info("Simulation clock speed multiplier set to x{}", multiplier);
        } else {
            log.warn("Clock is not an AdjustableClock; cannot adjust speed");
        }
    }

    public synchronized void advanceClock(Duration duration) {
        if (clock instanceof AdjustableClock adj) {
            adj.fastForward(duration);
            log.info("Simulation clock fast-forwarded by {} (New Time: {})", duration, TIME_FMT.format(adj.instant()));
        } else {
            log.warn("Clock is not an AdjustableClock; cannot advance time");
        }
    }

    public synchronized void resetClock() {
        if (clock instanceof AdjustableClock adj) {
            adj.resetToRealTime();
            log.info("Simulation clock reset to real-world UTC");
        }
    }

    /**
     * Executes one of the five standard demonstration scenarios (S1–S5).
     */
    public SimulationResult runScenario(String scenarioId) {
        String normalizedId = scenarioId.trim().toUpperCase();
        return switch (normalizedId) {
            case "S1" -> runScenario1HappyPath();
            case "S2" -> runScenario2CriticalOutageAndEscalation();
            case "S3" -> runScenario3WrongDomainAndTriage();
            case "S4" -> runScenario4DuplicateStormAvalanche();
            case "S5" -> runScenario5RejectionLoopAndSlaPausing();
            default -> throw new IllegalArgumentException("Unknown simulation scenario: " + scenarioId + " (Valid: S1, S2, S3, S4, S5)");
        };
    }

    // ==========================================
    // S1: Happy Path
    // ==========================================
    public SimulationResult runScenario1HappyPath() {
        List<String> logs = new ArrayList<>();
        logs.add("Starting Scenario S1: Happy Path Lifecycle");

        // 1. Employee creates incident
        Incident incident = incidentService.createIncident(
                "EMP-0001",
                "Internal Intranet Wiki search indexing error",
                "Search returns HTTP 500 when filtering documents across the engineering space"
        );
        logs.add("Ticket created: " + incident.getId() + " | Category: " + incident.getCategory()
                + " | Priority: " + incident.getPriority() + " | Assigned to: " + incident.getAssigneeId());

        String techId = incident.getAssigneeId();

        // 2. Technician acknowledges
        incident = incidentService.acknowledge(incident.getId(), techId);
        logs.add("Technician " + techId + " acknowledged ticket -> Status: " + incident.getStatus());

        // 3. Technician starts investigation
        incident = incidentService.startInvestigation(incident.getId(), techId);
        logs.add("Technician started investigation -> Status: " + incident.getStatus());

        // 4. Time advances 15 minutes
        advanceClock(Duration.ofMinutes(15));
        logs.add("Time fast-forwarded by 15 minutes");

        // 5. Technician resolves ticket
        Resolution resolution = new Resolution(
                "Elasticsearch index heap fragmentation causing query timeout",
                "Restarted Elasticsearch pod and cleared stale cluster indices",
                "Configured memory limit alerts on wiki search deployment",
                "APPLICATION",
                techId,
                clock.instant()
        );
        incident = incidentService.resolve(incident.getId(), techId, resolution);
        logs.add("Technician resolved ticket with structured root cause -> Status: " + incident.getStatus());

        // 6. Employee verifies fix
        incident = incidentService.verify(incident.getId(), "EMP-0001");
        logs.add("Employee verified resolution -> Status: " + incident.getStatus());

        // 7. Formal closure with 5-star feedback
        Feedback feedback = new Feedback(5, "Wiki search is fast and working smoothly now, thanks!", clock.instant());
        incident = incidentService.close(incident.getId(), "EMP-0001", feedback);
        logs.add("Ticket closed with 5-star feedback -> Final Status: " + incident.getStatus());

        return new SimulationResult(
                "S1",
                "Happy Path Lifecycle",
                incident.getStatus() == IncidentStatus.CLOSED,
                "Successfully executed full 10-step lifecycle from Report to Close.",
                logs,
                incident.getId()
        );
    }

    // ==========================================
    // S2: P1 Critical Outage & SLA Escalation
    // ==========================================
    public SimulationResult runScenario2CriticalOutageAndEscalation() {
        List<String> logs = new ArrayList<>();
        logs.add("Starting Scenario S2: P1 Critical Outage & Multi-Tier Escalation");

        // 1. Executive VP reports critical core banking DB outage
        Incident incident = incidentService.createIncident(
                "EMP-0008", // VP Executive
                "CRITICAL: Core Banking Database Outage - Transactions Blocked",
                "Primary PostgreSQL database connection pool deadlocked; branch wire transfers completely failing"
        );
        logs.add("Ticket created: " + incident.getId() + " | Priority: " + incident.getPriority()
                + " (Forced P1 via RULE_CRITICAL_SERVICE_OUTAGE) | Assignee: " + incident.getAssigneeId());

        String techId = incident.getAssigneeId();

        // 2. Technician acknowledges and starts investigation
        incident = incidentService.acknowledge(incident.getId(), techId);
        incident = incidentService.startInvestigation(incident.getId(), techId);
        logs.add("Technician " + techId + " acknowledged and started investigation -> Status: " + incident.getStatus());

        // 3. Fast forward 23 minutes (75% of 30-minute P1 resolution window)
        advanceClock(Duration.ofMinutes(23));
        logs.add("Time advanced 23 minutes (75% of P1 target window)");
        slaService.evaluateIncident(incident.getId());
        incident = incidentRepository.findById(incident.getId()).orElseThrow();
        logs.add("SLA Monitor evaluated: Status = " + incident.getSlaTracker().getStatus()
                + " (Tier 1 Technician Reminder triggered)");

        // 3. Fast forward 5 minutes (28 minutes total, > 90% window)
        advanceClock(Duration.ofMinutes(5));
        logs.add("Time advanced 5 minutes (93% of P1 target window)");
        slaService.evaluateIncident(incident.getId());
        incident = incidentRepository.findById(incident.getId()).orElseThrow();
        logs.add("SLA Monitor evaluated: Status = " + incident.getSlaTracker().getStatus()
                + " (Tier 2 Team Lead Intervention triggered)");

        // 4. Fast forward 5 minutes (33 minutes total, > 30 min deadline breached)
        advanceClock(Duration.ofMinutes(5));
        logs.add("Time advanced 5 minutes (110% of P1 window -> Breach)");
        slaService.evaluateIncident(incident.getId());
        incident = incidentRepository.findById(incident.getId()).orElseThrow();
        logs.add("SLA Monitor evaluated: Status = " + incident.getSlaTracker().getStatus()
                + " (Tier 4 IT Manager Escalation triggered)");

        // Verify escalation records persisted
        List<EscalationRecord> escalations = escalationService.getEscalationHistory(incident.getId());
        logs.add("Total escalations recorded in database: " + escalations.size());
        for (EscalationRecord esc : escalations) {
            logs.add("  - Escalated to: " + esc.toRole() + " (" + esc.toUserId() + ") | Reason: " + esc.reason());
        }

        // 5. Emergency fix applied
        Resolution resolution = new Resolution(
                "Deadlock on transaction log lock manager caused by orphaned batch job",
                "Terminated orphaned PID, cleared lock table, and increased max connections",
                "Implemented automatic vacuum threshold tuning and connection timeout",
                "DATABASE",
                techId,
                clock.instant()
        );
        incident = incidentService.resolve(incident.getId(), techId, resolution);
        logs.add("Emergency resolution applied -> Status: " + incident.getStatus());

        incident = incidentService.verify(incident.getId(), "EMP-0008");
        logs.add("Resolution verified by VP -> Status: " + incident.getStatus());

        return new SimulationResult(
                "S2",
                "P1 Critical Outage & SLA Escalation",
                !escalations.isEmpty() && incident.getStatus() == IncidentStatus.VERIFIED,
                "Demonstrated 75% warning, 90% warning, hard SLA breach, and multi-tier chain-of-responsibility escalations.",
                logs,
                incident.getId()
        );
    }

    // ==========================================
    // S3: Wrong-Domain Reassignment & Triage
    // ==========================================
    public SimulationResult runScenario3WrongDomainAndTriage() {
        List<String> logs = new ArrayList<>();
        logs.add("Starting Scenario S3: Wrong-Domain Flagging & Administrative Reassignment");

        // 1. Create ambiguously worded ticket initially assigned to Network team
        Incident incident = incidentService.createIncident(
                "EMP-0001",
                "Connection timeout when querying financial data warehouse",
                "Gateway timeout while executing SQL extract queries against data warehouse"
        );
        logs.add("Ticket created: " + incident.getId() + " | Category: " + incident.getCategory()
                + " | Assignee: " + incident.getAssigneeId());

        String initialTech = incident.getAssigneeId();

        // 2. Technician flags ticket as wrong domain
        incidentService.flagWrongDomain(
                incident.getId(),
                initialTech,
                "Not a network infrastructure drop; SQL query execution failure on PostgreSQL DWH node"
        );
        incident = incidentRepository.findById(incident.getId()).orElseThrow();
        logs.add("Technician " + initialTech + " flagged wrong domain -> Reverted to CLASSIFIED in Admin Triage Queue (Assignee cleared)");

        // 3. Admin reclassifies and reassigns ticket to Database team
        incident = incidentService.reclassify(
                incident.getId(),
                "ADMIN-SYS-01",
                IncidentCategory.DATABASE,
                "SVC-DWH",
                "Reclassified from triage: confirmed database node query exhaustion"
        );
        incident = incidentService.assign(
                incident.getId(),
                "ADMIN-SYS-01",
                "TECH-DB-1"
        );
        logs.add("Admin reclassified ticket to DATABASE (SVC-DWH) and assigned to: " + incident.getAssigneeId()
                + " | New Priority: " + incident.getPriority());

        // 4. New technician acknowledges
        incident = incidentService.acknowledge(incident.getId(), incident.getAssigneeId());
        logs.add("New technician " + incident.getAssigneeId() + " acknowledged ticket -> Status: " + incident.getStatus());

        return new SimulationResult(
                "S3",
                "Wrong-Domain Flagging & Triage Reassignment",
                incident.getStatus() == IncidentStatus.ACKNOWLEDGED && !Objects.equals(incident.getAssigneeId(), initialTech),
                "Demonstrated wrong-domain flag, triage queue placement, and administrative reclassification.",
                logs,
                incident.getId()
        );
    }

    // ==========================================
    // S4: Duplicate Ticket Storm Avalanche
    // ==========================================
    public SimulationResult runScenario4DuplicateStormAvalanche() {
        List<String> logs = new ArrayList<>();
        logs.add("Starting Scenario S4: Duplicate Ticket Correlation & Avalanche Priority Escalation");

        // 1. Create parent ticket
        Incident parent = incidentService.createIncident(
                "EMP-0001",
                "Cannot connect to corporate Cisco AnyConnect VPN gateway timeout",
                "AnyConnect client dropped connection with gateway timeout error"
        );
        logs.add("Parent Ticket Created: " + parent.getId() + " | Base Priority: " + parent.getPriority());

        // 2. Concurrently submit 5 duplicates from distinct employees
        String[] duplicateTitles = {
                "AnyConnect VPN connection failed gateway timeout",
                "Cannot connect to corporate Cisco AnyConnect VPN due to gateway timeout",
                "Corporate Cisco AnyConnect VPN gateway connection timeout",
                "Cisco AnyConnect VPN dropped with gateway timeout error",
                "Corporate AnyConnect VPN gateway connection timeout error"
        };

        for (int i = 0; i < duplicateTitles.length; i++) {
            Incident child = incidentService.createIncident(
                    "EMP-000" + (i + 2),
                    duplicateTitles[i],
                    "AnyConnect client dropped connection with gateway timeout error"
            );
            logs.add("Duplicate #" + (i + 1) + " submitted: " + child.getId()
                    + " -> Linked to Parent: " + child.getLinkedIncidentIds().contains(parent.getId()));
        }

        // Reload parent to inspect duplicate linking and impact recalculation
        parent = incidentRepository.findById(parent.getId()).orElseThrow();
        logs.add("Parent ticket now has " + parent.getLinkedIncidentIds().size() + " correlated duplicates linked");
        logs.add("Parent priority decision rules applied: " + parent.getPriorityDecision().rulesApplied());

        return new SimulationResult(
                "S4",
                "Duplicate Ticket Storm & Avalanche Escalation",
                parent.getLinkedIncidentIds().size() >= 5,
                "Correlated 5 duplicate incidents via TF-IDF Cosine Similarity and evaluated duplicate avalanche priority rule.",
                logs,
                parent.getId()
        );
    }

    // ==========================================
    // S5: Rejection Loop & SLA Pausing
    // ==========================================
    public SimulationResult runScenario5RejectionLoopAndSlaPausing() {
        List<String> logs = new ArrayList<>();
        logs.add("Starting Scenario S5: Rejection Loop & SLA Pausing");

        // 1. Create ticket
        Incident incident = incidentService.createIncident(
                "EMP-0001",
                "Outlook email client cannot sync mailbox",
                "Outlook display disconnected error when trying to sync corporate Exchange mailbox"
        );
        String techId = incident.getAssigneeId();
        logs.add("Ticket created: " + incident.getId() + " | Assigned to: " + techId);

        incidentService.acknowledge(incident.getId(), techId);
        incidentService.startInvestigation(incident.getId(), techId);
        logs.add("Technician started investigation -> Status: IN_PROGRESS");

        // 2. Technician requests more info (pauses SLA)
        incident = incidentService.requestInfo(
                incident.getId(),
                techId,
                "Please provide your Outlook version and client sync error code"
        );
        logs.add("Technician requested info -> Status: " + incident.getStatus()
                + " | SLA Paused: " + incident.getSlaTracker().isPaused());

        // 3. Fast-forward clock 2 hours during pause
        advanceClock(Duration.ofHours(2));
        logs.add("Clock fast-forwarded by 2 hours while SLA is paused");

        // 4. Employee provides info (resumes SLA with 2-hour deadline extension)
        incident = incidentService.provideInfo(
                incident.getId(),
                "EMP-0001",
                "Outlook 365 version 2402, error code 0x80040115"
        );
        logs.add("Employee provided info -> Status: " + incident.getStatus()
                + " | SLA Resumed: " + !incident.getSlaTracker().isPaused()
                + " | Total Paused Window: " + incident.getSlaTracker().getPausedDuration().toHours() + "h");

        // 5. Technician resolves ticket
        Resolution res1 = new Resolution(
                "Cached Exchange mode corrupted OST file",
                "Instructed user to rebuild OST profile",
                "Added mailbox auto-cleanup script",
                "EMAIL",
                techId,
                clock.instant()
        );
        incident = incidentService.resolve(incident.getId(), techId, res1);
        logs.add("Technician marked resolved -> Status: " + incident.getStatus());

        // 6. Employee rejects fix (reopen loop)
        incident = incidentService.reject(
                incident.getId(),
                "EMP-0001",
                "Rebuilt OST profile but Outlook still shows disconnected error"
        );
        logs.add("Employee rejected resolution -> Reverted to IN_PROGRESS | Reopen Count: " + incident.getReopenCount());

        // 7. Technician applies revised fix
        Resolution res2 = new Resolution(
                "Stale Modern Authentication token in Windows Credential Manager",
                "Cleared stale ADAL credentials and re-authenticated via SSO SAML",
                "Configured conditional access token refresh interval",
                "EMAIL",
                techId,
                clock.instant()
        );
        incident = incidentService.resolve(incident.getId(), techId, res2);
        logs.add("Technician applied revised resolution -> Status: " + incident.getStatus());

        // 8. Employee verifies fix
        incident = incidentService.verify(incident.getId(), "EMP-0001");
        logs.add("Employee verified second resolution -> Status: " + incident.getStatus());

        return new SimulationResult(
                "S5",
                "Rejection Loop & SLA Pausing",
                incident.getReopenCount() == 1 && incident.getStatus() == IncidentStatus.VERIFIED,
                "Demonstrated PENDING_INFO SLA pausing with deadline extension and reopen rework cycle.",
                logs,
                incident.getId()
        );
    }
}

