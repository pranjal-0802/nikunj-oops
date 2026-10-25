# Claude Code Master Prompt: I-ITSM (Java, OOP-first)

## 0. How you (Claude Code) must work
You are a senior Java engineer and OOP mentor helping a student team build a university project: an Intelligent IT Service Management & Incident Resolution System (I-ITSM). The course is Object-Oriented Programming (CS F213). Grading priorities, in order: (1) working MVP functionality (60/120), (2) quality of object-oriented design and technical depth, (3) intelligent features and innovation, (4) tests, docs and a recorded demo. Design quality is the differentiator, so every important class must exist for a reason I can defend in a viva.

Operating rules:
1. Work phase by phase (Section 9). Never start a phase until I say "proceed to Phase N".
2. Start of every phase: re-read this file, CLAUDE.md and docs/DECISIONS.md. Post a short plan (packages, classes, tests you will create). Then implement.
3. End of every phase: run mvn verify (all tests green), write docs/phase-N-summary.md (what was built, which pattern is where and why, known gaps), refresh the PlantUML class diagram in docs/uml/, commit with a conventional message, then stop and wait.
4. First action ever: create CLAUDE.md in the repo root containing Sections 2, 3 and 4 of this prompt condensed into rules, so they persist across sessions.
5. If a requirement is ambiguous, pick the most defensible option, record it as an ADR line in docs/DECISIONS.md (decision, alternatives, reason), and continue. Ask me only if you are truly blocked.
6. Put a one-line Pattern: or OOP: note in the Javadoc of every pattern-bearing class (e.g. `Pattern: Strategy. Lets admins swap assignment logic at runtime.`).
7. Do not add features not listed here. Do not use Lombok magic that hides encapsulation on domain classes (records and explicit constructors are preferred; Lombok is allowed only on DTOs/config).

---

## 1. Project context

### 1.1 The course brief (summary)
Build an I-ITSM that manages the incident lifecycle Report → Classify → Prioritize → Assign → Investigate → Resolve → Verify → Close for three user groups. Mandatory (MVP): user management, incident CRUD and workflow, priority calculation (impact, urgency, business criticality), ≥ 2 swappable technician assignment strategies, SLA calculation/deadline tracking/violation detection, ≥ 2 notification mechanisms, complete audit trail, ≥ 5 search/filter criteria plus reports, ≥ 2 intelligent features, persistence, role-based security, custom exceptions, ≥ 2 design patterns, ≥ 15 documented tests, 3 demo workflows. Illegal state transitions (e.g. NEW → CLOSED) must be blocked using object-oriented design. Hard deadline: 22 Nov 2026.

### 1.2 Our group's PRD decisions (authoritative, these override generic ideas)
- **Employee** raises a ticket with only two required fields: Incident Title* and Detailed Description*. The employee does not choose a category, and does not see priority anywhere. The employee sees only their own tickets, a status stepper (NEW → ASSIGNED → IN PROGRESS → RESOLVED → CLOSED), assignee, created time, resolution text, and an Audit Trail link.
- **Technician** is the primary problem solver: sees assigned tickets, acknowledges, moves through assigned → acknowledged → in progress → resolved, adds investigation notes, requests more info from the employee, writes a structured resolution summary before marking resolved (end-user verification follows), sees a personal workload dashboard (tickets raised/resolved and at which stage), and has a way to flag a ticket as out of scope / wrong domain.
- **Administrator** has a full view of tickets and technicians (roster with skills, workload, online status; master incident directory with search and filters; stat cards: total, open/new, in progress, resolved, critical). Most assignment is automated; the admin is human-in-the-loop with granular control over domain assignment, prioritization and resolution. The admin can view each technician's ticket-solving progress.
- Ticket flow Create → View → Update → Assign → Resolve → Close is visible on all three persona pages for a given ticket.
- Prioritisation inputs (PRD): designation of the reporter, number of days the ticket has been open, history of the reporter's previous tickets.
- Assignment inputs (PRD): load, experience, skills.

### 1.3 Gaps in the PRD that the brief requires (we add them)
SLA management, notifications (≥ 2 channels), audit trail, search with ≥ 5 criteria, ≥ 2 intelligent features, impact/urgency/business-criticality in priority, a clean lifecycle with ACKNOWLEDGED and VERIFIED states, security. Section 8 merges the PRD's priority factors with the brief's factors.

