# Claude Code Prompt, Round 3: Close the Remaining Findings, Harden the Demo, Polish the UI

> Put `IITSM_CODE_REVIEW_ROUND2.md` in `docs/dev/` and commit it. Paste this file as your first message. It continues the earlier batches (0-6); do not redo them. "**Round 2**" below means that review file; ids `N1`...`N8` come from it.

---

## 0. Rules

2. **Test-first, and tests must exercise the real path.** The previous round passed a test (F5) that checked state *before* the scheduled dispatcher ran. For every finding, write a failing test that reproduces the problem **through the same code path the app uses at runtime** (call the scheduler method, go through MockMvc and the real security filter chain, use the real service graph). Show the failing output, fix, show green.
3. **Honesty over green.** Every number or claim you write in a doc must come from a command you ran. If a statement in `docs/review/batch-*.md`, `README.md` or `requirements-traceability.md` is false or unverifiable, correct it and say so in the batch report. Use the Round 2 finding ids (do not relabel them).
4. Keep the OOP strengths: pure `domain` (no Spring/JPA), State, Strategy, Chain of Responsibility, Observer, Factory, Repository, polymorphic `User.canView/canExecute`. Record design trade-offs in `docs/DECISIONS.md`.
5. End of each batch: `./mvnw clean verify` green with the **real** test count and coverage in `docs/review/batch-N.md`, small conventional commits, stop and wait. Never commit secrets; never rewrite history; do not invent teammate names.

---

## 1. Findings to fix (acceptance criteria)

### N1: Triage and wrong-domain flow vs the dispatcher
- Add to `Incident`: `needsTriage` (boolean, persisted via Flyway migration + mapper) and `excludedTechnicianIds` (set, persisted). Set `needsTriage=true` when classification confidence is below `iitsm.classification.min-confidence` (single source of truth; delete the duplicate constant `ClassificationResult.TRIAGE_THRESHOLD` or derive it from config). Set the exclusion when a technician flags wrong domain.
- `UnassignedIncidentDispatcher` must skip `needsTriage` incidents, honour `excludedTechnicianIds`, assign through `TechnicianAssignmentService` (the same REQUIRES_NEW path creation uses, not `strategy.recommend` + `assignIncident` directly), and not assign duplicates independently (duplicates follow their parent).
- `/api/admin/triage-queue` returns one DTO list containing **both** low-confidence incidents and pending wrong-domain requests, with the classifier's top-3 suggestions and confidence. Admin actions: accept suggestion, reclassify, assign; each clears `needsTriage` and is audited.
- Tests: (a) create a low-confidence ticket, call `dispatchWaitingIncidents()`, assert still unassigned and present in the triage queue; (b) technician flags wrong domain, run dispatcher with free capacity, assert it goes to a different technician or stays in triage, never back to the flagging technician; (c) admin accepts a suggestion, dispatcher or direct assignment follows.

### N2: Team scoping and stringly-typed actions
- `IncidentService` (and any other caller) must pass the incident's **owning team** to the policy (resolve it from the incident's service) and use `IncidentAction` instead of strings. Remove or deprecate `IncidentAction.fromString` and the string overloads.
- Behaviour to guarantee and test as a role x action x ownership/team matrix (parameterized): employees only their own tickets; technicians their assigned tickets and unassigned tickets of **their team**; team leads every ticket of their team; admins all; no role may perform another role's actions; a technician from another team gets 403 for both view and act.
- The Team Lead UI tab must actually list the team's tickets (add `GET /api/technician/team-queue` returning a DTO; team-lead only).

