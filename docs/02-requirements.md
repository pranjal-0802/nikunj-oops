# Requirements Specification & Traceability Matrix: I-ITSM

## 1. Functional Requirements (FR)

### FR-1: Incident Intake & Automated Intelligence
- **FR-1.1**: The system shall accept incident reports from authenticated employees with title (5–120 characters) and description (20–4000 characters).
- **FR-1.2**: The system shall automatically classify incoming tickets into one of 9 ITIL categories (`HARDWARE`, `SOFTWARE`, `NETWORK`, `DATABASE`, `SECURITY`, `CLOUD`, `EMAIL`, `ACCESS`, `APPLICATION`) using a pure-Java Composite Classifier (`KeywordClassifier` + `NaiveBayesClassifier`).
- **FR-1.3**: The system shall match ticket text against registered IT services and associate the affected infrastructure service.
- **FR-1.4**: The system shall compute an explainable composite priority score using 6 weighted factors (Service Criticality 0.30, Impact 0.20, Urgency 0.15, Designation 0.15, Aging 0.10, History 0.10) and map the score to base priority (`P1`, `P2`, `P3`, `P4`).
- **FR-1.5**: The system shall evaluate a chain of Policy Override Rules:
  - `RULE_SECURITY_BREACH`: Security incidents with confirmed breach keywords force minimum priority P2.
  - `RULE_CRITICAL_SERVICE_OUTAGE`: Outages on Tier-4 Mission-Critical services force priority P1.
  - `RULE_DUPLICATE_AVALANCHE`: 5 or more correlated duplicate tickets elevate priority by one tier.
- **FR-1.6**: The system shall detect duplicate tickets using TF-IDF Vectorization and Cosine Similarity over a 72-hour sliding window. If similarity $\ge 0.70$, the system links the ticket bidirectionally to the cluster root parent and publishes `DomainEvent.DuplicateDetected`.
- **FR-1.7**: The system shall route the incident to an eligible technician belonging to the owning support team using the active `AssignmentStrategy` (`SKILL_BASED`, `LOAD_BALANCED`, `EXPERIENCE_BASED`, `ROUND_ROBIN`, `WEIGHTED_COMPOSITE`), enforcing hard capacity limits (`maxConcurrentIncidents`).

### FR-2: Incident State Machine & Lifecycle Management
- **FR-2.1**: The system shall enforce a 9-state finite state machine using the State Pattern:
  $$\text{NEW} \rightarrow \text{CLASSIFIED} \rightarrow \text{ASSIGNED} \rightarrow \text{ACKNOWLEDGED} \rightarrow \text{IN\_PROGRESS} \rightleftarrows \text{PENDING\_INFO} \rightarrow \text{RESOLVED} \rightarrow \text{VERIFIED} \rightarrow \text{CLOSED}$$
- **FR-2.2**: The system shall disallow illegal transitions and throw `InvalidStateTransitionException`.
- **FR-2.3**: Technicians shall acknowledge assigned tickets before starting investigation.
- **FR-2.4**: When additional details are requested from the reporter, the ticket shall transition to `PENDING_INFO`, immediately pausing the SLA target resolution clock.
- **FR-2.5**: When the employee provides diagnostic details, the ticket shall return to `IN_PROGRESS`, resuming the SLA clock with an extension equal to the paused duration.
- **FR-2.6**: Technicians shall record structured ITIL resolution metadata (root cause explanation, resolution steps, preventive actions, failure category).
- **FR-2.7**: Reporters shall verify resolutions. If the problem persists, the employee may reject the fix with a rationale, reopening the ticket (`reopenCount` incremented by 1) for rework.
- **FR-2.8**: Verified tickets shall be closed with a 1–5 star rating and feedback comment.

