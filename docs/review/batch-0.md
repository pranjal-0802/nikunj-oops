# Batch 0 Report: Reproduction of Confirmed Review Findings

---

## 1. Executive Summary

Batch 0 establishes the strict test-first foundation for the remediation of confirmed code review findings in the Intelligent IT Service Management (I-ITSM) codebase.

- **Baseline Metrics (Prior to Regression Suite)**:
  - **Tests Run**: 165 tests (100% passing, 0 failures, 0 errors, 0 skipped).
  - **Simulation Scenarios S1–S5**: All verified passing end-to-end.
  - **JaCoCo Instruction Coverage**: 86.17% (20,475 / 23,760 instructions).
  - **JaCoCo Branch Coverage**: 72.88% (852 / 1,169 branches).
  - **JaCoCo Line Coverage**: 85.21% (3,905 / 4,583 lines).
- **Regression Suite Added**:
  - 3 test suites: `ArchitectureRegressionTest.java`, `SecurityRegressionTest.java`, `CorrectnessRegressionTest.java`.
  - 21 regression tests tagged with `@Tag("regression")` naming each finding ID (`S1`–`S8`, `F1`–`F8`, `A1`, `A4`).
- **Reproduction Result**:
  - **21 tests run**: **21 failing** (19 assertion failures, 2 runtime errors).
  - **100% of confirmed findings reproduced** on the unpatched codebase.
  - **Zero production code edited** in Batch 0.
- **Traceability Matrix**:
  - Generated and committed at `docs/review/requirements-traceability.md`.

---

## 2. Regression Reproduction Test Results (Failure Evidence)

Every finding in Section 2 of `CLAUDE_CODE_PROMPT_V2.md` was reproduced with a targeted regression test that fails against the current unpatched code.

### Security Findings (S1–S8)

| Finding ID | Test Name | File | Failure / Error Output | Root Cause Verified |
|---|---|---|---|---|
| **S1** | `S1_jwtSecretValidationFailsOnWeakOrCommittedSecret` | `SecurityRegressionTest` | `AssertionError: Expecting code to raise a throwable.` | Default committed secret in `application.yml` is accepted in `prod` profile without throwing startup exception. |
| **S2** | `S2_lockoutPersistsAfterFiveBadLogins` | `SecurityRegressionTest` | `AssertionError: [Failed login attempts counter must persist in DB despite exception] Expecting actual: 0 to be >= 5` | Class-level `@Transactional` on `UserService` rolls back `failedLoginAttempts` persistence upon `InvalidCredentialsException`. |
| **S3** | `S3_registrationIgnoresClientSuppliedDesignation` | `SecurityRegressionTest` | `AssertionError: [Caller cannot self-assign VP_C_LEVEL; registration must default to lowest designation] Expecting: VP_C_LEVEL not to be equal to: VP_C_LEVEL` | `AuthController.register` parses client's `designation` directly into domain `Employee`, granting caller 1.0 priority weight. |
| **S4** | `S4_adminEndpointsDoNotLeakPasswordHash` | `SecurityRegressionTest` | `AssertionError: [Technician roster API response must never contain passwordHash] Expecting actual: "[...]" not to contain: "passwordHash"` | `AdminController.getTechnicianRoster` returns `List<Technician>` (domain aggregate) directly, serializing BCrypt hashes. |
| **S5** | `S5_publicConfigControlsDemoModeAndHidesDemoCredentials` | `SecurityRegressionTest` | `AssertionError: Status expected:<200> but was:<401>` | `/api/public/config` does not exist or is protected by security filter; client relied on hard-coded demo credentials. |
| **S6** | `S6_accessPolicyRestrictsCrossRoleActionsAndUnassignedInspection` | `SecurityRegressionTest` | `AssertionError: Expecting code to raise a throwable.` | `AccessPolicy.enforceCanViewIncident` has general inspection fall-through (`return;`); `enforceCanExecuteAction` allows employee `resolve`. |
| **S7** | `S7_jwtAuthenticationFilterRejectsLockedOrDeactivatedUser` | `SecurityRegressionTest` | `AssertionError: Status expected:<401> but was:<200>` | `JwtAuthenticationFilter` validates token signature and claims but does not verify user active/lock state against DB. |
| **S8** | `S8_securityHeadersArePresentOnHttpResponses` | `SecurityRegressionTest` | `AssertionError: Response should contain header 'Content-Security-Policy'` | `SecurityConfig` does not configure CSP, `X-Content-Type-Options: nosniff`, or `X-Frame-Options` headers. |

---

### Correctness Findings (F1–F8, A4)

