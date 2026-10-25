package com.iitsm.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.ValidationException;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * OOP: Invariant and Encapsulation tests for the Incident Aggregate Root.
 * Covers boundary validation, HTML escaping, and encapsulation rules.
 */
class IncidentEncapsulationTest {

    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.parse("2026-10-25T12:00:00Z");
    }

    @Test
    @DisplayName("Boundary: Title length 4 chars fails validation")
    void testTitleTooShort() {
        assertThatThrownBy(() -> new Incident("INC-1", "1234", "Valid description that meets length minimum.", "EMP-1", now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title length must be between 5 and 120");
    }

    @Test
    @DisplayName("Boundary: Title length exactly 5 chars succeeds")
    void testTitleMinimumValidLength() {
        Incident incident = new Incident("INC-1", "12345", "Valid description that meets length minimum.", "EMP-1", now);
        assertThat(incident.getTitle()).isEqualTo("12345");
    }

    @Test
    @DisplayName("Boundary: Title length exactly 120 chars succeeds")
    void testTitleMaximumValidLength() {
        String title120 = "A".repeat(120);
        Incident incident = new Incident("INC-1", title120, "Valid description that meets length minimum.", "EMP-1", now);
        assertThat(incident.getTitle()).hasSize(120);
    }

    @Test
    @DisplayName("Boundary: Title length 121 chars fails validation")
    void testTitleTooLong() {
        String title121 = "A".repeat(121);
        assertThatThrownBy(() -> new Incident("INC-1", title121, "Valid description that meets length minimum.", "EMP-1", now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Title length must be between 5 and 120");
    }

    @Test
    @DisplayName("Boundary: Description length 19 chars fails validation")
    void testDescriptionTooShort() {
        String desc19 = "A".repeat(19);
        assertThatThrownBy(() -> new Incident("INC-1", "Valid Title", desc19, "EMP-1", now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description length must be between 20 and 4000");
    }

    @Test
    @DisplayName("Boundary: Description length exactly 20 chars succeeds")
    void testDescriptionMinimumValidLength() {
        String desc20 = "A".repeat(20);
        Incident incident = new Incident("INC-1", "Valid Title", desc20, "EMP-1", now);
        assertThat(incident.getDescription()).hasSize(20);
    }

    @Test
    @DisplayName("Boundary: Description length exactly 4000 chars succeeds")
    void testDescriptionMaximumValidLength() {
        String desc4000 = "A".repeat(4000);
        Incident incident = new Incident("INC-1", "Valid Title", desc4000, "EMP-1", now);
        assertThat(incident.getDescription()).hasSize(4000);
    }

    @Test
    @DisplayName("Boundary: Description length 4001 chars fails validation")
    void testDescriptionTooLong() {
        String desc4001 = "A".repeat(4001);
        assertThatThrownBy(() -> new Incident("INC-1", "Valid Title", desc4001, "EMP-1", now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description length must be between 20 and 4000");
    }

    @Test
    @DisplayName("Invalid input: Blank title and null reporter fail validation")
    void testBlankAndNullFields() {
        assertThatThrownBy(() -> new Incident(" ", "Valid Title", "Valid description that is long enough.", "EMP-1", now))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Incident("INC-1", null, "Valid description that is long enough.", "EMP-1", now))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Incident("INC-1", "Valid Title", "Valid description that is long enough.", null, now))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Security: HTML tags in title and description are escaped")
    void testHtmlSanitization() {
        String rawTitle = "VPN <script>alert('xss')</script> down";
        String rawDesc = "User clicked on <a href='http://evil.com'>link</a> & got 500 error.";

        Incident incident = new Incident("INC-1", rawTitle, rawDesc, "EMP-1", now);

        assertThat(incident.getTitle()).doesNotContain("<script>");
        assertThat(incident.getTitle()).contains("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;");

        assertThat(incident.getDescription()).doesNotContain("<a href=");
        assertThat(incident.getDescription()).contains("&lt;a href=&#x27;http://evil.com&#x27;&gt;");
        assertThat(incident.getDescription()).contains("&amp;");
    }

    @Test
    @DisplayName("Encapsulation: Collections returned from getters are unmodifiable")
    void testGettersReturnUnmodifiableCollections() {
        Incident incident = new Incident("INC-1", "Valid Title", "Valid description that is long enough.", "EMP-1", now);
        incident.addNote(new InvestigationNote("N1", "T1", "Tech", "Investigation started", now, true));
        incident.linkIncident("INC-999");

        assertThatThrownBy(() -> incident.getNotes().add(new InvestigationNote("N2", "T1", "Tech", "Hack", now, false)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> incident.getLinkedIncidentIds().add("INC-888"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Business Rule: Priority override requires mandatory reason")
    void testPriorityOverrideRequiresReason() {
        Incident incident = new Incident("INC-1", "Valid Title", "Valid description that is long enough.", "EMP-1", now);
        PriorityDecision decision = new PriorityDecision(Priority.P3, 0.40, Collections.emptyList(), Collections.emptyList(), now, null, null);
        incident.classify(IncidentCategory.SOFTWARE, "SVC-1", decision, SlaPolicy.defaultFor(Priority.P3), now);

        assertThatThrownBy(() -> incident.overridePriority(Priority.P1, "ADMIN-1", "   ", now))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Mandatory reason required for priority override");

        incident.overridePriority(Priority.P1, "ADMIN-1", "Critical financial transaction blockage", now);
        assertThat(incident.getPriorityDecision().priority()).isEqualTo(Priority.P1);
        assertThat(incident.getPriorityDecision().overriddenBy()).isEqualTo("ADMIN-1");
        assertThat(incident.getPriorityDecision().overrideReason()).isEqualTo("Critical financial transaction blockage");
    }
}
