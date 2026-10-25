# Phase 2 Summary: Persistence, Security, and the Simulated Company

## 1. What Was Built
- **Flyway Database Schema (`src/main/resources/db/migration/V1__init_schema.sql`)**:
  - Full relational schema covering users, polymorphic employee/technician/admin columns, support teams, skills, active incidents, IT services with keywords, SLA policies, incidents, SLA trackers, investigation notes, resolutions, feedback, reassignment requests, audit records, notifications, and escalation records.
  - Dedicated B-Tree indexes created on `status`, `priority`, `assignee_id`, `service_id`, `created_at`, `reporter_id`, `email`, and `timestamp`.
  - Database ER Diagram committed to `docs/db/schema.puml` and documented in `docs/db/README.md`.
- **Spring Data JPA & Repository Pattern Adapters (`com.iitsm.infrastructure.persistence`)**:
  - JPA entities with `@Version` optimistic locking on `IncidentJpaEntity`.
  - Spring Data JPA interfaces extending `JpaRepository` and `JpaSpecificationExecutor`.
  - Adapters implementing domain repository ports:
    - `IncidentRepositoryAdapter implements IncidentRepository`
    - `UserRepositoryAdapter implements UserRepository`
    - `TechnicianRepositoryAdapter implements TechnicianRepository`
    - `ItServiceRepositoryAdapter implements ItServiceRepository`
    - `SupportTeamRepositoryAdapter implements SupportTeamRepository`
    - `SlaPolicyRepositoryAdapter implements SlaPolicyRepository`
  - Centralized translation of database and optimistic locking exceptions to `PersistenceFailureException`.
  - Mappers translating cleanly between pure domain entities and JPA models (`UserMapper`, `IncidentMapper`, `SupportTeamMapper`, `ItServiceMapper`, `SlaPolicyMapper`).
- **Security & JWT Subsystem (`com.iitsm.infrastructure.security`)**:
  - Stateless JWT token generator and validator (`JwtTokenProvider`) utilizing HMAC-SHA256.
  - `JwtAuthenticationFilter` for Bearer token resolution and `SecurityContext` propagation.
  - `SecurityConfig` configuring stateless sessions, method-level security (`@EnableMethodSecurity`), and BCrypt password encryption.
  - Custom `AuthenticationEntryPoint` and `AccessDeniedHandler` returning uniform JSON errors for `401 Unauthorized` and `403 Forbidden`.
  - Application-layer authorization policy ([`AccessPolicy`](file:///C:/Users/Dell/.gemini/antigravity/scratch/i-itsm/src/main/java/com/iitsm/domain/policy/AccessPolicy.java)) enforcing ownership and persona restrictions independent of web layer annotations.
  - Account lockout protection locking credentials upon the 5th consecutive failed attempt.
- **User Factory & User Application Service (`com.iitsm.application.service`)**:
  - `UserFactory`: Centralized creation and validation of `Employee`, `Technician`, `TeamLead`, and `Administrator`.
  - `UserService`: Manages registration, authentication, lockout increments/resets, and role enforcement.
- **Simulated Company Seeder ("Nimbus FinServe")**:
  - Deterministic generation using fixed seed `42` (`net.datafaker.Faker`).
  - Seeded:
    - 301 employees across 8 departments (Engineering, Finance, HR, Sales, Operations, Customer Support, Legal, Executive) with weighted designations.
    - 6 support teams: Network, Database & Storage, Application & Software, Security & Identity, Cloud Platform, Service Desk.
    - 24 technicians (6 Team Leads + 18 Technicians) with skills, experience, shifts, and capacities.
    - 2 Administrators: System Administrator (`admin@nimbusfinserve.com`) and IT Operations Manager (`manager@nimbusfinserve.com`).
    - 14 IT services with business criticalities, user bases, and keyword triggers.
    - 4 default SLA policies (P1 through P4).
- **DevOps & Tooling**:
  - `docker-compose.yml` for PostgreSQL 16 and Mailpit local testing.
  - `README.md` with environment requirements, demo credentials, and execution commands.

---

## 2. Design Patterns Implemented & Viva Defensibility

| Design Pattern | Location | Architectural Purpose & Rationale |
| :--- | :--- | :--- |
| **Repository Pattern (Ports & Adapters)** | `com.iitsm.domain.repository.*`<br>`com.iitsm.infrastructure.persistence.adapter.*` | Keeps the domain layer 100% free of Spring Data and Hibernate dependencies (Hexagonal Architecture). Adapters isolate JPA dirty-checking, transactions, and persistence exception translation. |
| **Optimistic Locking** | `IncidentJpaEntity.java` (`@Version`) | Safeguards against race conditions during simultaneous technician assignments without expensive pessimistic table locks. |
| **Factory Pattern** | `com.iitsm.application.service.UserFactory.java` | Encapsulates the instantiation of polymorphic `User` subtypes (`Employee`, `Technician`, `TeamLead`, `Administrator`) and ensures domain invariants are satisfied prior to persistence. |
| **Policy Pattern** | `com.iitsm.domain.policy.AccessPolicy.java` | Decouples data ownership and lifecycle permission checks from web controllers. Ensures employee ticket privacy is enforced inside application logic. |
| **Adapter Pattern** | `com.iitsm.infrastructure.persistence.mapper.*` | Bridges the impedance mismatch between rich domain aggregates and relational JPA entities. |

---

## 3. Verification & Metrics
- **Total Test Cases**: **75 unit and integration tests, 100% green**.
- **JaCoCo Coverage**: All coverage rules passed; line coverage across domain and application exceeds 70%.
- **Exit Criteria Validation**:
  - Application boots on H2 with Flyway migrations.
  - Seeded ~300 employees (301), 6 teams, 24 technicians, 14 services, 4 SLA policies verified via `CompanySeederIntegrationTest`.
  - All 5 demo accounts authenticate successfully and issue valid JWT tokens (`employee@...`, `tech.network@...`, `lead.network@...`, `admin@...`, `manager@...`).
  - Account lockout verified after 5 failed login attempts.
  - Optimistic locking collision correctly translated to `PersistenceFailureException`.
  - Unauthorized access returns consistent 401 JSON (`{"errorCode":"ERR_UNAUTHORIZED",...}`); forbidden access returns consistent 403 JSON (`{"errorCode":"ERR_FORBIDDEN",...}`).

---

## 4. Known Gaps & Next Steps (Phase 3)
1. **REST API Controllers**: Expose persona-specific REST endpoints for Employee, Technician, and Admin.
2. **Persona DTO Projections**: Implement `EmployeeIncidentView` (priority hidden), `TechnicianIncidentView`, and `AdminIncidentView`.
3. **Notification Subsystem**: Implement `NotificationChannel` interface with `InAppChannel` (SSE), `EmailChannel` (Mailpit JavaMail adapter), and `SmsSimulatorChannel`.
4. **Observer Wiring**: Wire domain event publishing to transactional `AuditTrailObserver` and post-commit `NotificationObserver`.
5. **Multi-Criteria Search**: Implement `SearchService` using composable JPA Specifications with $\ge 5$ search criteria.

