package com.iitsm.application.service;

import com.iitsm.domain.exception.DuplicateIncidentException;
import com.iitsm.domain.exception.InvalidCredentialsException;
import com.iitsm.domain.exception.UnauthorizedOperationException;
import com.iitsm.domain.exception.ValidationException;
import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.Administrator;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.Email;
import com.iitsm.domain.model.Employee;
import com.iitsm.domain.model.Skill;
import com.iitsm.domain.model.TeamLead;
import com.iitsm.domain.model.Technician;
import com.iitsm.domain.model.User;
import com.iitsm.domain.model.UserRole;
import com.iitsm.domain.repository.UserRepository;
import com.iitsm.infrastructure.security.JwtTokenProvider;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service managing user credentials, registration, and role lifecycle.
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final int maxFailedLogins;

    public UserService(
            UserRepository userRepository,
            UserFactory userFactory,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${iitsm.security.max-failed-logins:5}") int maxFailedLogins) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository cannot be null");
        this.userFactory = Objects.requireNonNull(userFactory, "userFactory cannot be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder cannot be null");
        this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider cannot be null");
        this.maxFailedLogins = maxFailedLogins;
    }

    public record AuthResult(String token, User user) {}

    public AuthResult authenticate(String emailStr, String rawPassword) {
        if (emailStr == null || rawPassword == null) {
            throw new InvalidCredentialsException("Email and password must be provided");
        }

        Email email = new Email(emailStr);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!user.isActive()) {
            throw new InvalidCredentialsException("User account is deactivated");
        }

        if (user.isLocked()) {
            throw new InvalidCredentialsException("Account is locked after too many failed login attempts");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            user.recordFailedLogin(maxFailedLogins);
            userRepository.save(user);
            if (user.isLocked()) {
                throw new InvalidCredentialsException("Account locked after 5 failed login attempts");
            }
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Successful authentication
        user.resetFailedLogins();
        userRepository.save(user);
        String token = tokenProvider.generateToken(user);
        return new AuthResult(token, user);
    }

    public Employee registerEmployee(
            String name,
            String emailStr,
            String rawPassword,
            Designation designation,
            String department,
            int seniorityYears) {

        Email email = new Email(emailStr);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("email", "Email address is already registered: " + emailStr);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        String id = "EMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Employee employee = userFactory.createEmployee(id, name, emailStr, passwordHash, designation, department, seniorityYears);
        return (Employee) userRepository.save(employee);
    }

    public Technician createTechnician(
            User creator,
            String name,
            String emailStr,
            String rawPassword,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {

        enforceAdminPrivileges(creator);

        Email email = new Email(emailStr);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("email", "Email address is already registered: " + emailStr);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        String id = "TECH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Technician technician = userFactory.createTechnician(
                id, name, emailStr, passwordHash, skills, experienceYears, maxCapacity, supportTeamId);
        return (Technician) userRepository.save(technician);
    }

    public TeamLead createTeamLead(
            User creator,
            String name,
            String emailStr,
            String rawPassword,
            Set<Skill> skills,
            int experienceYears,
            int maxCapacity,
            String supportTeamId) {

        enforceAdminPrivileges(creator);

        Email email = new Email(emailStr);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("email", "Email address is already registered: " + emailStr);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        String id = "LEAD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TeamLead lead = userFactory.createTeamLead(
                id, name, emailStr, passwordHash, skills, experienceYears, maxCapacity, supportTeamId);
        return (TeamLead) userRepository.save(lead);
    }

    public Administrator createAdministrator(
            User creator,
            String name,
            String emailStr,
            String rawPassword,
            AdminLevel adminLevel) {

        enforceAdminPrivileges(creator);

        Email email = new Email(emailStr);
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ValidationException("email", "Email address is already registered: " + emailStr);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        String id = "ADMIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Administrator admin = userFactory.createAdministrator(id, name, emailStr, passwordHash, adminLevel);
        return (Administrator) userRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(new Email(email));
    }

    private void enforceAdminPrivileges(User actor) {
        if (actor == null || !(actor instanceof Administrator)) {
            throw new UnauthorizedOperationException("Only administrators can create technicians or administrators");
        }
    }
}