---

## 2. Non-negotiable OOP and engineering rules
1. **Encapsulation**: aggregate roots (`Incident`, `Technician`, `SlaTracker`) have private fields, no public setters, and expose behaviour methods (`incident.acknowledge(by, now)`), not data mutation. Invariants are enforced in constructors and methods. Value objects (`Priority`, `Impact`, `Urgency`, `DeadlinePair`, `Email`, `Skill`) are immutable (records or final classes).
2. **Tell, don't ask**: services orchestrate; domain objects decide. No `if (status == X)` chains in services for lifecycle rules. Those live in State classes.
3. **Inheritance only for real is-a**: `User` (abstract) → `Employee`, `Technician`, `Administrator`; `Technician` → `TeamLead`. Everything else uses composition. Prefer interfaces over base classes for behaviour.
4. **Polymorphism through interfaces**: `AssignmentStrategy`, `PriorityFactor`, `IncidentClassifier`, `NotificationChannel`, `DomainEventListener`, `IncidentState`, `SimilarityMeasure`, `EscalationHandler`. Callers depend on the interface only.
5. **Open/Closed**: adding a new category, assignment strategy, notification channel or priority factor must require adding a class and a registration, never editing unrelated classes. Prove it with a test or a demo.
6. **Dependency direction**: web → application → domain ← infrastructure. The domain package must not import Spring. (JPA jakarta.persistence annotations on domain entities are an accepted, documented trade-off; log it in DECISIONS.md.)
7. **Constructor injection only**. No field injection. No static singletons; if a Singleton is wanted (e.g. a registry), let the Spring container own it.
8. **Time**: never call `Instant.now()` / `LocalDateTime.now()` in business code. Inject `java.time.Clock`. This enables SLA tests and the accelerated simulation in Phase 5.
9. **Exceptions**: a custom hierarchy under `IitsmException` (unchecked) with an error code: `InvalidStateTransitionException`, `TechnicianUnavailableException`, `TechnicianOverloadedException`, `ServiceNotFoundException`, `MissingSlaPolicyException`, `DuplicateIncidentException`, `UnauthorizedOperationException`, `InvalidCredentialsException`, `ValidationException`, `PersistenceFailureException`, `NotificationFailureException`. One `@RestControllerAdvice` maps them to consistent JSON errors. No swallowed exceptions; no `catch (Exception)` outside the global handler and notification channels.
10. **Collections**: choose deliberately and say why in a comment: `EnumMap` for state tables, `PriorityQueue` for the unassigned queue, `Deque` for escalation chains, `Map<String, Set<String>>` / inverted index for text features, `ConcurrentHashMap` for registries, unmodifiable views from getters.
11. **No business logic in controllers or repositories**. Controllers: validate, call one application service, map to a persona-specific DTO.
12. **Persona-specific DTOs**: `EmployeeIncidentView` (no priority, no internal notes), `TechnicianIncidentView`, `AdminIncidentView` (everything including priority breakdown). Hiding priority from the employee is enforced by the type of the response, not by the front end, and tested.
13. **Ownership and role checks happen in the application layer** (`AccessPolicy` interface), not only via `@PreAuthorize`. An employee requesting someone else's ticket gets `UnauthorizedOperationException`.
14. **Concurrency**: `@Version` optimistic locking on `Incident` and `Technician`; assignment runs inside a transaction so two incidents cannot overload one technician.
15. **Style**: Java 21, Google Java Style via spotless or checkstyle, meaningful names, methods under ~25 lines, no magic numbers (policy values live in configuration or enums).

---

