# Database Schema Reference & Data Dictionary: I-ITSM

This document details the PostgreSQL relational database schema for the **Intelligent IT Service Management (I-ITSM)** system. All database structures are managed through Flyway migration `V1__init_schema.sql`.

---

## 1. Relational Entity Overview

The schema is normalized to 3NF and comprises 23 tables spanning core entities, polymorphic hierarchies, associative join collections, audit trails, and reporting structures:

| Table Name | Entity Representation | Primary Key | Foreign Keys & Relationships |
| :--- | :--- | :--- | :--- |
| `users` | Base User account | `id` (VARCHAR) | Polymorphic parent table for employees, technicians, and administrators. |
| `employees` | Business End-User | `user_id` (VARCHAR) | `user_id` $\rightarrow$ `users(id)` (ON DELETE CASCADE) |
| `technicians` | Technical Engineer | `user_id` (VARCHAR) | `user_id` $\rightarrow$ `users(id)`, `support_team_id` $\rightarrow$ `support_teams(id)` |
| `administrators` | System / IT Admin | `user_id` (VARCHAR) | `user_id` $\rightarrow$ `users(id)` |
| `support_teams` | Specialized Support Team | `id` (VARCHAR) | `team_lead_id` $\rightarrow$ `users(id)` |
| `support_team_members` | Team Membership join | `(team_id, member_id)` | `team_id` $\rightarrow$ `support_teams(id)`, `member_id` $\rightarrow$ `users(id)` |
| `support_team_categories`| Handled ITIL Categories | `(team_id, category)` | `team_id` $\rightarrow$ `support_teams(id)` |
| `technician_skills` | Domain Skill tokens | `(technician_id, skill_name)`| `technician_id` $\rightarrow$ `technicians(user_id)` |
| `technician_active_incidents`| Concurrency Tracker | `(technician_id, incident_id)`| Capacity invariant tracking |
| `it_services` | Service Catalog Asset | `id` (VARCHAR) | `owning_team_id` $\rightarrow$ `support_teams(id)` |
| `it_service_keywords` | Matching Lexicon | `(service_id, keyword)` | `service_id` $\rightarrow$ `it_services(id)` |
| `sla_policies` | Priority SLA Target | `priority` (VARCHAR) | 1:1 mapping with Priority P1–P4 |
| `incidents` | Incident Aggregate Root | `id` (VARCHAR) | `reporter_id` $\rightarrow$ `users`, `service_id` $\rightarrow$ `it_services`, `assignee_id` $\rightarrow$ `technicians` |
| `incident_linked_ids` | Duplicate Correlation | `(incident_id, linked_id)` | `incident_id` $\rightarrow$ `incidents(id)` |
| `factor_contributions` | Priority Audit Record | `(incident_id, factor_name)`| `incident_id` $\rightarrow$ `incidents(id)` |
| `sla_trackers` | SLA Operational Monitor | `incident_id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents(id)` (1:1 relationship) |
| `investigation_notes` | Private / Public Notes | `id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents(id)` |
| `resolutions` | Structured ITIL Fix | `incident_id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents(id)`, `resolved_by` $\rightarrow$ `users(id)` |
| `feedback` | Reporter Closure Rating | `incident_id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents(id)` |
| `reassignment_requests`| Wrong-Domain Flags | `id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents`, `requesting_technician_id` $\rightarrow$ `users` |
| `audit_records` | Immutable Event Spine | `id` (VARCHAR) | Centralized state transition and administrative action log |
| `notifications` | In-App / Simulated Alerts | `id` (VARCHAR) | `recipient_id` $\rightarrow$ `users(id)` |
| `escalation_records` | Multi-Tier Escalations | `id` (VARCHAR) | `incident_id` $\rightarrow$ `incidents(id)`, `to_user_id` $\rightarrow$ `users(id)` |

---

## 2. Table Column Specifications & Constraints

