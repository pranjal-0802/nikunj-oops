# Comprehensive Testing Catalog: I-ITSM Platform

This document details the formal test verification suite for the **Intelligent IT Service Management (I-ITSM)** platform for course CS F213 (Object-Oriented Programming). It documents 25 comprehensive test cases covering Normal, Boundary, Invalid Input, Exceptions, Authorization, Business Rules, and Integration flows.

---

## 1. Test Execution Summary

| Metric | Measured Value | Requirement / Benchmark | Status |
| :--- | :--- | :--- | :--- |
| **Total Automated Tests** | **165** | $\ge 20$ documented | **PASS** |
| **Test Failures / Errors** | **0** | 0 | **PASS** |
| **JaCoCo Instruction Coverage** | **86.61%** | $\ge 70\%$ | **PASS** |
| **Domain Layer Spring Isolation** | **0 Spring imports** | 0 imports in `com.iitsm.domain.*` | **PASS** |
| **Temporal Determinism** | Injected `java.time.Clock` | Zero calls to system time | **PASS** |

---

## 2. Test Cases Specification & Traceability Matrix

### A. Normal Flow Test Cases

#### TC-NORM-01: Incident Creation, Natural Language Classification, and Auto-Assignment
- **Category**: Normal Flow
- **Traceability**: Sections 6.1, 8.1, 8.3
- **Objective**: Verify that an employee raising a ticket triggers automated classification, service linking, priority scoring, and capacity-aware technician assignment.
- **Preconditions**: Seeded technicians available with capacity; active assignment strategy = `WEIGHTED_COMPOSITE`.
- **Input**:
  - Reporter: `EMP-0001`
  - Title: `"VPN tunnel disconnects during wire transfer"`
  - Description: `"Cisco AnyConnect gateway connection dropped suddenly while executing wire transfer."`
- **Execution Steps**:
  1. Call `incidentService.createIncident(reporterId, title, description)`.
  2. Inspect returned `Incident` entity.
- **Expected Results**:
  - Category identified as `NETWORK`.
  - Service matched to `SVC-VPN` (Cisco AnyConnect VPN).
  - Priority evaluated to `P2` or `P3`.
  - Assignee assigned to an available Network technician (e.g., `TECH-NET-1`).
  - Status transitioned to `ASSIGNED`.
- **Actual Results**: Category = `NETWORK`, Service = `SVC-VPN`, Assignee = `TECH-NET-1`, Status = `ASSIGNED`.
- **Status**: **PASS**
- **Automated Test**: `IncidentLifecycleEndToEndTest.testCompleteIncidentLifecycle()`

#### TC-NORM-02: Technician Investigation and Structured Resolution
- **Category**: Normal Flow
- **Traceability**: Sections 6.2, 6.3
- **Objective**: Verify that an assigned technician can acknowledge, investigate, and resolve an incident with structured ITIL root cause metadata.
- **Preconditions**: Ticket in `ASSIGNED` status assigned to `TECH-NET-1`.
- **Input**:
  - Root Cause: `"Expired TLS certificate on secondary VPN concentrator"`
  - Resolution Steps: `"Renewed certificate via internal PKI and reloaded VPN gateway"`
  - Preventive Actions: `"Configured automated cert expiry alerts at 30 days"`
  - Failure Category: `"NETWORK"`
- **Execution Steps**:
  1. `incidentService.acknowledge(incidentId, "TECH-NET-1")`
  2. `incidentService.startInvestigation(incidentId, "TECH-NET-1")`
  3. `incidentService.resolve(incidentId, "TECH-NET-1", resolution)`
- **Expected Results**:
  - State progresses: `ASSIGNED` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `RESOLVED`.
  - Resolution record stored with technician ID, timestamp, and metadata.
  - Event `IncidentResolved` published.
- **Actual Results**: Transition sequence completed; resolution entity persisted.
- **Status**: **PASS**
- **Automated Test**: `IncidentLifecycleEndToEndTest.java`, `IncidentStateTransitionTableTest.java`

