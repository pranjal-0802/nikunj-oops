# Batch 9 Report: Architecture Honesty, SQL Aggregates & God Service Split

## 1. Summary of Changes
Batch 9 resolves Round 2 code review findings **N4, N6, and N8**.

### N4: Documentation Honesty & Hexagonal Domain Purity
- **ArchUnit Hexagonal Core Rule**: Added `N4_domainLayerMustBePristineHexagonalCore` to `ArchitectureRegressionTest`. Verifies `com.iitsm.domain..` has zero dependencies on `org.springframework..`, `jakarta..`, `com.iitsm.application..`, `com.iitsm.infrastructure..`, or `com.iitsm.web..`.
- **Port Standardization**: Unified port 8085 across the application, `Dockerfile` (`EXPOSE 8085`), and documentation.
- **Rubric Truth-up**: Deleted Section 7 ("Grading Rubric Self-Assessment") from `README.md`.
- **Accurate Metric Reporting**: Updated `README.md` with measured test counts (202 tests) and JaCoCo coverage (85% instruction coverage).

### N6: Removal of Unbounded In-Memory Scans
- Added database aggregate count queries to `IncidentRepository`, `SpringDataIncidentRepository`, and `IncidentRepositoryAdapter`:
  - `countByStatusIn(List<IncidentStatus> statuses)`
  - `countByPriority(Priority priority)`
  - `findOpenIncidentsSince(Instant since)`
  - `findActiveIncidents()`
- Refactored `IncidentService.getAdminStats` to query database counts directly rather than loading all incidents into memory with `findAll().stream()`.
- Refactored `DuplicateDetectionService` to constrain candidate scanning to a 24-hour sliding window (`findOpenIncidentsSince(cutoff)`).
- Refactored `SlaService.evaluateAllActiveIncidents` to query active non-terminal tickets (`findActiveIncidents()`).

### N8: God Service Decomposition
Decomposed the monolithic 800-line `IncidentService` into three cohesive, focused collaborators (< 300 lines each):
1. **`IncidentIntakePipeline`**: Orchestrates validation, ML NLP classification, service matching, duplicate correlation, multi-factor priority determination, and technician assignment.
2. **`IncidentLifecycleService`**: Manages state machine transitions (`acknowledge`, `startInvestigation`, `addNote`, `requestInfo`, `provideInfo`, `resolve`, `verify`, `reject`, `close`, `flagWrongDomain`).
3. **`AdminIncidentService`**: Manages administrative controls (`assign`, `overridePriority`, `reclassify`, `getAdminStats`, `getUnifiedTriageQueue`, `acceptTriageSuggestion`).
4. **`IncidentService` Facade**: Delegates to the three collaborators, preserving 100% backward compatibility for all controllers and tests.

---

## 2. Verification Metrics
- **Maven Goal**: `./mvnw clean verify`
- **Total Tests Run**: **202 passed (0 failures, 0 errors, 0 skipped)**
- **JaCoCo Instruction Coverage**: **85%** (23,855 of 27,866 instructions covered)
- **JaCoCo Branch Coverage**: **64%** (1,265 of 1,974 branches covered)
- **Total Classes Analyzed**: **251 classes**

