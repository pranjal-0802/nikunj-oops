# I-ITSM Code Review (nikunj-oops)

**Scope:** full read of the uploaded source (246 Java files: ~15.3k main lines, ~4.4k test lines; plus `app.js`, `index.html`, Flyway schema, `pom.xml`, `application.yml`, Docker/CI files) and the main docs.
**Method:** static code reading plus `grep` checks. **I could not build or run the tests** (Maven Central is not reachable from my sandbox), so the 165-test and 86.61%-coverage claims are *unverified*. Every finding below cites the file it came from; "confirmed" means I traced it through the code, not that I executed it. Please reproduce each one with a failing test before fixing (the companion prompt does this).

---

## 1. What is genuinely good (verified)

- **The domain package really is pure.** `grep` finds zero `org.springframework` or `jakarta.persistence` imports under `com.iitsm.domain`. JPA entities and mappers are separate, so the "pure domain" claim holds. This is rare in student projects and worth defending in the viva.
- **Optimistic locking survives the mapper.** `Incident.version` is copied to the JPA entity and back (`IncidentMapper`), so Hibernate's version check on `IncidentJpaEntity` works across the domain/entity boundary.
- **Real patterns, not name-dropping:** State (9 states, `IncidentStates` registry), Strategy (assignment, priority factors, classifiers, similarity), Chain of Responsibility (4 escalation handlers), Observer (sealed `DomainEvent`, 17 records, audit and notification observers), Factory, Repository adapters, Decorator-style `AdjustableClock`.
- **Persona DTOs exist** (`EmployeeIncidentView` etc.) and the employee view declares no priority field.
- **Good test breadth** for the domain: transition table test, encapsulation test, strategy tests, access policy test, SLA tests, end-to-end lifecycle test.
- Docs are unusually complete (UML, ER, ADRs, phase summaries).

Correction to my earlier note: the code has **17** event records; only the *architecture doc's list* omits `PriorityOverridden`.

---

## 2. Findings (ranked)

Severity: **Critical** = exploitable or breaks a core flow; **High** = wrong behaviour or false claim an evaluator can hit; **Medium** = design/scale; **Low** = hygiene.

### Security

