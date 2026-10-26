package com.iitsm.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.application.service.IncidentService;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Resolution;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataAuditRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flagship End-to-End integration test validating complete incident lifecycle:
 * Employee raises ticket -> Technician acknowledges -> Investigates -> Requests Info (SLA pauses) ->
 * Employee provides info (SLA resumes) -> Technician resolves -> Employee rejects -> Reopens ->
 * Technician resolves -> Employee verifies -> Admin closes.
 */
@SpringBootTest
@ActiveProfiles("dev")
class IncidentLifecycleEndToEndTest {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private SpringDataAuditRecordRepository auditRecordRepository;

    @Autowired
    private Clock clock;

    @Autowired
    private com.iitsm.domain.repository.UserRepository userRepository;

    @Autowired
    private com.iitsm.infrastructure.persistence.CompanySeeder companySeeder;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        if (userRepository.count() == 0) {
            companySeeder.seedCompanyData();
        }
    }

    @Test
    @DisplayName("Complete end-to-end incident lifecycle with audit trails and state transitions")
    void testCompleteIncidentLifecycle() {
        // Step 1: Employee raises incident
        String reporterId = "EMP-0001";
        Incident incident = incidentService.createIncident(
                reporterId,
                "VPN tunnel disconnected during wire transfer",
                "Cisco AnyConnect threw security gateway negotiation timeout error.");

        assertThat(incident.getId()).isNotNull();
        assertThat(incident.getStatus()).isIn(IncidentStatus.NEW, IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED);
        String technicianId = incident.getAssigneeId();
        assertThat(technicianId).isNotNull();

        // Step 2: Technician acknowledges incident
        Incident acknowledged = incidentService.acknowledge(incident.getId(), technicianId);
        assertThat(acknowledged.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);

        // Step 3: Technician starts investigation
        Incident inProgress = incidentService.startInvestigation(incident.getId(), technicianId);
        assertThat(inProgress.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);

        // Step 4: Technician requests more information from employee (pauses SLA)
        Incident pendingInfo = incidentService.requestInfo(incident.getId(), technicianId, "Please provide client log file");
        assertThat(pendingInfo.getStatus()).isEqualTo(IncidentStatus.PENDING_INFO);
        assertThat(pendingInfo.getSlaTracker().isPaused()).isTrue();

        // Step 5: Employee provides information (resumes SLA)
        Incident infoProvided = incidentService.provideInfo(incident.getId(), reporterId, "Uploaded vpn_debug.log to file share");
        assertThat(infoProvided.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(infoProvided.getSlaTracker().isPaused()).isFalse();

        // Step 6: Technician resolves incident
        Resolution resolution = new Resolution(
                "Expired certificate on secondary VPN node",
                "Renewed SSL cert and restarted gateway daemon",
                "Automated cert renewal alert in Nagios",
                "NETWORK",
                technicianId,
                clock.instant());

        Incident resolved = incidentService.resolve(incident.getId(), technicianId, resolution);
        assertThat(resolved.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(resolved.getResolution()).isNotNull();
        assertThat(resolved.getResolution().rootCause()).isEqualTo("Expired certificate on secondary VPN node");

        // Step 7: Employee rejects resolution (reopens ticket)
        Incident rejected = incidentService.reject(incident.getId(), reporterId, "Still receiving gateway timeout when reconnecting");
        assertThat(rejected.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(rejected.getReopenCount()).isEqualTo(1);

        // Step 8: Technician resolves again
        Resolution resolution2 = new Resolution(
                "Stale route table on local client",
                "Flushed DNS cache and updated default gateway route",
                "Distributed updated client profile",
                "NETWORK",
                technicianId,
                clock.instant());

        Incident resolvedAgain = incidentService.resolve(incident.getId(), technicianId, resolution2);
        assertThat(resolvedAgain.getStatus()).isEqualTo(IncidentStatus.RESOLVED);

        // Step 9: Employee verifies fix
        Incident verified = incidentService.verify(incident.getId(), reporterId);
        assertThat(verified.getStatus()).isEqualTo(IncidentStatus.VERIFIED);

        // Step 10: Admin formally closes incident with feedback
        Feedback feedback = new Feedback(5, "Prompt resolution once root cause was found!", clock.instant());
        Incident closed = incidentService.close(incident.getId(), "ADMIN-SYS-01", feedback);
        assertThat(closed.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(closed.getFeedback()).isNotNull();
        assertThat(closed.getFeedback().rating()).isEqualTo(5);

        // Audit Trail Verification
        List<AuditRecordJpaEntity> auditTrail = auditRecordRepository.findByIncidentIdOrderByTimestampAsc(incident.getId());
        assertThat(auditTrail).isNotEmpty();
        assertThat(auditTrail.size()).isGreaterThanOrEqualTo(8);

        List<String> actions = auditTrail.stream().map(AuditRecordJpaEntity::getAction).toList();
        assertThat(actions).contains(
                "IncidentCreated",
                "IncidentAcknowledged",
                "InvestigationStarted",
                "MoreInfoRequested",
                "IncidentResolved",
                "IncidentReopened",
                "ResolutionVerified",
                "IncidentClosed");
    }
}
