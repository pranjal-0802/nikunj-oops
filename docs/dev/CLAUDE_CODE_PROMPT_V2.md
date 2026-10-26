# Claude Code Prompt v2: Fix the Confirmed Review Findings (test-first), then redesign the UI

> **Setup before pasting:** put these three files in the repo under `docs/dev/` and commit them:
> 1. `IITSM_CODE_REVIEW.md` (the code review; this prompt calls it **the Review**)
> 2. `CLAUDE_CODE_PROMPT_AUDIT_AND_REDESIGN.md` (the earlier prompt; this prompt calls it **the Redesign Prompt**)
> 3. this file
>
> Then paste only this file as your first message. It **replaces Section 2 and Stage 0-2 of the Redesign Prompt** (those were hypotheses; the Review confirmed or refuted them from the source). Stages 3-6 of the Redesign Prompt (frontend foundation, persona UIs, quality, truth-up) still apply unchanged, with the amendments in Section 6 below.

---

## 0. Rules (same spirit as before)

1. Work **one batch at a time** (Section 4). Do not start a batch until I say "proceed to Batch N".
2. **Test-first for every finding:** write a test that fails because of the bug, show me the failure output, then fix, then show it green. Tag reproductions `@Tag("regression")` and name them after the finding id (`S2_lockoutPersistsAfterFiveBadLogins`, `F1_createIncidentWhenAllTechniciansFull_leavesTicketUnassigned`, ...). A fix without a test that fails on the old code is not accepted.
3. If you believe a finding in the Review is wrong, **prove it with a passing test and say so**; do not silently skip it. If you find a new problem, add it to `docs/review/AUDIT.md` with an id continuing the Review's scheme.
4. Do not weaken the existing OOP design: pure `domain` (no Spring/JPA imports), State, Strategy, Chain of Responsibility, Observer, Factory, Repository, persona DTOs, `AdjustableClock`. Record each design trade-off in `docs/DECISIONS.md`.
5. End of each batch: `./mvnw clean verify` green (report the **real** test count and coverage you measure, not the README's), write `docs/review/batch-N.md` (what changed, tests added, findings closed), small conventional commits, **stop and wait**.
6. Never commit secrets. Never rewrite git history. Commits should be small and attributable; ask me for each teammate's name/email before touching `CONTRIBUTING.md`; do not invent them.

---

## 1. Context

Spring Boot 3.3.4 / Java 21 backend for a CS F213 (OOP) project. Package roots: `com.iitsm.{domain,application,infrastructure,simulation,web}`. Domain is already free of Spring/JPA imports (verified). Existing UI: `src/main/resources/static/{index.html,app.js,styles.css}` (vanilla JS, Tailwind Play CDN, persona switcher with a hard-coded demo password, no login form). The grading rubric (120): Functionality 60, Technical Depth 15, System Design 10, UX/value-add 10, Innovation 10, Docs/demo 10, Testing 5. Evaluators read the code and watch a 15-20 minute demo of three workflows. Deadline: 22 Nov 2026.

---

## 2. Findings to fix (ids refer to the Review)

Implement exactly what the Review's "Fix" column says unless you document a better option in `DECISIONS.md`. Concrete acceptance criteria:

### Security
- **S1 JWT secret:** remove the committed secret from `application.yml`. Read `iitsm.security.jwt.secret` from environment; **fail startup** when it is missing, shorter than 32 bytes, or equals the old committed value (keep the old value only in `src/test/resources`). Access token 15 min + rotating refresh token stored hashed server-side (or at minimum a per-user `tokenVersion` claim checked by the filter). Update `docs/security.md` to match reality. Test: app context fails to start with the old secret under `prod`.
- **S2 Lockout:** move failed-attempt recording to a `LoginAttemptService` using `REQUIRES_NEW` (or `noRollbackFor = IitsmException.class` on that path). Time-based lockout (default 15 min, configurable, escalating), admin-unlock endpoint, per-IP rate limit on `/api/auth/login`. **One generic error body** for unknown email, wrong password, locked and deactivated accounts; log the real reason server-side. Tests via MockMvc: 5 bad logins lock; correct password during lock still fails; unlock after the window (use `AdjustableClock`); responses for the three cases are byte-identical.
- **S3 Registration:** ignore client-supplied `designation` and `department` weight effects; self-registration always creates the lowest designation; only admins can change designation (new admin endpoint, audited). Test: register with `"designation":"VP_C_LEVEL"` -> stored `INTERN`/`ASSOCIATE` (whichever is the configured lowest).
- **S4 Password-hash leak:** add `TechnicianSummaryDto`/`UserSummaryDto`; no controller may return a domain or JPA object. Add an ArchUnit rule (controllers' return types must be in `web.dto`) and a MockMvc test asserting no response from any admin endpoint contains `passwordHash`.
- **S5 Demo credentials:** delete the hard-coded password from the client. Seeding only in `dev`/`demo` profiles with `iitsm.demo-mode=true`; seeded password from `iitsm.demo.password` (env, with a documented default only in `dev`). The login page (Batch 4) shows demo accounts only when `/api/public/config` reports `demoMode=true`.
- **S6 Access policy:** introduce `enum IncidentAction`; replace the `instanceof`/string `switch` with role-specific policy objects (Strategy) or polymorphic methods on `User`. Rules to implement and test as a matrix (role x action x ownership/team): employees only their own tickets; technicians only incidents assigned to them or in their team's unassigned queue; team leads only their team; admins all; **a role may never perform another role's actions** (an `Employee` cannot `resolve`, a `Technician` cannot `verify`/`close`). Remove the "allow general technician inspection" fall-through.
- **S7:** `JwtAuthenticationFilter` must reject disabled/locked users and unknown subjects with 401 (JSON), never 500. **S8:** security headers (CSP, `X-Content-Type-Options`, `Referrer-Policy`, `frame-ancestors 'none'`), Swagger/OpenAPI only in `dev`/`demo`, H2 dependency scoped to test/dev, CORS allow-list.

### Correctness
- **F1 No capacity:** when no technician has capacity, `createIncident` saves the incident as `CLASSIFIED`, unassigned, emits an alert event to admins, and a scheduled `UnassignedIncidentDispatcher` retries assignment (idempotent). Never fall back to "all technicians". Tests: all technicians full -> ticket exists, unassigned, admin notified; free one slot -> next dispatcher tick assigns it to the highest-priority waiting ticket.
- **F2 Service matching:** `ServiceMatcher` returns `Optional.empty()` when nothing matches; seed a "General IT / Unclassified" service (criticality 1, owned by the Service Desk team); the "not working" ticket must land there, not in a random service. Remove the `"SRV-DEFAULT"` string.
- **F5 Triage by confidence:** add `iitsm.classification.min-confidence` (default 0.60). Below it, set `needsTriage`, leave unassigned, show it in the admin triage queue (which now lists both low-confidence incidents and wrong-domain requests via a proper DTO). Admin "accept suggestion" or "reclassify" assigns and audits.
- **F3/F4 Duplicates:** reorder `createIncident`: classify and identify service first, then duplicate detection with a **SQL query** (same service, open statuses, created within the window, limited count) instead of `findAll()`. A duplicate links to the cluster parent, **does not consume technician capacity and is not independently assigned**, shares the parent's assignee and resolution, and is notified when the parent progresses. When a duplicate is linked, **recompute the parent's priority with the true cluster size**, persist a new `PriorityDecision`, emit `PriorityCalculated`, audit it. Make scenario S4 assert that the parent's priority was elevated by `RULE_DUPLICATE_AVALANCHE` after five duplicates (fail the scenario if not).
- **F6 History factor:** count unresolved or reopened tickets from the same reporter on the same service within a configurable window (default 30 days); rename the context field accordingly; update tests and docs.
- **F7 Load consistency:** a single source of truth for technician load (derive from incidents with SQL count, or keep the set and add `@Version` plus a race test). Test: two threads create tickets competing for the last slot; exactly one gets it and the other becomes unassigned (no exception, no double booking).
- **A4 Conflicts:** translate optimistic-lock failures into `ConcurrentModificationException` -> HTTP 409 with a retry hint. Test with two threads acknowledging the same incident.
- **F8 Email:** `spring.mail.*` properties wired with a dev default pointing to Mailpit (`localhost:1025`); HTML + plain-text templates per event; `EmailChannel` throws `NotificationFailureException` on failure and the observer records it without rolling back. Keep a `notification.email.enabled` flag. Test with GreenMail (or equivalent) that an assignment event sends a real SMTP message.
- **F9 Compose:** compose uses profile `demo` (seeded) with Postgres + Mailpit; `docker compose up` then login works for all five demo roles. Add a `docker compose` smoke test script (`scripts/smoke.sh`) that logs in and creates a ticket.

### Architecture
- **A1 Layering:** create domain ports `AuditRepository`, `NotificationRepository`, `EscalationRecordRepository`, `ReassignmentRequestRepository`, `IncidentSearchPort` (with a domain `IncidentSearchCriteria` and a `Page` result). Move all Spring Data/JPA usage behind adapters in `infrastructure`. `application` and `web` must not import anything from `com.iitsm.infrastructure`, except Spring-managed config. `JwtTokenProvider` is reached through a domain/application `TokenService` port. Add **ArchUnit** rules: domain independent of Spring/JPA/application/infrastructure/web; application independent of infrastructure/web; web independent of infrastructure and of domain model types in return values; controllers call services only. Fail the build on violation.
- **A2 Policy design:** see S6.
- **A3 Scale:** replace `findAll()`-based `getAdminStats`, reports and duplicate scan with SQL aggregates/projections and pagination; add an index review for `status`, `priority`, `assignee_id`, `service_id`, `created_at`; add a test that seeds 5,000 incidents and asserts the dashboard and search respond within a stated budget (document the number you measured).

### Docs honesty
- **D1-D4:** rewrite `README.md` truthfully: what the UI is, correct ports, demo accounts and how to get them, real CI badge, JaCoCo numbers produced by CI, and a link to `docs/review/requirements-traceability.md`. Delete the 120/120 self-grade. Move `PROMPT.md` to `docs/dev/`. Add a hand-written classifier test set (~40 tickets, independent of the generator) with accuracy, per-class precision/recall and confusion matrix in `docs/testing/classifier-eval.md`, plus a plain sentence that the training data is synthetic. Fix `security.md`, `03-architecture.md` (event list: add `PriorityOverridden`) and the CI step name (or actually add Checkstyle/Spotless).

---

## 3. Requirements traceability (brief + group PRD)

Create `docs/review/requirements-traceability.md`: one row per brief item and per PRD item, with **evidence** (file, test name, UI screen). Mark anything missing and add it to the batches. Check especially: search with >= 5 criteria reachable from the admin UI; reports incl. technician stage breakdown; structured resolution; feedback; wrong-domain flow; employee never sees priority; employee sees only own tickets; two real notification channels; at least two intelligent features working end to end (duplicates, escalation, technician recommendation, SLA risk, suggested resolution).

---

## 4. Batches

### Batch 0: Reproduce (no fixes)
Build and run everything (`./mvnw clean verify`, run S1-S5). Record real test count, failures, coverage. For each finding in Sections 2 write the failing regression test and show me the failure list. Generate the traceability matrix. **Stop.**

### Batch 1: Stop the bleeding
S1, S2, S3, S4, S5 (server side), F1, F2, F9. Exit: all their regression tests green; app refuses to start with the old secret; `docker compose up` works end to end.

### Batch 2: Make the claims true
A1 (ports + ArchUnit), S6/A2 (policy matrix), S7, S8, F3, F4, F5, F6, F7, A4, A3, F8. Exit: ArchUnit green, authorization matrix green, S4 scenario asserts the avalanche, concurrency tests green.

### Batch 3: Docs truth-up (backend)
D1-D4, traceability, security doc, architecture doc, classifier evaluation, regenerated UML and ER from the real code and schema.

### Batch 4-6: UI
Follow **Stages 3, 4, 5 of the Redesign Prompt** (frontend foundation, persona experiences, quality/accessibility/E2E), with the amendments below, then **Stage 6** (final truth-up and demo pack).

---

## 5. Amendments to the Redesign Prompt

1. The old SPA (`static/index.html`, `app.js`, `styles.css`) is deleted once the new shell serves traffic; the new build must not use the Tailwind Play CDN or Google Fonts CDN. **Self-host fonts and compile Tailwind** so the demo works offline.
2. Login page + registration page are real; the demo-accounts panel is driven by `/api/public/config`; tokens live in memory with the refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie.
3. Add APIs the UI needs: `allowedActions` on every incident view, triage queue DTOs, service-status endpoint (service name + status only), admin-unlock, designation management, simulation controls (demo only).
4. Add Playwright E2E that **fail on regressions of this review**: employee network traffic and DOM never contain `priority`/`sla`/`internalNotes`; a technician cannot open another technician's ticket; six bad logins lock the account; the "not working" ticket lands in "General IT / Unclassified" and goes to triage.
5. The visual QA loop stays: screenshot each screen at 1440x900 and 390x844, critique, fix, re-shoot; save finals to `docs/screenshots/`.

---

## 6. Definition of done

- Every finding in the Review is closed with a regression test that fails on the old code, or has a documented, evidence-backed rebuttal.
- ArchUnit enforces the layering claims; no controller returns a domain/JPA object; no response contains a password hash.
- No default secret, no hard-coded credentials in client or server; lockout works over HTTP; registration cannot choose a designation.
- Creating a ticket never fails because of capacity, matching or duplicate logic; unassigned and low-confidence tickets are visible in the admin triage queue and get dispatched later.
- README, security doc and architecture doc contain only claims backed by a command or test.
- The new UI satisfies the Redesign Prompt's Definition of Done and the three demo workflows run from `docker compose up` in one take.

**Begin with Batch 0 now: reproduce the findings as failing tests and report the failure list. Do not change production code in Batch 0. Then stop and wait for my "proceed to Batch 1".**
