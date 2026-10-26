package com.iitsm.application.service;

import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.User;
import com.iitsm.domain.repository.UserRepository;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing failed login attempt counting and account lockout.
 * Uses REQUIRES_NEW propagation to ensure failed attempt tracking persists even if the outer transaction rolls back.
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;
    private final int maxFailedLogins;

    public LoginAttemptService(
            UserRepository userRepository,
            @Value("${iitsm.security.max-failed-logins:5}") int maxFailedLogins) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.maxFailedLogins = maxFailedLogins;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String emailStr) {
        if (emailStr == null || emailStr.isBlank()) {
            return;
        }
        try {
            Email email = new Email(emailStr);
            userRepository.findByEmail(email).ifPresent(user -> {
                user.recordFailedLogin(maxFailedLogins);
                userRepository.save(user);
                log.warn("Recorded failed login attempt for '{}'. Total attempts: {}, Locked: {}",
                        emailStr, user.getFailedLoginAttempts(), user.isLocked());
            });
        } catch (Exception e) {
            log.warn("Could not record failed login attempt for '{}': {}", emailStr, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(String emailStr) {
        if (emailStr == null || emailStr.isBlank()) {
            return;
        }
        try {
            Email email = new Email(emailStr);
            userRepository.findByEmail(email).ifPresent(user -> {
                if (user.getFailedLoginAttempts() > 0) {
                    user.resetFailedLogins();
                    userRepository.save(user);
                    log.info("Reset failed login attempts for '{}'", emailStr);
                }
            });
        } catch (Exception e) {
            log.warn("Could not reset failed logins for '{}': {}", emailStr, e.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void unlockUser(String userId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        userRepository.findById(userId).ifPresent(user -> {
            user.resetFailedLogins();
            user.setLocked(false);
            userRepository.save(user);
            log.info("Admin unlocked user account '{}'", userId);
        });
    }
}

