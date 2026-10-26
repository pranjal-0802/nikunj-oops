# I-ITSM Requirements Traceability Matrix

This document establishes bidirectional traceability between the CS F213 Course Brief, Group PRD specifications, the implementation files, test coverage, UI surfaces, and identified audit findings / remediations.

---

## 1. Course Brief & Architectural Requirements

| Requirement ID | Course Brief / Spec Requirement | Architecture / Design Pattern | Implementing File(s) | Verifying Test(s) | UI Surface | Status & Batch |
|---|---|---|---|---|---|---|
| **REQ-ARCH-01** | Pure Java Domain model free of framework/Spring/JPA dependencies | Clean / Hexagonal Architecture | `com.iitsm.domain.model.*` | `ArchitectureRegressionTest.A1_*`, `DomainEventPublisherTest` | N/A | Clean in Domain; Port adapters needed for repos (Batch 2) |
| **REQ-ARCH-02** | Layer decoupling: Web and Application must not depend directly on Infrastructure | Dependency Inversion / Ports & Adapters | `application.service.*`, `infrastructure.persistence.adapter.*` | `ArchitectureRegressionTest.A1_applicationLayerMustNotDependOnInfrastructure` | N/A | Leaks confirmed in A1 (Batch 2 fix) |
| **REQ-ARCH-03** | Domain aggregate roots protected; no leaking of JPA entities or domain entities from controllers | Encapsulation, DTO projection | `web.dto.*`, `web.controller.*` | `ArchitectureRegressionTest.A1_controllersMustNotReturnDomainOrJpaEntities`, `SecurityRegressionTest.S4_*` | Roster, Ticket tables | Leaks confirmed in S4/A1 (Batch 1 & 2 fix) |
| **REQ-ARCH-04** | State Pattern for Incident Lifecycle (New, Classified, Assigned, InProgress, WaitingOnUser, Resolved, Verified, Closed, Reopened, Cancelled) | State Pattern | `domain.state.IncidentState`, `domain.state.*State` | `IncidentStateTransitionTableTest`, `IncidentEncapsulationTest` | Status badge, action buttons | Verified, state transitions enforced |
| **REQ-ARCH-05** | Strategy Pattern for Intelligent Incident Assignment (Least Loaded, Round Robin, Skill-Weighted) | Strategy Pattern | `domain.strategy.assignment.*` | `AssignmentStrategyTest`, `IncidentLifecycleEndToEndTest` | Admin Strategy Toggle | Working; race condition fix in Batch 2 (F7) |
| **REQ-ARCH-06** | Strategy Pattern for Multi-Factor Priority Calculation (6 factors + Override rules) | Composite Strategy Pattern | `domain.strategy.priority.*`, `CompositePriorityCalculator` | `CompositePriorityCalculatorTest`, `CorrectnessRegressionTest.F3_*`, `F6_*` | Priority score breakdown card | Calculation working; avalanche and history window fixes in Batch 2 (F3, F6) |
| **REQ-ARCH-07** | Chain of Responsibility Pattern for Automated Escalation (Tier 1 Tech -> Tier 2 Team Lead -> Tier 3 Admin) | Chain of Responsibility | `domain.chain.escalation.*` | `EscalationChainTest`, `SimulationScenariosTest` | Escalation status banner | Verified passing in unit & scenario |
| **REQ-ARCH-08** | Observer Pattern for Decoupled Domain Events (Audit Trail, Real-Time Notifications) | Observer Pattern | `domain.event.*`, `infrastructure.observer.*` | `DomainEventPublisherTest`, `AuditTrailObserverTest` | Audit timeline view | Working; audit port extraction in Batch 2 |
| **REQ-ARCH-09** | Factory Pattern for aggregate creation (Incidents, Users) | Factory Method Pattern | `application.service.IncidentFactory`, `application.service.UserFactory` | `IncidentLifecycleEndToEndTest`, `CompanySeederIntegrationTest` | Create Ticket Modal | Verified passing |
| **REQ-ARCH-10** | Controllable Simulated Time (`AdjustableClock`) for deterministic SLA breaches and audit replay | Clock abstraction | `infrastructure.config.AdjustableClock` | `AdjustableClockTest`, `SimulationScenariosTest` | Demo Clock Header & Warp Controls | Verified passing |

