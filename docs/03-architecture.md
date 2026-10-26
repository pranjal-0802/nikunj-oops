# Architecture Specification: Intelligent IT Service Management (I-ITSM)

---

## 1. Architectural Philosophy & Hexagonal Structure

I-ITSM adopts a **Clean Architecture / Hexagonal (Ports & Adapters)** design. The system is partitioned into concentric layers with inward dependency direction. This structure is strictly verified on every build by ArchUnit tests in `ArchitectureRegressionTest`:

```
+-----------------------------------------------------------------------------------+
|                                 WEB LAYER                                         |
|    com.iitsm.web (Controllers, Persona DTOs, Mappers, RestControllerAdvice)      |
+-----------------------------------------------------------------------------------+
                                          | (calls)
                                          v
+-----------------------------------------------------------------------------------+
|                             APPLICATION LAYER                                     |
| com.iitsm.application (IncidentService, SlaService, UserService, AuditService)     |
| com.iitsm.application.port (TokenPort, IncidentSearchPort, RealtimePort)          |
+-----------------------------------------------------------------------------------+
                                          | (orchestrates)
                                          v
+-----------------------------------------------------------------------------------+
|                               DOMAIN LAYER (CORE)                                 |
| com.iitsm.domain (Entities, Value Objects, States, Strategies, Escalation Chains) |
| com.iitsm.domain.repository (IncidentRepository, UserRepository, Ports)           |
|           [PURE JAVA: ZERO SPRING DEPENDENCIES, ZERO SYSTEM TIME CALLS]           |
+-----------------------------------------------------------------------------------+
                                          ^ (implements ports)
                                          |
+-----------------------------------------------------------------------------------+
|                            INFRASTRUCTURE LAYER                                   |
| com.iitsm.infrastructure.persistence.adapter (JPA Adapters implementing Ports)   |
| com.iitsm.infrastructure.security, notification, scheduling, config               |
+-----------------------------------------------------------------------------------+
```

### Dependency Rules Enforced by ArchUnit:
1. **The Domain Core is Sovereign**: `com.iitsm.domain` contains business entities (`Incident`, `User`, `Technician`, `ItService`, `SlaTracker`), State machine, Strategy interfaces, and the Escalation chain. It has **zero dependencies** on Spring Framework (`org.springframework.*`) or JPA.
2. **Hexagonal Ports & Adapters**:
   - **Domain Repository Ports** (`com.iitsm.domain.repository`): `IncidentRepository`, `UserRepository`, `TechnicianRepository`, `SupportTeamRepository`, `ItServiceRepository`, `SlaPolicyRepository`, `AuditRepository`, `EscalationRecordRepository`, `ReassignmentRequestRepository`, `NotificationRepository`.
   - **Application Ports** (`com.iitsm.application.port`): `TokenPort`, `IncidentSearchPort`, `RealtimeNotificationPort`.
   - **Infrastructure Adapters** (`com.iitsm.infrastructure.persistence.adapter`): Implement domain/application ports using Spring Data JPA and entity mappers.
3. **Application & Web Isolation**:
   - `com.iitsm.application` and `com.iitsm.web` do not import or depend on `com.iitsm.infrastructure.*` (verified by `A1_applicationLayerMustNotDependOnInfrastructure` and `A1_webLayerMustNotDependOnInfrastructure`).
   - Controllers never return domain entities or JPA entities; all responses are projected through `com.iitsm.web.dto` (verified by `A1_controllersMustNotReturnDomainOrJpaEntities`).
   - Controllers depend on Application Services, never directly on repositories (verified by `A1_controllersMustNotDependOnRepositories`).

---

## 2. Package Organization

