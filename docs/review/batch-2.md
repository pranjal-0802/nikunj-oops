# Batch 2 Report: Correctness, Security & Hexagonal Architecture Remediation

---

## 1. Executive Summary

Batch 2 completes the comprehensive remediation of confirmed architecture, correctness, and authorization findings (A1, A2, A4, S6, S7, F3, F4, F5, F6, F7, F8). All 21 reproductions across `ArchitectureRegressionTest`, `SecurityRegressionTest`, and `CorrectnessRegressionTest` are now 100% green.

- **Status**: **186 of 186 tests PASSING (100% GREEN)**
- **Code Coverage**: **85% instruction coverage** (measured via JaCoCo across 244 classes)
- **Findings Closed**:
  - **A1 (Hexagonal Architecture / Inverted Dependencies)**: Closed (GREEN).
  - **A2 / S6 (Polymorphic Access Control Matrix)**: Closed (GREEN).
  - **A4 (Optimistic Lock Mapping to 409 Conflict)**: Closed (GREEN).
  - **S7 (Token Validation Rejection for Disabled/Locked Users)**: Closed (GREEN).
  - **F3 / F4 (Scoped Duplicate Detection & Avalanche Escalation)**: Closed (GREEN).
  - **F5 (Low-Confidence Classification & Human Triage Routing)**: Closed (GREEN).
  - **F6 (Scoped Reporter History Factor)**: Closed (GREEN).
  - **F7 (Concurrency Guard on Technician Capacity)**: Closed (GREEN).
  - **F8 (Scenario S4 Duplicate Avalanche Assertion)**: Closed (GREEN).

---

## 2. Changes Made & Architectural Enhancements

### Hexagonal Architecture & Ports/Adapters (Finding A1)
1. **Domain Ports Created (`com.iitsm.domain.repository`)**:
   - `AuditRepository`: Interface for persisting and querying `AuditRecord` entries.
   - `EscalationRecordRepository`: Interface for storing and retrieving `EscalationRecord` entries.
   - `ReassignmentRequestRepository`: Interface for managing `ReassignmentRequest` lifecycle.
   - `NotificationRepository`: Interface for notification persistence.
2. **Application Ports Created (`com.iitsm.application.port`)**:
   - `TokenPort`: Abstraction for JWT token generation and claims resolution.
   - `IncidentSearchPort`: Abstraction for multi-criteria incident search and dynamic filtering.
   - `RealtimeNotificationPort`: Abstraction for server-sent event (SSE) real-time event broadcasting.
3. **Infrastructure Adapters Implemented (`com.iitsm.infrastructure.persistence.adapter`)**:
   - `AuditRepositoryAdapter`: Adapts `SpringDataAuditRepository` and maps between domain `AuditRecord` and JPA `AuditRecordJpaEntity`.
   - `EscalationRecordRepositoryAdapter`: Adapts `SpringDataEscalationRecordRepository`.
   - `ReassignmentRequestRepositoryAdapter`: Adapts `SpringDataReassignmentRequestRepository`.
   - `NotificationRepositoryAdapter`: Adapts `SpringDataNotificationRepository`.
   - `IncidentSearchAdapter`: Adapts `IncidentRepositoryJpa` specification queries.
   - `JwtTokenProvider` implements `TokenPort`.
   - `SseEmitterRegistry` implements `RealtimeNotificationPort`.
4. **Application Services Created**:
   - `AuditService`: Mediates audit trail logging and queries between controllers and domain/ports.
   - `NotificationService`: Manages notification generation and retrieval.
5. **Controller Decoupling & ArchUnit Invariants**:
   - Controllers (`AdminController`, `EmployeeController`, `TechnicianController`, `NotificationController`) decoupled from repositories and infrastructure packages.
   - Created `ReassignmentRequestDto` and `NotificationDto`.
   - `ArchitectureRegressionTest` verifies all 4 ArchUnit rules pass:
     - Application layer does not depend on infrastructure.
     - Web layer does not depend on infrastructure.
     - Controllers do not return domain entities or JPA entities.
     - Controllers do not directly depend on repositories.

### Polymorphic Access Control & Authorization (Findings S6, A2)
1. **Polymorphic Action Verification**:
   - Created `IncidentAction` enum (`VIEW`, `ACKNOWLEDGE`, `START_INVESTIGATION`, `REQUEST_INFO`, `RESOLVE`, `VERIFY`, `CLOSE`, `FLAG_WRONG_DOMAIN`, `REASSIGN`, `OVERRIDE_PRIORITY`).
   - Replaced `instanceof` and string-based switches in `AccessPolicy` with polymorphic methods `canViewIncident` and `canExecuteAction` on the `User` class hierarchy:
     - `Employee`: Only permitted to view and verify/close their own tickets; blocked from technician or administrative actions.
     - `Technician`: Permitted to view incidents assigned to them or in their team queue; permitted to execute technician lifecycle actions only on tickets assigned to them.
     - `TeamLead`: Permitted to view and manage tickets across their designated support team.
     - `Administrator`: Global visibility and full administrative authority across all incidents.