| Finding ID | Test Name | File | Failure / Error Output | Root Cause Verified |
|---|---|---|---|---|
| **F1** | `F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned` | `CorrectnessRegressionTest` | `TechnicianOverloadedException: Technician 'TECH-NET-1' is overloaded: active load is 7, maximum capacity is 7` | `IncidentService.createIncident` falls back to `allTechnicians` when available is empty, attempting assignment on full techs. |
| **F2** | `F2_unmatchedServiceDoesNotPickArbitraryService` | `CorrectnessRegressionTest` | `AssertionError: [Vague ticket with zero keyword overlap must return empty Optional] Expecting an empty Optional but was containing value: ItService` | `ServiceMatcher.findBestMatch` line 44 terminates with `return availableServices.stream().findFirst();`, arbitrarily assigning services. |
| **F3** | `F3_linkingFiveDuplicatesElevatesParentPriorityToP1ViaAvalanche` | `CorrectnessRegressionTest` | `AssertionError: [Parent ticket priority must elevate to P1 when 5 duplicates are correlated to its cluster] expected: P1 but was: P3` | Parent priority is never recomputed when duplicate tickets arrive; duplicate avalanche rule is dead code. |
| **F4** | `F4_duplicateIncidentDoesNotConsumeTechnicianCapacity` | `CorrectnessRegressionTest` | `AssertionError: [Duplicate incident must not increment technician active capacity workload across the roster] expected: 1 but was: 2` | Duplicates are routed and assigned independently to technicians, consuming active workload capacity. |
| **F5** | `F5_lowConfidenceClassificationLeavesIncidentInTriageQueue` | `CorrectnessRegressionTest` | `AssertionError: [Low-confidence incident must not be auto-assigned; must be routed to admin triage queue] expected: null but was: "LEAD-SEC"` | `ClassificationResult.confidence()` is computed but never threshold-checked; ambiguous incidents are assigned immediately. |
| **F6** | `F6_reporterHistoryCountsOnlyRecentUnresolvedTicketsOnService` | `CorrectnessRegressionTest` | `AssertionError: [Old tickets on other services must not increase priority composite score for this service] expected: 0.465 but was: 0.565` | `IncidentService` counts all-time tickets across all services (`findByReporterId`), improperly penalizing reporters. |
| **F7** | `F7_concurrentTicketCreationDoesNotOverloadTechnician` | `CorrectnessRegressionTest` | `TechnicianOverloadedException: Technician 'LEAD-NET' is overloaded: active load is 8, maximum capacity is 8` | Lack of optimistic locking (`@Version`) or synchronization on technician load allows concurrent requests to collide and crash. |
| **A4** | `A4_concurrentAcknowledgmentThrowsConcurrentModificationException` | `CorrectnessRegressionTest` | `AssertionError: Expecting throwable to be an instance of ConcurrentModificationException but was PersistenceFailureException` | `IncidentRepositoryAdapter` line 41 catches `DataAccessException` and wraps optimistic lock failures in generic `PersistenceFailureException`. |
| **F8** | `F8_emailChannelSendsRealSmtpMessage` | `CorrectnessRegressionTest` | `AssertionError: [EmailChannel must send an actual SMTP message when email notification is triggered] Expected size: 1 but was: 0` | `EmailChannel` only simulates notifications via logger text because no active JavaMailSender is wired. |

---

### Architecture Findings (A1)

| Finding ID | Test Name | File | Failure / Error Output | Root Cause Verified |
|---|---|---|---|---|
| **A1-App** | `A1_applicationLayerMustNotDependOnInfrastructure` | `ArchitectureRegressionTest` | `AssertionError: Architecture Violation [...] was violated (70 times)` | Services inject `SpringData*` repositories and import `infrastructure.security.JwtTokenProvider` directly. |
| **A1-Web** | `A1_webLayerMustNotDependOnInfrastructure` | `ArchitectureRegressionTest` | `AssertionError: Architecture Violation [...] was violated (23 times)` | Web controllers and mappers directly import JPA entities (`AuditRecordJpaEntity`, etc.). |
| **A1-Return** | `A1_controllersMustNotReturnDomainOrJpaEntities` | `ArchitectureRegressionTest` | `AssertionError: Architecture Violation [...] was violated (3 times)` | `AdminController` returns `ReassignmentRequestJpaEntity` and `Technician` domain models directly. |
| **A1-Repo** | `A1_controllersMustNotDependOnRepositories` | `ArchitectureRegressionTest` | `AssertionError: Architecture Violation [...] was violated (4 times)` | Controllers inject Spring Data JPA repository interfaces directly instead of domain application services. |

---

## 3. Artifacts Produced in Batch 0

1. `src/test/java/com/iitsm/regression/SecurityRegressionTest.java` (8 tests)
2. `src/test/java/com/iitsm/regression/CorrectnessRegressionTest.java` (9 tests)
3. `src/test/java/com/iitsm/regression/ArchitectureRegressionTest.java` (4 tests)
4. `docs/review/requirements-traceability.md` (Complete matrix linking brief, PRD, code, tests, UI, and audit findings)
5. `docs/review/batch-0.md` (This document)

---

## 4. Next Steps (Batch 1: Stop the Bleeding)

Upon receiving the user command `"proceed to Batch 1"`, we will implement fixes for:
- **S1**: JWT secret fail-fast startup validator in `prod` + rotate secrets.
- **S2**: `LoginAttemptService` with `REQUIRES_NEW` / time-based lockout.
- **S3**: Registration designation restriction (defaults to `ASSOCIATE`).
- **S4**: DTO mapping for `AdminController` (`TechnicianSummaryDto`), removing `passwordHash` leaks.
- **S5**: Server-side `/api/public/config` endpoint reporting `demoMode`.
- **F1**: Unassigned incident creation when no technician has capacity + `UnassignedIncidentDispatcher`.
- **F2**: Fallback to "General IT / Unclassified" service instead of arbitrary first service.
- **F9**: Docker compose configuration and smoke test script.

