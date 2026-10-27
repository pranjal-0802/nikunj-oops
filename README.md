# Intelligent IT Service Management (I-ITSM)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Tests](https://img.shields.io/badge/Tests-205%20Passed%20(100%25)-success.svg)]()
[![JaCoCo Coverage](https://img.shields.io/badge/Line%20Coverage-87.4%25-brightgreen.svg)]()
[![Pure Java Domain](https://img.shields.io/badge/Domain%20Core-Zero%20Spring%20Imports-purple.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20Ports%20%26%20Adapters-informational.svg)]()
[![UI/UX](https://img.shields.io/badge/UI%2FUX-Light%20Mode%20Minimalist-orange.svg)]()

> A production-grade, enterprise IT Service Management platform built for **CS F213 (Object-Oriented Programming)**. Engineered around a 100% pure-Java domain core, 8 object-oriented design patterns, an explainable pure-Java NLP & multi-factor priority scoring engine, a multi-tier Chain of Responsibility escalation hierarchy, an accelerated virtual simulation engine (`AdjustableClock`), and a role-isolated single-page enterprise web console.

---

## 1. Key System Architecture & OOP Highlights

1. **Pure Java Hexagonal Domain Core**:
   - `com.iitsm.domain.*` has **zero imports** from Spring Framework (`org.springframework.*`), Jakarta (`jakarta.*`), or persistence libraries.
   - Enforced continuously via ArchUnit architectural test rules in `ArchitectureRegressionTest`.
   - Domain aggregate roots enforce behavioral encapsulation (no public setters; rich behavior methods; Tell-Don't-Ask).
2. **8 Viva-Defensible Object-Oriented Design Patterns**:
   - **State Pattern**: 9-state finite state machine with zero-allocation `EnumMap` flyweight singletons (`NewState`, `ClassifiedState`, `AssignedState`, `AcknowledgedState`, `InProgressState`, `PendingInfoState`, `ResolvedState`, `VerifiedState`, `ClosedState`).
   - **Strategy Pattern**: Dynamic technician routing (5 hot-swappable strategies: `RoundRobinAssignmentStrategy`, `WorkloadAwareAssignmentStrategy`, `SkillMatchingAssignmentStrategy`, `CompositeAssignmentStrategy`, `AvailabilityAwareAssignmentStrategy`), 6-factor priority scoring, NLP classifiers, and text similarity measures.
   - **Chain of Responsibility**: 4-tier progressive escalation hierarchy (Technician $\rightarrow$ Team Lead $\rightarrow$ System Admin $\rightarrow$ IT Operations Manager).
   - **Decorator Pattern**: `AdjustableClock` decorating `java.time.Clock` with speed multipliers ($\times 1, \times 60, \times 600$) and discrete fast-forwards (+15m, +1h, +24h).
   - **Observer Pattern**: Sealed `DomainEvent` spine decoupling transactional audit logging and multi-channel notifications.
   - **Composite Pattern**: `CompositeClassifier` and `CompositePriorityCalculator` pairing statistical Naive Bayes with rule-based heuristics.
   - **Factory Pattern**: `UserFactory` and `IncidentFactory` ensuring invariant enforcement and polymorphic creation.
   - **Repository Pattern (Ports & Adapters)**: Pure domain repository interfaces decoupled from Spring Data JPA adapters.
3. **Pure-Java Algorithmic Intelligence (Zero External Heavy ML)**:
   - Built-in `TextNormalizer`, `TfIdfVectorizer`, `KeywordClassifier`, and `NaiveBayesClassifier` trained on a 630-sample ITIL corpus (`corpus.csv`), achieving **$86.1\%$ accuracy** on unseen enterprise test incidents.
   - Sliding-window Cosine TF-IDF similarity correlation linking duplicate storm incidents to root cluster parents.
4. **Time-Accelerated Simulation Engine (`AdjustableClock`)**:
   - Zero `Thread.sleep()` calls; runs hours of SLA degradation and multi-tier escalations in milliseconds.
   - Pre-scripted canonical scenarios (S1–S5) demonstrating the full lifecycle, P1 outages, wrong-domain triage loops, duplicate storms, and clock pauses.
5. **Strict Role-Isolated Enterprise UI/UX**:
   - Persona isolation: Employees see only their self-service portal; technicians see only their assigned workload; administrators see command centre and unified triage.
   - Priority ranks (P1–P4), SLA targets/deadlines, and internal technician investigation notes are strictly hidden from employees at the DTO type level (`EmployeeIncidentView`).
6. **Embedded Single-Page Web Console**:
   - Modern, responsive SPA served directly by Spring Boot at `http://localhost:8085/` with zero Node.js/npm runtime prerequisites and full offline compatibility (zero external CDN scripts, compliant with strict CSP `script-src 'self'`).

---

## 2. Simulated Enterprise: Nimbus FinServe Inc.

### 2.1 Corporate Purpose & Infrastructure Overview
**Nimbus FinServe Inc.** is an institutional financial technology and digital banking institution operating high-frequency algorithmic trade execution, core banking settlement ledgers, real-time risk modeling, and institutional wealth management portals.

In financial services, minute-by-minute system downtime translates directly to financial transaction losses, liquidity risks, regulatory fines (SEC, FINRA, RBI), and institutional reputation damage. Therefore, **incident priority is mathematically calculated based on the business criticality of the affected IT infrastructure**, combined with reporter designation hierarchy, service outage heuristics, and duplicate storm clustering.

### 2.2 Corporate Hierarchy & Specialist Teams
```
                            Victoria Sterling
                        (Chief Executive Officer)
                                   │
                            Alexander Vance
                    (Chief IT Infrastructure Admin)
                                   │
      ┌───────────────────┬────────┴──────────┬───────────────────┐
      │                   │                   │                   │
Network Operations  Core Banking DB    Cybersecurity      Workplace & Floor
   (TEAM-NET)          (TEAM-DB)          (TEAM-SEC)          (TEAM-DESK)
      │                   │                   │                   │
Lead: Sarah Jenkins  Michael Chang      Hannah Fischer      Amanda Brooks
Tech: Elena Rostova  Priya Patel        Carlos Mendez       Jordan Bell
      (VPN Specialist) (Lead DBA)       (SecOps Engineer)   (Floor Specialist)
Tech: David Kim
      (Firewalls/BGP)
```

| Persona & Name | Corporate Role & Domain Specialization | Pre-seeded Email |
| :--- | :--- | :--- |
| **Victoria Sterling** | Chief Executive Officer (VP / C-Level Executive) | `ceo@nimbusfinserve.com` |
| **Vikram Malhotra** | Senior Quantitative Trading Analyst (Staff Reporter) | `employee@nimbusfinserve.com` |
| **Elena Rostova** | Senior Network Specialist (**VPN Gateways, WireGuard, DNS**) | `tech.net2@nimbusfinserve.com` |
| **David Kim** | Senior Network Specialist (**Cisco BGP Routing, Perimeter Firewalls**) | `tech.network@nimbusfinserve.com` |
| **Priya Patel** | Lead Database Administrator (**PostgreSQL Tuning, Oracle RAC Clusters**) | `tech.db1@nimbusfinserve.com` |
| **Carlos Mendez** | Cybersecurity Engineer (**Active Directory, SAML SSO, MFA**) | `tech.sec1@nimbusfinserve.com` |
| **Sarah Jenkins** | Network Support Team Lead (Escalation oversight & team queue) | `lead.network@nimbusfinserve.com` |
| **Alexander Vance** | Chief IT Infrastructure Administrator (Unified triage & policy controls) | `admin@nimbusfinserve.com` |

*Default password for all seeded accounts:* `Password123!`

### 2.3 IT Service Catalog & Infrastructure Criticality Tiers

Ticket priority (P1–P4) is derived dynamically from the IT service's criticality tier, reporter designation weight, and historical failure frequency:

| Criticality Tier | Impact Level | IT Service & Key Description | Default Priority & SLA Target | Financial & Infrastructure Impact Rationale |
| :---: | :---: | :--- | :---: | :--- |
| **Tier 4** | **Mission Critical** | `SVC-PAYMENT` Payment Gateway<br>`SVC-CORE-DB` Core Banking DB<br>`SVC-SSO` SSO & Identity Provider | **P1 (Critical)**<br>• Response: 15 min<br>• Resolve: 1 hour | **Direct Financial Outage**: Halts credit card settlements and ledger transactions. Revenue loss: up to \$200,000/hr. Triggers instant SMS + Email escalation. |
| **Tier 3** | **Business Operational** | `SVC-VPN` Corporate VPN Gateways<br>`SVC-CRM` Customer Support CRM<br>`SVC-EMAIL` Corporate Email & Calendar<br>`SVC-DWH` Data Warehouse Analytics | **P2 (High)**<br>• Response: 30 min<br>• Resolve: 4 hours | **Business Impairment**: Blocks remote trading desks and intraday risk/compliance pipelines. |
| **Tier 2** | **Departmental** | `SVC-WIFI` Office HQ Wi-Fi<br>`SVC-CICD` CI/CD Engineering Pipeline<br>`SVC-HRMS` HR & Payroll Portal | **P3 (Moderate)**<br>• Response: 1 hour<br>• Resolve: 8 hours | **Operational Degradation**: Internal productivity degradation with manual operational workarounds. |
| **Tier 1** | **Internal Support** | `SVC-PRINT` Office Printing<br>`SVC-HARDWARE` Laptop & Peripherals<br>`SVC-WIKI` Intranet Confluence Wiki<br>`SRV-GEN-001` General IT / Helpdesk | **P4 (Minor)**<br>• Response: 4 hours<br>• Resolve: 24 hours | **Routine Requests**: Localized peripheral issues without enterprise infrastructure risk. |

---

## 3. Minimalist Light-Mode UI/UX Design System

The web console interface is designed in a minimalist, professional **Light Mode** inspired by enterprise fintech dashboards, using a 3-color functional accent palette:

| Functional Color | Palette Hex | Application in Interface |
|---|---|---|
| **Primary Sky Blue** | `#0284C7` / `#72C3E8` | Navbar brand icon, primary buttons, active navigation tabs, workload capacity progress bars, specialist badges, focus outlines. |
| **Warning Amber Yellow** | `#F4AF34` / `#F59E0B` | Human-in-the-loop triage queue banners, pending info notices, P2/P3 priority indicators, CEO persona pill. |
| **Alert Crimson Red** | `#EA3F26` / `#DC2626` | P1 critical outage badges, SLA deadline breaches, destructive actions, error banners, Administrator persona pill. |

- **Light Canvas & Surfaces**: Clean `#F8FAFC` base with pure white (`#FFFFFF`) card surfaces and subtle borders (`#E2E8F0`).
- **High-Contrast Typography**: Slate 900 (`#0F172A`) for high legibility, Slate 600 (`#475569`) for metadata, and monospace for IDs and timestamps.
- **Zero External Dependencies**: Zero CDN links (`fonts.googleapis.com`, `cdnjs`, etc.). Fully offline-operable and compliant with strict Content Security Policy (`default-src 'self'`).

---

## 4. Evaluation Demonstration Protocol (Role-Isolated Experience)

To demonstrate the real-world operational separation of duties:

1. **Step 1: Employee Incident Intake**
   - Open [http://localhost:8085/](http://localhost:8085/) and sign in as **Vikram Malhotra** (`employee@nimbusfinserve.com`) or **Victoria Sterling** (`ceo@nimbusfinserve.com`).
   - Notice that **only** the Employee Self-Service Portal is visible in the top navigation.
   - Click template **"🌐 VPN Outage &rarr; Elena Rostova"** (or submit: *"VPN tunnel drops during institutional block trade"*).
   - The NLP classifier identifies `NETWORK` category and routes to domain specialist **Elena Rostova** (VPN Gateways).
   - An on-screen prompt displays the assigned domain specialist and provides a one-click button: **[Sign Out & Switch to Elena Rostova]**.
2. **Step 2: Specialist Technical Handling**
   - Sign in as **Elena Rostova** (`tech.net2@nimbusfinserve.com`).
   - Notice that Elena sees **only** the Technician Console with her personal workload queue and capacity meter.
   - Click **Acknowledge** (state moves to `ACKNOWLEDGED`, recording response SLA) &rarr; Click **Start Investigation** (moves to `IN_PROGRESS`) &rarr; Click **Add Note** (internal diagnostics) &rarr; Click **Resolve** and submit the structured resolution (*Root cause: Stale route table; Steps: Flushed routing daemon*).
   - A walkthrough prompt confirms: *"Incident resolved! Next step: Sign out & sign in as Employee to verify."*
3. **Step 3: Verification & Formal Closure**
   - Sign out and sign back in as **Vikram Malhotra** (`employee@nimbusfinserve.com`).
   - The ticket stepper shows **RESOLVED**.
   - Click **Verify & Close Ticket**, provide a 5-star rating, and the ticket formally transitions to `CLOSED`.

---

## 5. Email & Multi-Channel Notification System

I-ITSM implements the Observer pattern via `DomainEvent` listeners. Outgoing notifications are dispatched across `EmailChannel`, `InAppChannel`, and `SmsSimulatorChannel`.

### Option A: Send Notifications to a REAL Personal/Work Email Address (Recommended)
Since `@nimbusfinserve.com` is a simulated corporate domain, public SMTP servers (like Gmail) would bounce emails sent to fake domains. The platform provides a built-in `REAL_EMAIL_OVERRIDE` feature: every notification generated by the platform is safely delivered to your personal mailbox with the target simulated recipient indicated in the subject line!

To route all platform emails to your real inbox using Gmail SMTP:
1. In your Google Account, generate an **App Password** (Security &rarr; 2-Step Verification &rarr; App passwords).
2. Set the environment variables in PowerShell and launch the app:
```powershell
$env:SPRING_MAIL_HOST = "smtp.gmail.com"
$env:SPRING_MAIL_PORT = "587"
$env:SPRING_MAIL_USERNAME = "your-email@gmail.com"
$env:SPRING_MAIL_PASSWORD = "your-16-char-app-password"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = "true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "true"
$env:REAL_EMAIL_OVERRIDE = "your-email@gmail.com"

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\mvnw.cmd spring-boot:run
```
Whenever an incident is logged, assigned, acknowledged, escalated, or resolved, you will receive a real email in your Gmail inbox!

### Option B: Local Mailpit (Docker or Standalone Binary)
If Docker is installed:
```powershell
docker compose up mailpit -d
```
Open Mailpit Web UI at [http://localhost:8025/](http://localhost:8025/) to view all outgoing emails sent to `*@nimbusfinserve.com` with zero real email credentials required.

### Option C: 100% Offline (In-App Notification Bell & SMS Simulator)
If no mail server is running, the application **never fails or crashes**: the `NotificationRouter` isolates the channel failure with a warning, while simultaneously delivering notifications to:
1. The **In-App Notification Bell** in the top navbar (click the bell to view live real-time notifications).
2. The **SMS Simulator** logged to the application console (`[SMS-SIMULATOR] Sent SMS to EMP-0001: ...`).

---

## 6. Quick Start & Local Execution

### Prerequisites
- **JDK 21** (Eclipse Temurin or Microsoft OpenJDK 21)
- **Maven 3.9+** (or use included `./mvnw`)

### One-Command Launch (In-Memory Development Mode)
```powershell
# Windows PowerShell:
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\mvnw.cmd spring-boot:run
```
*(On Linux / macOS)*:
```bash
./mvnw spring-boot:run
```

Once started:
- 🌐 **Web Application**: Open [http://localhost:8085/](http://localhost:8085/)
- 📖 **Interactive Swagger UI**: [http://localhost:8085/swagger-ui.html](http://localhost:8085/swagger-ui.html)
- 📊 **Actuator Health**: [http://localhost:8085/actuator/health](http://localhost:8085/actuator/health)

### Running with Docker Compose (PostgreSQL 16 + Mailpit)
```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```
- Web Application: `http://localhost:8085/`
- PostgreSQL: `localhost:5432` (database: `iitsm_prod`)
- Mailpit Web Mailbox: [http://localhost:8025](http://localhost:8025)

---

## 7. Automated Verification & Testing Suite

To execute the entire test suite and generate the JaCoCo coverage report:
```powershell
# Windows PowerShell:
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\mvnw.cmd clean verify
```
*(On Linux / macOS)*:
```bash
./mvnw clean verify
```

### Verified Test Results
- **Total Tests Run**: **205**
- **Failures**: **0**
- **Errors**: **0**
- **Skipped**: **0**
- **Line Coverage**: **87.4%** (`target/site/jacoco/index.html`)
- **Architectural Rules**: Enforced via ArchUnit in `ArchitectureRegressionTest` to guarantee domain purity.

---

## 8. Canonical Simulation Scenarios (S1–S5)

The platform provides 5 automated scenarios that can be executed directly from the Web Console (Simulation Engine tab) or via REST API:

```bash
# 1. Obtain Admin JWT Token
TOKEN=$(curl -s -X POST http://localhost:8085/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@nimbusfinserve.com","password":"Password123!"}' | jq -r .token)

# 2. Run Any Scenario (S1..S5)
curl -X POST http://localhost:8085/api/simulation/run/S1 -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8085/api/simulation/run/S2 -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8085/api/simulation/run/S3 -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8085/api/simulation/run/S4 -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8085/api/simulation/run/S5 -H "Authorization: Bearer $TOKEN"
```

| Scenario ID | Scenario Name | Operational Flow Demonstrated |
| :--- | :--- | :--- |
| **S1** | **Happy Path Lifecycle** | Report ticket $\rightarrow$ Technician acknowledges $\rightarrow$ Investigates $\rightarrow$ Fast-forward 15 min $\rightarrow$ Resolves with root cause $\rightarrow$ Reporter verifies $\rightarrow$ 5-star closure. |
| **S2** | **P1 Outage & Multi-Tier Escalations** | VP reports database deadlock $\rightarrow$ Forced P1 via `RULE_CRITICAL_SERVICE_OUTAGE` $\rightarrow$ Fast-forward 23 min (75% warning $\rightarrow$ Tier 1 reminder) $\rightarrow$ 28 min (93% warning $\rightarrow$ Tier 2 Team Lead intervention) $\rightarrow$ 33 min (110% breach $\rightarrow$ Tier 4 IT Manager alert). |
| **S3** | **Wrong-Domain Flagging & Triage** | Ambiguous query timeout ticket assigned to Network $\rightarrow$ Tech flags out-of-scope $\rightarrow$ Reverts to `CLASSIFIED` in Triage Queue $\rightarrow$ Admin reclassifies to `DATABASE` and reassigns $\rightarrow$ Database tech acknowledges. |
| **S4** | **Duplicate Storm & Avalanche** | Primary VPN ticket created $\rightarrow$ 5 duplicate VPN tickets concurrently filed by different employees $\rightarrow$ TF-IDF Cosine Similarity links all duplicates to cluster root parent $\rightarrow$ `RULE_DUPLICATE_AVALANCHE` elevates priority. |
| **S5** | **Rejection Loop & SLA Clock Pausing** | Technician requests info $\rightarrow$ Status moves to `PENDING_INFO` and SLA clock pauses $\rightarrow$ Fast-forward 2 hours $\rightarrow$ User provides info $\rightarrow$ SLA clock resumes with 2h extension $\rightarrow$ Tech resolves $\rightarrow$ User rejects fix $\rightarrow$ Reopen count increments to 1 for rework cycle. |

---

## 9. Course Documentation Deliverables

All course documentation deliverables are cataloged under the `docs/` directory:

| Document | File Path | Contents |
| :--- | :--- | :--- |
| **Problem Analysis** | [`docs/01-problem-analysis.md`](docs/01-problem-analysis.md) | Industry context, operational inefficiencies, problem statement, stakeholder analysis, KPIs. |
| **Requirements Specification** | [`docs/02-requirements.md`](docs/02-requirements.md) | Functional (FR-1..6) and Non-Functional (NFR-1..6) specifications, Requirements Traceability Matrix. |
| **Architecture Specification**| [`docs/03-architecture.md`](docs/03-architecture.md) | Hexagonal Ports & Adapters, package structure, event-driven spine, concurrency & locking model. |
| **Technology Report** | [`docs/04-technology-report.md`](docs/04-technology-report.md) | Technology choices, pure Java NLP benchmark vs external ML, virtual clock vs real sleep. |
| **Design Patterns Catalog** | [`docs/05-design-patterns.md`](docs/05-design-patterns.md) | Viva defense guide for all 8 patterns (State, Strategy, Chain of Resp, Decorator, Observer, Composite, Factory, Repository). |
| **Security Architecture** | [`docs/security.md`](docs/security.md) | Stateless JWT, BCrypt, 5-strike lockout, persona privacy wall, application-layer `AccessPolicy`. |
| **Testing Catalog** | [`docs/testing/test-cases.md`](docs/testing/test-cases.md) | 25 documented test cases across Normal, Boundary, Invalid Input, Exceptions, Auth, Rules, and Integration. |
| **Database Schema Reference**| [`docs/db/schema.md`](docs/db/schema.md) | Relational schema reference, column constraints, and index definitions. |
| **Architecture Decisions** | [`docs/DECISIONS.md`](docs/DECISIONS.md) | Formal ADRs (ADR-001 through ADR-008) recording architectural trade-offs and rationale. |
| **Review Reports** | [`docs/review/`](docs/review/) | Audit reports and batch progression summaries. |