### FR-3: Wrong-Domain Triage & Reassignment Loop
- **FR-3.1**: Technicians may flag tickets as out-of-scope (`flagWrongDomain`) with a documented reason.
- **FR-3.2**: Flagging wrong-domain shall revert the ticket to `CLASSIFIED`, clear the assignee, release technician capacity, and place the ticket in the Admin Triage Queue.
- **FR-3.3**: Administrators shall reclassify tickets with updated category and service, recalculate priority, and reassign the ticket. The rejecting technician is excluded from reassignment.

### FR-4: SLA Monitoring & Multi-Tier Escalation Chain
- **FR-4.1**: The system shall attach default SLA policies based on priority:
  - **P1**: 5 min response / 30 min resolution
  - **P2**: 15 min response / 2 hour resolution
  - **P3**: 1 hour response / 8 hour resolution
  - **P4**: 4 hour response / 24 hour resolution
- **FR-4.2**: The system shall calculate continuous risk scores ($0.0 \dots 1.0$) and transition statuses:
  - $< 0.75$: `ON_TRACK`
  - $\ge 0.75$: `AT_RISK` $\rightarrow$ publishes `DomainEvent.SlaWarning`
  - $\ge 0.90$: `WARNING` $\rightarrow$ publishes `DomainEvent.SlaWarning`
  - $\ge 1.00$: `BREACHED` $\rightarrow$ publishes `DomainEvent.SlaViolated`
- **FR-4.3**: The system shall process escalations through a Chain of Responsibility:
  - **Tier 1 (`Tier1TechnicianHandler`)**: 75% resolution window $\rightarrow$ technician reminder alert.
  - **Tier 2 (`Tier2TeamLeadHandler`)**: 90% resolution window or missed response target $\rightarrow$ Team Lead intervention.
  - **Tier 3 (`Tier3SystemAdminHandler`)**: P2 hard breach or triage stagnation ($> 30$ mins) $\rightarrow$ System Admin alert.
  - **Tier 4 (`Tier4ItManagerHandler`)**: P1 breach on Mission-Critical service $\rightarrow$ IT Operations Manager escalation.

### FR-5: Simulation Engine & Virtual Clock Acceleration
- **FR-5.1**: The system shall decorate `java.time.Clock` via `AdjustableClock` supporting dynamic speed multipliers ($\times 1, \times 60, \times 600$), discrete fast-forward jumps, and resets.
- **FR-5.2**: The system shall provide automated end-to-end simulation scenarios (S1 Happy Path, S2 Critical Outage & SLA Escalation, S3 Wrong-Domain Triage, S4 Duplicate Storm Avalanche, S5 Rejection Loop & SLA Clock Pausing).

### FR-6: Executive & Operational Reporting
- **FR-6.1**: The system shall generate workload reports (team capacities, active tickets, load %).
- **FR-6.2**: The system shall generate service analysis reports (incident volume per service, criticality breakdown).
- **FR-6.3**: The system shall compute SLA performance metrics (% compliance, breach distribution).
- **FR-6.4**: The system shall calculate Mean Time to Resolution (MTTR) and Mean Time to Respond (MTTResp).

---

## 2. Non-Functional Requirements (NFR)

- **NFR-1 (Architectural Isolation)**: The domain layer (`com.iitsm.domain.*`) must have zero dependencies on Spring Framework.
- **NFR-2 (Persona Data Privacy)**: Priority, SLA operational targets, and internal notes must be strictly excluded from the `EmployeeIncidentView` DTO at the type level.
- **NFR-3 (Code Quality & Coverage)**: The project must achieve $\ge 70\%$ instruction coverage on domain and application bundles measured via JaCoCo.
- **NFR-4 (Security & Authentication)**: Stateless JWT authentication with BCrypt password hashing; accounts must lock on the 5th consecutive failed login.
- **NFR-5 (Extensibility - OCP)**: New categories, strategies, priority factors, classifiers, and escalation tiers must be addable without editing existing classes.
- **NFR-6 (Auditability)**: Every state change, assignment, note, and escalation must produce an immutable audit log record.