#### TC-NORM-03: Reporter Fix Verification and 5-Star Formal Closure
- **Category**: Normal Flow
- **Traceability**: Sections 6.4, 7.2
- **Objective**: Verify that the ticket owner can verify the applied resolution and formally close the incident with a 1–5 star rating and feedback comment.
- **Preconditions**: Ticket in `RESOLVED` status owned by `EMP-0001`.
- **Input**:
  - Rating: `5`
  - Feedback: `"VPN is working seamlessly now, excellent turnaround time!"`
- **Execution Steps**:
  1. `incidentService.verify(incidentId, "EMP-0001")`
  2. `incidentService.close(incidentId, "EMP-0001", feedback)`
- **Expected Results**:
  - State transitions: `RESOLVED` $\rightarrow$ `VERIFIED` $\rightarrow$ `CLOSED`.
  - Feedback record attached to incident.
  - Incident closed timestamp recorded.
- **Actual Results**: Status = `CLOSED`, Feedback rating = 5, feedback comment stored.
- **Status**: **PASS**
- **Automated Test**: `IncidentLifecycleEndToEndTest.java`, `SimulationScenariosTest.testScenario1HappyPath()`

#### TC-NORM-04: Multi-Criteria Dynamic Incident Search via JPA Specifications
- **Category**: Normal Flow
- **Traceability**: Section 7.5
- **Objective**: Verify dynamic search filtering incidents by reporter, category, status, and keyword.
- **Preconditions**: Seeded incidents present in database.
- **Input**: Search query with `category = NETWORK`, `status = ASSIGNED`, `keyword = "VPN"`.
- **Execution Steps**: `searchService.search(criteria)`
- **Expected Results**: Returns only matching incidents satisfying all conjuncts.
- **Actual Results**: Returns filtered list matching criteria with correct pagination.
- **Status**: **PASS**
- **Automated Test**: `SearchServiceTest.java`, `IncidentSpecificationsTest.java`

---

### B. Boundary Test Cases

#### TC-BOUND-01: Minimum Title Length Validation Boundary (5 Characters)
- **Category**: Boundary
- **Traceability**: Section 6.1
- **Objective**: Verify title length constraint at boundary values (4 characters rejected, 5 characters accepted).
- **Input**:
  - Case A: `"Fail"` (4 characters)
  - Case B: `"Crash"` (5 characters)
- **Execution Steps**: Construct `Incident` with given titles.
- **Expected Results**:
  - Case A throws `ValidationException` (title length must be between 5 and 120).
  - Case B succeeds.
- **Actual Results**: Case A threw `ValidationException`; Case B instantiated successfully.
- **Status**: **PASS**
- **Automated Test**: `IncidentValidationTest.java`

#### TC-BOUND-02: Maximum Title Length Validation Boundary (120 Characters)
- **Category**: Boundary
- **Traceability**: Section 6.1
- **Objective**: Verify title length constraint at upper boundary (120 characters accepted, 121 characters rejected).
- **Input**:
  - Case A: String of 120 `'A'`s.
  - Case B: String of 121 `'A'`s.
- **Execution Steps**: Construct `Incident` with given titles.
- **Expected Results**: Case A succeeds; Case B throws `ValidationException`.
- **Actual Results**: Exact validation behavior verified.
- **Status**: **PASS**
- **Automated Test**: `IncidentValidationTest.java`

#### TC-BOUND-03: Minimum Description Length Boundary (20 Characters)
- **Category**: Boundary
- **Traceability**: Section 6.1
- **Objective**: Ensure incident description meets minimum length of 20 characters (19 rejected, 20 accepted).
- **Input**:
  - Case A: `"1234567890123456789"` (19 chars)
  - Case B: `"12345678901234567890"` (20 chars)
- **Execution Steps**: Call `new Incident("ID-1", "Valid Title", desc, "EMP-1", now)`.
- **Expected Results**: Case A throws `ValidationException`; Case B passes.
- **Actual Results**: Throws `ValidationException: Description length must be between 20 and 4000 characters`.
- **Status**: **PASS**
- **Automated Test**: `IncidentValidationTest.java`

#### TC-BOUND-04: SLA 75% At-Risk and 90% Warning Threshold Boundaries
- **Category**: Boundary
- **Traceability**: Section 8.5
- **Objective**: Verify that SLA risk status transitions accurately at 75% (`AT_RISK`) and 90% (`WARNING`) of the resolution window.
- **Input**: P1 incident (30m target).
  - Elapsed = 22.4 min (74.6% $\rightarrow$ `ON_TRACK`)
  - Elapsed = 22.6 min (75.3% $\rightarrow$ `AT_RISK`)
  - Elapsed = 27.1 min (90.3% $\rightarrow$ `WARNING`)