## 3. Technology stack (decisions and options)
- **Language**: Java 21 (Course language, strong static typing, records, sealed types, pattern matching, first-class interfaces and generics)
- **Build**: Maven (multi-module optional, standard `mvn verify`)
- **Framework**: Spring Boot 3.3+ (Web, Validation, Data JPA, Security, Mail, Scheduling, Actuator)
- **Persistence**: PostgreSQL 16 (prod/demo), H2 (fast tests) with Flyway migrations
- **ORM**: Spring Data JPA / Hibernate, JPA Specification for search
- **Security**: Spring Security + JWT (jjwt), BCrypt, method security
- **Text intelligence**: Pure-Java TF-IDF + cosine similarity + Multinomial Naive Bayes written by us (No heavy dependency, fully explainable, demonstrates OOP)
- **Email**: Spring Mail (JavaMail) behind a `NotificationChannel` adapter; Mailpit (Docker) for dev; real SMTP/SendGrid for demo
- **Other channels**: In-app (persisted table + SSE push), SMS (simulated adapter)
- **API docs**: springdoc-openapi (Swagger UI)
- **Frontend**: React 18 + Vite + TypeScript + Tailwind, TanStack Query, React Router with role-guarded routes (Fallback: Thymeleaf + HTMX)
- **Testing**: JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc, JaCoCo (≥ 70% on domain), Testcontainers optional
- **Ops**: Docker Compose (app + Postgres + Mailpit), GitHub Actions running `mvn verify`
- **Libraries**: datafaker (synthetic people), mapstruct (DTO mapping, optional), commons-text (string utilities).

---

## 4. Architecture

### 4.1 Layers
- **Frontend** (React, 3 persona UIs: Employee portal, Technician console, Admin command centre)
- **Web**: `EmployeeController`, `TechnicianController`, `AdminController`, `AuthController`, `GlobalErrors`, persona DTOs + mappers + validation
- **Application**: `IncidentService`, `AssignmentService`, `PriorityService`, `ClassificationService`, `SlaService`, `EscalationService`, `SearchService`, `ReportingService`, `SimulationService` (orchestration, transactions, publishes domain events)
- **Domain (pure Java)**:
  - Model: `User` (abstract) → `Employee`, `Technician` → `TeamLead`, `Administrator` (AdminLevel); `Incident`, `ItService`, `SupportTeam`, `SlaPolicy`, `SlaTracker`, `Resolution`, `InvestigationNote`, `ReassignmentRequest`, ...
  - State: `IncidentState` + 9 state classes + `IncidentStates` registry
  - Strategy: `AssignmentStrategy`, `PriorityFactor`, `IncidentClassifier`, `SimilarityMeasure`
  - Event: `DomainEvent` (sealed) + `DomainEventPublisher` + `DomainEventListener` (Observer)
  - Repository: ports (`IncidentRepository`, `UserRepository`, ...)
  - Exception: `IitsmException` hierarchy
- **Infrastructure**:
  - Persistence: Spring Data JPA adapters, Specifications, Flyway
  - Notification: `InAppChannel`, `EmailChannel`, `SmsSimulatorChannel` (Adapters)
  - Security: JWT, BCrypt, `AccessPolicy` implementation
  - Scheduling: SLA monitor, aging recalculation
  - Observers: `AuditTrailObserver`, `NotificationObserver`, `CorrelationObserver`
  - Config

### 4.2 Package layout
`com.iitsm`
- `domain` (`model`, `state`, `strategy`, `policy`, `event`, `repository`, `exception`)
- `application` (`service`, `command`, `query`, `dto`)
- `infrastructure` (`persistence`, `notification`, `security`, `scheduling`, `observer`, `config`)
- `simulation` (`generator`, `scenario`)
- `web` (`controller`, `dto`, `mapper`, `error`)

### 4.3 Event-driven spine (Observer)
Sealed `DomainEvent` types: `IncidentCreated`, `IncidentClassified`, `PriorityCalculated`, `PriorityOverridden`, `DuplicateDetected`, `IncidentAssigned`, `ReassignmentRequested`, `IncidentAcknowledged`, `InvestigationStarted`, `MoreInfoRequested`, `IncidentResolved`, `ResolutionVerified`, `IncidentReopened`, `IncidentClosed`, `SlaWarning`, `SlaViolated`, `IncidentEscalated`.
`DomainEventPublisher` fans out to:
- `AuditTrailObserver`: writes `AuditRecord` synchronously in the same transaction.
- `NotificationObserver`: runs after commit; uses `NotificationFactory` and `NotificationRouter`; failing channel throws `NotificationFailureException` (logged, recorded, never rolls back transaction).
- `CorrelationObserver`: reacts to `IncidentCreated`/`IncidentClassified` to detect duplicates/correlations.