```
com.iitsm
├── domain
│   ├── model               # Entities (Incident, User, Technician, ItService, SlaTracker) & Value Objects
│   ├── state               # State Pattern: IncidentState, IncidentStates, 9 Concrete State implementations
│   ├── strategy
│   │   ├── nlp             # Pure Java NLP: TextNormalizer, TfIdfVectorizer
│   │   ├── classification  # Strategy: IncidentClassifier, KeywordClassifier, NaiveBayesClassifier, Composite
│   │   ├── priority        # Strategy: PriorityFactor (6 dimensions), CompositePriorityCalculator
│   │   ├── assignment      # Strategy: AssignmentStrategy (5 swappable concrete strategies), CandidateFilter
│   │   └── similarity      # Strategy: SimilarityMeasure, CosineTfIdfSimilarity, JaccardSimilarity
│   ├── chain.escalation    # Chain of Responsibility: EscalationHandler, Tiers 1-4, EscalationChainBuilder
│   ├── policy              # Polymorphic AccessPolicy, Business Criticality, Designation weights
│   ├── event               # Sealed DomainEvent hierarchy, listener interfaces
│   ├── repository          # Pure domain repository interfaces (Hexagonal Ports)
│   └── exception           # Standardized IitsmException hierarchy
├── application
│   ├── port                # Application-level ports: TokenPort, IncidentSearchPort, RealtimeNotificationPort
│   ├── service             # Application Services: IncidentService, SlaService, UserService, AuditService, etc.
│   └── dto                 # Application evaluation contexts and decision models
├── infrastructure
│   ├── persistence         # Spring Data JPA repositories, JPA entities, specification builders
│   │   ├── adapter         # Adapters implementing domain/application ports
│   │   ├── entity          # Relational JPA entities
│   │   └── mapper          # Bi-directional mappers between Domain and JPA entities
│   ├── security            # Spring Security 6, JwtTokenProvider, CustomUserDetailsService, LoginAttemptService
│   ├── notification        # Multi-channel notification adapters (Email, SMS simulator, SseEmitterRegistry)
│   ├── scheduling          # SlaMonitoringScheduler, UnassignedIncidentDispatcher
│   ├── observer            # AuditTrailObserver, NotificationObserver
│   └── config              # AdjustableClock, IntelligenceConfig, SecurityConfig
├── simulation              # Time-accelerated simulation engine: SimulationService, Scenarios S1-S5
└── web
    ├── controller          # EmployeeController, TechnicianController, AdminController, SimulationController, etc.
    ├── dto                 # Persona DTOs (EmployeeIncidentView, TechnicianIncidentView, AdminIncidentView, etc.)
    ├── mapper              # IncidentDtoMapper enforcing persona visibility boundaries
    └── error               # GlobalExceptionHandler, ApiError payload
```

---

## 3. Core Architectural Subsystems

### A. Finite State Machine (State Pattern)
- Every incident delegates lifecycle actions to its active `IncidentState`.
- `IncidentStates` registers stateless singleton state instances in an `EnumMap<IncidentStatus, IncidentState>`, ensuring zero heap allocations per transition.
- Disallowed actions throw `InvalidStateTransitionException` with exact error diagnostics.

### B. Event-Driven Spine (Observer Pattern)
- Domain state mutations emit immutable `DomainEvent` records through `DomainEventPublisher`.
- Sealed interface with 17 concrete event records (`IncidentCreated`, `IncidentClassified`, `PriorityCalculated`, `DuplicateDetected`, `IncidentAssigned`, `ReassignmentRequested`, `IncidentAcknowledged`, `InvestigationStarted`, `MoreInfoRequested`, `IncidentResolved`, `ResolutionVerified`, `IncidentReopened`, `IncidentClosed`, `SlaWarning`, `SlaViolated`, `IncidentEscalated`).
- Decouples audit logging (`AuditTrailObserver`) and notifications (`NotificationObserver`).

### C. Multi-Tier Escalation Engine (Chain of Responsibility Pattern)
- Evaluated by `EscalationHandler`:
  $$\text{Tier 4 (IT Manager)} \rightarrow \text{Tier 3 (System Admin)} \rightarrow \text{Tier 2 (Team Lead)} \rightarrow \text{Tier 1 (Technician)}$$
- Isolated handlers (`Tier4ItManagerHandler`, `Tier3SystemAdminHandler`, `Tier2TeamLeadHandler`, `Tier1TechnicianHandler`) assembled via `EscalationChainBuilder`.

### D. Time-Accelerated Simulation Engine (Decorator Pattern)
- `AdjustableClock` decorates standard `java.time.Clock`.
- Calculates virtual time dynamically via speed multipliers ($\times 1, \times 60, \times 600$) and discrete fast-forwards.
- Enables complete test scenarios (S1–S5) to execute in milliseconds without thread sleeping.
