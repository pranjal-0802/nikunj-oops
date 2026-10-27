# I-ITSM Code Review, Round 2 (updated repo `nikunj-oops-main (1)`)

**What I did:** diffed the new zip against the first one (274 Java files, was 246), then re-traced every Round 1 finding through the code. **I could not build or run anything** (Maven Central is unreachable from my sandbox), so test counts, coverage and "PASSED" claims in `docs/review/batch-*.md` are *unverified*. "Confirmed" below means I traced it in the source.

**Headline:** real, substantial progress. Most Round 1 security and layering findings are genuinely fixed. But the fixes introduced or left **four problems that matter for the demo and the viva**, and several documents now claim more than the code does.

---

## 1. Closed (verified in the code)

| Round 1 | Status | Evidence |
|---|---|---|
| S2 lockout never persisted | **Fixed** | `LoginAttemptService` uses `REQUIRES_NEW`; `UserService.authenticate` returns one generic "Invalid credentials" for unknown, deactivated, locked and wrong password |
| S3 self-chosen designation | **Fixed** | `AuthController.register` hard-codes `ASSOCIATE` |
| S4 password hash leak | **Fixed** | `TechnicianSummaryDto`; roster no longer returns the domain object |
| S6 access policy | **Fixed in design** | `User.canViewIncident/canExecuteAction` are abstract and overridden per role (real polymorphism); `IncidentAction` enum added. See N2 for the wiring bug |
| S7 JWT filter | **Fixed** | rejects disabled/locked users and bad tokens with 401 JSON |
| A1 layering | **Fixed** | no `import com.iitsm.infrastructure` remains in `application` or `web`; ports (`AuditRepository`, `TokenPort`, `IncidentSearchPort`, ...) added; 4 ArchUnit rules exist |
| F1 ticket lost at full capacity | **Fixed** | filtered by `hasCapacity`, ticket stays `CLASSIFIED` unassigned, `UnassignedIncidentDispatcher` added |
| F2 random service | **Mostly fixed** | `SRV-GEN-001 "General IT / Unclassified"` seeded and used as fallback |
| F3/F4 duplicates | **Mostly fixed** | SQL-scoped candidate query; duplicates inherit the parent's assignee without consuming capacity; parent's priority is recomputed with cluster size |
| F6 history factor | **Fixed** | `countRecentUnresolvedTicketsOnService` |
| F7 technician race | **Fixed in design** | `@Version` on the user entity (carried by `UserMapper`), assignment in a sub-transaction (`TechnicianAssignmentService`) |
| F8/F9 | **Fixed** | `spring.mail.host` default, compose uses the `demo` profile |
| UI | **Improved** | real login page, `/api/public/config` drives the demo-accounts panel, no CDN (works offline), CSP and frame/referrer headers |

---

## 2. Still open or newly introduced (ranked)

### High

**N1. The new dispatcher silently defeats human triage (and the wrong-domain flow).**
`UnassignedIncidentDispatcher.dispatchWaitingIncidents` runs every 30 s and assigns **every** `CLASSIFIED` ticket with no assignee. There is no persisted `needsTriage` flag anywhere on `Incident`, so a low-confidence ticket that `createIncident` deliberately left unassigned is auto-assigned within 30 seconds anyway. A ticket returned by a technician's wrong-domain flag is also re-dispatched with an **empty exclusion set**, so it can go straight back to the technician who flagged it. The `/triage-queue` endpoint still lists only `ReassignmentRequest`s. The F5 test passes because it checks the state immediately after creation, before the dispatcher ever runs, and there is **no dispatcher test at all**.
*Fix:* persist `needsTriage` (and the exclusion set) on the incident; dispatcher skips triage tickets and honours exclusions; triage queue lists both kinds with a proper DTO; test: create low-confidence ticket, run `dispatchWaitingIncidents()`, assert still unassigned; flag wrong domain, dispatch, assert a different technician.

**N2. Team scoping is never wired, so team leads cannot see their team's tickets.**
Every call in `IncidentService` is `enforceCanViewIncident(actor, incident)` or `enforceCanExecuteAction(actor, incident, "resolve")` (the string overload), and both overloads pass `owningTeamId = null`. `TeamLead.canViewIncident` and `Technician.canViewIncident` compare `supportTeamId` with that null, so the team branches are dead code. Result: a team lead can only see tickets assigned to themselves, and technicians cannot see their team's unassigned queue. The README's "Team Lead oversees team queue" is not true in the code. Stringly-typed actions (`"acknowledge"`) also remain.
*Fix:* resolve the incident's owning team from its service inside the policy call; use `IncidentAction` everywhere; add the role x action x team matrix test the prompt asked for.

