# Architecture Decision Records (ADR) - I-ITSM

This document records architectural, design, and technical decisions made during the project, documenting the context, alternatives considered, and rationale.

---

## ADR-001: Acceptance of `jakarta.persistence` Annotations on Domain Entities
- **Status**: Accepted
- **Context**: The domain layer must be clean and framework-independent. However, maintaining duplicate entity hierarchies (one pure domain POJO and one JPA entity) introduces substantial boilerplate mapping, bidirectional sync complexity, and potential dirty-checking inconsistencies.
- **Decision**: Allow standard `jakarta.persistence` annotations directly on domain entities while strictly prohibiting any `org.springframework.*` imports in `com.iitsm.domain`.
- **Alternatives Considered**:
  1. *Complete separation*: Pure POJOs in `domain`, separate `@Entity` classes in `infrastructure.persistence` mapped via MapStruct. (Rejected: excessive boilerplate for a 1-semester OOP project without architectural benefit, since `jakarta.persistence` is a vendor-neutral standard specification).
  2. *XML mapping (`orm.xml`)*: Pure POJOs mapped via external XML. (Rejected: brittle, difficult to refactor and maintain).
- **Reason**: Preserves high cohesion and readability of domain entities while keeping the domain completely free of Spring runtime dependencies.

---

## ADR-002: Pure-Java In-House NLP vs. External Heavyweight ML Libraries
- **Status**: Accepted
- **Context**: The system requires incident text classification, service matching, and duplicate detection.
- **Decision**: Implement TF-IDF vectorization, Cosine Similarity, and Multinomial Naive Bayes entirely in pure Java under `com.iitsm.domain.strategy`.
- **Alternatives Considered**:
  1. *External libraries (Smile, Weka, Apache OpenNLP)*. (Rejected: hides OOP implementation details, introduces dependency bloat, reduces viva defensibility).
  2. *External LLM APIs (OpenAI / Anthropic / Hugging Face)*. (Rejected: nondeterministic, requires API keys/network access, not self-contained for offline evaluation).
- **Reason**: Demonstrates core OOP principles (Strategy, Template Method, immutability, clean interface segregation), remains 100% explainable in code reviews and vivas, and executes deterministically without external dependencies.

---

## ADR-003: Injected `java.time.Clock` for Temporal Operations
- **Status**: Accepted
- **Context**: SLA tracking, aging calculation, ticket escalation, and simulation require precise temporal control and acceleration.
- **Decision**: Forbid direct calls to `Instant.now()`, `LocalDateTime.now()`, or `System.currentTimeMillis()` in business code; always inject `java.time.Clock`.
- **Alternatives Considered**:
  1. *Static mock frameworks (MockedStatic in Mockito)*. (Rejected: brittle, multi-threading issues, doesn't support interactive simulation).
  2. *Direct system time calls*. (Rejected: impossible to test SLA breaches deterministically or fast-forward time in demos).
- **Reason**: Enables rock-solid unit testing of SLA deadlines, pause-resume windows, and allows the `AdjustableClock` simulation engine to fast-forward time (x1, x60, x600).

---

## ADR-004: State Pattern with Stateless State Objects in EnumMap
- **Status**: Accepted
- **Context**: Incidents progress through 9 internal states with complex transition rules, action permissions, and SLA pausing.
- **Decision**: Implement the State Pattern where `Incident` delegates actions to `IncidentState` implementations. The state instances are stateless singletons registered in an `EnumMap<IncidentStatus, IncidentState>` within `IncidentStates`.
- **Alternatives Considered**:
  1. *Giant `switch(status)` / `if-else` blocks in services*. (Rejected: violates Tell-Don't-Ask and Single Responsibility Principle, fails OOP criteria).
  2. *Spring State Machine framework*. (Rejected: heavy dependency, leaks framework details into domain, overkill for defined linear/branching transitions).
  3. *State objects with stateful incident reference*. (Rejected: complicates JPA entity lifecycle and caching).
- **Reason**: Zero-allocation per transition, completely thread-safe, encapsulates transition logic cleanly, and enables trivial exhaustive transition matrix testing.

---

## ADR-005: Hexagonal Repository Ports & Adapters with Optimistic Locking
- **Status**: Accepted
- **Context**: Pure domain repositories must remain free of Spring dependencies, but the persistence layer must leverage Spring Data JPA, handle transactions, and guard against race conditions.
- **Decision**: Define pure Java repository ports in `com.iitsm.domain.repository` and implement them via Spring `@Component` adapters in `com.iitsm.infrastructure.persistence.adapter`. Catch all database errors and optimistic locking collisions (`@Version`) and translate them into domain `PersistenceFailureException`.
- **Alternatives Considered**:
  1. *Spring Data repositories extending domain interfaces directly*. (Rejected: couples domain to Spring Data types like `Pageable`, `Optional` Spring behaviors, and leaks framework infrastructure).
- **Reason**: Maintains strict architectural layer separation (`domain` never imports Spring), enables effortless switching between H2 for tests and PostgreSQL for production, and provides centralized exception translation.

---

## ADR-006: Stateless JWT Authentication with BCrypt and 5-Strike Lockout
- **Status**: Accepted
- **Context**: The system must provide role-based security across 3 personas (Employee, Technician, Admin) and enforce security boundaries (account lockout after 5 consecutive failed logins).
- **Decision**: Implement stateless JWT authentication via `jjwt` with BCrypt password hashing. Authentication failures track attempt counts and lock accounts on the 5th strike. Centralize 401 and 403 responses into consistent JSON objects.
- **Alternatives Considered**:
  1. *Stateful HTTP Sessions*. (Rejected: hard to scale, poor match for single-page applications and REST APIs).
  2. *OAuth2 / Keycloak*. (Rejected: requires external identity provider server setup, complicating standalone viva and grading evaluation).
- **Reason**: Lightweight, completely self-contained in pure Java, satisfies all security criteria, and returns clean, uniform JSON error contracts.
