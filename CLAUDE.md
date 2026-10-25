# CLAUDE.md - I-ITSM Project Rules & Operating Guidelines

## Operating Protocol & Phase Workflow
1. **Phase-by-Phase Execution**: Never start a phase until explicit user approval ("proceed to Phase N").
2. **Phase Start**: Re-read `PROMPT.md`, `CLAUDE.md`, and `docs/DECISIONS.md`. Post a short plan (packages, classes, tests to create) and wait for approval.
3. **Phase Completion**: Run `mvn verify` (all tests green), create `docs/phase-N-summary.md` (what was built, which pattern is where and why, known gaps), refresh PlantUML diagrams in `docs/uml/`, commit with a conventional commit message, and wait.
4. **Pattern & OOP Documentation**: Include a one-line `Pattern:` or `OOP:` note in the Javadoc of every pattern-bearing class (e.g., `Pattern: Strategy. Lets admins swap assignment logic at runtime.`).
5. **Decisions & Ambiguity**: Record any architectural or design choice in `docs/DECISIONS.md` as an ADR (decision, alternatives, reason).
6. **Feature Discipline**: Strictly follow specifications. No extra unlisted features. No Lombok on domain entities (records and explicit constructors preferred; Lombok allowed only on DTOs/config).

---

## 1. Non-Negotiable OOP & Engineering Rules (Section 2)
1. **Encapsulation & Immutability**:
   - Aggregate roots (`Incident`, `Technician`, `SlaTracker`) have private fields, no public setters, and expose rich behaviour methods (e.g., `incident.acknowledge(by, now)`).
   - Invariants are enforced in constructors and domain methods.
   - Value objects (`Priority`, `Impact`, `Urgency`, `DeadlinePair`, `Email`, `Skill`) are immutable (Java `record` or `final` class).
2. **Tell, Don't Ask**:
   - Application services orchestrate transactions; domain objects make business decisions.
   - No `if (status == X)` conditionals in services for lifecycle rules; lifecycle logic is encapsulated within `IncidentState` implementations.
3. **Inheritance for True Is-A Only**:
   - Class hierarchy: `User` (abstract) → `Employee`, `Technician`, `Administrator`; `Technician` → `TeamLead`.
   - All other relationships use composition or interface implementation. Prefer interfaces over base classes for polymorphic behaviour.
4. **Polymorphism via Interfaces**:
   - Callers depend exclusively on interfaces: `AssignmentStrategy`, `PriorityFactor`, `IncidentClassifier`, `NotificationChannel`, `DomainEventListener`, `IncidentState`, `SimilarityMeasure`, `EscalationHandler`.
5. **Open/Closed Principle (OCP)**:
   - Adding a new category, assignment strategy, notification channel, or priority factor must require creating a new class and registering it, without editing existing classes.
   - Every extensibility point must be proven with unit tests or demos.
6. **Strict Dependency Direction**:
   - `web` → `application` → `domain` ← `infrastructure`.
   - Domain package must have zero dependencies on Spring Framework (`jakarta.persistence` annotations on domain entities are an accepted documented trade-off recorded in `docs/DECISIONS.md`).
7. **Dependency Injection**:
   - Constructor injection only. No field injection (`@Autowired` on fields is prohibited).
   - No static singletons. Singleton lifecycles are managed by the Spring container or domain registries.
8. **Time Abstraction**:
   - Never call `Instant.now()` or `LocalDateTime.now()` directly in business/domain code. Always inject `java.time.Clock` to facilitate deterministic SLA tests and simulation clock acceleration.
9. **Exception Hierarchy**:
   - All custom exceptions derive unchecked from `IitsmException` with standard error codes.
   - Core exceptions: `InvalidStateTransitionException`, `TechnicianUnavailableException`, `TechnicianOverloadedException`, `ServiceNotFoundException`, `MissingSlaPolicyException`, `DuplicateIncidentException`, `UnauthorizedOperationException`, `InvalidCredentialsException`, `ValidationException`, `PersistenceFailureException`, `NotificationFailureException`.
   - Handled centrally via `@RestControllerAdvice` mapping to consistent JSON error responses.
   - Never swallow exceptions; no raw `catch (Exception)` except at global boundaries and notification dispatchers.
