# Claude Code Prompt: Audit and Redesign

This document captures the earlier Redesign Prompt reference for the I-ITSM project.
As specified in `CLAUDE_CODE_PROMPT_V2.md`:
- Section 2 and Stages 0-2 are superseded by `CLAUDE_CODE_PROMPT_V2.md` and `IITSM_CODE_REVIEW.md`.
- Stages 3-6 (Frontend Foundation, Persona UIs, Quality, Truth-up) remain active and apply to Batches 4-6, with the amendments specified in Section 5 of `CLAUDE_CODE_PROMPT_V2.md`.

---

## Stage 3: Frontend Foundation
- Offline-ready asset pipeline (no external CDN dependencies at demo time).
- Self-hosted fonts (`Inter`, `JetBrains Mono`).
- Local CSS compilation / bundled styles without Play CDN.
- Design tokens: consistent color system (slate dark mode, status/priority indicators), accessible contrast ratios (WCAG AA).
- Authentication flow: dedicated login and registration screens, session handling with secure tokens, public config endpoint (`/api/public/config`) for conditional demo account display.

---

## Stage 4: Persona Experiences
- **Employee Portal**: Streamlined 2-field intake form (Title, Description), real-time domain prediction chip, 5-stage visual lifecycle stepper, contextual actions (clarification reply, 1-5★ verification with reopen option). Strict privacy guarantee: no priority or internal notes rendered in DOM or network payload.
- **Technician Console**: Prioritized work queue, SLA deadline indicators with color-coded risk states, action modals for acknowledge, investigation, notes, information request (pausing SLA), wrong-domain return, and structured ITIL resolution.
- **Admin Command Centre**: Master incident directory with 5+ criteria search and filters, Triage & Reassignment Queue for low-confidence or wrong-domain tickets, dynamic assignment strategy switcher (`ROUND_ROBIN`, `LEAST_LOADED`, `WEIGHTED_COMPOSITE`).
- **Simulation Hub**: Virtual clock controls, speed multiplier toggles, discrete fast-forward jumps, scenario runners S1-S5 with monospace audit log streaming.
- **Executive Reports**: MTTR, MTTResp, SLA compliance %, team capacity gauges, service volume analysis, and escalation audit log.

---

## Stage 5: Quality, Accessibility, and End-to-End Testing
- Comprehensive end-to-end regression tests verifying that persona boundaries and invariants hold.
- Accessibility audit for WCAG AA compliance (keyboard navigability, focus management, ARIA attributes).
- Responsive layout verification at standard desktop (1440x900) and mobile (390x844) viewport dimensions.

---

## Stage 6: Final Truth-Up and Demo Pack
- Updated documentation matching actual measured metrics (real test counts, real JaCoCo instruction and line coverage).
- Verified `docker-compose.yml` environment with seeded demo accounts ready for end-to-end evaluation.
- Clean conventional git commit history.

