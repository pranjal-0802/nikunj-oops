# Phase 3 Summary: Incident Lifecycle Services, REST API, Audit, and Notifications

## 1. What Was Built
- **Application Services & Lifecycle Orchestration (`com.iitsm.application.service`)**:
  - `IncidentService`: Complete aggregate orchestrator managing state transitions across the full 10-step lifecycle:
    - `createIncident`, `acknowledge`, `startInvestigation`, `addNote` (internal / public flags), `requestInfo` (pauses SLA tracker), `provideInfo` (resumes SLA tracker), `resolve` (requires structured `Resolution`), `verify` (employee confirmation), `reject` (reopen loop incrementing `reopenCount`), `close` (attaches 1–5 star `Feedback`), `flagWrongDomain` (clears assignee, reverts to `CLASSIFIED`, places into triage queue), `assign` (assigns or reassigns technician), and `overridePriority` (audited administrative override with mandatory reason).
    - Supports seamless lookup by user database ID (`EMP-0001`, `TECH-NET-1`) or corporate email (`employee@nimbusfinserve.com`) via `loadUser`.
  - `IncidentFactory`: Factory ensuring new incidents are instantiated with validated parameters and injected `Clock`.
  - `IncidentUrgencyOrdering`: Comparator ordering technician queues by urgency: Priority rank (P1 > P2 > P3 > P4), then SLA resolution deadline (`resolutionDueAt`), then creation timestamp (`createdAt`).
  - `SearchService`: Dynamic query service applying composable JPA specifications for pagination and sorting.
- **Dynamic Search & Filtering ($\ge 5$ Criteria)**:
  - `com.iitsm.infrastructure.persistence.specification.IncidentSpecifications`: Composable criteria supporting:
    1. `status` (exact `IncidentStatus`)
    2. `priority` (exact `Priority`)
    3. `category` (exact `IncidentCategory`)
    4. `serviceId` (IT Service foreign key)
    5. `assigneeId` (assigned technician ID)
    6. `reporterId` (reporting employee ID)
    7. `containsText` (case-insensitive substring match on incident title and description)
    8. `createdBetween` (time interval filtering)
- **Persona DTO Views & Data Privacy Mapping (`com.iitsm.web.dto`, `com.iitsm.web.mapper`)**:
  - `EmployeeIncidentView`: Strict persona privacy invariant enforced by type contract:
    - **ZERO** priority fields (employees never see P1–P4).
    - **ZERO** SLA internals or deadlines (employees see only the 5-step public stepper).
    - **ZERO** internal technician notes (employees only receive customer-facing notes).
    - Includes an employee-safe public audit trail and allowed actions based on state.
  - `TechnicianIncidentView`: Operational view providing urgency score, SLA deadline countdown, internal and public notes, allowed lifecycle actions, and structured resolution history.
  - `AdminIncidentView`: Master audit view exposing complete priority score factor contributions, full SLA tracker state (pause durations, target margins), triage status, and complete immutable audit trail.
  - `IncidentDtoMapper`: Centralized adapter mapping pure domain models to persona-specific DTO projections.
- **REST Web Layer & Global Error Handling (`com.iitsm.web.controller`, `com.iitsm.web.advice`)**:
  - `GlobalExceptionHandler`: `@RestControllerAdvice` mapping domain exceptions (`ValidationException` $\rightarrow$ 400, `ServiceNotFoundException` $\rightarrow$ 404, `InvalidStateTransitionException` $\rightarrow$ 409, `UnauthorizedAccessException` $\rightarrow$ 403) into standardized RFC 7807 error responses.
  - `AuthController`: User registration, authentication, token issuance, and current user profile inspection.
  - `EmployeeController` (`/api/employee/incidents`): Ticket creation, personal incident history, providing requested info, verifying resolutions, and rejecting fixes.
  - `TechnicianController` (`/api/technician`): Urgency-ordered queue, personal workload dashboard, lifecycle actions (`acknowledge`, `start-investigation`, `notes`, `request-info`, `resolve`, `flag-wrong-domain`).
  - `AdminController` (`/api/admin`): Real-time system statistics cards, master incident search ($\ge 5$ criteria), wrong-domain triage queue (`/api/admin/triage-queue`), technician roster, administrative assignment/reassignment, priority override, and formal closure.
  - `NotificationController` (`/api/notifications`): Server-Sent Events stream (`/stream`), in-app notification queries, and read receipt acknowledgment.
- **Transactional Audit Subsystem (`com.iitsm.infrastructure.observer.AuditTrailObserver`)**:
  - Listens to all domain events published on the `DomainEventPublisher`.
  - Persists `AuditRecordJpaEntity` inside the **same database transaction** as the triggering state change.
  - Guarantees complete non-repudiation and forensic traceability for all state transitions, info requests, priority overrides, reassignments, and closures.
- **Multi-Channel Notification Subsystem (`com.iitsm.infrastructure.notification`)**:
  - `NotificationChannel`: Adapter interface (`getChannelName()`, `send(recipient, subject, message)`).
  - `InAppChannel`: Persists in-app notifications in `SpringDataNotificationRepository` and pushes live updates to active browser clients via `SseEmitterRegistry`.
  - `EmailChannel`: Wraps Spring's `JavaMailSender` targeting Mailpit, catching network/SMTP exceptions and isolating failures.
  - `SmsSimulatorChannel`: Simulates high-urgency SMS alerts for P1/P2 incidents.
  - `NotificationRouter`: Strategy router delivering across active channels with complete fault isolation: notification errors are logged and never cause domain transaction rollback.
  - `NotificationFactory`: Formats human-readable notification messages for all 17 sealed `DomainEvent` types.
  - `SseEmitterRegistry`: Thread-safe registry (`ConcurrentHashMap`) managing real-time SSE streams with heartbeat and automatic cleanup.