---

## 2. Security & Access Control Requirements

| Requirement ID | Security Requirement | Design Pattern / Mechanism | Implementing File(s) | Verifying Test(s) | UI Surface | Status & Batch |
|---|---|---|---|---|---|---|
| **REQ-SEC-01** | Strong, non-committed JWT signing secret with fail-fast validation in production | Validation / Secret Management | `application.yml`, `JwtTokenProvider` | `SecurityRegressionTest.S1_jwtSecretValidationFailsOnWeakOrCommittedSecret` | N/A | Finding S1 (Batch 1 fix) |
| **REQ-SEC-02** | Account lockout after 5 consecutive bad login attempts (survives exceptions) | Rate Limiting / Brute-force protection | `UserService`, `LoginAttemptService` | `SecurityRegressionTest.S2_lockoutPersistsAfterFiveBadLogins` | Login form error banner | Finding S2 (Batch 1 fix) |
| **REQ-SEC-03** | Privileged designation assignment protection; self-registration defaults to lowest designation | Privilege separation | `AuthController.register`, `UserFactory` | `SecurityRegressionTest.S3_registrationIgnoresClientSuppliedDesignation` | Registration Form | Finding S3 (Batch 1 fix) |
| **REQ-SEC-04** | No leaking of BCrypt password hashes in any administrative or roster API | Sensitive Data Exposure Prevention | `AdminController`, `TechnicianSummaryDto` | `SecurityRegressionTest.S4_adminEndpointsDoNotLeakPasswordHash` | Admin Technicians Table | Finding S4 (Batch 1 fix) |
| **REQ-SEC-05** | Demo credentials hidden in production; controlled via `/api/public/config` | Environmental Configuration | `PublicConfigController`, `index.html` | `SecurityRegressionTest.S5_publicConfigControlsDemoModeAndHidesDemoCredentials` | Quick Persona Switcher | Finding S5 (Batch 1 & 4 fix) |
| **REQ-SEC-06** | Strict role-based authorization matrix; technicians inspect only assigned/team tickets; employees never resolve | Strategy / Access Policy | `domain.policy.AccessPolicy` | `SecurityRegressionTest.S6_accessPolicyRestrictsCrossRoleActionsAndUnassignedInspection` | Action toolbars per persona | Finding S6 (Batch 2 fix) |
| **REQ-SEC-07** | Deactivated or locked accounts rejected immediately (401) on valid JWT presentation | Filter Enforcement | `JwtAuthenticationFilter` | `SecurityRegressionTest.S7_jwtAuthenticationFilterRejectsLockedOrDeactivatedUser` | Auth Interceptor | Finding S7 (Batch 2 fix) |
| **REQ-SEC-08** | Essential HTTP security headers (CSP, nosniff, frame-ancestors, CORS allowlist) | Defense in Depth | `SecurityConfig` | `SecurityRegressionTest.S8_securityHeadersArePresentOnHttpResponses` | Global Web Response | Finding S8 (Batch 2 fix) |

---

## 3. Persona Functional Workflows

