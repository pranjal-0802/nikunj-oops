package com.iitsm.infrastructure.persistence.repository;

import com.iitsm.infrastructure.persistence.entity.ItServiceJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataItServiceRepository extends JpaRepository<ItServiceJpaEntity, String> {

    Optional<ItServiceJpaEntity> findByName(String name);

    List<ItServiceJpaEntity> findByOwningTeamId(String owningTeamId);
}

