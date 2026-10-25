package com.iitsm.infrastructure.observer;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.event.DomainEvent;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.Resolution;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataAuditRecordRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(AuditTrailObserver.class)
class AuditTrailObserverTest {

    @Autowired
    private AuditTrailObserver observer;

    @Autowired
    private SpringDataAuditRecordRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
    }

    @Test
    @DisplayName("Observer synchronously persists audit records on IncidentCreated event")
    void testAuditRecordCreatedOnIncidentCreated() {
        Instant now = Instant.now();
        DomainEvent.IncidentCreated event = new DomainEvent.IncidentCreated(
                "EVT-001", now, "INC-999", "EMP-001", "Outlook crashing", "Crashes on startup");

        observer.onEvent(event);

        List<AuditRecordJpaEntity> records = auditRepository.findByIncidentIdOrderByTimestampAsc("INC-999");
        assertThat(records).hasSize(1);
        AuditRecordJpaEntity record = records.get(0);
        assertThat(record.getIncidentId()).isEqualTo("INC-999");
        assertThat(record.getActorId()).isEqualTo("EMP-001");
        assertThat(record.getAction()).isEqualTo("IncidentCreated");
        assertThat(record.getNewState()).isEqualTo("NEW");
        assertThat(record.getDescription()).contains("Outlook crashing");
    }

    @Test
    @DisplayName("Observer records state transitions on IncidentResolved event")
    void testAuditRecordOnIncidentResolved() {
        Instant now = Instant.now();
        Resolution resolution = new Resolution(
                "Corrupted cache",
                "Cleared Outlook profile cache",
                "Scheduled quarterly cleanup",
                "APPLICATION",
                "TECH-001",
                now);

        DomainEvent.IncidentResolved event = new DomainEvent.IncidentResolved(
                "EVT-002", now, "INC-999", "TECH-001", resolution);

        observer.onEvent(event);

        List<AuditRecordJpaEntity> records = auditRepository.findByIncidentIdOrderByTimestampAsc("INC-999");
        assertThat(records).hasSize(1);
        AuditRecordJpaEntity record = records.get(0);
        assertThat(record.getPreviousState()).isEqualTo("IN_PROGRESS");
        assertThat(record.getNewState()).isEqualTo("RESOLVED");
        assertThat(record.getDescription()).contains("Corrupted cache");
    }

    @Test
    @DisplayName("Observer records PriorityOverridden event with administrative reason")
    void testAuditRecordOnPriorityOverridden() {
        Instant now = Instant.now();
        DomainEvent.PriorityOverridden event = new DomainEvent.PriorityOverridden(
                "EVT-003", now, "INC-999", "ADMIN-001", Priority.P3, Priority.P1, "CEO escalates outage");

        observer.onEvent(event);

        List<AuditRecordJpaEntity> records = auditRepository.findByIncidentIdOrderByTimestampAsc("INC-999");
        assertThat(records).hasSize(1);
        AuditRecordJpaEntity record = records.get(0);
        assertThat(record.getActorId()).isEqualTo("ADMIN-001");
        assertThat(record.getDescription()).contains("CEO escalates outage");
    }
}

