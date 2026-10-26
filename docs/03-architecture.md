# Architecture Specification: Intelligent IT Service Management (I-ITSM)

## 1. Architectural Philosophy & Layered Structure
I-ITSM adopts a **Clean Architecture / Hexagonal (Ports & Adapters)** design. The system is partitioned into four strictly decoupled concentric layers with inward dependency direction:

```
+-----------------------------------------------------------------------------------+
|                                 WEB LAYER                                         |
|    com.iitsm.web (Controllers, Persona DTOs, Mappers, RestControllerAdvice)      |
+-----------------------------------------------------------------------------------+
                                          | (calls)
                                          v
+-----------------------------------------------------------------------------------+
|                             APPLICATION LAYER                                     |
| com.iitsm.application (IncidentService, SlaService, ReportingService, UserFactory)|
+-----------------------------------------------------------------------------------+
                                          | (orchestrates)
                                          v
+-----------------------------------------------------------------------------------+
|                               DOMAIN LAYER (CORE)                                 |
| com.iitsm.domain (Entities, Value Objects, States, Strategies, Escalation Chains) |
|           [PURE JAVA: ZERO SPRING DEPENDENCIES, ZERO SYSTEM TIME CALLS]           |
+-----------------------------------------------------------------------------------+
                                          ^ (implements ports)
                                          |
+-----------------------------------------------------------------------------------+
|                            INFRASTRUCTURE LAYER                                   |
| com.iitsm.infrastructure (JPA Adapters, Security, Notifications, Schedulers)      |
+-----------------------------------------------------------------------------------+
```

### Dependency Rules:
1. **The Domain Core is Sovereign**: `com.iitsm.domain` contains the business entities (`Incident`, `User`, `Technician`, `ItService`, `SlaTracker`), the State machine, Strategy interfaces, and the Escalation chain. It has zero dependencies on Spring Framework (`org.springframework.*`).
2. **Ports & Adapters (Hexagonal)**: Persistence interfaces (`IncidentRepository`, `UserRepository`, etc.) and event publisher ports (`DomainEventPublisher`) are declared in the domain layer. Spring Data JPA repositories and event publishers are implemented in `com.iitsm.infrastructure.persistence.adapter`.
3. **Inward Flow**: Controllers never interact directly with repositories; they invoke Application Services. Application Services orchestrate transactions and invoke rich domain methods.

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
│   ├── policy              # AccessPolicy, Business Criticality, Designation weights
│   ├── event               # Sealed DomainEvent hierarchy, listener interfaces
│   ├── repository          # Domain Repository Ports (pure interfaces)
│   └── exception           # Standardized IitsmException hierarchy
├── application
│   └── service             # Application Services: IncidentService, SlaService, ReportingService, etc.
├── infrastructure
│   ├── persistence         # Spring Data JPA repositories, JPA entities, adapters, specifications
│   ├── security            # Spring Security 6, JWT token provider, BCrypt, 5-strike lockout
│   ├── notification        # Multi-channel notification adapters (Email, SMS simulator, InApp)
│   ├── scheduling          # SlaMonitoringScheduler, background evaluators
│   ├── observer            # AuditTrailObserver, NotificationObserver
│   └── config              # AdjustableClock, DomainEventConfig, IntelligenceConfig
├── simulation              # Time-accelerated simulation engine: SimulationService, Scenarios S1-S5
└── web
    ├── controller          # EmployeeController, TechnicianController, AdminController, SimulationController
    ├── dto                 # Persona-specific DTOs (EmployeeIncidentView, TechnicianIncidentView, AdminIncidentView)
    ├── mapper              # DTO Mappers enforcing persona visibility boundaries
    └── error               # RestControllerAdvice, ApiError payload
```

---

## 3. Core Architectural Subsystems

### A. Finite State Machine (State Pattern)
- Every incident delegates lifecycle actions (`classify`, `assign`, `acknowledge`, `startInvestigation`, `requestInfo`, `provideInfo`, `resolve`, `verify`, `reject`, `close`, `flagWrongDomain`) to its active `IncidentState`.
- `IncidentStates` registers stateless singleton state instances in an `EnumMap<IncidentStatus, IncidentState>`, ensuring zero heap allocations per transition and thread-safe execution.
- Disallowed actions throw `InvalidStateTransitionException` with exact error diagnostics.

### B. Event-Driven Spine (Observer Pattern)
- Domain state mutations emit immutable `DomainEvent` records through `DomainEventPublisher`.
- `DomainEvent` is a sealed interface with 17 concrete event records:
  - `IncidentCreated`, `IncidentClassified`, `PriorityCalculated`, `DuplicateDetected`, `IncidentAssigned`, `ReassignmentRequested`, `IncidentAcknowledged`, `InvestigationStarted`, `MoreInfoRequested`, `IncidentResolved`, `ResolutionVerified`, `IncidentReopened`, `IncidentClosed`, `SlaWarning`, `SlaViolated`, `IncidentEscalated`.
- Observers:
  - **`AuditTrailObserver`**: Synchronously commits an immutable `AuditRecord` to the database.
  - **`NotificationObserver`**: Routes multi-channel messages (In-App, Simulated Email, Simulated SMS) to recipients.

### C. Multi-Tier Escalation Engine (Chain of Responsibility Pattern)
- Incident risk levels and SLA breaches are evaluated by `EscalationHandler`:
  $$\text{Tier 4 (IT Manager)} \rightarrow \text{Tier 3 (System Admin)} \rightarrow \text{Tier 2 (Team Lead)} \rightarrow \text{Tier 1 (Technician)}$$
- Encapsulates escalation criteria in isolated handlers (`Tier4ItManagerHandler`, `Tier3SystemAdminHandler`, `Tier2TeamLeadHandler`, `Tier1TechnicianHandler`) assembled via `EscalationChainBuilder`.

### D. Time-Accelerated Simulation Engine (Decorator Pattern)
- `AdjustableClock` decorates standard `java.time.Clock`.
- Calculates virtual time dynamically via:
  $$\text{VirtualTime} = \text{virtualAnchor} + (\text{RealNow} - \text{baseAnchor}) \times \text{multiplier} + \text{fastForwardOffset}$$
- Injected into all services, allowing hours of SLA degradation or complete scenarios (S1–S5) to execute in milliseconds without thread sleeping.

---

## 4. Concurrency & Data Consistency
1. **Optimistic Locking**: `@Version` timestamp/counter fields on `Incident` and `Technician` aggregate roots guard against concurrent overwrite race conditions.
2. **Transactional Boundaries**: Application service write methods are annotated with Spring's `@Transactional`, ensuring atomic commits across incident state changes, technician capacity updates, audit logs, and notification dispatches.
3. **Thread Safety**: Registries (`AssignmentStrategyRegistry`, `CategoryRegistry`) employ `ConcurrentHashMap`. Stateless strategies and states guarantee reentrant concurrency across HTTP worker threads.