### N3: Secrets, sessions and the demo
- Remove the default `JWT_SECRET` from `application.yml`: the app must refuse to start without one outside the `test` profile (keep a test-only constant under `src/test/resources`). Remove the hard-coded secret from `docker-compose.yml`; read it from `.env` (add `.env.example`, git-ignore `.env`) and make `scripts/smoke.sh|bat` generate one if missing. Test: context fails to start with no secret, with a short secret and with the old committed value in **every** profile except `test`.
- Implement a real `tokenVersion`: store it on the user, put it in the token, check it in `JwtAuthenticationFilter`, and bump it on logout, password change, deactivation and admin lock. Test the revocation.
- Sessions: add a refresh flow (rotating refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie, stored hashed server-side) **or**, if you decide that is too large for the deadline, make access-token lifetime configurable per profile (15 min `prod`, 120 min `demo`/`dev`) and have the UI show a "session expires in 2 minutes" toast with a one-click renew. Either way the 20-minute demo must never hit a surprise logout; add an automated test that simulates 30 minutes on `AdjustableClock` and checks that a UI session using the chosen mechanism is still valid in `demo`.
- Lockout: time-based (default 15 min, doubles on repeated lockouts, configurable), admin-unlock endpoint with an audited action, per-IP+email rate limit on `/api/auth/login`, and constant-time behaviour for unknown emails (perform a dummy BCrypt match). Tests via MockMvc through the real filter chain.
- Swagger/OpenAPI only in `dev`/`demo`; `demo-mode` default `false` in the base config and `true` only in `dev`/`demo`; keep the demo password out of client JS by serving the demo accounts (emails only) from `/api/public/config` and letting the login form accept the demo password typed or auto-filled by the server-provided hint only in demo mode.

### N5: SLA and assignment correctness
- `Incident.updatePriorityDecision` must **preserve** `respondedAt`, paused duration, breach/warning flags and escalation history. Rebuild only the deadlines (from the original start, minus pauses) and keep the tracker's recorded events. Tests: acknowledge, then raise priority with the avalanche rule; assert response is still `MET`, no false breach, no duplicate escalation. Also test a bump to a *tighter* SLA that is already past due (it must become `BREACHED` once and be recorded once).
- Race test for dispatcher vs creation competing for the last slot: exactly one wins, the other stays unassigned, no exception, no double booking.