### Authentication Security (Finding S7)
1. **Locked/Deactivated Account Enforcement**:
   - `JwtAuthenticationFilter` inspects `UserDetails.isEnabled()` and `UserDetails.isAccountNonLocked()`.
   - Immediately returns HTTP 401 JSON (`{"errorCode":"ERR_UNAUTHORIZED","message":"Account is locked or deactivated"}`) when a user is deactivated or locked, even if their JWT signature is valid and non-expired.

### Concurrency & Optimistic Locking (Findings F7, A4)
1. **Isolated Technician Assignment**:
   - Created `TechnicianAssignmentService` with `@Transactional(propagation = Propagation.REQUIRES_NEW)`.
   - Isolated technician capacity reservation from caller incident transactions so concurrency collisions do not mark outer transactions as rollback-only.
2. **Optimistic Lock Conflict Mapping**:
   - `GlobalExceptionHandler` maps `ConcurrentModificationException` and JPA optimistic lock exceptions to HTTP 409 Conflict with JSON payload.

### Incident Classification, Triage & Duplicates (Findings F3, F4, F5, F6, F8)
1. **Scoped Duplicate Detection**:
   - Replaced unbounded `findAll()` duplicate scanning with SQL-scoped queries filtering by matching service, open status, and creation timestamp within the correlation window.
   - Duplicate tickets link to parent clusters, consume zero technician workload capacity, share assignee and resolution, and receive status updates when parent progresses.
   - Parent priority is dynamically elevated via `RULE_DUPLICATE_AVALANCHE` as cluster size grows, verified in Scenario S4.
2. **Confidence-Gated Human Triage (F5)**:
   - Added `iitsm.classification.min-confidence` (default 0.60).
   - Ambiguous tickets or tickets with weak keyword matches are capped at 0.55 confidence and routed to the administrator triage queue unassigned in `CLASSIFIED` state with `needsTriage = true`.
   - Unified triage queue DTO in `AdminController.getTriageQueue` serves both low-confidence tickets and wrong-domain reassignment requests.
3. **Chronic Reporter History Scoping (F6)**:
   - Scoped history factor calculation to count only recent unresolved or reopened tickets submitted by the same reporter for the same IT service within the past 30 days.

---

## 3. Regression Test Verification Suite

All 21 regression tests across three dedicated regression suites are **100% GREEN**:

| Finding | Test Name | Suite | Status |
|---|---|---|---|
| **S1** | `S1_jwtSecretValidationFailsOnWeakOrCommittedSecret` | `SecurityRegressionTest` | **PASSED** |
| **S2** | `S2_lockoutPersistsAfterFiveBadLogins` | `SecurityRegressionTest` | **PASSED** |
| **S3** | `S3_registrationIgnoresClientSuppliedDesignation` | `SecurityRegressionTest` | **PASSED** |
| **S4** | `S4_adminEndpointsDoNotLeakPasswordHash` | `SecurityRegressionTest` | **PASSED** |
| **S5** | `S5_publicConfigControlsDemoModeAndHidesDemoCredentials` | `SecurityRegressionTest` | **PASSED** |
| **S6** | `S6_accessPolicyEnforcesRoleMatrixAndOwnership` | `SecurityRegressionTest` | **PASSED** |
| **S7** | `S7_jwtAuthenticationFilterRejectsLockedOrDeactivatedUser` | `SecurityRegressionTest` | **PASSED** |
| **S8** | `S8_securityHeadersArePresentOnHttpResponses` | `SecurityRegressionTest` | **PASSED** |
| **F1** | `F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned` | `CorrectnessRegressionTest` | **PASSED** |
| **F2** | `F2_unmatchedServiceDoesNotPickArbitraryService` | `CorrectnessRegressionTest` | **PASSED** |
| **F3** | `F3_duplicateDetectionQueriesOnlyRelevantServiceAndWindow` | `CorrectnessRegressionTest` | **PASSED** |
| **F4** | `F4_duplicateIncidentDoesNotConsumeTechnicianCapacity` | `CorrectnessRegressionTest` | **PASSED** |
| **F5** | `F5_lowConfidenceClassificationLeavesIncidentInTriageQueue` | `CorrectnessRegressionTest` | **PASSED** |
| **F6** | `F6_reporterHistoryCountsOnlyRecentUnresolvedTicketsOnService` | `CorrectnessRegressionTest` | **PASSED** |
| **F7** | `F7_concurrentIncidentCreationDoesNotExceedTechnicianCapacity` | `CorrectnessRegressionTest` | **PASSED** |
| **F8** | `F8_scenarioS4VerifiesDuplicateAvalanchePriorityElevation` | `CorrectnessRegressionTest` | **PASSED** |
| **A4** | `A4_optimisticLockConflictYields409Conflict` | `CorrectnessRegressionTest` | **PASSED** |
| **A1** | `A1_applicationLayerMustNotDependOnInfrastructure` | `ArchitectureRegressionTest` | **PASSED** |
| **A1** | `A1_webLayerMustNotDependOnInfrastructure` | `ArchitectureRegressionTest` | **PASSED** |
| **A1** | `A1_controllersMustNotReturnDomainOrJpaEntities` | `ArchitectureRegressionTest` | **PASSED** |
| **A1** | `A1_controllersMustNotDependOnRepositories` | `ArchitectureRegressionTest` | **PASSED** |

Total Project Test Results: **186 passed, 0 failed, 0 skipped**.
Coverage: **85% instructions, 64% branches** (JaCoCo).

