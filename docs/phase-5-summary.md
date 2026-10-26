# Phase 5 Summary: SLA Monitoring, Escalation Chain, Reporting, and Simulation Engine

## 1. Executive Summary & Grading Readiness
Phase 5 completes the operational automation and time-acceleration backbone of the Intelligent IT Service Management (I-ITSM) platform for course CS F213 (Object-Oriented Programming).

Key achievements in Phase 5:
- **Time-Accelerated Virtual Simulation Engine**: Built `AdjustableClock` extending `java.time.Clock` supporting dynamic speed multipliers ($\times 1$, $\times 60$, $\times 600$), discrete fast-forward jumps, and state resets.
- **5 Scripted Real-World End-to-End Scenarios (S1–S5)**: Fully automated demonstration scenarios covering the entire incident lifecycle, P1 SLA breaches, wrong-domain triage loops, duplicate storms, and clock pauses.
- **Multi-Tier Escalation Chain of Responsibility**: Fully decoupled 4-tier escalation hierarchy (`Tier1TechnicianHandler` $\rightarrow$ `Tier2TeamLeadHandler` $\rightarrow$ `Tier3SystemAdminHandler` $\rightarrow$ `Tier4ItManagerHandler`).
- **SLA Monitoring Subsystem**: Continuous SLA tracking ($0.0 \dots 1.0$ risk score) with event publishing (`SlaWarning`, `SlaViolated`) and background daemon scanning.
- **Executive & Operational Reporting Engine**: REST endpoints generating aggregated metrics on team workload, service distribution, SLA compliance %, MTTR, MTTResp, and escalation history.
- **Quality & Verification**: 165/165 tests passing (100% green) with **86.61% instruction coverage** across the application (exceeding the $\ge 70\%$ threshold).

---

## 2. What Was Built

### A. Time-Accelerated Simulation Engine (`com.iitsm.simulation`, `com.iitsm.infrastructure.config`)
- **`AdjustableClock`**:
  - Custom virtual `Clock` decorator extending `java.time.Clock`.
  - Maintains a real baseline anchor (`baseAnchor`), a virtual baseline anchor (`virtualAnchor`), and an active speed multiplier ($1\times, 60\times, 600\times$).
  - Virtual time formula:
    $$\text{VirtualNow} = \text{virtualAnchor} + (\text{RealNow} - \text{baseAnchor}) \times \text{multiplier} + \text{fastForwardOffset}$$
  - Injected as the application-wide `Clock` bean across all services, repositories, and schedulers.
  - Zero `Thread.sleep()` calls; hours of SLA degradation execute in milliseconds during testing.
- **`SimulationService`**:
  - High-level coordinator managing simulation clock state and executing 5 canonical enterprise demonstration scenarios:
    - **Scenario S1 (Happy Path Lifecycle)**:
      Employee reports search indexing failure $\rightarrow$ Technician acknowledges $\rightarrow$ Starts investigation $\rightarrow$ Fast-forward 15 min $\rightarrow$ Resolves with root cause $\rightarrow$ Reporter verifies $\rightarrow$ 5-star closure.
    - **Scenario S2 (P1 Outage & Multi-Tier Escalations)**:
      VP reports core banking database outage $\rightarrow$ Forced P1 via `RULE_CRITICAL_SERVICE_OUTAGE` $\rightarrow$ Technician investigates $\rightarrow$ Fast-forward 23 min (75% warning $\rightarrow$ Tier 1 Technician reminder) $\rightarrow$ Fast-forward 5 min (93% warning $\rightarrow$ Tier 2 Team Lead intervention) $\rightarrow$ Fast-forward 5 min (110% breach $\rightarrow$ Tier 4 IT Operations Manager escalation) $\rightarrow$ Emergency fix applied $\rightarrow$ Verified.
    - **Scenario S3 (Wrong-Domain Flagging & Triage Reassignment)**:
      Ambiguously worded query timeout ticket assigned to Network team $\rightarrow$ Technician flags wrong domain with note $\rightarrow$ Reverts to `CLASSIFIED` in Admin Triage Queue $\rightarrow$ System Administrator reclassifies to `DATABASE` and reassigns to Database team $\rightarrow$ New technician acknowledges.
    - **Scenario S4 (Duplicate Ticket Storm & Avalanche Escalation)**:
      Initial VPN gateway outage ticket submitted $\rightarrow$ 5 duplicate tickets concurrently submitted by distinct employees $\rightarrow$ TF-IDF vectorizer correlates tickets and resolves cluster root parent $\rightarrow$ Parent ticket accumulates 5 linked duplicates, triggering `RULE_DUPLICATE_AVALANCHE` priority elevation.
    - **Scenario S5 (Rejection Loop & SLA Pausing)**:
      Ticket created $\rightarrow$ Technician requests additional diagnostic info $\rightarrow$ Status moves to `PENDING_INFO` and SLA clock pauses $\rightarrow$ Fast-forward 2 hours $\rightarrow$ Employee provides info $\rightarrow$ SLA clock resumes with 2-hour deadline extension $\rightarrow$ Technician resolves $\rightarrow$ Employee rejects fix with feedback $\rightarrow$ Ticket reopens (`reopenCount = 1`) $\rightarrow$ Technician applies final fix $\rightarrow$ Verified $\rightarrow$ Closed.
