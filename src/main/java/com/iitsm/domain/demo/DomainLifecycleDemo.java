package com.iitsm.domain.demo;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.event.InMemoryDomainEventPublisher;
import com.iitsm.domain.exception.InvalidStateTransitionException;
import com.iitsm.domain.model.AvailabilityStatus;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.InvestigationNote;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.state.PublicStatusMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * OOP: Standalone domain demonstration walking an incident through its full lifecycle
 * from NEW to CLOSED without any framework or database dependencies.
 */
public class DomainLifecycleDemo {

    public static void main(String[] args) {
        Clock clock = Clock.systemUTC();
        Instant now = clock.instant();

        System.out.println("================================================================================");
        System.out.println("  I-ITSM :: Pure Domain Core Lifecycle Demonstration (NEW -> CLOSED)");
        System.out.println("================================================================================\n");

        // 1. Setup Event-Driven Spine (Observer Pattern)
        InMemoryDomainEventPublisher publisher = new InMemoryDomainEventPublisher();
        publisher.registerListener(event -> {
            System.out.printf("  [EVENT PUBLISHED] %s -> Incident: %s, Actor: %s%n",
                    event.getClass().getSimpleName(), event.incidentId(), event.actorId());
        });

        // 2. Setup Actors
        Employee alice = new Employee(
                "EMP-001",
                "Alice Chen",
                new Email("alice.chen@nimbusfinserve.com"),
                "$2a$12$e0MYzXyjpJS7Pd0RVvHwHeFj7bK2B3vQyO.",
                Designation.SENIOR_ASSOCIATE,
                "Engineering",
                4);

        Skill sqlSkill = new Skill("PostgreSQL Tuning", IncidentCategory.DATABASE);
        Technician bob = new Technician(
                "TECH-002",
                "Bob Martinez",
                new Email("bob.martinez@nimbusfinserve.com"),
                "$2a$12$e0MYzXyjpJS7Pd0RVvHwHeFj7bK2B3vQyO.",
                Set.of(sqlSkill),
                6,
                5,
                "TEAM-DB");

        System.out.printf("Reporter: %s (%s, %s)%n", alice.getName(), alice.getDepartment(), alice.getDesignation());
        System.out.printf("Technician: %s (Experience: %d yrs, Capacity: %d, Initial Load: %d)%n%n",
                bob.getName(), bob.getExperienceYears(), bob.getMaxCapacity(), bob.getActiveIncidentCount());

        // 3. Step 1: Create Ticket (NEW)
        Incident incident = new Incident(
                "INC-1001",
                "Core Banking DB: SELECT queries timing out on checkout ledger",
                "Multiple production checkout transactions failing with HTTP 500. Queries on transaction_ledger taking > 30s.",
                alice.getId(),
                now);

        publisher.publish(new DomainEvent.IncidentCreated(
                UUID.randomUUID().toString(), now, incident.getId(), alice.getId(),
                incident.getTitle(), incident.getDescription()));

        printIncidentState("1. Created", incident);

        // 4. Step 2: Classify & Prioritize (CLASSIFIED)
        now = now.plusSeconds(30);
        PriorityDecision priorityDecision = new PriorityDecision(
                Priority.P1,
                0.88,
                List.of(
                        new FactorContribution("ServiceCriticalityFactor", 1.0, 0.30, 0.30, "Tier 4 Core Banking DB"),
                        new FactorContribution("ImpactFactor", 1.0, 0.20, 0.20, "Wide userbase outage"),
                        new FactorContribution("UrgencyFactor", 0.9, 0.15, 0.135, "Database timing out"),
                        new FactorContribution("ReporterDesignationFactor", 0.45, 0.15, 0.0675, "Senior Associate")
                ),
                List.of("RULE_CRITICALITY_4_OUTAGE_P1"),
                now,
                null,
                null);

        SlaPolicy slaPolicy = SlaPolicy.defaultFor(Priority.P1);
        incident.classify(IncidentCategory.DATABASE, "SVC-CORE-DB", priorityDecision, slaPolicy, now);

        publisher.publish(new DomainEvent.IncidentClassified(
                UUID.randomUUID().toString(), now, incident.getId(), "SYSTEM",
                incident.getCategory(), incident.getServiceId()));

        printIncidentState("2. Classified", incident);

        // 5. Step 3: Assign to Technician Bob (ASSIGNED)
        now = now.plusSeconds(30);
        bob.assignIncident(incident.getId());
        incident.assign(bob.getId(), now);

        publisher.publish(new DomainEvent.IncidentAssigned(
                UUID.randomUUID().toString(), now, incident.getId(), "SYSTEM", bob.getId()));

        printIncidentState("3. Assigned", incident);
        System.out.printf("     Assignee Load: %d / %d (Load Ratio: %.2f)%n",
                bob.getActiveIncidentCount(), bob.getMaxCapacity(), bob.getLoadRatio());

        // 6. Step 4: Bob Acknowledges Ticket (ACKNOWLEDGED)
        now = now.plusSeconds(60);
        incident.acknowledge(bob.getId(), now);

        publisher.publish(new DomainEvent.IncidentAcknowledged(
                UUID.randomUUID().toString(), now, incident.getId(), bob.getId()));

        printIncidentState("4. Acknowledged", incident);
        System.out.printf("     SLA Responded At: %s, SLA Status: %s%n",
                incident.getSlaTracker().getRespondedAt(), incident.getSlaTracker().getStatus());

        // 7. Step 5: Start Investigation (IN_PROGRESS)
        now = now.plusSeconds(60);
        incident.startInvestigation(bob.getId(), now);
        incident.addNote(new InvestigationNote(
                "NOTE-1", bob.getId(), bob.getName(), "Examined active PostgreSQL locks using pg_stat_activity.", now, true));

        publisher.publish(new DomainEvent.InvestigationStarted(
                UUID.randomUUID().toString(), now, incident.getId(), bob.getId()));

        printIncidentState("5. In Progress", incident);

        // 8. Step 6: Request Information from Alice (PENDING_INFO - Pauses SLA)
        now = now.plusSeconds(180);
        incident.requestInfo(bob.getId(), "Could you provide recent transaction IDs from the failing checkout requests?", now);

        publisher.publish(new DomainEvent.MoreInfoRequested(
                UUID.randomUUID().toString(), now, incident.getId(), bob.getId(), "Requesting checkout correlation IDs"));

        printIncidentState("6. Pending Info (SLA Clock Paused)", incident);
        System.out.printf("     SLA Is Paused: %s, Paused At: %s%n",
                incident.getSlaTracker().isPaused(), incident.getSlaTracker().getPausedAt());

        // 9. Step 7: Alice provides information (IN_PROGRESS - Resumes SLA)
        now = now.plusSeconds(300); // 5 minutes later
        incident.provideInfo(alice.getId(), "Transaction IDs: TX-9021, TX-9022 failing with lock timeout.", now);
        incident.addNote(new InvestigationNote(
                "NOTE-2", alice.getId(), alice.getName(), "Failing TX IDs supplied by user.", now, false));

        printIncidentState("7. In Progress (SLA Resumed)", incident);
        System.out.printf("     Total SLA Paused Duration: %d seconds, SLA Status: %s%n",
                incident.getSlaTracker().getPausedDuration().toSeconds(), incident.getSlaTracker().getStatus());

        // 10. Step 8: Bob Resolves the Incident (RESOLVED)
        now = now.plusSeconds(300);
        Resolution resolution = new Resolution(
                "Unindexed foreign key on transaction_ledger causing sequential table scan and exclusive lock contention.",
                "Terminated blocking query PID 4821 and executed CREATE INDEX CONCURRENTLY idx_tx_ledger_account_date ON transaction_ledger(account_id, tx_date);",
                "Added migration script to CI/CD repository and lowered lock_timeout to 5s.",
                "DATABASE",
                bob.getId(),
                now);

        incident.resolve(resolution, now);
        bob.releaseIncident(incident.getId());

        publisher.publish(new DomainEvent.IncidentResolved(
                UUID.randomUUID().toString(), now, incident.getId(), bob.getId(), resolution));

        printIncidentState("8. Resolved", incident);
        System.out.printf("     SLA Compliance: %s, Technician Released Load: %d / %d%n",
                incident.getSlaTracker().getStatus(), bob.getActiveIncidentCount(), bob.getMaxCapacity());

        // 11. Step 9: Alice Verifies Resolution (VERIFIED)
        now = now.plusSeconds(120);
        incident.verify(alice.getId(), now);

        publisher.publish(new DomainEvent.ResolutionVerified(
                UUID.randomUUID().toString(), now, incident.getId(), alice.getId()));

        printIncidentState("9. Verified", incident);

        // 12. Step 10: Admin Closes Ticket (CLOSED)
        now = now.plusSeconds(60);
        Feedback feedback = new Feedback(5, "Outstanding response and clear root cause explanation. Queries are sub-5ms now!", now);
        incident.close("ADMIN-01", feedback, now);

        publisher.publish(new DomainEvent.IncidentClosed(
                UUID.randomUUID().toString(), now, incident.getId(), "ADMIN-01", feedback));

        printIncidentState("10. Closed", incident);
        System.out.printf("     User Feedback Rating: %d/5 ('%s')%n",
                incident.getFeedback().rating(), incident.getFeedback().comment());

        // 13. Verify Invariant: Closed state prohibits any further transitions
        System.out.println("\n--- Testing Invariant Enforcement: Illegal Transition on CLOSED ---");
        try {
            incident.acknowledge(bob.getId(), now);
            System.err.println("FAILED: Illegal transition did not throw!");
        } catch (InvalidStateTransitionException ex) {
            System.out.printf("SUCCESS: Caught expected %s: %s%n",
                    ex.getClass().getSimpleName(), ex.getMessage());
        }

        System.out.println("\n================================================================================");
        System.out.println("  Pure Domain Core Demonstration Finished Successfully.");
        System.out.printf("  Total Events Published: %d%n", publisher.getPublishedHistory().size());
        System.out.println("================================================================================");
    }

    private static void printIncidentState(String step, Incident incident) {
        System.out.printf("  -> [%s]%n", step);
        System.out.printf("     Internal State: %s | Public Stepper View: %s%n",
                incident.getStatus(), PublicStatusMapper.toPublicStatus(incident.getStatus()));
        System.out.printf("     Allowed Actions: %s%n", incident.getAllowedActions());
    }
}