| ID | Sev | Finding | Evidence | Fix |
|---|---|---|---|---|
| S1 | Critical | **JWT can be forged by anyone who has the repo.** The signing secret is committed in `application.yml` (and the test config), there is no prod override or startup check, and the filter trusts the token subject (email). Anyone can mint an admin token for `admin@nimbusfinserve.com` on any deployment that kept the default. Token lifetime is 24 h; `security.md` says 1 h. | `src/main/resources/application.yml` (`iitsm.security.jwt.secret`), `JwtAuthenticationFilter` | No default secret; read from env; fail startup if missing, < 32 bytes or equal to the known value; 15 min access token + refresh; fix the doc. |
| S2 | Critical | **Account lockout never persists.** `UserService` is `@Transactional` at class level. On a wrong password it calls `recordFailedLogin` + `save`, then throws `InvalidCredentialsException`, which is a `RuntimeException` (`IitsmException`). Spring rolls the transaction back, so the counter increment is discarded and nobody is ever locked. The only lockout tests are at domain and adapter level, none through `/api/auth/login`. | `UserService.authenticate`, `IitsmException`, no `noRollbackFor` anywhere | Record attempts in a separate `REQUIRES_NEW` service (or `noRollbackFor`); add an HTTP integration test (6 wrong logins). Then make it time-based with an admin unlock (permanent lock is a DoS vector) and return one generic error for unknown / wrong / locked (currently messages differ, which enumerates accounts). |
| S3 | Critical | **Self-registration lets the caller choose their designation** (`RegisterRequest.designation`), including `VP_C_LEVEL` (priority weight 1.0). Anyone can register as a C-level and inflate the priority of every ticket. | `AuthController.register`, `RegisterRequest`, `Designation` | Ignore client designation; register at the lowest level; only admins change it. |
| S4 | High | **Password hash leaks to the admin client.** `AdminController.getTechnicianRoster` returns `List<Technician>` (a domain object) straight to Jackson. `User.getPasswordHash()` is public and nothing hides it, so the BCrypt hash, failed-login count and lock flag are serialized. | `AdminController` line ~159, `User` | Return a `TechnicianSummaryDto`. Add a test that no response body contains `passwordHash`. |
| S5 | High | **Hard-coded demo password in client JS and no login screen.** The "persona switcher" calls `/api/auth/login` with `'Password123!'` for any persona, including admin. There is no login form in `index.html`. The seeder (`CompanySeeder`) hashes the same password and `auto-seed` defaults to true. | `app.js` `authenticateUser`, `CompanySeeder`, `application.yml` | Real login page; demo-account panel only when `demo-mode=true`; seeding off by default outside `dev`/`demo`. |
| S6 | High | **Access policy is weaker than documented.** `enforceCanViewIncident` ends with "Allow general technician inspection of active incidents" and returns, so any technician can read any ticket (Javadoc says assigned/team only). Team leads can act on any ticket regardless of team. `enforceCanExecuteAction` only checks the branch matching the actor's own type, so an `Employee` passes the technician-action cases and a `Technician` passes the employee-action cases; only the URL role guards stop it. Actions are bare strings. | `domain/policy/AccessPolicy.java` | Enum `IncidentAction`; policy per role (Strategy) with team scoping; tests per role x action x ownership. Fix `security.md` ("TECHNICIAN may view any incident" is currently true, the doc implies otherwise). |
| S7 | Medium | **Deactivated or locked users keep access until the token expires.** `JwtAuthenticationFilter` builds the authentication from `UserDetails` without checking `isEnabled()` / `isAccountNonLocked()`. A deleted user causes `UsernameNotFoundException` inside the filter (500, not 401). | `JwtAuthenticationFilter` | Check flags; catch and return 401; token-version claim. |
| S8 | Medium | No security headers, Swagger UI and `/v3/api-docs` are public in every profile, H2 is a runtime dependency of the production jar. | `SecurityConfig`, `pom.xml` | Headers (CSP, nosniff, frame-ancestors); gate Swagger by profile; H2 `test`/`dev` only. |

### Correctness and behaviour

