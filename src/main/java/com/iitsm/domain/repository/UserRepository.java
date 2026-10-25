package com.iitsm.domain.repository;

import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import java.util.List;
import java.util.Optional;

/**
 * Pattern: Repository.
 * Domain port for User hierarchy persistence and authentication lookups.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(String id);

    Optional<User> findByEmail(Email email);

    List<User> findByRole(UserRole role);

    List<User> findAll();

    long count();
}