10. **Deliberate Collection Selection**:
    - State lookup: `EnumMap`.
    - Unassigned queue: `PriorityQueue` with `IncidentUrgencyOrdering`.
    - Escalation chains: `Deque`.
    - Inverted indices / text features: `Map<String, Set<String>>`.
    - Registries: `ConcurrentHashMap`.
    - Getters must return unmodifiable collections (`Collections.unmodifiableList`, `Set.copyOf`, etc.).
11. **Controller & Repository Cleanliness**:
    - Zero business logic in controllers or repositories.
    - Controllers: validate input DTOs, invoke one application service method, map output to persona-specific DTOs.
12. **Persona-Specific DTO Projections**:
    - `EmployeeIncidentView`: stripped of priority, SLA internals, and internal technician notes. Hiding priority is enforced by response type, not UI filtering.
    - `TechnicianIncidentView`: includes queue priority, SLA due times, notes, and state-driven actions.
    - `AdminIncidentView`: complete inspection view including priority factor breakdowns and audit logs.
13. **Application-Layer Authorization**:
    - Ownership and role validation happen in application services via `AccessPolicy`, not solely via web security annotations.
    - An employee requesting another user's ticket throws `UnauthorizedOperationException`.
14. **Concurrency & Thread Safety**:
    - Optimistic locking via `@Version` on `Incident` and `Technician`.
    - Incident assignment runs in transactions to guarantee technician capacity invariants under race conditions.
15. **Code Style & Metrics**:
    - Java 21, Google Java Style formatting.
    - Method lengths under ~25 lines.
    - Zero magic numbers (policies in configuration or domain enums).

---

## 2. Technology Stack & Architectural Decisions (Section 3)
- **Language**: Java 21 (Records, sealed classes, pattern matching, strong static typing).
- **Build**: Maven (`mvn clean verify`).
- **Backend Framework**: Spring Boot 3.3+ (Web, Validation, Data JPA, Security, Mail, Scheduling, Actuator).
- **Persistence**: PostgreSQL 16 (production/demo profile), H2 (fast in-memory test profile) with Flyway migrations.
- **ORM / Querying**: Spring Data JPA + Hibernate; JPA Specifications for dynamic multi-criteria search.
- **Security**: Spring Security 6 + stateless JWT (`jjwt`), BCrypt password hashing, method-level security, 5-strike account lockout.
- **Text & Machine Learning Intelligence**: Pure-Java TF-IDF + Cosine Similarity + Multinomial Naive Bayes classifier (self-contained, explainable, zero heavy ML libraries).
- **Notifications**:
  - Email: `JavaMailSender` adapter targeting Mailpit in dev/demo or external SMTP.
  - In-App: Persisted notifications table + Server-Sent Events (SSE).
  - SMS: Simulated notification adapter.
- **API Documentation**: SpringDoc OpenAPI 3 (`/swagger-ui.html`).
- **Frontend**: React 18 + Vite + TypeScript + Tailwind CSS (3 role-based consoles: Employee, Technician, Admin).
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc, JaCoCo (≥ 70% coverage on domain and application).
- **Containerization & CI**: Docker Compose (`app`, `postgres`, `mailpit`), GitHub Actions CI running `mvn verify`.

---

## 3. Architecture & Package Structure (Section 4)

