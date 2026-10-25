package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.domain.model.Priority;
import com.iitsm.infrastructure.persistence.entity.SlaPolicyJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataSlaPolicyRepository extends JpaRepository<SlaPolicyJpaEntity, String> {

    @Query("SELECT p FROM SlaPolicyJpaEntity p WHERE p.priority = :priority AND p.serviceIdOverride = :serviceId")
    Optional<SlaPolicyJpaEntity> findByServiceIdAndPriority(
            @Param("serviceId") String serviceId, @Param("priority") Priority priority);

    @Query("SELECT p FROM SlaPolicyJpaEntity p WHERE p.priority = :priority AND p.serviceIdOverride IS NULL")
    Optional<SlaPolicyJpaEntity> findDefaultByPriority(@Param("priority") Priority priority);
}
