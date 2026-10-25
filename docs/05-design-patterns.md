# Design Patterns Catalog - I-ITSM

This document provides a detailed breakdown of all object-oriented design patterns implemented across the I-ITSM codebase, documenting their intent, participating classes, file paths, and viva defense rationale.

---

## Summary Matrix

| Design Pattern | Category | Key Classes & Interfaces | File Path | Architectural Intent & Viva Defense |
| :--- | :--- | :--- | :--- | :--- |
| **State** | Behavioral | `IncidentState`, `NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState` | `com.iitsm.domain.state.*` | Encapsulates lifecycle rules and valid transitions within dedicated state objects. Eliminates procedural `if (status == X)` branches, enforces "Tell, Don't Ask", and guarantees invalid transitions fail fast with `InvalidStateTransitionException`. |
| **Flyweight / Registry** | Creational / Behavioral | `IncidentStates` | `com.iitsm.domain.state.IncidentStates.java` | Stores stateless singleton instances of each `IncidentState` in an `EnumMap`. Yields zero heap allocations during transitions and $O(1)$ state lookups. |
| **Observer** | Behavioral | `DomainEvent` (sealed), `DomainEventPublisher`, `DomainEventListener`, `InMemoryDomainEventPublisher` | `com.iitsm.domain.event.*` | Decouples domain state changes from side effects (audit logging, notifications, duplicate detection). Implements the event-driven spine. |
| **Factory** | Creational | `UserFactory` | `com.iitsm.application.service.UserFactory.java` | Centralizes creation and invariant validation of polymorphic `User` subtypes (`Employee`, `Technician`, `TeamLead`, `Administrator`). |
| **Repository (Ports & Adapters)** | Structural | Ports: `IncidentRepository`, `UserRepository`, `TechnicianRepository`, `ItServiceRepository`, `SupportTeamRepository`, `SlaPolicyRepository`<br>Adapters: `*RepositoryAdapter` | `com.iitsm.domain.repository.*`<br>`com.iitsm.infrastructure.persistence.adapter.*` | Decouples pure domain business logic from Spring Data and Hibernate JPA persistence details. Enables swapping databases (PostgreSQL/H2) without modifying domain code. |
| **Adapter / Mapper** | Structural | `PublicStatusMapper`, `IncidentMapper`, `UserMapper` | `com.iitsm.domain.state.PublicStatusMapper.java`<br>`com.iitsm.infrastructure.persistence.mapper.*` | Maps between internal domain models and external projections (5-step public stepper for employees; JPA entities for database storage). |
| **Strategy / Policy** | Behavioral | `AccessPolicy` | `com.iitsm.domain.policy.AccessPolicy.java` | Decouples role-based access rules and data visibility invariants from HTTP controller layer annotations. |
| **Open/Closed Principle** | Design Principle | `CategoryRegistry` | `com.iitsm.domain.model.CategoryRegistry.java` | Enables dynamic registration of new incident categories at runtime without modifying existing classes or enums. |

