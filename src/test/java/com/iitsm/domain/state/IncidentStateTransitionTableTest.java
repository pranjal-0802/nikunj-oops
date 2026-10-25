package com.iitsm.domain.state;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.InvalidStateTransitionException;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaPolicy;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * OOP: Comprehensive State Transition Table Test.
 * Exhaustively exercises all (state, action) pairs and verifies illegal transitions throw InvalidStateTransitionException.
 */
class IncidentStateTransitionTableTest {

    private Instant now;
    private PriorityDecision samplePriorityDecision;
    private SlaPolicy sampleSlaPolicy;
    private Resolution sampleResolution;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-25T10:00:00Z");
        samplePriorityDecision = new PriorityDecision(
                Priority.P2, 0.65, Collections.emptyList(), Collections.emptyList(), now, null, null);
        sampleSlaPolicy = SlaPolicy.defaultFor(Priority.P2);
        sampleResolution = new Resolution(
                "Root cause description", "Steps taken to resolve", "Preventive action plan",
                "SOFTWARE", "TECH-1", now);
    }

    private Incident createIncidentInState(IncidentStatus status) {
        Incident incident = new Incident(
                "INC-TEST", "Valid Title for Incident", "Valid description that exceeds twenty characters.", "EMP-1", now);

        if (status == IncidentStatus.NEW) {
            return incident;
        }

        // NEW -> CLASSIFIED
        incident.classify(IncidentCategory.SOFTWARE, "SVC-1", samplePriorityDecision, sampleSlaPolicy, now);
        if (status == IncidentStatus.CLASSIFIED) {
            return incident;
        }

        // CLASSIFIED -> ASSIGNED
        incident.assign("TECH-1", now);
        if (status == IncidentStatus.ASSIGNED) {
            return incident;
        }

        // ASSIGNED -> ACKNOWLEDGED
        incident.acknowledge("TECH-1", now);
        if (status == IncidentStatus.ACKNOWLEDGED) {
            return incident;
        }

        // ACKNOWLEDGED -> IN_PROGRESS
        incident.startInvestigation("TECH-1", now);
        if (status == IncidentStatus.IN_PROGRESS) {
            return incident;
        }

        // IN_PROGRESS -> PENDING_INFO
        if (status == IncidentStatus.PENDING_INFO) {
            incident.requestInfo("TECH-1", "Need logs", now);
            return incident;
        }

        // IN_PROGRESS -> RESOLVED
        incident.resolve(sampleResolution, now);
        if (status == IncidentStatus.RESOLVED) {
            return incident;
        }

        // RESOLVED -> VERIFIED
        incident.verify("EMP-1", now);
        if (status == IncidentStatus.VERIFIED) {
            return incident;
        }

        // VERIFIED -> CLOSED
        incident.close("ADMIN-1", new Feedback(5, "Great work", now), now);
        if (status == IncidentStatus.CLOSED) {
            return incident;
        }

        throw new IllegalArgumentException("Unhandled status: " + status);
    }

    @Test
    @DisplayName("NewState allows only classify and moves to CLASSIFIED")
    void testNewStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.NEW);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(incident.getAllowedActions()).containsExactly("classify");

        // Illegal actions on NEW
        assertActionThrows(incident, () -> incident.assign("T1", now), "assign");
        assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        assertActionThrows(incident, () -> incident.startInvestigation("T1", now), "startInvestigation");
        assertActionThrows(incident, () -> incident.requestInfo("T1", "msg", now), "requestInfo");
        assertActionThrows(incident, () -> incident.provideInfo("E1", "resp", now), "provideInfo");
        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");
        assertActionThrows(incident, () -> incident.verify("E1", now), "verify");
        assertActionThrows(incident, () -> incident.reject("E1", "reason", now), "reject");
        assertActionThrows(incident, () -> incident.close("A1", null, now), "close");
        assertActionThrows(incident, () -> incident.flagWrongDomain("T1", "reason", now), "flagWrongDomain");

        // Permitted action
        incident.classify(IncidentCategory.DATABASE, "SVC-DB", samplePriorityDecision, sampleSlaPolicy, now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
    }

    @Test
    @DisplayName("ClassifiedState allows only assign and moves to ASSIGNED")
    void testClassifiedStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.CLASSIFIED);
        assertThat(incident.getAllowedActions()).containsExactly("assign");

        assertActionThrows(incident, () -> incident.classify(IncidentCategory.CLOUD, "SVC-2", samplePriorityDecision, sampleSlaPolicy, now), "classify");
        assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");

        incident.assign("TECH-99", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(incident.getAssigneeId()).isEqualTo("TECH-99");
    }

    @Test
    @DisplayName("AssignedState allows acknowledge and flagWrongDomain")
    void testAssignedStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.ASSIGNED);
        assertThat(incident.getAllowedActions()).containsExactlyInAnyOrder("acknowledge", "flagWrongDomain");

        // Test flagWrongDomain reverts to CLASSIFIED
        incident.flagWrongDomain("TECH-1", "Wrong expertise", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(incident.getAssigneeId()).isNull();

        // Re-assign and test acknowledge moves to ACKNOWLEDGED
        incident.assign("TECH-2", now);
        incident.acknowledge("TECH-2", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    }

    @Test
    @DisplayName("AcknowledgedState allows startInvestigation and flagWrongDomain")
    void testAcknowledgedStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.ACKNOWLEDGED);
        assertThat(incident.getAllowedActions()).containsExactlyInAnyOrder("startInvestigation", "flagWrongDomain");

        assertActionThrows(incident, () -> incident.assign("T1", now), "assign");
        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");

        incident.startInvestigation("TECH-1", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("InProgressState allows requestInfo and resolve")
    void testInProgressStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getAllowedActions()).containsExactlyInAnyOrder("requestInfo", "resolve");

        assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        assertActionThrows(incident, () -> incident.verify("E1", now), "verify");

        // Move to PENDING_INFO
        incident.requestInfo("TECH-1", "Need more details", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.PENDING_INFO);
    }

    @Test
    @DisplayName("PendingInfoState allows provideInfo and resumes to IN_PROGRESS")
    void testPendingInfoStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.PENDING_INFO);
        assertThat(incident.getAllowedActions()).containsExactly("provideInfo");

        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");
        assertActionThrows(incident, () -> incident.close("A1", null, now), "close");

        incident.provideInfo("EMP-1", "Logs uploaded to shared drive", now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("ResolvedState allows verify (to VERIFIED) and reject (to IN_PROGRESS with reopenCount+1)")
    void testResolvedStateTransitions() {
        // Sub-scenario 1: Reject reopens incident
        Incident incidentToReopen = createIncidentInState(IncidentStatus.RESOLVED);
        assertThat(incidentToReopen.getAllowedActions()).containsExactlyInAnyOrder("verify", "reject");
        assertThat(incidentToReopen.getReopenCount()).isEqualTo(0);

        incidentToReopen.reject("EMP-1", "Issue still recurs under load", now);
        assertThat(incidentToReopen.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incidentToReopen.getReopenCount()).isEqualTo(1);

        // Sub-scenario 2: Verify moves to VERIFIED
        Incident incidentToVerify = createIncidentInState(IncidentStatus.RESOLVED);
        incidentToVerify.verify("EMP-1", now);
        assertThat(incidentToVerify.getStatus()).isEqualTo(IncidentStatus.VERIFIED);
    }

    @Test
    @DisplayName("VerifiedState allows close and moves to CLOSED with feedback")
    void testVerifiedStateTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.VERIFIED);
        assertThat(incident.getAllowedActions()).containsExactly("close");

        assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");

        Feedback feedback = new Feedback(4, "Resolved smoothly", now);
        incident.close("ADMIN-1", feedback, now);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getFeedback()).isEqualTo(feedback);
    }

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    @DisplayName("Every state rejects illegal actions by throwing InvalidStateTransitionException")
    void testIllegalTransitionsThrow(IncidentStatus status) {
        Incident incident = createIncidentInState(status);
        Set<String> allowed = incident.getAllowedActions();

        if (!allowed.contains("assign")) {
            assertActionThrows(incident, () -> incident.assign("T1", now), "assign");
        }
        if (!allowed.contains("acknowledge")) {
            assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        }
        if (!allowed.contains("startInvestigation")) {
            assertActionThrows(incident, () -> incident.startInvestigation("T1", now), "startInvestigation");
        }
        if (!allowed.contains("requestInfo")) {
            assertActionThrows(incident, () -> incident.requestInfo("T1", "msg", now), "requestInfo");
        }
        if (!allowed.contains("provideInfo")) {
            assertActionThrows(incident, () -> incident.provideInfo("E1", "resp", now), "provideInfo");
        }
        if (!allowed.contains("resolve")) {
            assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");
        }
        if (!allowed.contains("verify")) {
            assertActionThrows(incident, () -> incident.verify("E1", now), "verify");
        }
        if (!allowed.contains("reject")) {
            assertActionThrows(incident, () -> incident.reject("E1", "reason", now), "reject");
        }
        if (!allowed.contains("close")) {
            assertActionThrows(incident, () -> incident.close("A1", null, now), "close");
        }
        if (!allowed.contains("flagWrongDomain")) {
            assertActionThrows(incident, () -> incident.flagWrongDomain("T1", "reason", now), "flagWrongDomain");
        }
    }

    @Test
    @DisplayName("ClosedState allows strictly zero transitions")
    void testClosedStateHasZeroAllowedTransitions() {
        Incident incident = createIncidentInState(IncidentStatus.CLOSED);
        assertThat(incident.getAllowedActions()).isEmpty();

        assertActionThrows(incident, () -> incident.acknowledge("T1", now), "acknowledge");
        assertActionThrows(incident, () -> incident.startInvestigation("T1", now), "startInvestigation");
        assertActionThrows(incident, () -> incident.resolve(sampleResolution, now), "resolve");
        assertActionThrows(incident, () -> incident.verify("E1", now), "verify");
        assertActionThrows(incident, () -> incident.close("A1", null, now), "close");
    }

    private void assertActionThrows(Incident incident, Runnable action, String actionName) {
        assertThatThrownBy(action::run)
                .isInstanceOf(InvalidStateTransitionException.class)
                .satisfies(ex -> {
                    InvalidStateTransitionException ste = (InvalidStateTransitionException) ex;
                    assertThat(ste.getFromState()).isEqualTo(incident.getStatus());
                    assertThat(ste.getAttemptedAction()).isEqualTo(actionName);
                    assertThat(ste.getErrorCode()).isEqualTo("ERR_INVALID_STATE_TRANSITION");
                });
    }
}
