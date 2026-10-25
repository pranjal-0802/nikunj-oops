# Intelligent IT Service Management (I-ITSM)

A production-grade, Object-Oriented IT Service Management and Incident Resolution System built for CS F213 (Object-Oriented Programming).

---

## 1. System Architecture & Tech Stack
- **Language**: Java 21 (Records, Sealed Interfaces, Pattern Matching)
- **Framework**: Spring Boot 3.3.4 (Web, Data JPA, Security, Scheduling, Actuator)
- **Database & Persistence**: PostgreSQL 16 (production), H2 in-memory (automated testing), Flyway migrations
- **Security**: Spring Security 6 with stateless JWT authentication, BCrypt hashing, and application-level `AccessPolicy`
- **Testing**: JUnit 5, AssertJ, Mockito, JaCoCo coverage (target ≥ 70%)
- **Email Testing**: Mailpit (local SMTP on port 1025, Web UI on port 8025)

---

## 2. Demo Accounts & Credentials

All seeded demo accounts use the standard password: `Password123!`

| Persona | Email | Purpose & Scope |
| :--- | :--- | :--- |
| **Employee** | `employee@nimbusfinserve.com` | Raises tickets, views personal tickets, provides info, verifies fix. Priority is hidden. |
| **Technician (Network)** | `tech.network@nimbusfinserve.com` | Acknowledges, investigates, resolves tickets, flags out-of-scope. |
| **Team Lead (Network)** | `lead.network@nimbusfinserve.com` | Oversees network team queue, handles escalations. |
| **Administrator** | `admin@nimbusfinserve.com` | System administrator with full catalog, override, and triage capabilities. |
| **IT Manager** | `manager@nimbusfinserve.com` | Top of escalation chain with enterprise incident oversight. |

---

## 3. Running Locally

### Prerequisites
- JDK 21 (Microsoft OpenJDK 21 or Eclipse Temurin)
- Apache Maven 3.9+ (or use included `./mvnw`)
- Docker & Docker Compose (optional for PostgreSQL & Mailpit)

### Quick Start (In-Memory Development Profile)
By default, the application runs with an in-memory H2 database with automatic company seeding:
```bash
./mvnw spring-boot:run
```
The application will boot on port `8080`.

### Running with Docker Compose (PostgreSQL + Mailpit)
```bash
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```
- PostgreSQL: `localhost:5432`
- Mailpit Web Interface: `http://localhost:8025`

---

## 4. Running Tests & Quality Verification

To run all unit and integration tests with JaCoCo verification:
```bash
./mvnw clean verify
```
Test results and coverage reports are generated at:
- `target/site/jacoco/index.html`