| Requirement ID | Workflow / Feature Description | User Persona | Implementing File(s) | Verifying Test(s) | UI Surface | Status & Batch |
|---|---|---|---|---|---|---|
| **REQ-WF-01** | Submit incident with title, description, and auto-derived employee department | Employee | `IncidentService.createIncident`, `EmployeeController` | `IncidentLifecycleEndToEndTest`, `EmployeePrivacyControllerTest` | Employee "Report Incident" Modal | Working |
| **REQ-WF-02** | Employee sees ONLY own reported tickets and never sees internal technical priority or SLA timer | Employee | `IncidentDtoMapper`, `EmployeeIncidentView` | `EmployeePrivacyControllerTest`, `IncidentLifecycleEndToEndTest` | Employee Ticket List & Details | Verified; employee view omits P1/SLA |
| **REQ-WF-03** | Provide additional info when technician requests clarification | Employee | `IncidentService.provideInfo`, `EmployeeController` | `IncidentLifecycleEndToEndTest` | "Provide Info" Dialog | Working |
| **REQ-WF-04** | Verify resolution or reject and reopen ticket upon resolution failure | Employee | `IncidentService.verify`, `IncidentService.reject` | `IncidentLifecycleEndToEndTest`, `IncidentStateTransitionTableTest` | "Verify & Close" / "Reopen" Buttons | Working |
| **REQ-WF-05** | Submit 1–5 star rating and feedback upon incident resolution | Employee | `IncidentService.submitFeedback`, `Feedback` | `IncidentLifecycleEndToEndTest` | Feedback Modal | Working |
| **REQ-WF-06** | Technician inspects assigned queue and acknowledges ticket | Technician | `IncidentService.acknowledge`, `TechnicianController` | `TechnicianControllerTest`, `IncidentLifecycleEndToEndTest` | Technician Assigned Queue | Working |
| **REQ-WF-07** | Start investigation, add investigation notes, and record root cause hypotheses | Technician | `IncidentService.startInvestigation`, `IncidentService.addInvestigationNote` | `TechnicianControllerTest`, `IncidentLifecycleEndToEndTest` | Investigation Notes Panel | Working |
| **REQ-WF-08** | Resolve ticket with structured resolution (Root Cause, Fix Actions, Preventative Recommendations) | Technician | `IncidentService.resolve`, `Resolution` | `TechnicianControllerTest`, `IncidentLifecycleEndToEndTest` | "Resolve Incident" Modal | Working |
| **REQ-WF-09** | Flag wrong domain ticket to re-route to admin triage queue | Technician | `IncidentService.flagWrongDomain` | `TechnicianControllerTest` | "Flag Wrong Domain" Button | Working |
| **REQ-WF-10** | Request reassignment to another technician or support team | Technician | `IncidentService.requestReassignment` | `TechnicianControllerTest` | "Request Reassignment" Dialog | Working |
| **REQ-WF-11** | Team Lead reviews team queue, workload distribution, and approves/rejects reassignments | Team Lead | `IncidentService.approveReassignment`, `AdminController` | `AdminControllerTest` | Team Lead Dashboard | Working |
| **REQ-WF-12** | Admin manages triage queue for low-confidence classifications and wrong-domain incidents | Administrator | `IncidentService.reclassify`, `AdminController` | `CorrectnessRegressionTest.F5_*`, `AdminControllerTest` | Admin Triage Queue Table | Finding F5 (Batch 2 fix) |
| **REQ-WF-13** | Multi-criteria search (>= 5 criteria: status, priority, category, service, text, reporter, assignee, date range) | Administrator | `SearchService`, `IncidentSpecifications` | `SearchServiceTest`, `IncidentSpecificationsTest` | Advanced Search Bar & Filters | Working; SQL pagination in Batch 2 |
| **REQ-WF-14** | Reporting and analytics: SLA compliance, MTTR, ticket volume by category/service, and technician stage breakdown | Administrator | `ReportingService`, `ReportController` | `ReportingServiceTest`, `ReportControllerTest` | Analytics & Reports Dashboard | Working; aggregate queries in Batch 2 |

---

## 4. Intelligent ITSM Features

