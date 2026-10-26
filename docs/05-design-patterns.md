# Design Patterns Catalog & Viva Defense Guide: I-ITSM

This document provides a comprehensive, rigorous architectural breakdown of the **8 Object-Oriented Design Patterns** implemented across the I-ITSM platform for course CS F213 (Object-Oriented Programming).

---

## 1. Master Design Patterns Matrix

| # | Design Pattern | Gang of Four (GoF) Category | Core Classes & Interfaces | Code Package | Primary Architectural Benefit |
| :-: | :--- | :--- | :--- | :--- | :--- |
| **1** | **State** | Behavioral | `IncidentState`, `IncidentStates`, `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState` | `com.iitsm.domain.state.*` | Encapsulates 9-state lifecycle rules and action guards. Enforces "Tell, Don't Ask" and eliminates procedural `if/switch` blocks in application services. |
| **2** | **Strategy** | Behavioral | `AssignmentStrategy`, `PriorityFactor`, `IncidentClassifier`, `SimilarityMeasure` | `com.iitsm.domain.strategy.*` | Decouples classification, prioritization, routing, and text similarity algorithms into interchangeable strategies, satisfying the Open/Closed Principle. |
| **3** | **Chain of Responsibility** | Behavioral | `EscalationHandler`, `AbstractEscalationHandler`, `Tier4ItManagerHandler`, `Tier3SystemAdminHandler`, `Tier2TeamLeadHandler`, `Tier1TechnicianHandler`, `EscalationChainBuilder` | `com.iitsm.domain.chain.escalation.*` | Passes high-risk or SLA-breached incidents along a strictly prioritized chain of 4 operational tiers without coupling the sender to specific receivers. |
| **4** | **Decorator** | Structural | `AdjustableClock` extending `java.time.Clock` | `com.iitsm.infrastructure.config.AdjustableClock.java` | Transparently adds virtual time offsets and dynamic speed multipliers ($\times 1, \times 60, \times 600$) to `java.time.Clock` without altering the interface consumed by domain code. |
| **5** | **Observer** | Behavioral | `DomainEvent` (sealed), `DomainEventPublisher`, `AuditTrailObserver`, `NotificationObserver`, `NotificationRouter` | `com.iitsm.domain.event.*`, `com.iitsm.infrastructure.observer.*` | Implements the event spine, decoupling state changes from transactional audit logging, notifications, and duplicate correlation. |
| **6** | **Composite** | Structural | `CompositeClassifier`, `CompositePriorityCalculator` | `com.iitsm.domain.strategy.classification.*`, `com.iitsm.domain.strategy.priority.*` | Combines multiple statistical or heuristic algorithms into unified decision objects with transparent mathematical explainability. |
| **7** | **Factory** | Creational | `UserFactory`, `IncidentFactory` | `com.iitsm.application.service.UserFactory.java`, `com.iitsm.domain.model.IncidentFactory.java` | Centralizes object instantiation, ensures invariant enforcement, and encapsulates creation of polymorphic user hierarchies. |
| **8** | **Repository (Ports & Adapters)** | Structural | Ports: `IncidentRepository`, `UserRepository`, `TechnicianRepository`, `ItServiceRepository`, `SupportTeamRepository`, `SlaPolicyRepository`<br>Adapters: `*RepositoryAdapter` | `com.iitsm.domain.repository.*`, `com.iitsm.infrastructure.persistence.adapter.*` | Decouples pure Java domain models from Spring Data JPA infrastructure. Catches database exceptions and maps them to domain unchecked exceptions. |

---

## 2. In-Depth Pattern Analysis & Viva Defense Talking Points

### Pattern 1: State Pattern (Behavioral)
- **Problem Solved**: An incident goes through 9 discrete states (`NEW` $\rightarrow$ `CLASSIFIED` $\rightarrow$ `ASSIGNED` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `IN_PROGRESS` $\rightleftarrows$ `PENDING_INFO` $\rightarrow$ `RESOLVED` $\rightarrow$ `VERIFIED` $\rightarrow$ `CLOSED`). Writing procedural checks like `if (status == ASSIGNED)` scattered across controllers and services leads to brittle code and violates Single Responsibility and Tell-Don't-Ask principles.
- **Implementation**:
  - `IncidentState` interface defines all 11 lifecycle operations.
  - Default methods in `IncidentState` throw `InvalidStateTransitionException`. Concrete states only override permitted actions.
  - `IncidentStates` stores stateless singleton instances in an `EnumMap<IncidentStatus, IncidentState>`. Transitions allocate zero heap memory.
- **Viva Defense**:
  > *"Instead of asking the incident for its status and making decisions externally, the service tells the incident to execute an action (`incident.resolve(...)`). The incident delegates directly to its current state object. Adding a new lifecycle state requires only adding a new state class and registering it in `IncidentStates`, completely leaving existing state classes untouched."*

---