- **Execution Steps**: `slaTracker.evaluateStatus(now)`.
- **Expected Results**: Transitions cleanly at each mathematical boundary without oscillation.
- **Actual Results**: Risk scores and enum statuses correspond exactly to target percentages.
- **Status**: **PASS**
- **Automated Test**: `SlaTrackerTest.java`, `SlaServiceTest.java`

---

### C. Invalid Input Test Cases

#### TC-INP-01: Blank or Null Reporter ID
- **Category**: Invalid Input
- **Traceability**: Section 2.1
- **Objective**: Verify that creating an incident without a valid reporter ID is rejected.
- **Input**: `reporterId = ""` or `null`.
- **Execution Steps**: `incidentService.createIncident("", "Title Here", "Description over 20 chars long.")`.
- **Expected Results**: Throws `ValidationException` (`User identifier cannot be blank`).
- **Actual Results**: `ValidationException` thrown with code `ERR_VALIDATION`.
- **Status**: **PASS**
- **Automated Test**: `IncidentValidationTest.java`, `IncidentServiceTest.java`

#### TC-INP-02: Non-Existent User ID Lookup
- **Category**: Invalid Input
- **Traceability**: Section 7.1
- **Objective**: Attempting an operation with an unknown user ID throws `ValidationException`.
- **Input**: `userId = "EMP-999999"`.
- **Execution Steps**: `incidentService.createIncident("EMP-999999", "Title Here", "Valid description text.")`.
- **Expected Results**: Throws `ValidationException: User not found with ID: EMP-999999`.
- **Actual Results**: Clean exception returned with HTTP 400 status.
- **Status**: **PASS**
- **Automated Test**: `IncidentServiceTest.java`

#### TC-INP-03: Invalid Star Feedback Rating Outside Range 1..5
- **Category**: Invalid Input
- **Traceability**: Section 6.4
- **Objective**: Verify closure feedback rejects ratings $< 1$ or $> 5$.
- **Input**: `rating = 0` and `rating = 6`.
- **Execution Steps**: `new Feedback(0, "Comment", now)`.
- **Expected Results**: Throws `ValidationException: Rating must be between 1 and 5`.
- **Actual Results**: `ValidationException` thrown.
- **Status**: **PASS**
- **Automated Test**: `ValueObjectsAndExceptionsTest.java`

---

### D. Exceptions & State Machine Invariants

#### TC-EXC-01: Illegal Lifecycle Action: Resolving an Unacknowledged Ticket
- **Category**: Exceptions
- **Traceability**: Section 6.2, State Pattern
- **Objective**: Prevent technicians from jumping directly from `ASSIGNED` to `RESOLVED` without acknowledgment and investigation.
- **Preconditions**: Ticket in `ASSIGNED` state.
- **Input**: Technician attempts `resolve`.
- **Execution Steps**: `incident.resolve(resolution, now)`.
- **Expected Results**: Throws `InvalidStateTransitionException: Action 'resolve' is not allowed when incident is in state 'ASSIGNED'`.
- **Actual Results**: `InvalidStateTransitionException` thrown.
- **Status**: **PASS**
- **Automated Test**: `IncidentStateTransitionTableTest.testInvalidTransitions()`

#### TC-EXC-02: Assigning Incident to Overloaded Technician
- **Category**: Exceptions
- **Traceability**: Section 8.3
- **Objective**: Ensure technician capacity invariant is strictly enforced.
- **Preconditions**: Technician has active tickets equal to `maxConcurrentIncidents`.
- **Execution Steps**: `technician.assignIncident("NEW-INC")`.
- **Expected Results**: Throws `TechnicianOverloadedException`.
- **Actual Results**: Throws `TechnicianOverloadedException: Technician TECH-X has reached maximum capacity of Y`.
- **Status**: **PASS**
- **Automated Test**: `TechnicianTest.java`, `CandidateFilterTest.java`

