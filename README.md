# Intelligent IT Service Management (I-ITSM)

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Pure Java Domain](https://img.shields.io/badge/Domain%20Core-Zero%20Spring%20Imports-purple.svg)]()
[![Architecture](https://img.shields.io/badge/Architecture-Hexagonal%20Ports%20%26%20Adapters-informational.svg)]()
[![Build & Tests](https://img.shields.io/badge/Maven-clean%20verify%20green-success.svg)]()

> A production-grade, enterprise IT Service Management platform built for **CS F213 (Object-Oriented Programming)**. Engineered around a 100% pure-Java domain core, 8 object-oriented design patterns, an explainable pure-Java NLP & multi-factor priority scoring engine, a multi-tier Chain of Responsibility escalation hierarchy, an accelerated virtual simulation engine (`AdjustableClock`), and a role-isolated single-page enterprise web console.

---

## 1. Key System Architecture & OOP Highlights

1. **Pure Java Hexagonal Domain Core**:
   - `com.iitsm.domain.*` has **zero imports** from Spring Framework (`org.springframework.*`), Jakarta (`jakarta.*`), or persistence libraries.
   - Enforced continuously via ArchUnit architectural test rules in `ArchitectureRegressionTest`.
   - Domain aggregate roots enforce behavioral encapsulation (no public setters; rich behavior methods; Tell-Don't-Ask).
2. **8 Viva-Defensible Object-Oriented Design Patterns**:
   - **State Pattern**: 9-state finite state machine with zero-allocation `EnumMap` flyweight singletons.
   - **Strategy Pattern**: Dynamic technician routing (5 strategies), 6-factor priority scoring, NLP classifiers, and text similarity measures.
   - **Chain of Responsibility**: 4-tier progressive escalation hierarchy (Technician $\rightarrow$ Team Lead $\rightarrow$ System Admin $\rightarrow$ IT Operations Manager).
   - **Decorator Pattern**: `AdjustableClock` decorating `java.time.Clock` with speed multipliers ($\times 1, \times 60, \times 600$) and discrete fast-forwards.
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

## 2. Simulated Company Infrastructure: Nimbus FinServe Inc.

### 2.1 Corporate Purpose & Infrastructure Overview
**Nimbus FinServe Inc.** is an institutional financial services firm operating low-latency algorithmic trading infrastructure, core digital banking ledgers, risk analytics pipelines, and wealth management services. 

Because minute-by-minute system availability directly prevents market capital loss and regulatory non-compliance, **incident priority is strictly derived from the criticality of the impacted IT service to the company's financial infrastructure**.

### 2.2 Corporate Hierarchy & Specialist Teams
```
                            Julian Thorne
                      (Chief Executive Officer)
                                 │
                           Elena Rostova
                    (Chief Technology Officer)
                                 │
                          Alexander Vance
                   (Chief IT Infrastructure Admin)
                                 │
     ┌───────────────────┬───────┴───────────┬───────────────────┐
     │                   │                   │                   │
Network Operations  Core Banking DB    Cybersecurity      Workplace & Floor
  (TEAM-NET)          (TEAM-DB)          (TEAM-SEC)          (TEAM-DESK)
     │                   │                   │                   │
Lead: Sarah Jenkins  Priya Patel        Marcus Brody        Carlos Ramirez
Tech: David Kim     (Lead DBA)         (SecOps Engineer)   (Floor Specialist)
```

| Persona & Name | Corporate Title & Domain Expertise | Pre-seeded Email |
| :--- | :--- | :--- |
| **Vikram Malhotra** | Quantitative Trading Analyst (Retail & Equity Derivatives) | `employee@nimbusfinserve.com` |
| **David Kim** | Senior Network Engineer (Low-latency fiber, BGP routers, FIX tunnels) | `tech.network@nimbusfinserve.com` |
| **Priya Patel** | Lead Database Administrator (Oracle RAC, PostgreSQL, Liquibase pipelines) | `tech.database@nimbusfinserve.com` |
| **Sarah Jenkins** | Network Operations Team Lead (Escalation handling & queue management) | `lead.network@nimbusfinserve.com` |
| **Alexander Vance** | Chief IT Infrastructure Administrator (Unified triage, policy overrides, audit) | `admin@nimbusfinserve.com` |

*Default password for all seeded evaluation accounts:* `Password123!`

### 2.3 IT Service Catalog & Infrastructure Criticality Tiers

Ticket priority (P1–P4) is computed dynamically based on the IT service's criticality tier, reporter designation weight, and historical failure frequency:

| Tier | Criticality Level | Service Key & Description | Baseline Priority & Target SLA | Financial / Operational Impact Rationale |
| :---: | :---: | :--- | :---: | :--- |
| **Tier 1** | **Mission Critical** | `SRV-TRD-001` FIX Protocol Gateway<br>`SRV-DB-001` Core Banking Ledger<br>`SRV-NET-001` Optical Exchange Backplane | **P1 (Critical)**<br>• Response: 5 min<br>• Resolve: 30 min | **Direct Financial Outage**: Halts equity trading and transactions. Carries exchange penalties and severe financial liability. |
| **Tier 2** | **High Criticality** | `SRV-AUTH-001` Active Directory & SSO<br>`SRV-RISK-001` Quantitative Risk Engine<br>`SRV-VPN-001` Global Trading Tunnel | **P2 (High)**<br>• Response: 15 min<br>• Resolve: 2 hours | **Business Impairment**: Blocks remote trading teams and intraday portfolio risk/margin valuation. |
| **Tier 3** | **Standard** | `SRV-MAIL-001` Corporate Exchange Email<br>`SRV-REP-001` Regulatory Reporting Portal<br>`SRV-CRM-001` Wealth Management CRM | **P3 (Moderate)**<br>• Response: 1 hour<br>• Resolve: 8 hours | **Operational Degradation**: Non-real-time business workflows with redundant backup processes. |
| **Tier 4** | **Low / Departmental**| `SRV-WKP-001` Trading Floor Workstation & Print<br>`SRV-GEN-001` General IT Support | **P4 (Minor)**<br>• Response: 4 hours<br>• Resolve: 24 hours | **Routine Requests**: Localized peripheral or cosmetic issues without market impact. |

---

## 3. Evaluation Demo Protocol (Role-Isolated Experience)

To demonstrate the real-world operational separation of duties:

1. **Step 1: Employee Incident Intake**
   - Open [http://localhost:8085/](http://localhost:8085/) and sign in as **Vikram Malhotra** (`employee@nimbusfinserve.com`).
   - Notice that **only** the Employee Self-Service Portal is visible.
   - Click template **"🌐 VPN Disconnected"** or submit: *"VPN tunnel drops during institutional block trade"*.
   - The ticket is immediately classified into `NETWORK` and assigned to specialist **David Kim (Network Operations)**.
   - An on-screen evaluation prompt appears instructing the evaluator to sign out and log in as David Kim.
2. **Step 2: Specialist Technical Handling**
   - Click **Exit / Sign Out** in the top-right navbar.
   - On the sign-in card, click the quick evaluation pill for **David Kim** (`tech.network@nimbusfinserve.com`).
   - Notice that David sees **only** the Technician Console with his real-time workload capacity.
   - Click the assigned ticket $\rightarrow$ Click **Acknowledge** $\rightarrow$ Click **Start Investigation** $\rightarrow$ Click **Add Technical Note** (internal diagnostics) $\rightarrow$ Click **Resolve** and enter the structured fix (*"BGP tunnel re-keyed and MTU resized"*).
3. **Step 3: Verification & Formal Closure**
   - Click **Exit / Sign Out** and log back in as **Vikram Malhotra** (`employee@nimbusfinserve.com`).
   - The ticket progress stepper now reflects **Resolved**.
   - Vikram clicks **Verify & Rate Resolution**, submits a 5-star rating, and the ticket formally transitions to `CLOSED`.

---

## 4. Quick Start & Local Execution

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

## 5. Real SMTP External Email Configuration

By default, in local development mode, emails are routed to the local Mailpit virtual SMTP server (`localhost:1025`) and viewed at `http://localhost:8025`.

To deliver **REAL** emails to external email accounts (e.g., your personal Gmail or corporate inbox), set standard Spring Boot environment variables before launching:

### Option A: Gmail SMTP (Using Google App Password)
1. Go to Google Account $\rightarrow$ Security $\rightarrow$ 2-Step Verification $\rightarrow$ **App passwords**.
2. Generate a 16-character App Password for "Mail".
3. In PowerShell, set:
```powershell
$env:SPRING_MAIL_HOST = "smtp.gmail.com"
$env:SPRING_MAIL_PORT = "587"
$env:SPRING_MAIL_USERNAME = "youraccount@gmail.com"
$env:SPRING_MAIL_PASSWORD = "xxxx xxxx xxxx xxxx"  # Your 16-character Google App Password
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = "true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "true"

.\mvnw.cmd spring-boot:run
```

### Option B: Mailtrap (Safe Real SMTP Cloud Testing)
```powershell
$env:SPRING_MAIL_HOST = "live.smtp.mailtrap.io"
$env:SPRING_MAIL_PORT = "587"
$env:SPRING_MAIL_USERNAME = "your_mailtrap_user"
$env:SPRING_MAIL_PASSWORD = "your_mailtrap_password"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH = "true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE = "true"

.\mvnw.cmd spring-boot:run
```

When tickets are created, assigned, or escalated, `EmailChannel` dispatches MIME multipart HTML messages to the user's registered corporate email.

---

## 6. Running Verification Tests & Coverage Report

To execute the entire test suite and generate the JaCoCo coverage report:
```bash
./mvnw clean verify
```
*(On Windows PowerShell)*:
```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
.\mvnw.cmd clean verify
```

Coverage report generated at: `target/site/jacoco/index.html`.

---

## 7. Simulation Scenarios (S1–S5)

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

## 8. Course Documentation Deliverables

All course documentation deliverables are cataloged under the `docs/` directory:

| Document | File Path | Contents |
| :--- | :--- | :--- |
| **Problem Analysis** | [`docs/01-problem-analysis.md`](docs/01-problem-analysis.md) | Industry context, operational inefficiencies, problem statement, stakeholder analysis, KPIs. |
| **Requirements & RTM** | [`docs/02-requirements.md`](docs/02-requirements.md) | Functional (FR-1..6) and Non-Functional (NFR-1..6) specifications, Requirements Traceability Matrix. |
| **Architecture Specification**| [`docs/03-architecture.md`](docs/03-architecture.md) | Hexagonal Ports & Adapters, package structure, event-driven spine, concurrency & locking model. |
| **Technology Report** | [`docs/04-technology-report.md`](docs/04-technology-report.md) | Technology choices, pure Java NLP benchmark vs external ML, virtual clock vs real sleep. |
| **Design Patterns Catalog** | [`docs/05-design-patterns.md`](docs/05-design-patterns.md) | Viva defense guide for all 8 patterns (State, Strategy, Chain of Resp, Decorator, Observer, Composite, Factory, Repository). |
| **Security Architecture** | [`docs/security.md`](docs/security.md) | Stateless JWT, BCrypt, 5-strike lockout, persona privacy wall, application-layer `AccessPolicy`. |
| **Testing Catalog** | [`docs/testing/test-cases.md`](docs/testing/test-cases.md) | 25 documented test cases across Normal, Boundary, Invalid Input, Exceptions, Auth, Rules, and Integration. |
| **Database Schema Reference**| [`docs/db/schema.md`](docs/db/schema.md) | Relational schema reference, column constraints, and index definitions. |
| **Architecture Decisions** | [`docs/DECISIONS.md`](docs/DECISIONS.md) | Formal ADRs (ADR-001 through ADR-008) recording architectural trade-offs and rationale. |
| **Batch Reports** | [`docs/review/`](docs/review/) | Audit reports and batch progression summaries. |
