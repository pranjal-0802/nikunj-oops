package com.iitsm.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pattern: Observer.
 * Tests domain event dispatching and polymorphic event subscriber handling.
 */
class DomainEventPublisherTest {

    @Test
    @DisplayName("Observer: Listeners receive published domain events and support Java 21 pattern matching")
    void testEventDispatchingAndPatternMatching() {
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        List<DomainEvent> capturedEvents = new ArrayList<>();

        publisher.registerListener(capturedEvents::add);

        Instant now = Instant.parse("2026-10-25T15:00:00Z");
        DomainEvent createdEvent = new DomainEvent.IncidentCreated(
                "EVT-1", now, "INC-100", "EMP-42", "Office Wi-Fi Down", "Wi-Fi disconnected on 3rd floor");
        DomainEvent acknowledgedEvent = new DomainEvent.IncidentAcknowledged(
                "EVT-2", now.plusSeconds(30), "INC-100", "TECH-12");

        publisher.publish(createdEvent);
        publisher.publish(acknowledgedEvent);

        assertThat(capturedEvents).hasSize(2);
        assertThat(publisher.getPublishedHistory()).hasSize(2);

        // Verify Java 21 exhaustive pattern matching on sealed DomainEvent
        DomainEvent first = capturedEvents.get(0);
        String eventSummary = switch (first) {
            case DomainEvent.IncidentCreated created -> "Created: " + created.title();
            case DomainEvent.IncidentAcknowledged ack -> "Acknowledged by: " + ack.actorId();
            default -> "Other";
        };

        assertThat(eventSummary).isEqualTo("Created: Office Wi-Fi Down");
    }

    @Test
    @DisplayName("Observer: All 17 sealed DomainEvent variants instantiate and dispatch reliably")
    void testAllSealedDomainEvents() {
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        List<DomainEvent> captured = new ArrayList<>();
        publisher.registerListener(captured::add);

        Instant now = Instant.parse("2026-10-25T16:00:00Z");
        PriorityDecision decision = new PriorityDecision(
                Priority.P1, 0.85, Collections.emptyList(), Collections.emptyList(), now, null, null);
        Resolution resolution = new Resolution(
                "Root cause", "Steps", "Preventive", "SOFTWARE", "TECH-1", now);
        Feedback feedback = new Feedback(5, "Great", now);

        List<DomainEvent> events = List.of(
                new DomainEvent.IncidentCreated("E1", now, "I1", "U1", "T", "D"),
                new DomainEvent.IncidentClassified("E2", now, "I1", "U1", IncidentCategory.SOFTWARE, "S1"),
                new DomainEvent.PriorityCalculated("E3", now, "I1", "U1", decision),
                new DomainEvent.PriorityOverridden("E4", now, "I1", "U1", Priority.P3, Priority.P1, "Urgent"),
                new DomainEvent.DuplicateDetected("E5", now, "I1", "U1", "I0", 0.85),
                new DomainEvent.IncidentAssigned("E6", now, "I1", "U1", "T1"),
                new DomainEvent.ReassignmentRequested("E7", now, "I1", "U1", "T1", "Wrong skill"),
                new DomainEvent.IncidentAcknowledged("E8", now, "I1", "U1"),
                new DomainEvent.InvestigationStarted("E9", now, "I1", "U1"),
                new DomainEvent.MoreInfoRequested("E10", now, "I1", "U1", "Need info"),
                new DomainEvent.IncidentResolved("E11", now, "I1", "U1", resolution),
                new DomainEvent.ResolutionVerified("E12", now, "I1", "U1"),
                new DomainEvent.IncidentReopened("E13", now, "I1", "U1", "Still broken", 1),
                new DomainEvent.IncidentClosed("E14", now, "I1", "U1", feedback),
                new DomainEvent.SlaWarning("E15", now, "I1", "U1", "At 90%"),
                new DomainEvent.SlaViolated("E16", now, "I1", "U1", "Resolution", now),
                new DomainEvent.IncidentEscalated("E17", now, "I1", "U1", "TECH", "LEAD", "TL-1", "SLA delay")
        );

        events.forEach(publisher::publish);

        assertThat(captured).hasSize(17);
        assertThat(publisher.getPublishedHistory()).hasSize(17);

        publisher.clearHistory();
        assertThat(publisher.getPublishedHistory()).isEmpty();
    }
}
