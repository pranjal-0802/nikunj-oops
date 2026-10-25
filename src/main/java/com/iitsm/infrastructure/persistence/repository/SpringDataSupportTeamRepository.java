package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.infrastructure.persistence.entity.SupportTeamJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSupportTeamRepository extends JpaRepository<SupportTeamJpaEntity, String> {

    Optional<SupportTeamJpaEntity> findByName(String name);

    @Query("SELECT t FROM SupportTeamJpaEntity t JOIN t.handledCategories c WHERE c = :category")
    List<SupportTeamJpaEntity> findByHandledCategory(@Param("category") IncidentCategory category);
}
