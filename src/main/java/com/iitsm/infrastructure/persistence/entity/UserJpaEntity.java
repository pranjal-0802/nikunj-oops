package com.iitsm.infrastructure.persistence.entity;

import com.iitsm.domain.model.AdminLevel;
import com.iitsm.domain.model.AvailabilityStatus;
import com.iitsm.domain.model.Designation;
import com.iitsm.domain.model.UserRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * JPA entity representing user persistence.
 * Maps User hierarchy and specific persona fields into a unified schema structure.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @Column(name = "user_type", nullable = false, length = 32)
    private String userType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // Employee specific fields (nullable for non-employees)
    @Enumerated(EnumType.STRING)
    @Column(name = "designation", length = 32)
    private Designation designation;

    @Column(name = "department", length = 64)
    private String department;

    @Column(name = "seniority_years")
    private Integer seniorityYears;

    // Technician specific fields (nullable for non-technicians)
    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", length = 32)
    private AvailabilityStatus availabilityStatus;

    @Column(name = "support_team_id", length = 64)
    private String supportTeamId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_skills", joinColumns = @JoinColumn(name = "technician_id"))
    private Set<TechnicianSkillEmbeddable> skills = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_active_incidents", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "incident_id")
    private Set<String> activeIncidentIds = new HashSet<>();

    // Administrator specific fields
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", length = 32)
    private AdminLevel adminLevel;

    @jakarta.persistence.Version
    @Column(name = "version")
    private Long version;

    public UserJpaEntity() {}

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Designation getDesignation() { return designation; }
    public void setDesignation(Designation designation) { this.designation = designation; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Integer getSeniorityYears() { return seniorityYears; }
    public void setSeniorityYears(Integer seniorityYears) { this.seniorityYears = seniorityYears; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public Integer getMaxCapacity() { return maxCapacity; }
    public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

    public AvailabilityStatus getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public String getSupportTeamId() { return supportTeamId; }
    public void setSupportTeamId(String supportTeamId) { this.supportTeamId = supportTeamId; }

    public Set<TechnicianSkillEmbeddable> getSkills() { return skills; }
    public void setSkills(Set<TechnicianSkillEmbeddable> skills) { this.skills = skills; }

    public Set<String> getActiveIncidentIds() { return activeIncidentIds; }
    public void setActiveIncidentIds(Set<String> activeIncidentIds) { this.activeIncidentIds = activeIncidentIds; }

    public AdminLevel getAdminLevel() { return adminLevel; }
    public void setAdminLevel(AdminLevel adminLevel) { this.adminLevel = adminLevel; }
}

