# Security Architecture & Data Privacy Specification: I-ITSM

---

## 1. Authentication & Security Architecture Overview

The I-ITSM platform implements a defense-in-depth security model combining stateless JSON Web Token (JWT) authentication, BCrypt password hashing, automated account lockout protection, and multi-tier role-based access control (RBAC) with polymorphic application-layer authorization.

```
[ Client Request ]
       |
       v
[ JwtAuthenticationFilter ] ---> [ Bearer Token Extraction & Validation ]
       |                                     |
       |                              [ Check User Status: Disabled/Locked? ]
       |                                     |
       v                                     v (401 if locked/disabled)
[ Spring Security FilterChain ] ---> [ Role Authority Mapping: ROLE_EMPLOYEE / ROLE_TECHNICIAN / ROLE_ADMIN ]
       |
       v
[ Security Headers Guard ]   ---> [ CSP, nosniff, frame-ancestors 'none', Referrer-Policy ]
       |
       v
[ Controller Layer ]         ---> [ Returns Only web.dto Types (No Domain/JPA Entities) ]
       |
       v
[ Application Service Layer] ---> [ Polymorphic AccessPolicy Invariants (Ownership, Scope) ]
       |
       v
[ Persona-Specific DTO View] ---> [ Strict Attribute Projection (Masking Priority & Internal Notes) ]
```

---

## 2. Core Security Controls

### A. Stateless JWT Authentication & Secret Safety
- **Endpoint**: `POST /api/auth/login` accepts `{ "email": "...", "password": "..." }`.
- **Token Generation**: Uses `io.jsonwebtoken:jjwt-api:0.12.6`. Tokens are signed using HMAC-SHA-256 (`Keys.hmacShaKeyFor`) with a minimum 256-bit cryptographic secret (`iitsm.security.jwt.secret`).
- **Production Guard**: The application fails context startup in the `prod` profile if the secret is missing, shorter than 32 bytes, or equals the committed repository default.
- **Token Lifetime**: 15 minutes (900,000 ms) default access token lifespan.
- **Claims**: Stores user subject email, `userId`, `role`, `name`, and `tokenVersion`.
- **Filter Invalidation**: `JwtAuthenticationFilter` validates user status on every request. Tokens for locked or deactivated accounts are rejected immediately with HTTP 401 JSON (`{"errorCode":"ERR_UNAUTHORIZED","message":"Account is locked or deactivated"}`).

### B. BCrypt Hashing, Brute-Force Lockout & Uniform Auth Responses
- All user passwords are encrypted using `BCryptPasswordEncoder` with a work factor of 10.
- **5-Strike Account Lockout Policy**:
  - Failed login attempts are recorded via `LoginAttemptService` with `@Transactional(propagation = Propagation.REQUIRES_NEW)` so failed attempts persist in the database even when `InvalidCredentialsException` rolls back caller transactions.
  - Upon the 5th consecutive failed login attempt, `user.lock()` marks the account locked.
  - A successful authentication resets `failedLoginAttempts` back to 0.
  - Administrators can unlock locked accounts via `POST /api/admin/users/{id}/unlock`.
- **Zero Enumeration (Uniform Error Body)**:
  - Unknown email, incorrect password, locked account, and deactivated account all return byte-identical HTTP 401 responses:
    `{"errorCode":"ERR_INVALID_CREDENTIALS","message":"Invalid credentials"}`
  - The precise diagnostic reason is logged server-side for security audit purposes without leaking account existence to clients.

### C. Self-Registration Protection
- The public registration endpoint (`POST /api/auth/register`) strictly ignores client-supplied `designation` values.
- New accounts are always provisioned with the lowest designation (`Designation.ASSOCIATE`).
- Promotion to elevated designations (`VP_C_LEVEL`, `SENIOR_VP`, etc.) requires authenticated administrative authorization via `PUT /api/admin/users/{userId}/designation`.

### D. Password Hash Suppression & ArchUnit Enforcement
- Controllers never return domain entities (`User`, `Technician`) or JPA entities (`UserJpaEntity`).
- Dedicated transfer objects (`TechnicianSummaryDto`, `UserSummaryDto`) omit `passwordHash` and security-sensitive attributes.
- ArchUnit automated test `A1_controllersMustNotReturnDomainOrJpaEntities` enforces this restriction at build time.

### E. HTTP Security Headers
All HTTP responses enforce the following security headers:
- `Content-Security-Policy`: `"default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'"`
- `X-Content-Type-Options`: `nosniff`
- `X-Frame-Options`: `DENY`
- `Referrer-Policy`: `strict-origin-when-cross-origin`

---

## 3. Persona Data Privacy Invariant (Section 7.2)

### A. The Employee Privacy Wall
Business employees must never see internal engineering details:
1. **Priority Ranks (P1–P4)**: Completely masked. An employee cannot view calculated priority, 6-factor score breakdowns, or priority override decisions.
2. **SLA Deadlines & Risk Scores**: Operational resolution targets, at-risk flags, and breach countdowns are strictly hidden.
3. **Internal Engineering Notes**: Only public communication (requests for info and answers) is shared. Private troubleshooting logs and root cause hypotheses remain internal.

### B. Enforced by DTO Type Separation
The privacy wall is enforced at compile time by distinct response DTO types:
- `EmployeeIncidentView`: Contains only public fields (`id`, `title`, `description`, `category`, `status`, `createdAt`, `resolutionSummary`). Does not declare fields for `priority`, `slaDeadline`, `riskScore`, or `internalNotes`.
- `TechnicianIncidentView`: Exposes priority, SLA due dates, risk badges, and investigation notes.
- `AdminIncidentView`: Full operational inspection view including 6-factor score contributions and complete audit logs.

### C. Polymorphic Access Control (`AccessPolicy`)
Access control uses object-oriented dynamic dispatch via `User` subclasses:
- `Employee`: Only permitted to view and verify/close their own tickets; blocked from technician or administrative actions.
- `Technician`: Permitted to view incidents assigned to them or in their team queue; permitted to execute technician lifecycle actions only on assigned incidents.
- `TeamLead`: Permitted to inspect and manage incidents across their support team.
- `Administrator`: Global visibility and full administrative authority across all incidents.

---

## 4. OWASP Top 10 Mitigation Summary

| Vulnerability | Mitigation in I-ITSM |
| :--- | :--- |
| **A01: Broken Access Control** | Polymorphic `AccessPolicy` verifies ownership and role capability; DTO projection prevents horizontal and vertical data leakage. |
| **A02: Cryptographic Failures** | BCrypt work factor 10; JWT secret length >= 256 bits validated on startup; no credentials committed in production configuration. |
| **A03: Injection (SQL / HQL)** | Exclusively parameterized Spring Data JPA queries; strongly-typed JPA Specifications (`CriteriaBuilder`); zero string concatenation. |
| **A04: Insecure Design** | Hexagonal architecture; pure domain core with zero framework imports; finite state machine fails fast on invalid state transitions. |
| **A05: Security Misconfiguration** | Strict CSP, `nosniff`, `frame-ancestors 'none'`; Swagger/OpenAPI restricted to non-production profiles; default ports separated. |
| **A07: Identification & Auth Failures** | 5-strike account lockout via `REQUIRES_NEW`; uniform error responses preventing account enumeration; short 15-minute access token lifespan. |
