package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.Priority;
import com.iitsm.domain.model.SlaPolicy;
import com.iitsm.domain.repository.SlaPolicyRepository;
import com.iitsm.infrastructure.persistence.entity.SlaPolicyJpaEntity;
import com.iitsm.infrastructure.persistence.mapper.SlaPolicyMapper;
import com.iitsm.infrastructure.persistence.repository.SpringDataSlaPolicyRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
public class SlaPolicyRepositoryAdapter implements SlaPolicyRepository {

    private final SpringDataSlaPolicyRepository springDataRepository;

    public SlaPolicyRepositoryAdapter(SpringDataSlaPolicyRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public SlaPolicy save(SlaPolicy policy) {
        Objects.requireNonNull(policy, "policy cannot be null");
        try {
            SlaPolicyJpaEntity jpaEntity = SlaPolicyMapper.toJpaEntity(policy);
            SlaPolicyJpaEntity saved = springDataRepository.save(jpaEntity);
            return SlaPolicyMapper.toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save SLA policy: " + policy.getId(), ex);
        }
    }

    @Override
    public Optional<SlaPolicy> findById(String id) {
        try {
            return springDataRepository.findById(id).map(SlaPolicyMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find SLA policy by ID: " + id, ex);
        }
    }

    @Override
    public Optional<SlaPolicy> findByServiceIdAndPriority(String serviceId, Priority priority) {
        Objects.requireNonNull(priority, "priority cannot be null");
        try {
            return springDataRepository.findByServiceIdAndPriority(serviceId, priority)
                    .map(SlaPolicyMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find SLA policy for service: " + serviceId, ex);
        }
    }

    @Override
    public Optional<SlaPolicy> findDefaultByPriority(Priority priority) {
        Objects.requireNonNull(priority, "priority cannot be null");
        try {
            return springDataRepository.findDefaultByPriority(priority)
                    .map(SlaPolicyMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find default SLA policy for priority: " + priority, ex);
        }
    }

    @Override
    public List<SlaPolicy> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(SlaPolicyMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all SLA policies", ex);
        }
    }
}
