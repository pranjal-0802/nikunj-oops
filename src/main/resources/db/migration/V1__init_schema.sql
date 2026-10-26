-- =============================================================================
-- Flyway Migration V1: Initial Schema for I-ITSM
-- =============================================================================

-- 1. Users Hierarchy Table
CREATE TABLE users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    user_type VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Polymorphic Persona Fields
    designation VARCHAR(32),
    department VARCHAR(64),
    seniority_years INT,
    experience_years INT,
    max_capacity INT,
    availability_status VARCHAR(32),
    support_team_id VARCHAR(64),
    admin_level VARCHAR(32),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- 2. Employee Profile Details
CREATE TABLE employees (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    designation VARCHAR(32) NOT NULL,
    department VARCHAR(64) NOT NULL,
    seniority_years INT NOT NULL DEFAULT 0
);

-- 3. Support Teams Table
CREATE TABLE support_teams (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    team_lead_id VARCHAR(64)
);

-- 4. Technician Profile Details
CREATE TABLE technicians (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    experience_years INT NOT NULL DEFAULT 0,
    max_capacity INT NOT NULL DEFAULT 5,
    availability_status VARCHAR(32) NOT NULL DEFAULT 'ONLINE',
    support_team_id VARCHAR(64) REFERENCES support_teams(id) ON DELETE SET NULL
);

-- Support Team Members
CREATE TABLE support_team_members (
    support_team_id VARCHAR(64) NOT NULL REFERENCES support_teams(id) ON DELETE CASCADE,
    technician_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (support_team_id, technician_id)
);

-- Support Team Handled Categories
CREATE TABLE support_team_categories (
    support_team_id VARCHAR(64) NOT NULL REFERENCES support_teams(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL,
    PRIMARY KEY (support_team_id, category)
);

-- Technician Skills
CREATE TABLE technician_skills (
    technician_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_name VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    PRIMARY KEY (technician_id, skill_name, category)
);

-- Technician Active Assigned Incident IDs
CREATE TABLE technician_active_incidents (
    technician_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    incident_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (technician_id, incident_id)
);

-- 5. Administrator Profile Details
CREATE TABLE administrators (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    admin_level VARCHAR(32) NOT NULL
);

-- 6. IT Services Catalogue
CREATE TABLE it_services (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE,
    owning_team_id VARCHAR(64) REFERENCES support_teams(id) ON DELETE SET NULL,
    business_criticality VARCHAR(32) NOT NULL,
    user_base INT NOT NULL DEFAULT 0
);

CREATE TABLE it_service_keywords (
    service_id VARCHAR(64) NOT NULL REFERENCES it_services(id) ON DELETE CASCADE,
    keyword VARCHAR(64) NOT NULL,
    PRIMARY KEY (service_id, keyword)
);

-- 7. SLA Policies
CREATE TABLE sla_policies (
    id VARCHAR(64) PRIMARY KEY,
    priority VARCHAR(16) NOT NULL,
    service_id_override VARCHAR(64) REFERENCES it_services(id) ON DELETE CASCADE,
    response_target_seconds BIGINT NOT NULL,
    resolution_target_seconds BIGINT NOT NULL,
    warning_threshold_ratio DOUBLE PRECISION NOT NULL DEFAULT 0.75
);

-- 8. Incidents Table (Aggregate Root)
CREATE TABLE incidents (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    reporter_id VARCHAR(64) NOT NULL REFERENCES users(id),
    category VARCHAR(32),
    service_id VARCHAR(64) REFERENCES it_services(id),
    priority VARCHAR(16),
    composite_score DOUBLE PRECISION,
    rules_applied TEXT,
    priority_calculated_at TIMESTAMP WITH TIME ZONE,
    priority_overridden_by VARCHAR(64),
    priority_override_reason TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    assignee_id VARCHAR(64) REFERENCES users(id),
    reopen_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Required Performance & Reporting Indexes
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_priority ON incidents(priority);
CREATE INDEX idx_incidents_assignee ON incidents(assignee_id);
CREATE INDEX idx_incidents_service ON incidents(service_id);
CREATE INDEX idx_incidents_created_at ON incidents(created_at);
CREATE INDEX idx_incidents_reporter ON incidents(reporter_id);

-- Incident Linked Duplicates
CREATE TABLE incident_linked_ids (
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    linked_incident_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (incident_id, linked_incident_id)
);

-- Factor Contributions (Transparency for Priority calculation)
CREATE TABLE factor_contributions (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    factor_name VARCHAR(64) NOT NULL,
    raw_score DOUBLE PRECISION NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    weighted_score DOUBLE PRECISION NOT NULL,
    explanation TEXT
);

CREATE INDEX idx_factor_contributions_incident ON factor_contributions(incident_id);

-- 9. SLA Trackers Table (Composition with Incident)
CREATE TABLE sla_trackers (
    incident_id VARCHAR(64) PRIMARY KEY REFERENCES incidents(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    response_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolution_due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    paused_duration_seconds BIGINT NOT NULL DEFAULT 0,
    paused_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(32) NOT NULL DEFAULT 'ON_TRACK'
);

-- 10. Investigation Notes
CREATE TABLE investigation_notes (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    author_id VARCHAR(64) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    note_text TEXT NOT NULL,
    internal_only BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_investigation_notes_incident ON investigation_notes(incident_id);

-- 11. Resolutions Table
CREATE TABLE resolutions (
    incident_id VARCHAR(64) PRIMARY KEY REFERENCES incidents(id) ON DELETE CASCADE,
    root_cause TEXT NOT NULL,
    steps_taken TEXT NOT NULL,
    preventive_action TEXT NOT NULL,
    confirmed_category VARCHAR(32) NOT NULL,
    resolved_by VARCHAR(64) NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 12. End-User Feedback Table
CREATE TABLE feedback (
    incident_id VARCHAR(64) PRIMARY KEY REFERENCES incidents(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- 13. Reassignment Requests Table
CREATE TABLE reassignment_requests (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    requesting_technician_id VARCHAR(64) NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    reviewed_by_admin_id VARCHAR(64) REFERENCES users(id)
);

CREATE INDEX idx_reassignment_incident ON reassignment_requests(incident_id);

-- 14. Audit Records Table
CREATE TABLE audit_records (
    id VARCHAR(64) PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    incident_id VARCHAR(64) NOT NULL,
    previous_state VARCHAR(32),
    new_state VARCHAR(32),
    description TEXT NOT NULL
);

CREATE INDEX idx_audit_records_incident ON audit_records(incident_id);
CREATE INDEX idx_audit_records_timestamp ON audit_records(timestamp);

-- 15. In-App and Email Notifications Table
CREATE TABLE notifications (
    id VARCHAR(64) PRIMARY KEY,
    recipient_id VARCHAR(64) NOT NULL REFERENCES users(id),
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    delivered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id);

-- 16. Escalation Records Table
CREATE TABLE escalation_records (
    id VARCHAR(64) PRIMARY KEY,
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    from_role VARCHAR(32) NOT NULL,
    to_role VARCHAR(32) NOT NULL,
    to_user_id VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    escalated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_escalation_incident ON escalation_records(incident_id);