| Requirement ID | Intelligent Capability | Design / Algorithm | Implementing File(s) | Verifying Test(s) | UI Surface | Status & Batch |
|---|---|---|---|---|---|---|
| **REQ-INT-01** | Automated NLP classification of title and description into IT category with confidence score | Multinomial Naive Bayes + TF-IDF Vectorizer | `NaiveBayesClassifier`, `TfIdfVectorizer`, `TextNormalizer` | `NaiveBayesClassifierTest`, `TfIdfVectorizerTest`, `TextNormalizerTest` | Category prediction badge + confidence bar | Working |
| **REQ-INT-02** | Automated IT Service identification and matching | Keyword Token Overlap / ServiceMatcher | `ServiceMatcher` | `CorrectnessRegressionTest.F2_*` | Matched Service Tag | Finding F2 fallback fix (Batch 1) |
| **REQ-INT-03** | Automated Duplicate Detection and Correlation (Jaccard similarity on 3-shingles within recency window) | Jaccard Shingling + Cluster Correlation | `DuplicateDetectionService`, `DuplicateDetectionTest` | `DuplicateDetectionTest`, `CorrectnessRegressionTest.F3_*`, `F4_*` | Duplicate Warning Banner & Linked Cluster Pills | Finding F3/F4 fixes (Batch 2) |
| **REQ-INT-04** | SLA Breach Risk Early Warning System | SLA Tracker & Linear Breach Predictor | `SlaTracker`, `IncidentService.checkSlaBreaches` | `SlaTrackerTest`, `SimulationScenariosTest` | SLA Risk indicator (Safe/AtRisk/Breached) | Working |
| **REQ-INT-05** | Automated Escalation Chain on SLA Breach | Chain of Responsibility | `EscalationChain`, `Tier1TechnicianHandler`, `Tier2TeamLeadHandler`, `Tier3AdminHandler` | `EscalationChainTest`, `SimulationScenariosTest` | Escalation alert toast and ticket badge | Working |
| **REQ-INT-06** | Historical Solution Recommender for recurring incidents | Vector Space Similarity over Resolved Tickets | `ResolutionRecommender` | `AdminIntelligenceEndpointsTest` | Suggested Resolution Accordion | Working |
| **REQ-INT-07** | Dual Notification Channels: Real-time SSE in-app and SMTP email delivery | In-App Observer + JavaMailSender | `NotificationSystem`, `EmailChannel` | `NotificationSystemTest`, `CorrectnessRegressionTest.F8_*` | Bell notification menu & Mailpit inbox | Finding F8 real SMTP fix (Batch 2) |

---

## 5. Course Simulation Scenarios (S1–S5)

| Scenario ID | Test Scenario Description | Key Assertions | Implementing Test | Verification Result |
|---|---|---|---|---|
| **S1** | Normal Flow: P3 Laptop Display Issue -> Assigned -> Resolved -> Verified -> 5-star Feedback | Full lifecycle completion, SLA met, feedback recorded | `SimulationScenariosTest.scenario1_normalFlow_laptopDisplay` | **PASSING** |
| **S2** | SLA Breach & Escalation: P1 Core Banking Gateway Timeout -> Unacknowledged -> Tier 1 Breach -> Tier 2 Escalation | SLA tracker marks breach, Tier 2 escalation record created, admin alerted | `SimulationScenariosTest.scenario2_slaBreachAndEscalation_coreBankingGateway` | **PASSING** |
| **S3** | Reopen Flow: VPN Connectivity Issue -> Resolved -> Employee Verification Rejection -> Reopened -> Investigated -> Re-resolved | Reopen count incremented, state transitions to REOPENED, resolution versioning | `SimulationScenariosTest.scenario3_reopenFlow_vpnConnectivity` | **PASSING** |
| **S4** | Duplicate Storm: ATM Switch Crash -> 5 duplicate tickets filed -> Correlation links to root incident | Linked incident count >= 5, cluster tracking (Avalanche priority elevation to P1 enforced in Batch 2) | `SimulationScenariosTest.scenario4_duplicateStorm_atmSwitchCrash` | **PASSING** (Avalanche assertion added in Batch 2) |
| **S5** | Wrong Domain Re-Routing: Payroll Direct Deposit Error -> Tech flags wrong domain -> Admin reclassifies and reassigns | State transition to WRONG_DOMAIN, triage queue entry, reclassification event | `SimulationScenariosTest.scenario5_wrongDomainRerouting_payrollDirectDeposit` | **PASSING** |

---

## 6. Audit & Review Findings Cross-Reference