---

## 2. Design Patterns Implemented & Viva Defensibility

| Design Pattern | Location | Architectural Purpose & Rationale |
| :--- | :--- | :--- |
| **Observer Pattern (Transactional)** | `com.iitsm.infrastructure.observer.AuditTrailObserver.java` | Synchronous event listener persisting audit records within the caller's transaction. Guarantees that any state transition is permanently recorded in the audit log without polluting domain aggregate logic. |
| **Observer Pattern (Decoupled)** | `com.iitsm.infrastructure.observer.NotificationObserver.java` | Asynchronous event listener translating domain events into multi-channel alerts (In-App, Email, SMS). Isolate notification side-effects from the core transaction. |
| **Adapter Pattern** | `com.iitsm.infrastructure.notification.InAppChannel.java`<br>`com.iitsm.infrastructure.notification.EmailChannel.java`<br>`com.iitsm.infrastructure.notification.SmsSimulatorChannel.java` | Standardizes disparate delivery mechanisms behind the uniform `NotificationChannel` interface. |
| **Adapter / Presenter Pattern** | `com.iitsm.web.mapper.IncidentDtoMapper.java` | Bridges pure domain aggregates to persona-specific DTO projections, strictly enforcing view-level invariants and privacy rules. |
| **Factory Pattern** | `com.iitsm.application.service.IncidentFactory.java`<br>`com.iitsm.infrastructure.notification.NotificationFactory.java` | Encapsulates the instantiation of aggregate roots with clock/validation dependencies, and centralizes event-to-notification message templating. |
| **Strategy / Comparator Pattern** | `com.iitsm.domain.model.IncidentUrgencyOrdering.java` | Implements pluggable prioritization logic to sort technician queues by dynamic urgency (Priority > SLA Due Time > Creation Time). |
| **Specification Pattern** | `com.iitsm.infrastructure.persistence.specification.IncidentSpecifications.java` | Implements composable, reusable Spring Data JPA predicates allowing arbitrary combinations of $\ge 5$ filter criteria. |
| **Policy Pattern** | `com.iitsm.domain.policy.AccessPolicy.java`<br>`com.iitsm.application.policy.DefaultAccessPolicy.java` | Decouples business authorization checks (e.g., ticket ownership, role-based action execution) from both controllers and domain entities. |
| **State Pattern** | `com.iitsm.domain.state.*` | Encapsulates state-specific lifecycle rules and validates transitions across all 10 states without monolithic conditional blocks. |

---

## 3. Persona Data Privacy Invariant Verification

The system enforces strict employee privacy through a dual-layer defense:
1. **Type Contract Enforcement**: `EmployeeIncidentView` does not contain priority or SLA fields. Any attempt by an employee to inspect raw priority scores, SLA clocks, or internal technician notes is structurally impossible via the employee API endpoints.
2. **Access Control Enforcement**: `DefaultAccessPolicy` verifies that non-admin and non-technician users can only query tickets where `incident.reporterId == actor.id`. Access to tickets belonging to other employees immediately triggers an `UnauthorizedAccessException` (HTTP 403).
3. **Automated Verification**: `EmployeePrivacyControllerTest` tests and asserts:
   - Priority field is absent in response body (`jsonPath("$.priority").doesNotExist()`).
   - SLA Tracker is absent in response body (`jsonPath("$.slaTracker").doesNotExist()`).
   - Priority Score is absent in response body (`jsonPath("$.priorityScore").doesNotExist()`).
   - Internal notes are filtered out (`jsonPath("$.internalNotes").doesNotExist()`).
   - Cross-employee access attempts return HTTP 403 Forbidden.

---

## 4. Verification & Metrics

- **Total Test Cases**: **104 tests across 25 test classes, 100% green**.
- **JaCoCo Coverage**:
  - Analyzed 162 classes in the bundle.
  - Total line coverage: **79.9%** (exceeds the 70% threshold).
  - All coverage checks met during `mvn verify`.
- **Flagship Integration Test (`IncidentLifecycleEndToEndTest`)**:
  - Exercises complete lifecycle across 10 distinct states:
    `NEW` $\rightarrow$ `ASSIGNED` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `PENDING_INFO` (SLA paused) $\rightarrow$ `IN_PROGRESS` (SLA resumed) $\rightarrow$ `RESOLVED` $\rightarrow$ `IN_PROGRESS` (reopen loop #1) $\rightarrow$ `RESOLVED` $\rightarrow$ `VERIFIED` $\rightarrow$ `CLOSED` (with feedback).
  - Validates that the synchronous audit trail contains entries for all 10 operations.
  - Validates that notification dispatches succeed across simulated In-App, Email, and SMS channels.

---

## 5. Known Gaps & Next Steps (Phase 4)

Phase 3 establishes the complete transactional and lifecycle foundation. In accordance with the project roadmap, the following intelligent capabilities are intentionally reserved for Phase 4:
1. **Text Preprocessing & Feature Extraction**:
   - Tokenization, stop-word removal, and TF-IDF calculation (`com.iitsm.domain.ml.TfIdfVectorizer`).
2. **Category Classification ML Model**:
   - Multinomial Naive Bayes classifier trained on historical incident datasets (`com.iitsm.domain.ml.NaiveBayesClassifier`).
3. **Composite Priority Scoring Engine**:
   - Multi-factor evaluation: Category Baseline, Role/Seniority Impact, Business Criticality of affected IT Service, and Keyword Urgency Boost.
4. **Smart Technician Routing**:
   - Swappable assignment strategies (Round-Robin, Lowest Workload, Skill-Match, Composite Affinity).