### 3.1 Layered Architecture
```
Frontend (React 18: Employee Portal | Technician Console | Admin Command Centre)
       │ (REST + JWT, SSE)
       ▼
Web Layer (com.iitsm.web)
       │ (Controllers, Persona DTOs, Mappers, GlobalExceptionHandler)
       ▼
Application Layer (com.iitsm.application)
       │ (IncidentService, AssignmentService, PriorityService, SlaService, etc.)
       ▼
Domain Layer (com.iitsm.domain - Pure Java Core, Zero Spring Imports)
       ▲ (Ports / Interfaces: Repositories, Publishers, Strategies)
       │
Infrastructure Layer (com.iitsm.infrastructure)
         (JPA Adapters, Flyway, Security, Notification Adapters, Observers, Schedulers)
```

### 3.2 Package Organization
```
com.iitsm
├── domain
│   ├── model          # Entities (Incident, User, ItService, SlaPolicy) & Value Objects
│   ├── state          # IncidentState interface, State implementations, IncidentStates registry
│   ├── strategy       # AssignmentStrategy, PriorityFactor, IncidentClassifier, SimilarityMeasure
│   ├── policy         # Business rules, CandidateFilter, AccessPolicy
│   ├── event          # Sealed DomainEvent hierarchy & listener ports
│   ├── repository     # Domain repository interfaces (ports)
│   └── exception      # IitsmException hierarchy
├── application
│   ├── service        # Application services orchestrating domain interactions
│   ├── command        # Command models for write operations
│   ├── query          # Query models for read operations
│   └── dto            # Internal application DTOs
├── infrastructure
│   ├── persistence    # Spring Data JPA repositories, JPA entities/adapters, specifications
│   ├── notification   # InAppChannel, EmailChannel, SmsChannel adapters
│   ├── security       # JWT auth, user details, AccessPolicy implementations
│   ├── scheduling     # SLA breach monitors, AgingFactor recomputation
│   ├── observer       # AuditTrailObserver, NotificationObserver, CorrelationObserver
│   └── config         # Spring configuration beans
├── simulation
│   ├── generator      # Synthetic company & incident data generators
│   └── scenario       # ScenarioRunner S1-S5 for live demonstrations
└── web
    ├── controller     # Persona-specific controllers (Employee, Technician, Admin, Auth)
    ├── dto            # Persona request/response DTOs (EmployeeIncidentView, etc.)
    ├── mapper         # Domain-to-DTO mappers
    └── error          # RestControllerAdvice, ApiError payload
```

### 3.3 Event-Driven Spine (Observer Pattern)
- Sealed `DomainEvent` hierarchy: `IncidentCreated`, `IncidentClassified`, `PriorityCalculated`, `PriorityOverridden`, `DuplicateDetected`, `IncidentAssigned`, `ReassignmentRequested`, `IncidentAcknowledged`, `InvestigationStarted`, `MoreInfoRequested`, `IncidentResolved`, `ResolutionVerified`, `IncidentReopened`, `IncidentClosed`, `SlaWarning`, `SlaViolated`, `IncidentEscalated`.
- Observers:
  - `AuditTrailObserver`: Synchronous, transactional recording of `AuditRecord`.
  - `NotificationObserver`: Post-commit asynchronous notification dispatching via `NotificationRouter`.
  - `CorrelationObserver`: Triggers duplicate analysis and impact reassessment upon ticket creation.

### 3.4 State Machine & Lifecycle (State Pattern)
- States: `NEW` → `CLASSIFIED` → `ASSIGNED` → `ACKNOWLEDGED` → `IN_PROGRESS` ⇄ `PENDING_INFO` → `RESOLVED` → `VERIFIED` → `CLOSED`.
- Reassignment: `ASSIGNED` / `ACKNOWLEDGED` → `CLASSIFIED` (wrong-domain approved).
- Reopen: `RESOLVED` → `IN_PROGRESS` (increment `reopenCount`).
- `Incident` delegates all actions to its current `IncidentState` instance.
- Illegal actions throw `InvalidStateTransitionException`.
- `PublicStatusMapper` projects internal states to Employee 5-step stepper: `NEW`, `ASSIGNED`, `IN PROGRESS`, `RESOLVED`, `CLOSED`.