| Review Finding ID | Summary of Issue | Severity | Target Batch | Test Verifying Reproduction & Fix |
|---|---|---|---|---|
| **S1** | Hardcoded JWT secret in `application.yml`; prod context starts with default secret | High | Batch 1 | `SecurityRegressionTest.S1_jwtSecretValidationFailsOnWeakOrCommittedSecret` |
| **S2** | `@Transactional` rollback discards `failedLoginAttempts`, preventing account lockout | High | Batch 1 | `SecurityRegressionTest.S2_lockoutPersistsAfterFiveBadLogins` |
| **S3** | Self-registration endpoint trusts client-supplied `designation`, granting executive priority | High | Batch 1 | `SecurityRegressionTest.S3_registrationIgnoresClientSuppliedDesignation` |
| **S4** | Admin technician roster API returns domain model directly, leaking BCrypt password hashes | High | Batch 1 | `SecurityRegressionTest.S4_adminEndpointsDoNotLeakPasswordHash` |
| **S5** | Hardcoded demo credentials in client JS; no public config endpoint controlling demo mode | Med | Batch 1 | `SecurityRegressionTest.S5_publicConfigControlsDemoModeAndHidesDemoCredentials` |
| **F1** | Ticket creation throws `TechnicianOverloadedException` when all technicians full instead of staying unassigned | High | Batch 1 | `CorrectnessRegressionTest.F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned` |
| **F2** | `ServiceMatcher.findBestMatch` returns arbitrary first service on zero keyword overlap | Med | Batch 1 | `CorrectnessRegressionTest.F2_unmatchedServiceDoesNotPickArbitraryService` |
| **F9** | Docker compose configuration with PostgreSQL and Mailpit | Med | Batch 1 | `scripts/smoke.sh`, Docker Compose integration |
| **A1** | Layering violations: application & web directly import infrastructure and JPA repositories | High | Batch 2 | `ArchitectureRegressionTest.A1_*` (4 ArchUnit rules) |
| **S6 / A2** | `AccessPolicy` allows unassigned technician inspection and permits employees to execute technician actions | High | Batch 2 | `SecurityRegressionTest.S6_accessPolicyRestrictsCrossRoleActionsAndUnassignedInspection` |
| **S7** | `JwtAuthenticationFilter` accepts tokens for deactivated/locked users until expiration | High | Batch 2 | `SecurityRegressionTest.S7_jwtAuthenticationFilterRejectsLockedOrDeactivatedUser` |
| **S8** | Security headers (CSP, nosniff, frame-ancestors, CORS allowlist) missing | Med | Batch 2 | `SecurityRegressionTest.S8_securityHeadersArePresentOnHttpResponses` |
| **F3** | Linked duplicate tickets do not recalculate parent priority; avalanche rule never triggered | High | Batch 2 | `CorrectnessRegressionTest.F3_linkingFiveDuplicatesElevatesParentPriorityToP1ViaAvalanche` |
| **F4** | Duplicate incidents consume technician capacity and are assigned independently | High | Batch 2 | `CorrectnessRegressionTest.F4_duplicateIncidentDoesNotConsumeTechnicianCapacity` |
| **F5** | Classification confidence is computed but ignored; low-confidence incidents are assigned immediately | Med | Batch 2 | `CorrectnessRegressionTest.F5_lowConfidenceClassificationLeavesIncidentInTriageQueue` |
| **F6** | Chronic reporter history counts all-time tickets across all services instead of recent window on same service | Med | Batch 2 | `CorrectnessRegressionTest.F6_reporterHistoryCountsOnlyRecentUnresolvedTicketsOnService` |
| **F7** | Race condition: concurrent ticket creation overloads technician due to lack of `@Version` locking | High | Batch 2 | `CorrectnessRegressionTest.F7_concurrentTicketCreationDoesNotOverloadTechnician` |
| **A4** | Optimistic locking failures throw generic `PersistenceFailureException` instead of `ConcurrentModificationException` | Med | Batch 2 | `CorrectnessRegressionTest.A4_concurrentAcknowledgmentThrowsConcurrentModificationException` |
| **F8** | `EmailChannel` only simulates notifications without connecting to real SMTP server | Med | Batch 2 | `CorrectnessRegressionTest.F8_emailChannelSendsRealSmtpMessage` |
| **A3** | Dashboard and reporting rely on in-memory `findAll()` instead of SQL aggregations and indexing | Med | Batch 2 | Performance test (5,000 incidents benchmark) |
