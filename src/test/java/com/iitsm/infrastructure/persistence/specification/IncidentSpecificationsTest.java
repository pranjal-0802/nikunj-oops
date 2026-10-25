package com.iitsm.infrastructure.persistence.specification;

import static org.assertj.core.api.Assertions.assertThat;

import com.iitsm.domain.model.BusinessCriticality;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.UserRole;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import com.iitsm.infrastructure.persistence.entity.ItServiceJpaEntity;
import com.iitsm.infrastructure.persistence.entity.UserJpaEntity;
import com.iitsm.infrastructure.persistence.repository.SpringDataIncidentRepository;
import com.iitsm.infrastructure.persistence.repository.SpringDataItServiceRepository;
import com.iitsm.infrastructure.persistence.repository.SpringDataUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
class IncidentSpecificationsTest {

    @Autowired
    private SpringDataIncidentRepository incidentRepository;

    @Autowired
    private SpringDataUserRepository userRepository;

    @Autowired
    private SpringDataItServiceRepository itServiceRepository;

    private Instant baseTime;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        itServiceRepository.deleteAll();
        userRepository.deleteAll();
        baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        createUser("EMP-001", "emp1@nimbusfinserve.com", UserRole.ROLE_EMPLOYEE);
        createUser("EMP-002", "emp2@nimbusfinserve.com", UserRole.ROLE_EMPLOYEE);
        createUser("TECH-001", "tech1@nimbusfinserve.com", UserRole.ROLE_TECHNICIAN);
        createUser("TECH-002", "tech2@nimbusfinserve.com", UserRole.ROLE_TECHNICIAN);

        createService("SRV-VPN", "VPN Gateway");
        createService("SRV-SAP", "SAP ERP System");
        createService("SRV-HARDWARE", "Workstation Hardware");

        IncidentJpaEntity inc1 = new IncidentJpaEntity();
        inc1.setId("INC-TEST-1");
        inc1.setTitle("VPN connection drops unexpectedly");
        inc1.setDescription("GlobalProtect drops tunnel connection every 15 minutes");
        inc1.setReporterId("EMP-001");
        inc1.setAssigneeId("TECH-001");
        inc1.setCategory(IncidentCategory.NETWORK);
        inc1.setPriority(Priority.P2);
        inc1.setStatus(IncidentStatus.IN_PROGRESS);
        inc1.setServiceId("SRV-VPN");
        inc1.setCreatedAt(baseTime.minus(2, ChronoUnit.HOURS));
        inc1.setVersion(0L);

        IncidentJpaEntity inc2 = new IncidentJpaEntity();
        inc2.setId("INC-TEST-2");
        inc2.setTitle("SAP ERP invoice generation failure");
        inc2.setDescription("Database lock timeout when processing month-end billing");
        inc2.setReporterId("EMP-002");
        inc2.setAssigneeId("TECH-002");
        inc2.setCategory(IncidentCategory.DATABASE);
        inc2.setPriority(Priority.P1);
        inc2.setStatus(IncidentStatus.NEW);
        inc2.setServiceId("SRV-SAP");
        inc2.setCreatedAt(baseTime.minus(1, ChronoUnit.HOURS));
        inc2.setVersion(0L);

        IncidentJpaEntity inc3 = new IncidentJpaEntity();
        inc3.setId("INC-TEST-3");
        inc3.setTitle("Laptop screen flickering");
        inc3.setDescription("Display flickers intermittently on Dell Latitude");
        inc3.setReporterId("EMP-001");
        inc3.setAssigneeId("TECH-001");
        inc3.setCategory(IncidentCategory.HARDWARE);
        inc3.setPriority(Priority.P4);
        inc3.setStatus(IncidentStatus.CLOSED);
        inc3.setServiceId("SRV-HARDWARE");
        inc3.setCreatedAt(baseTime);
        inc3.setVersion(0L);

        incidentRepository.saveAll(List.of(inc1, inc2, inc3));
    }

    @Test
    @DisplayName("Filter by status matches expected incidents")
    void testFilterByStatus() {
        Specification<IncidentJpaEntity> spec = IncidentSpecifications.hasStatus(IncidentStatus.IN_PROGRESS);
        List<IncidentJpaEntity> results = incidentRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("INC-TEST-1");
    }

    @Test
    @DisplayName("Filter by priority matches expected incidents")
    void testFilterByPriority() {
        Specification<IncidentJpaEntity> spec = IncidentSpecifications.hasPriority(Priority.P1);
        List<IncidentJpaEntity> results = incidentRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("INC-TEST-2");
    }

    @Test
    @DisplayName("Filter by category matches expected incidents")
    void testFilterByCategory() {
        Specification<IncidentJpaEntity> spec = IncidentSpecifications.hasCategory(IncidentCategory.HARDWARE);
        List<IncidentJpaEntity> results = incidentRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("INC-TEST-3");
    }

    @Test
    @DisplayName("Filter by serviceId matches expected incidents")
    void testFilterByServiceId() {
        Specification<IncidentJpaEntity> spec = IncidentSpecifications.hasServiceId("SRV-SAP");
        List<IncidentJpaEntity> results = incidentRepository.findAll(spec);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("INC-TEST-2");
    }

    @Test
    @DisplayName("Filter by assigneeId matches expected incidents")
    void testFilterByAssigneeId() {
        Specification<IncidentJpaEntity> spec = IncidentSpecifications.hasAssigneeId("TECH-001");
        List<IncidentJpaEntity> results = incidentRepository.findAll(spec);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Filter by text search matches case-insensitively in title or description")
    void testFilterByTextSearch() {
        Specification<IncidentJpaEntity> specTitle = IncidentSpecifications.containsText("VPN");
        assertThat(incidentRepository.findAll(specTitle)).hasSize(1);

        Specification<IncidentJpaEntity> specDesc = IncidentSpecifications.containsText("database lock");
        assertThat(incidentRepository.findAll(specDesc)).hasSize(1);
    }

    @Test
    @DisplayName("Composite specification combines >= 5 criteria effectively")
    void testCompositeSpecification() {
        Specification<IncidentJpaEntity> composite = Specification.where(IncidentSpecifications.hasStatus(IncidentStatus.IN_PROGRESS))
                .and(IncidentSpecifications.hasPriority(Priority.P2))
                .and(IncidentSpecifications.hasCategory(IncidentCategory.NETWORK))
                .and(IncidentSpecifications.hasServiceId("SRV-VPN"))
                .and(IncidentSpecifications.hasReporterId("EMP-001"))
                .and(IncidentSpecifications.containsText("GlobalProtect"));

        List<IncidentJpaEntity> results = incidentRepository.findAll(composite);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("INC-TEST-1");
    }

    private void createUser(String id, String email, UserRole role) {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(id);
        user.setName(id);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyHash");
        user.setRole(role);
        user.setUserType(role.name());
        user.setActive(true);
        userRepository.save(user);
    }

    private void createService(String id, String name) {
        ItServiceJpaEntity service = new ItServiceJpaEntity();
        service.setId(id);
        service.setName(name);
        service.setBusinessCriticality(BusinessCriticality.TIER_2_DEPARTMENTAL);
        service.setUserBase(100);
        itServiceRepository.save(service);
    }
}