### N6: Remove in-memory full scans
- Replace `findAll()` in `IncidentService.getAdminStats`, all of `ReportingService`, `AuditService` and the `DuplicateDetectionService` fallback with SQL aggregates/projections and pagination (`Page`/`Slice` through the domain `Page` type, not Spring's, at the port boundary). Add a test that seeds 5,000 incidents and asserts the dashboard, search and duplicate check finish within a budget you measure and write down. Review indexes for `status`, `priority`, `assignee_id`, `service_id`, `created_at`, `(service_id, category, created_at)`.

### N8: Break up the god service
- Split `IncidentService` (627 lines) into focused collaborators, keeping behaviour identical: `IncidentIntakePipeline` (ordered `IntakeStep` objects: classify, match service, detect duplicates, prioritise, attach SLA, assign; Chain of Responsibility or Template Method so a new step is one new class), `IncidentLifecycleService` (acknowledge/investigate/resolve/verify/reject/close), `AdminIncidentService` (override, reassign, reclassify, triage). Keep a thin facade only if controllers need it. Add an ArchUnit/size guard (no class over ~300 lines, no method over ~40 lines) as a soft check reported in the batch notes.

### N4: Documentation honesty
- `README.md`: delete section 7 (self-assessment) entirely; one port everywhere (8085) and fix `Dockerfile EXPOSE`; real CI badge; test count and coverage copied from `target/` output; describe the UI as it really is.
- Add the missing ArchUnit rules: `domain` must not depend on `org.springframework..`, `jakarta..`, `application..`, `infrastructure..`, `web..`. Put all ArchUnit rules in `com.iitsm.architecture` and make `batch-5.md` match the files that exist (list real test class + method names via a script, not by hand).
- Move `PROMPT.md` to `docs/dev/`. Regenerate `requirements-traceability.md` with evidence (file, test, UI screen) and honest PARTIAL/MISSING rows. Fix the architecture doc's event list (17 records, include `PriorityOverridden`).

---

## 2. UI work for the vanilla SPA (no framework rewrite)

The current UI is `static/index.html` + `app.js` (1.7k lines) + `styles.css` (1.1k lines). Decide with me before starting Batch 5 whether to keep vanilla (default) or rebuild in React; **assume vanilla** unless I say otherwise.
1. **Modularise:** convert `app.js` to ES modules (`type="module"`): `api.js`, `auth.js`, `router.js`, `store.js`, `ui/` (components: StatusStepper, PriorityBadge, SlaCountdown, Timeline, DataTable, Modal, Toast, Skeleton, EmptyState), `views/employee/`, `views/technician/`, `views/admin/`, `views/simulation/`. No inline event handlers in HTML; no raw `innerHTML` with untrusted data: create a tiny `h()` helper that builds DOM nodes and escapes text by default.
2. **Design system:** CSS variables for colour, spacing, radius, type (self-hosted Inter + JetBrains Mono); light and dark themes; semantic status/priority colours that also use icon + text; 8px grid; consistent card, table, form and button styles.
3. **Persona experiences** exactly as in the earlier redesign spec (`CLAUDE_CODE_PROMPT_AUDIT_AND_REDESIGN.md`, Sections 5.3-5.7): employee (two-field form, service status strip, 5-step stepper, no priority anywhere), technician (queue-first split view, `allowedActions`-driven action bar, structured resolution form, wrong-domain dialog, workload dashboard, Team tab for team leads), admin (KPI cards, directory with the 5+ filters and a row drawer showing priority factor breakdown and assignment ranking, triage queue, technician roster, configuration incl. live strategy switch, reports, audit explorer, simulation panel with clock-speed control).
4. Every data view has loading skeleton, empty, error (with retry) and populated states. Keyboard accessible, visible focus, labelled inputs, `aria-live` for toasts and errors, contrast AA, responsive to 360 px.
5. **Verification:** add Playwright E2E for the three demo workflows and these guards: employee DOM and network responses never contain `priority`, `sla`, `internalNotes`; a technician cannot open another team's ticket; a team lead can open their team's ticket; six bad logins lock the account and the message is identical to a wrong password; a low-confidence ticket shows up in triage and stays there after the dispatcher runs; `axe-core` finds no serious/critical issues on any route. Save screenshots (1440x900 and 390x844) to `docs/screenshots/` and critique them against the spec before finishing each screen.

---

## 3. Batches

### Batch 7: Reproduce N1, N2, N3, N5 as failing runtime-path tests (no production changes)
Write the failing tests listed above, run them, report the failure list. Also run the existing suite and report the real counts and which of the tests named in `batch-5.md` do not exist. **Stop.**

### Batch 8: Fix N1, N2, N3, N5
Make the tests green. Update `docs/security.md` and `docs/03-architecture.md` to match. Exit: triage and wrong-domain demos work end to end with the scheduler running; team lead sees the team; no default secret; demo session cannot expire mid-recording.

### Batch 9: N4, N6, N8
Docs truth-up, ArchUnit domain-purity rule, SQL aggregates and the 5,000-incident benchmark, service split.

### Batch 10: UI modularisation and design system
Sections 2.1-2.2 and the employee portal.

### Batch 11: Technician console, admin command centre, simulation panel
Sections 2.3-2.4, with screenshots.

### Batch 12: E2E, accessibility, demo pack
Playwright suite, axe, Lighthouse notes, `docs/dev/DEMO_SCRIPT.md` (minute-by-minute for a 15-20 minute video, three workflows, a slot for each member to introduce themselves and explain a module), reset instructions, final checklist against the brief and the **22 Nov 2026** deadline.

---

## 4. Definition of done

- Every Round 2 finding is closed with a failing-then-passing regression test that goes through the real runtime path, or has a documented evidence-backed rebuttal.
- Low-confidence and wrong-domain tickets stay in admin triage until a human acts; team leads see and act on their team's tickets; a recording of 20 minutes never hits a session expiry.
- No default or committed secrets anywhere (code, YAML, compose, JS).
- README, security doc, architecture doc and batch reports contain only claims backed by a command you ran; the self-grade is gone.
- The UI is modular, accessible, consistent across personas, covered by Playwright and axe, and the three workflows run from `docker compose up` in one take.