**N3. The JWT secret is still committed, and sessions die after 15 minutes.**
`application.yml` still ships `${JWT_SECRET:404E6352...}` as the default, the startup check only blocks that value under the `prod` profile, and `docker-compose.yml` hard-codes another secret (`JWT_SECRET: nimbus-finserve-...-production-grade`) under the `demo` profile. Anyone with the public repo can forge an admin token against any dev or compose deployment. The `tokenVersion` claim is always `1` and is never checked, so there is no revocation. There is no refresh token, so after 15 minutes the UI shows "Session expired" (`app.js` 401 handler), which will interrupt a 15-20 minute demo recording.
*Fix:* no default secret outside tests (compose reads a `.env`/generated value); real `tokenVersion` check; refresh endpoint with an HttpOnly cookie, or a longer expiry for the `demo` profile only.

**N4. Documentation now contradicts itself and the code.**
- `README.md` has the new truthful numbers at the top (186 tests, 85% coverage) **and the old section 7 "Grading Rubric Self-Assessment" still at the bottom** (120/120, "165 tests", "86.61%", wrong rubric). It also still says port 8080 in one place and 8085 elsewhere.
- `docs/review/batch-5.md` lists tests that I could not find: no `F9_...`, `A2_...` or `A3_...` test exists; there is no `ArchitectureTest` and no `domainMustBePure` rule (the only ArchUnit file is `regression/ArchitectureRegressionTest` with four `A1_` rules). So **"domain stays pure" is true today but not enforced by any test**. Finding ids in the batch reports also drift from the Review's ids (F3-F9 are relabelled).
- `requirements-traceability.md` (110 lines) contains no MISSING/PARTIAL rows at all, which is not credible given N1/N2.
- `PROMPT.md` is still in the repo root.
An evaluator who greps for any of these will find them in a minute.

### Medium

**N5. Re-prioritising a ticket rebuilds its SLA tracker from scratch.** `Incident.updatePriorityDecision` does `SlaTracker.create(createdAt, policy)`, discarding `respondedAt`, paused time and breach state. A parent that was already acknowledged and is then bumped by the avalanche rule can be treated as never responded and trigger a false response breach or escalation. The dispatcher also assigns via `strategy.recommend` + `tech.assignIncident` directly, not through the new REQUIRES_NEW `TechnicianAssignmentService`, so it can race with ticket creation.

**N6. `findAll()` scaling debt remains (A3 not done).** `IncidentService.getAdminStats` (line ~331), five places in `ReportingService`, `AuditService.findAll`, and a fallback `findAll()` in `DuplicateDetectionService` still load whole tables.

**N7. Smaller security leftovers.** Lockout is still permanent with no rate limit or time-based unlock, and unknown-email requests skip BCrypt, so response time still reveals which emails exist. Swagger and `/v3/api-docs` are public in all profiles. The token lives in `sessionStorage` and `demoPassword: 'Password123!'` is still a string in `app.js` (only used when demo mode is on, but `demo-mode: true` is the default in the base config).

**N8. `IncidentService` is a 627-line god class** (intake pipeline, lifecycle, admin operations, dashboards). `createIncident` alone is ~90 lines mixing classification, service matching, duplicates, priority, SLA, assignment and events. Evaluators grade OOP design, so split it (for example an `IncidentIntakePipeline` of steps, `IncidentLifecycleService`, `AdminIncidentService`).

### Low
- UI is still hand-written vanilla JS: `app.js` 1,764 lines, `index.html` 945, `styles.css` 1,091, no modules, no frontend tests, no screenshots in `docs/`. I could not see it rendered, so I can't judge the visual quality; the structure is the risk.
- I count 170 test methods (`@Test` + `@ParameterizedTest`) against the claimed 186; parameterized expansion could explain it, but it is unverified.
- `getAdminStats`/reports compute in memory; `IncidentAction.fromString` keeps strings alive.

---

## 3. What I would do in the next 3 days

1. **N1 + N2** (a day): they are functional bugs in flows you will demo (triage, wrong domain, team lead).
2. **N3** (half a day): secret handling and session length, so the recording does not break mid-demo.
3. **N4** (half a day): delete the stale README section, correct `batch-5.md`, add the missing ArchUnit rule for the domain, regenerate the traceability matrix honestly.
4. Then N5, N8, N6.
5. **UI decision:** with about 26 days left, do not rewrite in React unless one teammate owns it end to end. The cheaper path is to modularise `app.js` into ES modules per persona, add Playwright E2E for the three workflows plus an `axe` check, and spend time on visual polish and empty/loading/error states.