### 4.4 Lifecycle (State pattern)
Internal states: `NEW` → `CLASSIFIED` → `ASSIGNED` → `ACKNOWLEDGED` → `IN_PROGRESS` ⇄ `PENDING_INFO` → `RESOLVED` → `VERIFIED` → `CLOSED`.
Extra transitions:
- `ASSIGNED`/`ACKNOWLEDGED` → `CLASSIFIED` (wrong-domain flag approved, awaiting reassignment)
- `RESOLVED` → `IN_PROGRESS` (employee rejects the fix, reopen count +1)
- `CLASSIFIED` with low confidence goes to the admin triage queue.

Incident holds `IncidentStatus` enum and delegates actions (`assign`, `acknowledge`, `startInvestigation`, `requestInfo`, `provideInfo`, `resolve`, `verify`, `reject`, `close`, `flagWrongDomain`) to `IncidentState` resolved from `IncidentStates.of(status)` (stateless singletons in `EnumMap`). Illegal calls throw `InvalidStateTransitionException`.
`PublicStatusMapper` collapses internal states into the employee's 5-step stepper: `NEW`, `ASSIGNED`, `IN PROGRESS`, `RESOLVED`, `CLOSED`.

---

## 5. Domain model
- `User` (abstract: id, name, email, passwordHash, role, active). `Employee` (designation, department, Seniority), `Technician` (skills Set<Skill>, experienceYears, maxCapacity, AvailabilityStatus, SupportTeam, active incidents derived), `TeamLead` extends `Technician`, `Administrator` (AdminLevel: SYSTEM_ADMIN, IT_MANAGER).
- `ItService` (name, owning SupportTeam, BusinessCriticality 1–4, userBase, keywords), `IncidentCategory` (enum plus CategoryRegistry: HARDWARE, SOFTWARE, NETWORK, DATABASE, CLOUD, SECURITY, AUTHENTICATION, APPLICATION, STORAGE).
- `SupportTeam` (name, handled categories, TeamLead, members: aggregation).
- `Incident` (id, title, description, reporter, category, service, PriorityDecision, status, assignee, SlaTracker (composition), List<InvestigationNote>, Resolution, Feedback, reopenCount, linkedIncidents, version).
- `PriorityDecision` (value P1..P4, score, List<FactorContribution>, rule overrides applied, calculatedAt, overriddenBy/reason). Explainability is a feature.
- `SlaPolicy` (priority, optional service override, responseTarget, resolutionTarget, warningThreshold) and `SlaTracker` (responseDueAt, resolutionDueAt, respondedAt, resolvedAt, pausedDuration, SlaStatus ON_TRACK / AT_RISK / WARNING / BREACHED / MET). Clock pauses while PENDING_INFO.
- `ReassignmentRequest` (incident, technician, reason, status PENDING/APPROVED/REJECTED), `AuditRecord`, `Notification`, `EscalationRecord`.

---

## 6. The three personas
- **6.1 Employee portal**: Register/login (self-reg creates only EMPLOYEE; lockout after 5 failed logins). Raise Incident (title 5-120 chars, description 20-4000 chars, trimmed, HTML-escaped). My Tickets (status badge, stepper, resolution text, employee-safe audit trail, provide info on PENDING_INFO, verify/reject on RESOLVED, feedback 1-5 + comment on closed). Never sees priority, SLA internals, internal notes, others' tickets.
- **6.2 Technician console**: My Queue sorted by priority then SLA due time (`IncidentUrgencyOrdering`). Actions per ticket determined by state's `allowedActions()`: Acknowledge, Start investigation, Add note, Request more info (pauses SLA), Resolve (structured resolution: root cause, steps taken, preventive action, category confirmation), Flag wrong domain. Personal dashboard (active load vs capacity, tickets raised/assigned, count per stage, SLA at-risk, avg resolution time). Team Leads see team queue and escalated items.
- **6.3 Admin command centre**: Stat cards. Master Incident Directory (search/filter, assign, reassign, override priority with reason, resolve, close). Technician roster (skills, experience, workload, status, drill-down). Triage queue (low-confidence classifications & approved reassignment requests). Configuration (users, skills, services, categories, SLA policies, active assignment strategy switch live, priority weights/thresholds, notification preferences). Reports (workload, service analysis, SLA violations, MTTR by category, escalation log, audit log viewer). Simulation panel.

---

