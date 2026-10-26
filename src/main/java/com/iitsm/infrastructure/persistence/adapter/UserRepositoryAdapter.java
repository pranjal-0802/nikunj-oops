package com.iitsm.infrastructure.persistence.adapter;

import com.iitsm.domain.exception.PersistenceFailureException;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import com.iitsm.domain.repository.UserRepository;
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
 * Adapts SpringDataUserRepository to the pure Java UserRepository domain port.
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = Objects.requireNonNull(springDataRepository, "springDataRepository cannot be null");
    }

    @Override
    public User save(User user) {
        Objects.requireNonNull(user, "user cannot be null");
        try {
            UserJpaEntity jpaEntity = UserMapper.toJpaEntity(user);
            UserJpaEntity saved = springDataRepository.save(jpaEntity);
            user.setVersion(saved.getVersion());
            return UserMapper.toDomain(saved);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to save user: " + user.getId(), ex);
        }
    }

    @Override
    public Optional<User> findById(String id) {
        try {
            return springDataRepository.findById(id).map(UserMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find user by ID: " + id, ex);
        }
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        Objects.requireNonNull(email, "email cannot be null");
        try {
            return springDataRepository.findByEmail(email.value()).map(UserMapper::toDomain);
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find user by email: " + email, ex);
        }
    }

    @Override
    public List<User> findByRole(UserRole role) {
        try {
            return springDataRepository.findByRole(role).stream()
                    .map(UserMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to find users by role: " + role, ex);
        }
    }

    @Override
    public List<User> findAll() {
        try {
            return springDataRepository.findAll().stream()
                    .map(UserMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to list all users", ex);
        }
    }

    @Override
    public long count() {
        try {
            return springDataRepository.count();
        } catch (DataAccessException ex) {
            throw new PersistenceFailureException("Failed to count users", ex);
        }
    }
}

