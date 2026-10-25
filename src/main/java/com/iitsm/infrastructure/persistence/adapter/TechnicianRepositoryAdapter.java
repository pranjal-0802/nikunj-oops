package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.UserRole;
import com.iitsm.domain.repository.TechnicianRepository;
import com.iitsm.infrastructure.persistence.entity.UserJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.UserMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataUserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * Pattern: Adapter / Repository.
 * Adapts SpringDataUserRepository queries for Technician entities to TechnicianRepository domain port.
 */
@Component
public class TechnicianRepositoryAdapter implements TechnicianRepository {

    private final SpringDataUserRepository springDataRepository;

    public TechnicianRepositoryAdapter(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public Technician save(Technician technician) {
        Objects.requireNonNull(technician, "technician cannot be null");
        try {
            UserJpaEntity jpaEntity = UserMapper.toJpaEntity(technician);
            UserJpaEntity saved = springDataRepository.save(jpaEntity);
            return (Technician) UserMapper.toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save technician: " + technician.getId(), ex);
        }
    }

    @Override
    public Optional<Technician> findById(String id) {
        try {
            return springDataRepository.findById(id)
                    .filter(u -> u.getRole() == UserRole.ROLE_TECHNICIAN || u.getRole() == UserRole.ROLE_TEAM_LEAD)
                    .map(u -> (Technician) UserMapper.toDomain(u));
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find technician by ID: " + id, ex);
        }
    }

    @Override
    public List<Technician> findBySupportTeamId(String supportTeamId) {
        try {
            return springDataRepository.findBySupportTeamId(supportTeamId).stream()
                    .filter(u -> u.getRole() == UserRole.ROLE_TECHNICIAN || u.getRole() == UserRole.ROLE_TEAM_LEAD)
                    .map(u -> (Technician) UserMapper.toDomain(u))
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find technicians by team ID: " + supportTeamId, ex);
        }
    }

    @Override
    public List<Technician> findAvailableTechnicians() {
        try {
            return springDataRepository.findAvailableTechnicians().stream()
                    .map(u -> (Technician) UserMapper.toDomain(u))
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to query available technicians", ex);
        }
    }

    @Override
    public List<Technician> findBySkillCategory(IncidentCategory category) {
        try {
            return springDataRepository.findTechniciansBySkillCategory(category).stream()
                    .map(u -> (Technician) UserMapper.toDomain(u))
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find technicians by skill category: " + category, ex);
        }
    }

    @Override
    public List<Technician> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .filter(u -> u.getRole() == UserRole.ROLE_TECHNICIAN || u.getRole() == UserRole.ROLE_TEAM_LEAD)
                    .map(u -> (Technician) UserMapper.toDomain(u))
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all technicians", ex);
        }
    }
}

