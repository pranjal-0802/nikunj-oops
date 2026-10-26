# Security Architecture & Data Privacy Specification: I-ITSM

## 1. Authentication & Security Architecture Overview
The I-ITSM platform implements a defense-in-depth security model combining stateless JSON Web Token (JWT) authentication, BCrypt password hashing, automated account lockout protection, and multi-tier role-based access control (RBAC).

```
[ Client Request ]
       |
       v
[ JwtAuthenticationFilter ] ---> [ Bearer Token Extraction & Validation ]
       |                                     |
       v                                     v
[ Spring Security FilterChain ] ---> [ Role Authority Mapping: ROLE_EMPLOYEE / ROLE_TECHNICIAN / ROLE_ADMIN ]
       |
       v
[ Controller Method Security ] ---> [@PreAuthorize("hasRole(...)")]
       |
       v
[ Application Service Layer ]  ---> [ AccessPolicy Enforcement (Ownership, Scope, Privacy) ]
       |
       v
[ Persona-Specific DTO Mapper] ---> [ Selective Attribute Projection (Hiding Priority & Internal Notes) ]
```

---

## 2. Core Security Controls

### A. Stateless JWT Authentication
- **Endpoint**: `POST /api/auth/login` accepts `{ "email": "...", "password": "..." }`.
- **Token Generation**: Uses the standard `jjwt` library (`io.jsonwebtoken:jjwt-api:0.12.6`). Tokens are signed using HMAC-SHA-256 (`Keys.hmacShaKeyFor`) with a 256-bit cryptographic secret (`iitsm.security.jwt.secret`).
- **Token Expiration**: Configurable lifetime (default: 1 hour / 3,600,000 ms).
- **Claims**: Stores user subject ID, email, and granted authority (`ROLE_EMPLOYEE`, `ROLE_TECHNICIAN`, `ROLE_TEAM_LEAD`, `ROLE_ADMIN`).

### B. BCrypt Password Hashing & Brute-Force Protection
- All user passwords are encrypted using `BCryptPasswordEncoder` with a work factor of 10.
- **5-Strike Account Lockout Policy**:
  - Failed login attempts increment `User.failedLoginAttempts`.
  - Upon the 5th consecutive failed login attempt, `User.accountLocked` is set to `true`, and the account is immediately disabled.
  - Subsequent login attempts are rejected with `InvalidCredentialsException: Account is locked due to 5 consecutive failed login attempts`.
  - A successful authentication resets `failedLoginAttempts` back to 0.

---

## 3. Persona Data Privacy Invariant (Section 7.2)

### A. The Employee Privacy Wall
A foundational design requirement of the I-ITSM system is that business employees must never see internal engineering details:
1. **Priority Ranks (P1–P4)**: Completely masked. An employee cannot view the calculated priority, the 6-factor score breakdown, or the priority decision rules.
2. **SLA Deadlines & Risk Scores**: Operational resolution targets, at-risk flags, and breach counters are strictly hidden.
3. **Internal Engineering Notes**: Only public communication (e.g. requests for info) is shared. Private troubleshooting logs and root cause hypotheses remain internal.

### B. Enforced by DTO Type Separation, Not UI Filtering
The privacy wall is enforced at the server side by distinct response DTO types:
- `EmployeeIncidentView`: Contains only public fields (`id`, `title`, `description`, `category`, `status`, `createdAt`, `resolutionSummary`). Does not declare fields for `priority`, `slaDeadline`, `riskScore`, or `internalNotes`.
- `TechnicianIncidentView`: Exposes priority, SLA due dates, risk badges, and investigation action permissions.
- `AdminIncidentView`: Full operational inspection view including 6-factor score contributions and complete audit logs.

### C. Application-Layer Access Control (`AccessPolicy`)
Access control is not left solely to URL filters. The `AccessPolicy` domain service verifies business ownership:
- `enforceCanView(user, incident)`: If an employee attempts to query an incident owned by another employee, the system throws `UnauthorizedOperationException` returning HTTP 403 Forbidden.
- `enforceAdmin(user)`: Enforces administrator privileges for catalog reconfiguration, triage reclassification, and strategy switching.

---

## 4. OWASP Top 10 Mitigation Summary

| Vulnerability | Mitigation in I-ITSM |
| :--- | :--- |
| **A01: Broken Access Control / IDOR** | `AccessPolicy` strictly validates that the requesting user matches `incident.getReporterId()` or possesses `ROLE_ADMIN` / `ROLE_TECHNICIAN`. |
| **A02: Cryptographic Failures** | Passwords stored strictly as BCrypt hashes; JWT secret enforced to at least 256 bits; sensitive internal fields never serialized to employees. |
| **A03: Injection (SQL / HQL)** | Exclusively parameterized queries using Spring Data JPA and Hibernate ORM; zero string concatenation in queries. JPA Specifications use strongly-typed `CriteriaBuilder`. |
| **A04: Insecure Design** | Strict layered architecture; domain core has zero framework imports; tell-don't-ask state transitions fail fast on invalid commands. |
| **A05: Security Misconfiguration** | Centralized `SecurityConfig` with explicit authorization paths; custom 401/403 handlers emitting consistent JSON error bodies. |
| **A07: Identification and Authentication Failures** | Automated 5-strike account lockout; strong BCrypt hashing; stateless session management (`SessionCreationPolicy.STATELESS`). |