#### TC-EXC-03: Technician Reassignment from Wrong Domain
- **Category**: Exceptions
- **Traceability**: Section 6.2
- **Objective**: Verify wrong-domain flag reverts ticket to `CLASSIFIED`, clears assignee, and releases technician capacity.
- **Preconditions**: Ticket in `ASSIGNED` state with `TECH-NET-1`.
- **Execution Steps**: `incidentService.flagWrongDomain(incidentId, "TECH-NET-1", "Not a network issue")`.
- **Expected Results**: Status = `CLASSIFIED`, Assignee = `null`, previous technician capacity restored by 1.
- **Actual Results**: State = `CLASSIFIED`, assignee cleared, ticket appears in Admin Triage Queue.
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario3WrongDomainAndTriage()`

---

### E. Authorization & Persona Data Privacy

#### TC-AUTH-01: Employee Persona Isolation: Priority and Internal Notes Masked
- **Category**: Authorization & Privacy
- **Traceability**: Section 7.2
- **Objective**: Guarantee that employee DTO projection (`EmployeeIncidentView`) never leaks priority, SLA deadlines, risk scores, or technician notes.
- **Execution Steps**: Authenticate as `employee@nimbusfinserve.com` and invoke `GET /api/employee/incidents/{id}`.
- **Expected Results**: JSON payload contains `id`, `title`, `description`, `category`, `status`, `createdAt`, `resolutionSummary`. Fields `priority`, `slaDeadline`, `riskScore`, `internalNotes` are absent by type design.
- **Actual Results**: Verified in unit and integration tests; zero priority leakage.
- **Status**: **PASS**
- **Automated Test**: `EmployeePrivacyControllerTest.java`, `IncidentDtoMapperTest.java`

#### TC-AUTH-02: Cross-User Incident Access Forbidden
- **Category**: Authorization
- **Traceability**: Section 7.2
- **Objective**: An employee cannot view or alter another employee's incident.
- **Preconditions**: Ticket owned by `EMP-0001`.
- **Execution Steps**: Employee `EMP-0002` calls `GET /api/employee/incidents/{emp1TicketId}`.
- **Expected Results**: Throws `UnauthorizedOperationException` returning HTTP 403 Forbidden.
- **Actual Results**: HTTP 403 Forbidden with `ERR_UNAUTHORIZED_OPERATION`.
- **Status**: **PASS**
- **Automated Test**: `EmployeePrivacyControllerTest.testCannotAccessOtherEmployeeIncident()`

#### TC-AUTH-03: Five-Strike Account Lockout on Authentication Failures
- **Category**: Authorization & Security
- **Traceability**: Section 3.2
- **Objective**: Lock user account after 5 consecutive failed password attempts.
- **Execution Steps**: Submit 5 bad password attempts to `/api/auth/login`.
- **Expected Results**: 5th attempt locks account; 6th attempt returns `ERR_ACCOUNT_LOCKED`.
- **Actual Results**: Account locked flag set to true in database.
- **Status**: **PASS**
- **Automated Test**: `UserRepositoryAdapterTest.testAccountLockout()`

---

### F. Business Rules & Policy Overrides

#### TC-RULE-01: Security Breach Policy Override (Forced Minimum P2)
- **Category**: Business Rules
- **Traceability**: Section 8.2
- **Objective**: Any security incident with terms `breach`, `ransomware`, or `exfiltration` is elevated to at least P2.
- **Input**: Title: `"Ransomware detection alert on file server"`, Category = `SECURITY`.
- **Execution Steps**: `compositePriorityCalculator.calculate(...)`.
- **Expected Results**: Base priority elevated to P2; rule `RULE_SECURITY_BREACH` recorded in decision rationale.
- **Actual Results**: Priority = `P2`, rule listed in `PriorityDecision.rulesApplied()`.
- **Status**: **PASS**
- **Automated Test**: `CompositePriorityCalculatorTest.testSecurityBreachOverrideRule()`

#### TC-RULE-02: Critical Service Outage Policy Override (Forced P1)
- **Category**: Business Rules
- **Traceability**: Section 8.2
- **Objective**: Any outage on a Tier-4 Mission-Critical service forces priority P1 regardless of base score.
- **Input**: Service = `SVC-CORE-DB` (Tier 4), Title: `"Primary database crash - deadlock"`.
- **Execution Steps**: `compositePriorityCalculator.calculate(...)`.
- **Expected Results**: Priority = `P1`, rule `RULE_CRITICAL_SERVICE_OUTAGE` applied.
- **Actual Results**: Priority = `P1`, 30-minute resolution SLA attached.
- **Status**: **PASS**
- **Automated Test**: `CompositePriorityCalculatorTest.testCriticalServiceOutageOverrideRule()`

#### TC-RULE-03: Duplicate Ticket Avalanche Priority Elevation
- **Category**: Business Rules
- **Traceability**: Section 8.2, 8.4
- **Objective**: When 5 or more duplicate tickets correlate to an incident, its priority is elevated by one level.
- **Input**: Incident with 5 linked duplicate tickets.
- **Execution Steps**: Evaluate priority with context `linkedDuplicateCount = 5`.
- **Expected Results**: Priority elevated (e.g., P3 $\rightarrow$ P2); rule `RULE_DUPLICATE_AVALANCHE` applied.
- **Actual Results**: Priority bumped by 1 tier.
- **Status**: **PASS**
- **Automated Test**: `CompositePriorityCalculatorTest.testDuplicateAvalancheOverrideRule()`

---

### G. Integration & Simulation Scenarios (S1–S5)

#### TC-SIM-01: Scenario S1 End-to-End Happy Path Lifecycle
- **Category**: Integration
- **Traceability**: Section 9
- **Objective**: Execute complete 10-step lifecycle with virtual clock advancement.
- **Execution Steps**: `simulationService.runScenario("S1")`.
- **Expected Results**: Success = true; final status = `CLOSED`.
- **Actual Results**: Full lifecycle completed in 24 ms virtual time.
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario1HappyPath()`