---

## 3. Requirements Traceability Matrix (RTM)

| Req ID | Requirement Description | Implementation Classes | Automated Test Verification | Status |
| :--- | :--- | :--- | :--- | :--- |
| **FR-1.1** | Incident Intake (5–120 title, 20–4000 desc) | `Incident.java`, `IncidentFactory.java` | `IncidentValidationTest.java` | **VERIFIED** |
| **FR-1.2** | Pure Java NLP Classification | `CompositeClassifier.java`, `NaiveBayesClassifier.java` | `IncidentClassifierBenchmarkTest.java` | **VERIFIED** |
| **FR-1.3** | IT Service Identification | `ServiceMatcher.java`, `ItService.java` | `ServiceMatcherTest.java` | **VERIFIED** |
| **FR-1.4** | 6-Factor Composite Priority Engine | `CompositePriorityCalculator.java`, `PriorityFactor.java` | `CompositePriorityCalculatorTest.java` | **VERIFIED** |
| **FR-1.5** | Policy Override Rules Chain | `CompositePriorityCalculator.java` | `CompositePriorityCalculatorTest.java` | **VERIFIED** |
| **FR-1.6** | Duplicate Ticket Storm Correlation | `DuplicateDetectionService.java`, `CosineTfIdfSimilarity.java` | `DuplicateDetectionTest.java`, `SimulationScenariosTest.java` | **VERIFIED** |
| **FR-1.7** | Capacity-Aware Assignment Strategies | `AssignmentStrategy.java`, `CandidateFilter.java` | `AssignmentStrategyTest.java` | **VERIFIED** |
| **FR-2.1** | 9-State Machine Lifecycle | `IncidentState.java`, `IncidentStates.java` | `IncidentStateTransitionTableTest.java` | **VERIFIED** |
| **FR-2.4** | SLA Pausing during `PENDING_INFO` | `SlaTracker.java`, `PendingInfoState.java` | `SlaTrackerTest.java`, `SimulationScenariosTest.java` | **VERIFIED** |
| **FR-2.7** | Resolution Rejection & Rework Loop | `ResolvedState.java`, `Incident.java` | `IncidentLifecycleEndToEndTest.java` | **VERIFIED** |
| **FR-3.1** | Wrong-Domain Triage Workflow | `IncidentService.java`, `ClassifiedState.java` | `SimulationScenariosTest.java` | **VERIFIED** |
| **FR-4.2** | SLA Monitoring & Risk Scoring | `SlaService.java`, `SlaTracker.java` | `SlaServiceTest.java` | **VERIFIED** |
| **FR-4.3** | 4-Tier Chain of Responsibility Escalation | `EscalationHandler.java`, `EscalationService.java` | `EscalationChainTest.java` | **VERIFIED** |
| **FR-5.1** | Time-Accelerated Simulation Clock | `AdjustableClock.java`, `SimulationService.java` | `AdjustableClockTest.java` | **VERIFIED** |
| **FR-6.1** | Operational Reporting Engine | `ReportingService.java`, `ReportController.java` | `ReportingServiceTest.java` | **VERIFIED** |
| **NFR-1** | Zero Spring Imports in Domain Core | `pom.xml`, `com.iitsm.domain.*` | Architecture check, `mvn verify` | **VERIFIED** |
| **NFR-2** | Employee Persona Data Privacy | `EmployeeIncidentView.java`, `AccessPolicy.java` | `EmployeePrivacyControllerTest.java` | **VERIFIED** |
| **NFR-3** | $\ge 70\%$ JaCoCo Code Coverage | `pom.xml`, Maven JaCoCo Plugin | Measured at **86.61%** | **VERIFIED** |
| **NFR-4** | JWT Security & 5-Strike Lockout | `SecurityConfig.java`, `UserService.java` | `SecurityAuthIntegrationTest.java` | **VERIFIED** |

