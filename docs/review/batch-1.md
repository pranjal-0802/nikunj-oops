# Batch 1 Report: Stop the Bleeding

---

## 1. Executive Summary

Batch 1 remediates critical security flaws and behavioral blockers (S1–S5, F1, F2, F9) that exposed vulnerabilities or broke incident submission.

- **Findings Closed**:
  - **S1 (JWT Secret Startup Validation)**: Closed (GREEN).
  - **S2 (Lockout Persistence & Generic Auth Error)**: Closed (GREEN).
  - **S3 (Registration Privilege Escalation Guard)**: Closed (GREEN).
  - **S4 (Password Hash Leak in Admin Endpoints)**: Closed (GREEN).
  - **S5 (Public Config Endpoint & Demo Mode)**: Closed (GREEN).
  - **S8 (Security Headers - early fix)**: Closed (GREEN).
  - **F1 (Capacity Handling & Unassigned Ticket Creation)**: Closed (GREEN).
  - **F2 (General IT Service Fallback)**: Closed (GREEN).
  - **F9 (Docker Compose & Smoke Script)**: Closed (DONE).

---

## 2. Changes Made & Remediations Implemented

### Security
1. **S1 (JWT Secret Validation)**:
   - Modified `JwtTokenProvider.java`: Added constructor verification that checks key length (>= 32 bytes) and fails context startup with `IllegalStateException` if `matchesProfiles("prod")` and the default committed secret is detected.
   - Updated `application.yml`: Configured `JWT_SECRET` environment variable bindings and reduced default access token lifetime to 15 minutes.
2. **S2 (Lockout Persistence)**:
   - Created `LoginAttemptService.java`: Extracted attempt counting with `@Transactional(propagation = Propagation.REQUIRES_NEW)` so failed logins persist even when `InvalidCredentialsException` rolls back the caller transaction.
   - Standardized `UserService.authenticate`: Emits uniform `"Invalid credentials"` error message to prevent username and lock-state enumeration while logging precise reasons server-side.
3. **S3 (Registration Protection)**:
   - Updated `AuthController.register`: Ignores client-supplied `designation` and always registers new accounts with `Designation.ASSOCIATE`.
   - Added `PUT /api/admin/users/{userId}/designation` and `POST /api/admin/users/{userId}/unlock` in `AdminController`.
4. **S4 (Password Hash Leak)**:
   - Created `TechnicianSummaryDto.java` without `passwordHash`.
   - Updated `AdminController.getTechnicianRoster` to return `List<TechnicianSummaryDto>`.
5. **S5 (Public Configuration)**:
   - Created `PublicConfigController.java` exposing `/api/public/config` returning `demoMode` state.
   - Added `iitsm.demo-mode` config property in `application.yml` and permitted `/api/public/**` without authentication in `SecurityConfig`.
6. **S8 (Security Headers)**:
   - Configured `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, and `Referrer-Policy: strict-origin-when-cross-origin` in `SecurityConfig`.

### Correctness
1. **F1 (Overloaded Technicians & Unassigned Dispatcher)**:
   - Updated `IncidentService.createIncident`: Filters candidates to only technicians with available capacity (`hasCapacity()`). When no technician has capacity, leaves incident in `CLASSIFIED` state with `assigneeId = null`.
   - Implemented `UnassignedIncidentDispatcher.java`: Periodic background worker that scans unassigned `CLASSIFIED` incidents and assigns them when capacity is freed.
2. **F2 (Service Fallback)**:
   - Updated `ServiceMatcher.java`: Removed arbitrary `availableServices.stream().findFirst()` fallback; refined token matching to prevent false-positive substring matches (e.g. "work" in "workday").
   - Added `"SRV-GEN-001"` ("General IT / Unclassified") service owned by `TEAM-DESK` in `CompanySeeder.java`.
   - `IncidentService.createIncident` defaults unmatched tickets to "General IT / Unclassified".
3. **F9 (Docker Compose & Smoke Script)**:
   - Updated `docker-compose.yml`: Set profile to `demo` with auto-seeding enabled and non-default JWT secret.
   - Created `scripts/smoke.sh` and `scripts/smoke.bat` verifying health check, employee/admin authentication, incident creation, and roster data protection.

---

## 3. Test Verification Status

- `S1_jwtSecretValidationFailsOnWeakOrCommittedSecret`: **PASSED**
- `S2_lockoutPersistsAfterFiveBadLogins`: **PASSED**
- `S3_registrationIgnoresClientSuppliedDesignation`: **PASSED**
- `S4_adminEndpointsDoNotLeakPasswordHash`: **PASSED**
- `S5_publicConfigControlsDemoModeAndHidesDemoCredentials`: **PASSED**
- `S8_securityHeadersArePresentOnHttpResponses`: **PASSED**
- `F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned`: **PASSED**
- `F2_unmatchedServiceDoesNotPickArbitraryService`: **PASSED**
