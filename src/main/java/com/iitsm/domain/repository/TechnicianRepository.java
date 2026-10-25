package com.iitsm.domain.repository;

import com.iitsm.domain.model.AvailabilityStatus;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Technician;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for Technician entity querying and assignment routing.
 */
public interface TechnicianRepository {

    Technician save(Technician technician);

    Optional<Technician> findById(String id);

    List<Technician> findBySupportTeamId(String supportTeamId);

    List<Technician> findAvailableTechnicians();

    List<Technician> findBySkillCategory(IncidentCategory category);

    List<Technician> findAll();
}

