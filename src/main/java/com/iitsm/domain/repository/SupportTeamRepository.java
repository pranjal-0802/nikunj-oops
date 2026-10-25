package com.iitsm.domain.repository;

import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.SupportTeam;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for Support Team aggregations.
 */
public interface SupportTeamRepository {

    SupportTeam save(SupportTeam team);

    Optional<SupportTeam> findById(String id);

    Optional<SupportTeam> findByName(String name);

    List<SupportTeam> findByHandledCategory(IncidentCategory category);

    List<SupportTeam> findAll();
}