- **`SimulationController`**:
  - Protected REST endpoints (`ROLE_ADMIN`):
    - `GET /api/simulation/status`: Returns current virtual time, speed multiplier, paused state, and total offset.
    - `POST /api/simulation/speed?multiplier=60`: Adjusts simulation playback speed.
    - `POST /api/simulation/advance?minutes=30`: Fast-forwards virtual clock by specified minutes.
    - `POST /api/simulation/reset`: Resets virtual clock back to host system time.
    - `POST /api/simulation/run/{scenarioId}`: Runs pre-scripted scenario (S1..S5) and returns full execution logs and ticket ID.

---

### B. Multi-Tier Escalation Engine — Chain of Responsibility (`com.iitsm.domain.chain.escalation`)
- **`EscalationHandler`**:
  - Core handler interface defining `setNext(EscalationHandler next)` and `handle(Incident incident, EscalationContext context)`.
- **`AbstractEscalationHandler`**:
  - Implements the Chain of Responsibility workflow:
    ```java
    public Optional<EscalationAction> handle(Incident incident, EscalationContext context) {
        if (canHandle(incident, context)) {
            return Optional.of(execute(incident, context));
        }
        return (next != null) ? next.handle(incident, context) : Optional.empty();
    }
    ```
- **Tiers Implemented**:
  1. **`Tier4ItManagerHandler`**: Highest priority handler in chain. Triggers on hard SLA breach of P1 incidents affecting Mission-Critical (Tier 4) IT services. Escalates directly to the IT Operations Manager (`ADMIN-MGR-01`).
  2. **`Tier3SystemAdminHandler`**: Triggers on hard SLA breach of P2 incidents, or when any incident is stagnating in triage (`CLASSIFIED` with null assignee for $> 30$ minutes). Escalates to System Administrator (`ADMIN-SYS-01`).
  3. **`Tier2TeamLeadHandler`**: Triggers when an active incident reaches $\ge 90\%$ of its resolution window, or breaches its initial response target. Alerts the owning Team Lead (`LEAD-*`).
  4. **`Tier1TechnicianHandler`**: Triggers when an active incident reaches $\ge 75\%$ of its resolution window. Issues an urgent reminder alert to the assigned technician.
- **`EscalationChainBuilder`**: Assembles the default chain: Tier 4 $\rightarrow$ Tier 3 $\rightarrow$ Tier 2 $\rightarrow$ Tier 1.
- **`EscalationService`**:
  - Evaluates incidents against the escalation chain.
  - Persists `EscalationRecord` JPA entities tracking `fromRole`, `toRole`, `toUserId`, `reason`, and `timestamp`.
  - Publishes `DomainEvent.IncidentEscalated` triggering multi-channel notifications (SMS simulator and email simulation).

---

### C. SLA Monitoring Subsystem (`com.iitsm.application.service`, `com.iitsm.domain.model`)
- **`SlaTracker.getRiskScore(Instant now)`**:
  - Computes continuous risk score $0.0 \dots 1.0$:
    $$\text{RiskScore} = \min\left(1.0, \frac{\text{effectiveElapsed}}{\text{targetDuration}}\right)$$
  - Takes paused duration during `PENDING_INFO` into account.
- **`SlaEvaluationResult`**: DTO returning incident ID, priority, status, elapsed duration, remaining duration, SLA status (`ON_TRACK`, `AT_RISK`, `WARNING`, `BREACHED`), and escalation action taken.
- **`SlaService`**:
  - Evaluates active (non-terminal) incidents against their SLA targets.
  - Automatically updates SLA status on the incident aggregate root.
  - Emits `DomainEvent.SlaWarning` at 75% (`AT_RISK`) and 90% (`WARNING`), and `DomainEvent.SlaViolated` at 100% (`BREACHED`).
  - Calls `EscalationService.evaluateAndEscalate(incident)` whenever a threshold is reached.
- **`SlaMonitoringScheduler`**:
  - Background daemon running every 30 seconds (`@Scheduled`) to proactively evaluate all active incidents and trigger escalations.

---

### D. Executive & Operational Reporting Engine (`com.iitsm.application.service`, `com.iitsm.web.controller`)
- **`ReportingService`**:
  - **Workload Report (`/api/admin/reports/workload`)**:
    - Team-by-team capacity, active tickets, load percentage, and technician-level ticket distribution.
  - **Service Analysis Report (`/api/admin/reports/services`)**:
    - Incident volume breakdown per IT service, criticality rating, and percentage distribution.
  - **SLA Performance Report (`/api/admin/reports/sla`)**:
    - Total tickets, compliant count, breached count, overall compliance percentage, and breach distribution across P1–P4 priorities.
  - **MTTR & MTTResp Report (`/api/admin/reports/mttr`)**:
    - Mean Time to Resolution (MTTR) in minutes across resolved/closed incidents.
    - Mean Time to Respond (MTTResp) in minutes from creation to first technician acknowledgment.
    - Breakdown of MTTR by category.
  - **Escalation History Report (`/api/admin/reports/escalations`)**:
    - Complete log of all multi-tier escalations across the enterprise.
