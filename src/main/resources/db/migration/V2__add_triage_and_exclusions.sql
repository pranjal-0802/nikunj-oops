-- =============================================================================
-- Flyway Migration V2: Triage Persistence, Technician Exclusions & Token Revocation
-- =============================================================================

-- 1. Add needs_triage to incidents table
ALTER TABLE incidents ADD COLUMN needs_triage BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_incidents_needs_triage ON incidents(needs_triage);

-- 2. Add excluded technicians collection table for incidents (wrong-domain routing)
CREATE TABLE incident_excluded_technicians (
    incident_id VARCHAR(64) NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    technician_id VARCHAR(64) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (incident_id, technician_id)
);
CREATE INDEX idx_incident_excluded_techs_incident ON incident_excluded_technicians(incident_id);

-- 3. Add token_version and locked_until to users table for revocation & time-based lockout
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
