package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.IncidentStatus;
import com.iitsm.infrastructure.persistence.entity.IncidentJpaEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataIncidentRepository
        extends JpaRepository<IncidentJpaEntity, String>, JpaSpecificationExecutor<IncidentJpaEntity> {

    List<IncidentJpaEntity> findByReporterId(String reporterId);

    List<IncidentJpaEntity> findByAssigneeId(String assigneeId);

    List<IncidentJpaEntity> findByStatus(IncidentStatus status);

    List<IncidentJpaEntity> findByCategory(IncidentCategory category);

    @Query("SELECT i FROM IncidentJpaEntity i WHERE i.serviceId = :serviceId AND i.category = :category AND i.createdAt >= :since AND i.status NOT IN ('RESOLVED', 'VERIFIED', 'CLOSED')")
    List<IncidentJpaEntity> findOpenIncidentsByServiceAndCategorySince(
            @Param("serviceId") String serviceId,
            @Param("category") IncidentCategory category,
            @Param("since") Instant since);
}

