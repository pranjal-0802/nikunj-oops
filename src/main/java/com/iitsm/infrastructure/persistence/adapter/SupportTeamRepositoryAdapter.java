package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.IncidentCategory;
import com.iitsm.domain.model.SupportTeam;
import com.iitsm.domain.repository.SupportTeamRepository;
import com.iitsm.infrastructure.persistence.entity.SupportTeamJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.SupportTeamMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataSupportTeamRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class SupportTeamRepositoryAdapter implements SupportTeamRepository {

    private final SpringDataSupportTeamRepository springDataRepository;

    public SupportTeamRepositoryAdapter(SpringDataSupportTeamRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public SupportTeam save(SupportTeam team) {
        Objects.requireNonNull(team, "team cannot be null");
        try {
            SupportTeamJpaEntity jpaEntity = SupportTeamMapper.toJpaEntity(team);
            SupportTeamJpaEntity saved = springDataRepository.save(jpaEntity);
            return SupportTeamMapper.toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save support team: " + team.getId(), ex);
        }
    }

    @Override
    public Optional<SupportTeam> findById(String id) {
        try {
            return springDataRepository.findById(id).map(SupportTeamMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find support team by ID: " + id, ex);
        }
    }

    @Override
    public Optional<SupportTeam> findByName(String name) {
        try {
            return springDataRepository.findByName(name).map(SupportTeamMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find support team by name: " + name, ex);
        }
    }

    @Override
    public List<SupportTeam> findByHandledCategory(IncidentCategory category) {
        try {
            return springDataRepository.findByHandledCategory(category).stream()
                    .map(SupportTeamMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find support teams by category: " + category, ex);
        }
    }

    @Override
    public List<SupportTeam> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(SupportTeamMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all support teams", ex);
        }
    }
}
