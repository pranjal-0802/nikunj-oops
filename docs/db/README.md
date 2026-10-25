# I-ITSM Database Documentation

## Schema Overview
The database schema is managed via Flyway migrations (`src/main/resources/db/migration/V1__init_schema.sql`). It supports PostgreSQL 16 in production/demo and H2 in-memory mode for fast automated test execution.

### Core Tables & Entity Relationships
1. **`users`**: Polymorphic base table storing authentication credentials, BCrypt hashes, active flags, and account lockout tracking (`failed_login_attempts`, `locked`).
2. **`employees`**: Sub-table holding department, designation, and seniority.
3. **`technicians`**: Sub-table holding max capacity, experience years, availability status, and owning support team.
4. **`administrators`**: Sub-table holding admin privilege tier (`SYSTEM_ADMIN`, `IT_MANAGER`).
5. **`support_teams`**: Support team entities mapping to team leads, member technician rosters, and handled categories.
6. **`it_services`**: Catalog of organizational services with business criticality (Tiers 1–4), user base size, and matching keywords.
7. **`sla_policies`**: Contractual response and resolution targets per priority and service overrides.
8. **`incidents`**: Central aggregate root storing lifecycle status, assignee, priority calculation breakdown, and `@Version` optimistic locking column.
9. **`sla_trackers`**: Tracks response/resolution deadlines, paused duration during `PENDING_INFO`, and SLA compliance status (`ON_TRACK`, `AT_RISK`, `WARNING`, `BREACHED`, `MET`).
10. **`investigation_notes`**: Chronological log of technician and employee investigation updates, flagged as internal or public.
11. **`resolutions`**: Structured technician resolution reports (root cause, steps taken, preventive action, confirmed category).
12. **`feedback`**: End-user ratings (1–5) and optional comments submitted after closure.
13. **`reassignment_requests`**: Technician wrong-domain flag requests reviewed by administrators.
14. **`audit_records`**: Immutable transactional log of all lifecycle actions and state changes.

### Indexed Columns for Query Optimization
- `incidents(status)`: Filters unassigned queues, active technician work, and status transitions.
- `incidents(priority)`: Enables priority-based ordering (`IncidentUrgencyOrdering`).
- `incidents(assignee_id)`: Quick retrieval of personal technician workloads.
- `incidents(service_id)`: Multi-criteria searching and duplicate correlation analysis.
- `incidents(created_at)`: Temporal reporting, MTTR metrics, and SLA window audits.
- `users(email)`: Rapid authentication lookup.
- `audit_records(incident_id)` & `audit_records(timestamp)`: Fast audit trail reconstruction.