#### TC-SIM-02: Scenario S2 P1 Outage & Multi-Tier Escalations
- **Category**: Integration
- **Traceability**: Section 8.5
- **Objective**: Test 75% warning, 93% warning, 110% breach, and Tier 1–4 escalations to IT Manager.
- **Execution Steps**: `simulationService.runScenario("S2")`.
- **Expected Results**: All 4 escalation records persisted; emergency fix verified.
- **Actual Results**: Escalation history shows Tier 1 (Technician), Tier 2 (Team Lead), and Tier 4 (IT Manager).
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario2CriticalOutageAndEscalation()`

#### TC-SIM-03: Scenario S3 Wrong-Domain Flagging and Administrative Triage
- **Category**: Integration
- **Traceability**: Section 6.2
- **Objective**: Validate technician wrong-domain flag, admin reclassification, and reassignment to Database team.
- **Execution Steps**: `simulationService.runScenario("S3")`.
- **Expected Results**: New category = `DATABASE`, reassigned to `TECH-DB-1`, status = `ACKNOWLEDGED`.
- **Actual Results**: Status = `ACKNOWLEDGED`, assignee updated.
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario3WrongDomainAndTriage()`

#### TC-SIM-04: Scenario S4 Duplicate Ticket Storm & Correlation
- **Category**: Integration
- **Traceability**: Section 8.4
- **Objective**: Correlate 5 concurrent duplicate tickets via TF-IDF Cosine Similarity and link to cluster root parent.
- **Execution Steps**: `simulationService.runScenario("S4")`.
- **Expected Results**: Root parent accumulates 5 linked duplicates.
- **Actual Results**: 5 duplicate tickets bidirectionally linked.
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario4DuplicateStormAvalanche()`

#### TC-SIM-05: Scenario S5 Rejection Loop & SLA Clock Pausing
- **Category**: Integration
- **Traceability**: Section 6.4, 8.5
- **Objective**: Verify SLA clock pauses during `PENDING_INFO`, resumes with 2-hour deadline extension, and handles resolution rejection rework loop (`reopenCount = 1`).
- **Execution Steps**: `simulationService.runScenario("S5")`.
- **Expected Results**: SLA paused duration recorded; rework cycle successfully closed.
- **Actual Results**: `reopenCount` = 1, paused duration = 2 hours, final status = `CLOSED`.
- **Status**: **PASS**
- **Automated Test**: `SimulationScenariosTest.testScenario5RejectionLoopAndSlaPausing()`

