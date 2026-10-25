package com.iitsm.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.FactorContribution;
import com.iitsm.domain.model.Feedback;
import com.iitsm.domain.model.Incident;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.PriorityDecision;
import com.iitsm.domain.model.Resolution;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.model.Technician;
import com.iitsm.infrastructure.persistence.entity.AuditRecordJpaEntity;
import com.iitsm.web.dto.AdminIncidentView;
import com.iitsm.web.dto.EmployeeIncidentView;
import com.iitsm.web.dto.TechnicianIncidentView;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IncidentDtoMapperTest {

    @Test
    @DisplayName("Maps Incident to Employee, Technician, and Admin views with appropriate details")
    void testDtoProjections() {
        Instant now = Instant.now();
        Incident incident = new Incident("INC-MAP-01", "Database deadlocks in batch job", "Multiple threads competing for lock", "EMP-01", now);

        PriorityDecision pd = new PriorityDecision(
                Priority.P2,
                0.65,
                List.of(new FactorContribution("ServiceCriticality", 0.8, 0.3, 0.24, "Tier 3")),
                List.of("HIGH_IMPACT_RULE"),
                now,
                null,
                null);

        SlaPolicy policy = new SlaPolicy("SLA-P2", Priority.P2, null, Duration.ofMinutes(15), Duration.ofHours(2), 0.75);
        incident.classify(IncidentCategory.DATABASE, "SRV-DB", pd, policy, now);
        incident.assign("TECH-01", now);
        incident.acknowledge("TECH-01", now);
        incident.startInvestigation("TECH-01", now);

        Resolution res = new Resolution("Lock table exhausted", "Increased lock table size", "Tuned query", "DATABASE", "TECH-01", now);
        incident.resolve(res, now);
        incident.setFeedback(new Feedback(5, "Excellent support", now));

        Employee reporter = new Employee("EMP-01", "Alice Reporter", new Email("alice@nimbus.com"), "hash", Designation.MANAGER, "Finance", 5);
        Technician tech = new Technician("TECH-01", "Bob Technician", new Email("bob@nimbus.com"), "hash", Collections.emptySet(), 4, 5, "TEAM-DB");

        AuditRecordJpaEntity audit = new AuditRecordJpaEntity();
        audit.setId("AUD-1");
        audit.setAction("IncidentCreated");
        audit.setDescription("Created");
        audit.setTimestamp(now);
        audit.setIncidentId("INC-MAP-01");
        audit.setActorId("EMP-01");

        // 1. Employee View
        EmployeeIncidentView empView = IncidentDtoMapper.toEmployeeView(incident, tech.getName(), List.of(audit));
        assertThat(empView.id()).isEqualTo("INC-MAP-01");
        assertThat(empView.status()).isEqualTo("RESOLVED");
        assertThat(empView.assigneeName()).isEqualTo("Bob Technician");
        assertThat(empView.auditTrail()).hasSize(1);

        // 2. Technician View
        TechnicianIncidentView techView = IncidentDtoMapper.toTechnicianView(incident, reporter);
        assertThat(techView.id()).isEqualTo("INC-MAP-01");
        assertThat(techView.reporterName()).isEqualTo("Alice Reporter");
        assertThat(techView.priority()).isEqualTo(Priority.P2);
        assertThat(techView.resolution()).isNotNull();
        assertThat(techView.resolution().rootCause()).isEqualTo("Lock table exhausted");

        // 3. Admin View
        AdminIncidentView adminView = IncidentDtoMapper.toAdminView(incident, reporter, tech, List.of(audit));
        assertThat(adminView.id()).isEqualTo("INC-MAP-01");
        assertThat(adminView.assigneeName()).isEqualTo("Bob Technician");
        assertThat(adminView.priorityDecision()).isNotNull();
        assertThat(adminView.priorityDecision().compositeScore()).isEqualTo(0.65);
        assertThat(adminView.slaTracker()).isNotNull();
        assertThat(adminView.feedback()).isNotNull();
        assertThat(adminView.feedback().rating()).isEqualTo(5);
        assertThat(adminView.auditTrail()).hasSize(1);
    }
}
