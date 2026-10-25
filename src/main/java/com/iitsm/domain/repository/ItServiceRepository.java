package com.iitsm.domain.repository;

import com.iitsm.domain.model.ItService;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for IT Service catalog entries.
 */
public interface ItServiceRepository {

    ItService save(ItService service);

    Optional<ItService> findById(String id);

    Optional<ItService> findByName(String name);

    List<ItService> findByOwningTeamId(String owningTeamId);

    List<ItService> findAll();
}

