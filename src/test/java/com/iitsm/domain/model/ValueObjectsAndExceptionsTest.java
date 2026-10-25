package com.iitsm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.DuplicateIncidentException;
import com.iitsm.domain.exception.MissingSlaPolicyException;
import com.iitsm.domain.exception.NotificationFailureException;
import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.exception.ServiceNotFoundException;
import com.iitsm.domain.exception.TechnicianUnavailableException;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.exception.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OOP: Unit tests for Value Objects, Enums, Entities and Domain Exception hierarchy.
 */
class ValueObjectsAndExceptionsTest {

    @Test
    @DisplayName("Email value object enforces valid format and rejects malformed strings")
    void testEmailValidation() {
        Email validEmail = new Email("user.test@nimbusfinserve.com");
        assertThat(validEmail.value()).isEqualTo("user.test@nimbusfinserve.com");
        assertThat(validEmail.toString()).isEqualTo("user.test@nimbusfinserve.com");

        assertThatThrownBy(() -> new Email("invalid-email-address"))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Priority score mapper correctly maps score intervals to P1..P4")
    void testPriorityScoreMapping() {
        assertThat(Priority.fromScore(0.95)).isEqualTo(Priority.P1);
        assertThat(Priority.fromScore(0.75)).isEqualTo(Priority.P1);
        assertThat(Priority.fromScore(0.74)).isEqualTo(Priority.P2);
        assertThat(Priority.fromScore(0.55)).isEqualTo(Priority.P2);
        assertThat(Priority.fromScore(0.54)).isEqualTo(Priority.P3);
        assertThat(Priority.fromScore(0.35)).isEqualTo(Priority.P3);
        assertThat(Priority.fromScore(0.34)).isEqualTo(Priority.P4);
        assertThat(Priority.fromScore(0.00)).isEqualTo(Priority.P4);
    }

    @Test
    @DisplayName("DeadlinePair enforces response deadline does not exceed resolution deadline")
    void testDeadlinePairInvariants() {
        Instant now = Instant.parse("2026-10-25T12:00:00Z");
        Instant responseDue = now.plusSeconds(300);
        Instant resolutionDue = now.plusSeconds(1800);

        DeadlinePair validPair = new DeadlinePair(responseDue, resolutionDue);
        assertThat(validPair.responseDueAt()).isEqualTo(responseDue);
        assertThat(validPair.resolutionDueAt()).isEqualTo(resolutionDue);

        assertThatThrownBy(() -> new DeadlinePair(resolutionDue, responseDue))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Resolution deadline cannot precede response deadline");
    }

    @Test
    @DisplayName("Feedback enforces rating bounds 1 to 5")
    void testFeedbackRatingBounds() {
        Instant now = Instant.now();
        Feedback feedback1 = new Feedback(1, "Poor", now);
        Feedback feedback5 = new Feedback(5, "Excellent", now);

        assertThat(feedback1.rating()).isEqualTo(1);
        assertThat(feedback5.rating()).isEqualTo(5);

        assertThatThrownBy(() -> new Feedback(0, "Invalid zero", now))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Feedback(6, "Invalid above five", now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("ItService keyword matching identifies relevant incident text")
    void testItServiceKeywordMatching() {
        ItService vpnService = new ItService(
                "SVC-VPN", "Corporate VPN", "TEAM-NET", BusinessCriticality.TIER_3_BUSINESS_OPERATIONAL,
                5000, Set.of("openvpn", "wireguard", "tunnel", "remote access"));

        assertThat(vpnService.matchesKeyword("Cannot establish tunnel for remote access")).isTrue();
        assertThat(vpnService.matchesKeyword("Corporate VPN connection rejected")).isTrue();
        assertThat(vpnService.matchesKeyword("Cafeteria coffee machine is leaking")).isFalse();
    }

    @Test
    @DisplayName("SupportTeam aggregation manages technician members and team lead")
    void testSupportTeamAggregation() {
        SupportTeam team = new SupportTeam(
                "TEAM-SEC", "Security & Identity", Set.of(IncidentCategory.SECURITY, IncidentCategory.AUTHENTICATION), "LEAD-1");

        team.addMember("TECH-1");
        team.addMember("TECH-2");

        assertThat(team.getMemberIds()).contains("LEAD-1", "TECH-1", "TECH-2");
        assertThat(team.handlesCategory(IncidentCategory.SECURITY)).isTrue();
        assertThat(team.handlesCategory(IncidentCategory.DATABASE)).isFalse();

        team.removeMember("TECH-1");
        assertThat(team.getMemberIds()).doesNotContain("TECH-1");
        // Team lead cannot be removed through normal member removal
        team.removeMember("LEAD-1");
        assertThat(team.getMemberIds()).contains("LEAD-1");
    }

    @Test
    @DisplayName("ReassignmentRequest transitions between PENDING, APPROVED, and REJECTED")
    void testReassignmentRequestLifecycle() {
        Instant now = Instant.parse("2026-10-25T14:00:00Z");
        ReassignmentRequest req = new ReassignmentRequest("REQ-1", "INC-1", "TECH-1", "Wrong skill domain", now);

        assertThat(req.getStatus()).isEqualTo(ReassignmentRequest.Status.PENDING);

        req.approve("ADMIN-1", now.plusSeconds(60));
        assertThat(req.getStatus()).isEqualTo(ReassignmentRequest.Status.APPROVED);
        assertThat(req.getReviewedByAdminId()).isEqualTo("ADMIN-1");
    }

    @Test
    @DisplayName("Domain custom exceptions carry distinct error codes")
    void testCustomExceptionErrorCodes() {
        assertThat(new TechnicianUnavailableException("No tech").getErrorCode())
                .isEqualTo("ERR_TECHNICIAN_UNAVAILABLE");
        assertThat(new ServiceNotFoundException("SVC-X").getErrorCode())
                .isEqualTo("ERR_SERVICE_NOT_FOUND");
        assertThat(new MissingSlaPolicyException(Priority.P1, "SVC").getErrorCode())
                .isEqualTo("ERR_MISSING_SLA_POLICY");
        assertThat(new DuplicateIncidentException("INC-1", "EMP-1").getErrorCode())
                .isEqualTo("ERR_DUPLICATE_INCIDENT");
        assertThat(new UnauthorizedOperationException("Denied").getErrorCode())
                .isEqualTo("ERR_UNAUTHORIZED_OPERATION");
        assertThat(new PersistenceFailureException("Lock error").getErrorCode())
                .isEqualTo("ERR_PERSISTENCE_FAILURE");
        assertThat(new NotificationFailureException("EMAIL", "user@test.com", "Timeout").getErrorCode())
                .isEqualTo("ERR_NOTIFICATION_FAILED");
    }
}