| ID | Sev | Finding | Evidence | Fix |
|---|---|---|---|---|
| F1 | Critical | **Ticket creation fails when every technician is at capacity.** `CandidateFilter` drops full technicians; `IncidentService.createIncident` then does `if (candidates.isEmpty()) candidates = allTechs`, a strategy picks one, and `Technician.assignIncident` throws `TechnicianOverloadedException`. The employee gets an error and the ticket is lost. The same chain throws `TechnicianUnavailableException` from the strategies on an empty list. | `IncidentService` lines ~169-185, `Technician.assignIncident` (~line 100), `SkillBasedStrategy` | If nobody has capacity, save the incident as `CLASSIFIED` unassigned, alert the admin, and let a scheduler retry. Test: fill all capacity, then create. |
| F2 | High | **Unmatched tickets get an arbitrary service.** `ServiceMatcher.findBestMatch` ends with `return availableServices.stream().findFirst()` when no keyword matches. A vague ticket such as "not working" (exactly the sample in your PRD) is attributed to whichever service the DB returns first, with its business criticality and owning team. (`"SRV-DEFAULT"` is only used when the catalogue is empty, and no such row exists, so it would violate the FK on `incidents.service_id`.) | `ServiceMatcher` last lines, `IncidentService` line ~155, `V1__init_schema.sql` | Return `Optional.empty()`; seed a "General IT / Unclassified" service; route low-confidence tickets to triage. |
| F3 | High | **The duplicate "avalanche" rule is unreachable in the real flow and S4 does not prove it.** `RULE_DUPLICATE_AVALANCHE` fires at >= 5 linked duplicates, but `createIncident` passes `incident.getLinkedIncidentIds().size()` of the *new* ticket, which is at most 1-2. The parent ticket is never re-prioritised when duplicates arrive. Scenario S4 just prints the parent's *creation-time* rules. The unit test passes because it injects `5` directly into the context. | `IncidentService` line ~163, `CompositePriorityCalculator` ~109, `SimulationService.runScenario4...` | On linking, recompute the parent's priority using cluster size, emit `PriorityCalculated`, audit it, and assert it in S4. |
| F4 | High | **Duplicates are still assigned to their own technicians** (assignment runs after linking), and duplicate detection runs *before* classification: `newIncident.getCategory()` is null, so it calls `incidentRepository.findAll()` and compares text with every open ticket in the last 72 h. Service is ignored. This is an O(N) full-table load per ticket. | `DuplicateDetectionService.checkAndLinkDuplicate`, `IncidentService` order of steps | Classify first, query candidates by service + category + time window in SQL, attach duplicates to the parent without consuming capacity. |
| F5 | High | **Low-confidence classification is not routed to humans.** `ClassificationResult.confidence` is computed but nothing gates on it. The "triage queue" endpoint only lists `ReassignmentRequest`s. | `IncidentService.createIncident`, `AdminController.getTriageQueue` | Threshold (config) -> `CLASSIFIED` + `needsTriage`, unassigned; admin accepts or reclassifies. |
| F6 | Medium | **"Reporter history" counts all-time tickets** (`findByReporterId(...).size()`) but is documented as recent/recurring. A legitimate frequent reporter is boosted forever. | `IncidentService` line ~161, `ReporterHistoryFactor` | Count unresolved/reopened tickets on the same service in the last N days. |
| F7 | Medium | **Technician load has two sources of truth** (`Technician.activeIncidentIds` vs counts derived from incidents in the dashboard) and `Technician` has no `@Version`, so two simultaneous creations can both pick the last free slot. Optimistic locking only protects `Incident`. | `Technician`, `IncidentService.getTechnicianDashboard`, `UserJpaEntity` | Derive load from incidents (single source) or add a version and test the race. |
| F8 | Medium | **Email is only simulated in the default run.** `application.yml` has no `spring.mail.*`; without it no `JavaMailSender` bean exists and `EmailChannel` just logs. It only sends when the Docker env vars are set. Messages are plain text although the Javadoc says HTML. | `EmailChannel`, `application.yml`, `docker-compose.yml` | Config properties + a dev Mailpit default; HTML templates. |
| F9 | High | **`docker compose up` yields a system nobody can log into as staff.** The compose file sets `SPRING_PROFILES_ACTIVE=prod`; the `prod` profile sets `auto-seed: false`; self-registration creates only employees. | `docker-compose.yml`, `application.yml` | Use a `demo` profile in compose, or add an explicit first-admin bootstrap. |

### Architecture and OOP (these matter most for your grade)