## 7. Simulating a company ("Nimbus FinServe")
- ~300 employees across 8 departments (Engineering, Finance, HR, Sales, Operations, Customer Support, Legal, Executive). Designations: Intern (0.15), Associate (0.30), Senior Associate (0.45), Manager (0.60), Senior Manager (0.70), Director (0.85), VP/C-level (1.00).
- 14 IT services with owning teams, business criticality (1-4), user bases.
- 6 Support teams with team leads and 2-4 technicians each. Plus SYSTEM_ADMIN and IT_MANAGER.
- Synthetic corpus generator (>= 600 samples, >= 50 per category) with seed 42 in `src/main/resources/sim/corpus.csv`. Separate hand-written test set of ~40 realistic tickets.
- `AdjustableClock` (x1, x60, x600). Scenarios S1-S5.

---

## 8. Intelligence design
- **8.1 Classification**: `TextNormalizer` -> `KeywordClassifier`, `NaiveBayesClassifier` / `TfIdfCentroidClassifier`, `CompositeClassifier` (fallback chain, confidence threshold >= 0.60, triage if lower). Service identification matching keywords and service names.
- **8.2 Priority**: Weighted sum of 6 factors: `ServiceCriticalityFactor` (0.30), `ImpactFactor` (0.20), `UrgencyFactor` (0.15), `ReporterDesignationFactor` (0.15), `AgingFactor` (0.10), `ReporterHistoryFactor` (0.10). Thresholds: score >= 0.75 -> P1, >= 0.55 -> P2, >= 0.35 -> P3, else P4. Override rules chain (SECURITY + breach -> at least P2; crit 4 + down/outage -> P1; >= 5 linked duplicates -> bump one level). Admin override with reason.
- **8.3 Assignment**: `AssignmentStrategy` interface (`AssignmentRecommendation recommend(Incident, List<Technician>)`). Strategies: `SkillBasedStrategy`, `LoadBasedStrategy`, `ExperienceBasedStrategy`, `RoundRobinStrategy`, `WeightedCompositeStrategy` (default: skills 0.5, load 0.3, experience 0.2, tuned by priority). `AssignmentStrategyRegistry`. `CandidateFilter` (same team first, cross-team fallback, capacity check, exclusion of rejector).
- **8.4 Duplicate detection & correlation**: `SimilarityMeasure` (`CosineTfIdfSimilarity`, `JaccardSimilarity`). Similarity >= 0.70 within last N hours -> `DuplicateDetected`, link to parent. Correlation raises parent's `ImpactFactor`.
- **8.5 SLA & Escalation**: `SlaService.attach(incident)` (P1 5m/30m, P2 15m/2h, P3 1h/8h, P4 4h/24h). `SlaMonitor` (AT_RISK 75%, WARNING 90%, BREACHED). SLA risk score detection. Chain of Responsibility `EscalationHandler`: Technician -> Team Lead -> System Admin -> IT Manager.

---

## 9. Phases
- **Phase 1**: Foundation and pure domain core (no web, no DB yet)
- **Phase 2**: Persistence, security and the simulated company
- **Phase 3**: Incident lifecycle services, REST API, audit and notifications
- **Phase 4**: Intelligence core (classification, prioritization, assignment, duplicates)
- **Phase 5**: SLA monitoring, escalation, reporting and the simulation engine
- **Phase 6**: Frontend, hardening, tests, documentation and demo

---

## 10. Testing requirements (Minimum 20 documented cases in `docs/testing/test-cases.md`)
- Normal, Boundary, Invalid input, Exceptions, Authorization, Business rules, Integration. Coverage >= 70% on domain and application.

---

## 11. Documentation deliverables
- `docs/01-problem-analysis.md`
- `docs/02-requirements.md`
- `docs/03-architecture.md`
- `docs/uml/` (use case, class, state, sequence diagrams in PlantUML + rendered PNG)
- `docs/db/` (ER diagram, schema, relationships)
- `docs/04-technology-report.md`
- `docs/05-design-patterns.md`
- `docs/security.md`
- `README.md`
- `docs/phase-N-summary.md` after each phase

---

## 12. Definition of done
- Every MVP item demonstrably working; employee never sees priority or other users' tickets.
- >= 6 patterns present and documented; Open/Closed proven by tests; domain package has zero Spring imports.
- Classification, prioritization, assignment, duplicate detection, SLA risk and escalation work on simulated company.
- `mvn verify` green in CI; docker compose up brings up working demo; seed and scenarios reproducible.
- All docs, UML, ER diagram, technology report, >= 20 documented tests ready by 22 Nov 2026.

