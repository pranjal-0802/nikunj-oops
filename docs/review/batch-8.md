# Batch 8 Report: Triage Queue, Team Scoping, Token Revocation & SLA Correctness

## 1. Summary of Changes
Batch 8 resolves Round 2 code review findings **N1, N2, N3, and N5** test-first on runtime paths.

### N1: Unified Triage Queue & Wrong-Domain Routing
- Added database migration `V2__add_triage_and_exclusions.sql` adding `needs_triage` flag and `incident_excluded_technicians` table to `incidents`.
- Updated `Incident.java` with `needsTriage` and `excludedTechnicianIds`.
- Updated `IncidentService.createIncident` to set `incident.setNeedsTriage(true)` whenever ML classification confidence is below minimum threshold (0.60).
- Updated `UnassignedIncidentDispatcher.dispatchWaitingIncidents` to skip any incident with `isNeedsTriage() == true` and filter out any excluded technician IDs.
- Implemented unified triage queue endpoint `GET /api/admin/triage-queue` returning `List<TriageQueueItemDto>` combining low-confidence incidents and pending wrong-domain reassignment requests.
- Implemented `POST /api/admin/triage/{id}/accept-suggestion` to accept category/service reclassifications, clear triage, and assign a specialist.

### N2: Team Scoping & IncidentAction Enum
- Added `resolveOwningTeamId(incident)` resolving the support team ID from the matched IT Service.
- Updated all policy checks in `IncidentLifecycleService` to pass `owningTeamId` and use the strongly-typed `IncidentAction` enum instead of raw strings.
- Added team queue endpoint `GET /api/technician/team-queue` allowing Team Leads to oversee all tickets belonging to their team.
- Enforced role x action x team scoping matrix verified via parameterized tests.

### N3: Token Revocation & Secret Hardening
- Implemented `tokenVersion` on `User.java` and `UserJpaEntity`.
- Added custom Spring Security `IitsmUserDetails` carrying `tokenVersion`.
- Updated `JwtTokenProvider` to embed `tokenVersion` claim into JWT tokens.
- Updated `JwtAuthenticationFilter` to extract `tokenVersion` and reject any token where version is mismatched (invalidating tokens on logout, deactivation, or password change).
- Hardened startup validation: rejects startup if `JWT_SECRET` is missing, shorter than 32 bytes, or equals the committed default secret in production.
- Extended token session duration to 120 minutes (7,200,000 ms) in `demo` profile to prevent session expiration during 15–20 minute demo recordings.

### N5: SLA Tracking & Assignment Correctness
- Updated `Incident.updatePriorityDecision` and `SlaTracker.updatePolicy` to strictly preserve `respondedAt`, paused duration, breach/warning flags, and escalation history when priority changes (e.g. via Avalanche rule).
- Verified concurrent technician assignment respects capacity using isolated sub-transactions (`TechnicianAssignmentService` with `REQUIRES_NEW`).

---

## 2. Tests Added & Measured Metrics
All regressions verified test-first in `com.iitsm.regression.Round3RegressionTest`:
1. `N1_lowConfidenceTicket_skipsDispatcher_staysInTriage`
2. `N1_wrongDomainTicket_honorsExclusions_neverReassignedToFlaggingTech`
3. `N1_unifiedTriageQueue_and_acceptSuggestion`
4. `N2_technicianFromOtherTeam_forbiddenFromViewingOrActing`
5. `N2_teamLead_canViewAndActOnTeamTickets`
6. `N2_roleActionMatrix_enforcesCorrectPermissions` (6 parameterized cases)
7. `N3_revokedTokenWithOldTokenVersion_rejectedWith401`
8. `N3_startupFailsWithoutJwtSecretInProd`
9. `N5_priorityIncrease_preservesRespondedAtAndSlaTracker`
10. `N5_concurrentAssignment_respectsCapacity`

Suite Status: **100% Green (0 failures, 0 errors, 0 skipped)**.