| ID | Sev | Finding | Evidence | Fix |
|---|---|---|---|---|
| A1 | High | **The hexagon leaks above the domain.** `application.*` imports infrastructure types: `IncidentService.flagWrongDomain` returns `ReassignmentRequestJpaEntity`; `SearchService`, `EscalationService`, `IncidentService` use Spring Data repositories and JPA entities; `UserService` depends on `JwtTokenProvider`. `web.*` returns JPA entities (`AdminController.getTriageQueue`) and calls Spring Data repositories directly (`EmployeeController`, `AdminController`, `NotificationController`). This contradicts `03-architecture.md` ("controllers never interact with repositories"), so an examiner can disprove it with one `grep`. | `grep -rn "import com.iitsm.infrastructure" application web` | Ports for audit, notification, escalation, reassignment and search in the domain; return domain objects/DTOs; add **ArchUnit** rules so the claim is machine-checked. |
| A2 | Medium | **`AccessPolicy` is anti-polymorphic.** It is an interface whose behaviour lives in `default` methods using `instanceof` chains and string `switch`, with an empty `DefaultAccessPolicy` class. That is the opposite of the polymorphism the course grades. | `AccessPolicy`, `DefaultAccessPolicy` | `User.canView(incident)` / role-specific policy objects; `IncidentAction` enum. |
| A3 | Medium | **Scalability claims vs reality.** `getAdminStats`, duplicate detection and reports load whole tables (`findAll()`) and compute in memory. | `IncidentService.getAdminStats`, `ReportingService` | SQL aggregates/projections; pagination. |
| A4 | Low | `IncidentRepositoryAdapter` converts every `DataAccessException` (including optimistic-lock conflicts) into `PersistenceFailureException`. Make a lost update a specific `ConcurrentModificationException` -> HTTP 409 and add a two-thread test. | adapter | |

### Documentation honesty and hygiene

| ID | Sev | Finding | Fix |
|---|---|---|---|
| D1 | High | README **self-grades 120/120 using a rubric that is not yours** (60/25/15/10/10) and says "embedded React SPA". The UI is vanilla JS (`app.js`, 1,543 lines of string-template `innerHTML`), Tailwind via the **Play CDN** (`cdn.tailwindcss.com`, not meant for production and needs internet at demo time) and Google Fonts. No React anywhere. | Remove the self-grade; state what the UI is. |
| D2 | Medium | CI step is labelled "Checkstyle, Tests, JaCoCo" but no Checkstyle is configured; badges are static images ("165 Passed", "86.61%"); I count 149 test methods (147 `@Test` + 2 parameterized), so 165 may be parameter expansion, **unverified**. Coverage is quoted as *instruction* coverage while the build gate is *line* coverage. | Real CI badge; publish the JaCoCo report as a CI artifact. |
| D3 | Low | `PROMPT.md` and `CLAUDE.md` in the repo root; only 9 commits; port mismatch (README 8080 vs 8085; Dockerfile `EXPOSE 8080`). | Move prompts to `docs/dev/`; commit under each member's name; fix ports. |
| D4 | Low | Classifier accuracy (86.1%) comes from a synthetic corpus; unless the test set was written independently of the generator it is optimistic. | Hand-written test set + confusion matrix + a sentence saying the data is synthetic. |

---

## 3. UI/UX diagnosis (from `index.html`, `app.js`, `styles.css`)

- **No login or registration screen.** The app auto-logs in as a seeded user and a top-bar "persona switcher" re-authenticates with a hard-coded password. This is a demo trick, not a product, and the brief explicitly requires register/login and different UIs per user.
- **One monolithic script** (1.5k lines) building HTML with template strings: no components, inconsistent states, high bug risk. 41 `innerHTML` writes; ticket fields are passed through `escapeHtml` but error messages and other fields are interpolated raw.
- **Dependency on the internet at demo time** (Tailwind Play CDN, Google Fonts). If the venue Wi-Fi fails, the UI loses its styling.
- **No design system, no empty/loading/error states, no accessibility work, no responsive strategy beyond utility classes.**
- The previously written redesign spec (React + Vite + TS + Tailwind built locally + shadcn/ui, three persona shells, `allowedActions` from the API) still applies and also fixes the CDN problem.

---

## 4. Suggested order of work

1. **Day 1 (stop the bleeding):** S1, S2, S3, S4, F1, F2, F9. Each is small, and each gets a failing test first.
2. **Day 2-3 (make claims true):** A1 + ArchUnit, S6/A2, F3-F5, S7, README honesty (D1-D3).
3. **Then UI** (Stages 3-4 of the redesign prompt), then E2E + accessibility, then the demo script.

If you only have time for five fixes: **S1, S2, S3, F1, A1**. Those are the ones an evaluator is most likely to hit or disprove in a live demo or a code read.