### Pattern 2: Strategy Pattern (Behavioral)
- **Problem Solved**: Different incident domains require different technician routing algorithms (skills vs workload vs seniority) and different text similarity algorithms (cosine TF-IDF vs Jaccard). Hardcoding these choices inside domain entities violates the Open/Closed Principle.
- **Implementation**:
  - Strategy Interfaces: `AssignmentStrategy`, `PriorityFactor`, `IncidentClassifier`, `SimilarityMeasure`.
  - Concrete Strategies for Assignment: `SkillBasedStrategy`, `LoadBasedStrategy`, `ExperienceBasedStrategy`, `RoundRobinStrategy`, `WeightedCompositeStrategy`.
  - Concrete Strategies for Priority: `ServiceCriticalityFactor` (0.30), `ImpactFactor` (0.20), `UrgencyFactor` (0.15), `ReporterDesignationFactor` (0.15), `AgingFactor` (0.10), `ReporterHistoryFactor` (0.10).
  - Concrete Strategies for Similarity: `CosineTfIdfSimilarity`, `JaccardSimilarity`.
  - `AssignmentStrategyRegistry`: Thread-safe runtime registry (`ConcurrentHashMap`) enabling zero-downtime hot-swapping via Admin REST endpoints.
- **Viva Defense**:
  > *"Callers depend exclusively on the Strategy interfaces. When an administrator switches from `ROUND_ROBIN` to `LOAD_BALANCED` via the REST API, the system changes behavior dynamically at runtime without restarting the application or changing a single line of domain code."*

---

### Pattern 3: Chain of Responsibility Pattern (Behavioral)
- **Problem Solved**: High-priority incidents and SLA breaches require tiered escalation. Deciding whether an alert goes to the assigned technician, the team lead, the system administrator, or the IT operations manager involves complex rules based on elapsed SLA percentages (75%, 90%, 100%) and service criticality.
- **Implementation**:
  - `EscalationHandler` interface defines `setNext(EscalationHandler next)` and `handle(Incident incident, EscalationContext context)`.
  - `AbstractEscalationHandler` provides the template method skeleton: if `canHandle` evaluates to true, `execute` returns an `EscalationAction`; otherwise, the request passes to `next.handle(...)`.
  - 4 Tiers:
    - `Tier4ItManagerHandler`: Catastrophic P1 breach on Mission-Critical service $\rightarrow$ alerts IT Operations Manager (`ADMIN-MGR-01`).
    - `Tier3SystemAdminHandler`: P2 hard breach or triage stagnation ($> 30$ mins) $\rightarrow$ alerts System Administrator (`ADMIN-SYS-01`).
    - `Tier2TeamLeadHandler`: 90% resolution window or missed response target $\rightarrow$ alerts Team Lead (`LEAD-*`).
    - `Tier1TechnicianHandler`: 75% resolution window $\rightarrow$ issues urgent reminder to assigned technician.
- **Viva Defense**:
  > *"The Chain of Responsibility decouples the trigger of an escalation from the specific operational receiver. Each handler focuses strictly on its own threshold criteria. If a new escalation level (e.g., Executive Board or Vendor SLA) is required, we simply write a new handler class and link it into the chain via `EscalationChainBuilder` without modifying existing handlers."*

---

### Pattern 4: Decorator Pattern (Structural)
- **Problem Solved**: Business logic requires temporal evaluation (SLA due dates, aging, time-accelerated simulation). Directly calling `Instant.now()` prevents deterministic unit testing and makes live interactive demonstration of multi-hour SLA windows impossible.
- **Implementation**:
  - `AdjustableClock` extends Java's standard `java.time.Clock`.
  - Decorates time retrieval with virtual anchors, speed multipliers ($\times 1, \times 60, \times 600$), and fast-forward duration offsets.
  - Callers receive `AdjustableClock` as the standard `java.time.Clock` bean via Spring dependency injection.
- **Viva Defense**:
  > *"By decorating `java.time.Clock`, domain entities and application services remain completely unaware of whether time is real or simulated. Hours of SLA degradation execute in milliseconds during tests without calling `Thread.sleep()`."*

---

### Pattern 5: Observer Pattern (Behavioral)
- **Problem Solved**: When an incident changes state, secondary actions must occur: audit logs must be recorded, notifications must be sent, and SLA monitors must be alerted. Hardcoding email or database calls inside domain entities creates tight coupling.
- **Implementation**:
  - Sealed `DomainEvent` hierarchy defining 17 immutable event records.
  - `DomainEventPublisher` port implemented by `InMemoryDomainEventPublisher`.
  - Observers: `AuditTrailObserver` (transactional database logging) and `NotificationObserver` (asynchronous multi-channel dispatching).
- **Viva Defense**:
  > *"The domain core focuses purely on business rules and emits lightweight domain events. The observers handle secondary infrastructure concerns independently. Adding a new notification channel (e.g., Slack or PagerDuty) requires only registering a new listener, leaving the domain core completely untouched."*

---

### Pattern 6: Composite Pattern (Structural)
- **Problem Solved**: Automated classification and priority scoring need to combine multiple independent algorithms (statistical Naive Bayes + technical lexicon Keyword matching) into a unified result.
- **Implementation**:
  - `CompositeClassifier` implements `IncidentClassifier` and aggregates `KeywordClassifier` and `NaiveBayesClassifier`.
  - `CompositePriorityCalculator` evaluates 6 individual `PriorityFactor` strategies, normalizes weights ($\sum w_i = 1.0$), and produces a unified `PriorityDecision`.
- **Viva Defense**:
  > *"The Composite treats individual factors and complex aggregations uniformly, providing transparent mathematical explainability in every priority calculation."*