### `incidents` Table
- `id` (VARCHAR(64), PK): Unique business ID format `INC-XXXXXXXX` (UUID prefix).
- `title` (VARCHAR(255), NOT NULL): Incident summary, validated $5 \le \text{length} \le 120$.
- `description` (TEXT, NOT NULL): Comprehensive narrative, validated $20 \le \text{length} \le 4000$.
- `reporter_id` (VARCHAR(64), NOT NULL, FK $\rightarrow$ `users(id)`).
- `category` (VARCHAR(32)): One of 9 ITIL categories (`HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `SECURITY`, `CLOUD`, `EMAIL`, `ACCESS`, `APPLICATION`).
- `service_id` (VARCHAR(64), FK $\rightarrow$ `it_services(id)`): Linked infrastructure asset.
- `priority` (VARCHAR(16)): `P1`, `P2`, `P3`, or `P4`.
- `composite_score` (DOUBLE PRECISION): 6-factor mathematical score ($0.0 \dots 1.0$).
- `rules_applied` (TEXT): Audit record of policy override rules triggered (e.g., `RULE_CRITICAL_SERVICE_OUTAGE`).
- `status` (VARCHAR(32), NOT NULL): Active state in 9-state finite state machine.
- `assignee_id` (VARCHAR(64), FK $\rightarrow$ `technicians(user_id)`).
- `reopen_count` (INT, DEFAULT 0): Number of times resolution was rejected by reporter.
- `created_at` (TIMESTAMP WITH TIME ZONE, NOT NULL).
- `version` (BIGINT, DEFAULT 0, NOT NULL): Optimistic locking concurrency control.

### `sla_trackers` Table
- `incident_id` (VARCHAR(64), PK, FK $\rightarrow$ `incidents(id)`).
- `created_at` (TIMESTAMP WITH TIME ZONE, NOT NULL).
- `response_due_at` (TIMESTAMP WITH TIME ZONE, NOT NULL).
- `resolution_due_at` (TIMESTAMP WITH TIME ZONE, NOT NULL).
- `responded_at` (TIMESTAMP WITH TIME ZONE, NULLABLE).
- `resolved_at` (TIMESTAMP WITH TIME ZONE, NULLABLE).
- `paused_duration_seconds` (BIGINT, DEFAULT 0, NOT NULL): Accumulated pause time while in `PENDING_INFO`.
- `paused_at` (TIMESTAMP WITH TIME ZONE, NULLABLE).
- `status` (VARCHAR(32), NOT NULL): `ON_TRACK`, `AT_RISK`, `WARNING`, `BREACHED`.

### `escalation_records` Table
- `id` (VARCHAR(64), PK): Unique record identifier.
- `incident_id` (VARCHAR(64), NOT NULL, FK $\rightarrow$ `incidents(id)`).
- `tier` (INT, NOT NULL): Escalation hierarchy tier (1, 2, 3, or 4).
- `from_role` (VARCHAR(32), NOT NULL): Source role or system daemon initiating escalation.
- `to_role` (VARCHAR(32), NOT NULL): `TECHNICIAN`, `TEAM_LEAD`, `ADMINISTRATOR`, `IT_MANAGER`.
- `to_user_id` (VARCHAR(64), NOT NULL, FK $\rightarrow$ `users(id)`).
- `reason` (TEXT, NOT NULL): Operational trigger explanation.
- `timestamp` (TIMESTAMP WITH TIME ZONE, NOT NULL).

---

## 3. Database Indices & Performance Optimization
```sql
CREATE INDEX idx_incidents_status_category ON incidents(status, category);
CREATE INDEX idx_incidents_assignee ON incidents(assignee_id);
CREATE INDEX idx_incidents_reporter ON incidents(reporter_id);
CREATE INDEX idx_incidents_created_at ON incidents(created_at);
CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, read);
CREATE INDEX idx_escalations_incident_id ON escalation_records(incident_id);
CREATE INDEX idx_audit_records_incident_id ON audit_records(incident_id);
```

