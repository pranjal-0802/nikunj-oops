package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.ItService;
import com.iitsm.domain.repository.ItServiceRepository;
import com.iitsm.infrastructure.persistence.entity.ItServiceJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.ItServiceMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataItServiceRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class ItServiceRepositoryAdapter implements ItServiceRepository {

    private final SpringDataItServiceRepository springDataRepository;

    public ItServiceRepositoryAdapter(SpringDataItServiceRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public ItService save(ItService service) {
        Objects.requireNonNull(service, "service cannot be null");
        try {
            ItServiceJpaEntity jpaEntity = ItServiceMapper.toJpaEntity(service);
            ItServiceJpaEntity saved = springDataRepository.save(jpaEntity);
            return ItServiceMapper.toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save IT service: " + service.getId(), ex);
        }
    }

    @Override
    public Optional<ItService> findById(String id) {
        try {
            return springDataRepository.findById(id).map(ItServiceMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find IT service by ID: " + id, ex);
        }
    }

    @Override
    public Optional<ItService> findByName(String name) {
        try {
            return springDataRepository.findByName(name).map(ItServiceMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find IT service by name: " + name, ex);
        }
    }

    @Override
    public List<ItService> findByOwningTeamId(String owningTeamId) {
        try {
            return springDataRepository.findByOwningTeamId(owningTeamId).stream()
                    .map(ItServiceMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find IT services by owning team: " + owningTeamId, ex);
        }
    }

    @Override
    public List<ItService> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(ItServiceMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all IT services", ex);
        }
    }
}
