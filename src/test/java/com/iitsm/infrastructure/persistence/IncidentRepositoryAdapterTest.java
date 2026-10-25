package com.iitsm.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.InvestigationNote;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.repository.IncidentRepository;
import com.iitsm.infrastructure.persistence.adapter.IncidentRepositoryAdapter;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataIncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class IncidentRepositoryAdapterTest {

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private SpringDataIncidentRepository springDataRepository;

    @Test
    @Transactional
    @DisplayName("IncidentRepositoryAdapter persists aggregate root with SLA tracker and notes")
    void testSaveAndHydrateIncident() {
        Instant now = Instant.parse("2026-10-25T14:00:00Z");
        Incident incident = new Incident(
                "INC-REPO-01",
                "Payment checkout fails with 502",
                "Transaction gateway timeout when processing card payments.",
                "EMP-DEMO-01",
                now);

        PriorityDecision decision = new PriorityDecision(
                Priority.P1, 0.90, List.of(new FactorContribution("Crit", 1.0, 0.3, 0.3, "P1")),
                List.of("RULE_CRIT"), now, null, null);

        incident.classify(IncidentCategory.CLOUD, "SVC-PAYMENT", decision, SlaPolicy.defaultFor(Priority.P1), now);
        incident.assign("TECH-NET-1", now);
        incident.acknowledge("TECH-NET-1", now);
        incident.startInvestigation("TECH-NET-1", now);
        incident.addNote(new InvestigationNote("NOTE-REPO-1", "TECH-NET-1", "Tech Name", "Inspecting logs", now, true));

        incidentRepository.save(incident);

        Optional<Incident> found = incidentRepository.findById("INC-REPO-01");
        assertThat(found).isPresent();

        Incident loaded = found.get();
        assertThat(loaded.getId()).isEqualTo("INC-REPO-01");
        assertThat(loaded.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(loaded.getAssigneeId()).isEqualTo("TECH-NET-1");
        assertThat(loaded.getPriorityDecision().priority()).isEqualTo(Priority.P1);
        assertThat(loaded.getNotes()).hasSize(1);
        assertThat(loaded.getNotes().get(0).noteText()).isEqualTo("Inspecting logs");
        assertThat(loaded.getSlaTracker()).isNotNull();
    }

    @Test
    @DisplayName("Concurrency: Optimistic locking conflict translates to PersistenceFailureException")
    void testOptimisticLockingFailureTranslation() {
        Instant now = Instant.now();
        Incident incident = new Incident(
                "INC-CONCURRENCY-01",
                "Concurrent update ticket",
                "Testing optimistic lock exceptions when multiple transactions write.",
                "EMP-DEMO-01",
                now);

        incidentRepository.save(incident);

        // Fetch two distinct JPA entity snapshots
        IncidentJpaEntity entity1 = springDataRepository.findById("INC-CONCURRENCY-01").orElseThrow();
        IncidentJpaEntity entity2 = springDataRepository.findById("INC-CONCURRENCY-01").orElseThrow();

        // 1st update succeeds and increments version in DB
        entity1.setTitle("First successful update to title");
        springDataRepository.saveAndFlush(entity1);

        // 2nd update has stale version -> saving via adapter must throw PersistenceFailureException
        entity2.setTitle("Stale collision update to title");
        Incident staleIncident = com.iitsm.infrastructure.persistence.mapper.IncidentMapper.toDomain(entity2);

        assertThatThrownBy(() -> incidentRepository.save(staleIncident))
                .isInstanceOf(PersistenceFailureException.class)
                .satisfies(ex -> {
                    PersistenceFailureException pfe = (PersistenceFailureException) ex;
                    assertThat(pfe.getErrorCode()).isEqualTo("ERR_PERSISTENCE_FAILURE");
                });
    }
}

