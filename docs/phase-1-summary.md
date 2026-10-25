# Phase 1 Summary: Foundation and Pure Domain Core

## 1. What Was Built
- **Project Scaffold**: Standard Maven project (`pom.xml`) configured for Java 21, JUnit 5, AssertJ, Mockito, and JaCoCo coverage enforcement.
- **Repository Initialization**: Git tracking with `.gitignore`, `.github/workflows/ci.yml`, `PROMPT.md`, `CLAUDE.md`, and initial `docs/DECISIONS.md`.
- **Value Objects & Enums (`com.iitsm.domain.model`)**:
  - `IncidentStatus`: 9 internal lifecycle states.
  - `Priority`: P1..P4 with numeric ranks and normalized score mapping.
  - `Impact` & `Urgency`: Normalized 3-tier scales.
  - `BusinessCriticality`: Tiers 1–4 with normalized impact scores.
  - `AvailabilityStatus`: ONLINE, BUSY, OFFLINE.
  - `SlaStatus`: ON_TRACK, AT_RISK (75%), WARNING (90%), BREACHED, MET.
  - `AdminLevel`: SYSTEM_ADMIN, IT_MANAGER.
  - `Designation`: 7 organizational levels with capped priority weights (0.15 to 1.00).
  - `IncidentCategory` & `CategoryRegistry`: Enum plus thread-safe dynamic registry (`ConcurrentHashMap`).
  - Immutable Value Records: `Skill`, `Email`, `DeadlinePair`, `FactorContribution`, `PriorityDecision`, `Resolution`, `InvestigationNote`, `Feedback`.
- **Entities & Aggregate Roots (`com.iitsm.domain.model`)**:
  - `User` (abstract), `Employee`, `Technician`, `TeamLead` (extends `Technician`), `Administrator`.
  - `ItService`: Service catalogue entity with keyword matching.
  - `SupportTeam`: Team aggregation (`SupportTeam o-- Technician`).
  - `Incident`: Aggregate root with private fields, no public setters, invariant checks, HTML sanitization, and state delegation.
  - `SlaPolicy` & `SlaTracker`: SLA deadline computation, dynamic pausing/resuming during `PENDING_INFO`, and breach evaluation.
  - `ReassignmentRequest`: Wrong-domain re-routing tracking.
- **State Pattern Subsystem (`com.iitsm.domain.state`)**:
  - `IncidentState` interface with safe defaults throwing `InvalidStateTransitionException`.
  - 9 State classes: `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState`.
  - `IncidentStates`: Registry holding stateless singletons in an `EnumMap`.
  - `PublicStatusMapper`: Projects the 9 internal states into the Employee 5-step stepper (`NEW`, `ASSIGNED`, `IN PROGRESS`, `RESOLVED`, `CLOSED`).
- **Domain Event Spine (`com.iitsm.domain.event`)**:
  - Sealed `DomainEvent` hierarchy permitting all 17 distinct event records.
  - `DomainEventListener` interface and `DomainEventPublisher` port.
  - `InMemoryDomainEventPublisher`: Thread-safe publication via `CopyOnWriteArrayList`.
- **Repository Ports (`com.iitsm.domain.repository`)**:
  - Clean interfaces with zero framework imports: `IncidentRepository`, `UserRepository`, `TechnicianRepository`, `ItServiceRepository`, `SupportTeamRepository`, `SlaPolicyRepository`.
- **Demonstration (`com.iitsm.domain.demo`)**:
  - `DomainLifecycleDemo`: Executable Java application proving an incident transitioning through all states from `NEW` to `CLOSED` without frameworks or databases.

---

## 2. Design Patterns Implemented & Viva Defensibility

| Design Pattern | Location | Architectural Purpose & Rationale |
| :--- | :--- | :--- |
| **State Pattern** | `com.iitsm.domain.state.*` | Encapsulates lifecycle transition invariants and allowed actions within dedicated state classes. Eliminates procedural `if (status == X)` conditionals in application services and enforces the "Tell, Don't Ask" principle. |
| **Flyweight / Registry** | `IncidentStates.java` | Reuses stateless singleton state objects stored in a high-performance `EnumMap` (`O(1)` access, zero heap allocation per transition). |
| **Observer Pattern** | `com.iitsm.domain.event.*` | Decouples lifecycle state mutations from side effects (audit logging, notifications, duplicate correlation) via sealed `DomainEvent` publishing. |
| **Open/Closed Principle** | `CategoryRegistry.java` | Permits introducing new incident categories dynamically at runtime without modifying core enums or existing classes. |
| **Mapper / Adapter** | `PublicStatusMapper.java` | Shields end-users from internal workflow complexity by adapting 9 internal states into a simplified 5-step stepper. |
| **Repository Port** | `com.iitsm.domain.repository.*` | Inversion of Control (Hexagonal Architecture port) ensuring pure domain logic remains completely decoupled from persistence details. |

---

## 3. Test Suite Verification & Metrics
- **Total Test Cases**: 63 unit tests (Exceeds Phase 1 requirement of ≥ 40 tests).
- **All Tests Green**: 100% passing.
- **JaCoCo Coverage**: Exceeds required 70% threshold across domain logic.
- **Key Test Scenarios**:
  1. `IncidentStateTransitionTableTest`: Exhaustively tested all 9 states against 10 actions; verified that every invalid action throws `InvalidStateTransitionException` with exact error metadata.
  2. `IncidentEncapsulationTest`: Tested title length boundary (4/5/120/121), description length boundary (19/20/4000/4001), HTML sanitization, and unmodifiable collections.
  3. `UserHierarchyTest`: Tested 5th vs 6th failed login lockout, technician capacity limits (capacity vs capacity + 1), and role hierarchy.
  4. `SlaTrackerTest`: Tested deadline calculation, status progression (`ON_TRACK` → `AT_RISK` → `WARNING` → `BREACHED`), and resolution window extension when paused in `PENDING_INFO`.
  5. `CategoryRegistryTest`: Demonstrated Open/Closed extensibility with dynamic categories.
  6. `PublicStatusMapperTest`: Tested full coverage of state projection to employee stepper.

---

## 4. Known Gaps & Next Steps (Phase 2)
1. **Persistence**: JPA entities and Spring Data repository adapters implementing the repository ports will be added in Phase 2.
2. **Flyway Migrations**: PostgreSQL schema definitions with indexes on status, priority, assignee, service, and timestamps.
3. **Spring Security**: JWT authentication, BCrypt password hashing, and application-layer `AccessPolicy`.
4. **Synthetic Company Seeder**: Deterministic generation of Nimbus FinServe (~300 employees, 6 teams, ~20 technicians, 14 IT services).