- **`ReportController`**:
  - Protected endpoints requiring `ROLE_ADMIN` authority, returning typed JSON reports.

---

## 3. Design Patterns Defensibility Table

| Pattern | Classes / Interfaces | OOP Justification & Viva Talking Points |
| :--- | :--- | :--- |
| **Chain of Responsibility** | `EscalationHandler`<br>`AbstractEscalationHandler`<br>`Tier4ItManagerHandler`<br>`Tier3SystemAdminHandler`<br>`Tier2TeamLeadHandler`<br>`Tier1TechnicianHandler`<br>`EscalationChainBuilder` | Decouples the sender of an escalation request from its receivers. Allows four distinct operational tiers with independent escalation criteria (criticality, breach vs warning, triage stagnation) to process incidents in a strictly prioritized sequence. New escalation tiers (e.g. Executive Board or Vendor SLA) can be introduced without modifying existing handlers (Open/Closed Principle). |
| **Decorator** | `AdjustableClock` extending `java.time.Clock` | Transparently decorates standard Java time operations with virtual offset and speed multiplication without changing the `Clock` interface consumed by domain entities and services. Callers do not know whether time is real or simulated. |
| **Strategy** | `AssignmentStrategy`<br>`PriorityFactor`<br>`IncidentClassifier`<br>`SimilarityMeasure` | Retained and leveraged across simulation scenarios. Swappable algorithms for priority calculation, natural language classification, technician routing, and duplicate text comparison. |
| **State** | `IncidentState`<br>`IncidentStates`<br>`Incident` | Governs the 9-state lifecycle (`REPORTED`, `ASSIGNED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `PENDING_INFO`, `RESOLVED`, `VERIFIED`, `CLOSED`, `REOPENED`). In Scenario S5, cleanly transitions through `PENDING_INFO` (pausing SLA) and `REOPENED` (incrementing rework counter). |
| **Observer** | `DomainEventPublisher`<br>`NotificationObserver`<br>`AuditTrailObserver`<br>`SlaWarning`<br>`IncidentEscalated` | Decouples SLA breach and escalation events from secondary side-effects (logging audit records, dispatching email/SMS notifications to technicians, leads, and managers). |
| **Composite** | `CompositeClassifier`<br>`CompositePriorityCalculator` | Combines statistical and heuristic inputs into unified decision objects with full mathematical explainability. |

---

## 4. Test Verification & Code Quality Metrics

### A. Test Execution Summary
- **Total Tests Run**: 165
- **Failures**: 0
- **Errors**: 0
- **Skipped**: 0
- **Success Rate**: 100%

### B. JaCoCo Code Coverage
- **Total Instructions**: 23,719
- **Covered Instructions**: 20,544
- **Project-Wide Instruction Coverage**: **86.61%** (Requirement: $\ge 70\%$)
- **Domain Layer (`com.iitsm.domain.*`)**: 0 Spring Framework dependencies (100% pure Java).

### C. Test Suites Breakdown
- `AdjustableClockTest`: Speed multipliers ($\times 1, \times 60, \times 600$), discrete fast-forward jumps, clock resets.
- `EscalationChainTest`: Tier 1 (75% reminder), Tier 2 (90% lead warning), Tier 3 (P2 breach / triage stagnation), Tier 4 (P1 mission-critical breach), on-track ticket no-op.
- `SlaServiceTest`: Warning detection, breach detection, pause during `PENDING_INFO`, resume with extended deadline.
- `ReportingServiceTest`: Workload aggregation, service breakdown, SLA compliance %, MTTR/MTTResp calculations.
- `SimulationScenariosTest`: End-to-end execution of scenarios S1, S2, S3, S4, S5.
- `SimulationControllerTest`: REST API security, clock controls, scenario execution endpoints.
- `ReportControllerTest`: Admin report endpoints, JWT authentication and authority enforcement.

---

## 5. Artifacts and Diagrams Refreshed
- `docs/DECISIONS.md`: Added **ADR-008** (Chain of Responsibility for Multi-Tier Escalations and Virtual Time-Accelerated Simulation).
- `docs/uml/class-diagram.puml`: Updated with `AdjustableClock`, `EscalationHandler` hierarchy, `SlaService`, `ReportingService`, and `SimulationService`.

---

## 6. Known Gaps Reserved for Phase 6
- **Web Frontend**: Modern single-page responsive UI (React + Vite + Tailwind CSS + Lucide icons) supporting Employee, Technician, and Admin portals.
- **Simulation Dashboard UI**: Interactive playback controls (Speed slider $\times 1, \times 60, \times 600$, Fast-Forward $+15\text{m}, +1\text{h}$, Scenario launcher buttons S1–S5 with real-time log streaming).
- **Executive Reporting Dashboard**: Visual KPI charts (MTTR, SLA compliance gauge, Team Workload bar charts, Category distribution).
- **Final Project Documentation**: Comprehensive project report, API specification sheet, design pattern Viva guide, and demonstration video script.

