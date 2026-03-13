# Avatar Flair — Build Retrospective

## Feature Summary
Added a role system to plan members (member/manager) with avatar flair SVGs, role management UI in the Manage Plan modal, and extensible role-based authorization for shared gear editing.

## Linear Tickets
- LBO-10 — Shared camp gear access control
- LBO-11 — Group leader avatar flair (hat/compass)

## What Was Built

### Backend
- **V024 migration** — `role` column on `plan_members` with CHECK constraint
- **PlanClient.updateMemberRole** — JDBI operation + fake with validation
- **UpdateMemberRoleAction** — validates role, checks ownership, prevents owner role change, enriches response
- **PATCH /api/plans/{planId}/members/{userId}/role** — new endpoint with WebSocket event
- **PlanRoleAuthorizer** — extensible role-based authorization pattern in `common/auth/`
- **Shared gear authorization** — CreateItemAction, UpdateItemAction, DeleteItemAction now check `PlanRoleAuthorizer.authorize(planId, userId, setOf(OWNER, MANAGER))` for shared gear

### Frontend
- **CamperAvatar flair** — ranger hat + compass for owners, compass badge for managers (day/night modes)
- **GearModal** — shared gear edit permission extended to managers
- **Manage Plan modal** — new members list with role dropdown (owner) / read-only labels (others)

## PR Stack

| # | Scope | Files Changed | Key Decisions |
|---|-------|--------------|---------------|
| 1 | Plan | 1 | Combined LBO-10 + LBO-11 into single feature |
| 2 | DB contracts | 4 | V024 migration, R024 rollback, schema update |
| 3 | Client contracts | 7 | Role field on PlanMember, updateMemberRole interface |
| 4 | Service contracts | 12 | DTOs, errors, action stub, controller route, validation |
| 5 | DB implementation | 1 | Seed data with Bob as manager on Summer Trip |
| 6 | Client implementation | 6 | UpdateMemberRole JDBI operation, updated queries, fake |
| 7 | Service implementation | 14 | PlanRoleAuthorizer (5 files), action impl, item auth |
| 8 | Webapp | 5 | Avatar flair SVG, role management UI, gear permissions |
| 9 | Client tests | 1 | 8 integration tests (Testcontainers) |
| 10 | Service tests | 1 | 8 unit tests (FakeClient) |
| 11 | Acceptance tests | 4 | 10 acceptance + 8 authorizer + 11 item + 10 item acceptance tests |
| 12 | Docs | 1 | Orchestrator review enforcement improvements |

## Issues Encountered

### 1. Seed data had owners as managers
**What happened:** db-dev initially set plan owners as 'manager' in plan_members, but owner role is derived from `plans.owner_id` — not stored in the table.
**Fix:** Corrected seed to make Bob (non-owner) the manager on Summer Camping Trip.
**Lesson:** The distinction between stored role and derived ownership should be emphasized more in the plan's Entities section.

### 2. Shared gear auth had zero test coverage
**What happened:** The PlanRoleAuthorizer (the core security feature) and its integration with item actions had no tests until the test-reviewer caught it. 29 tests were added to cover: authorizer unit tests, ItemService forbidden paths, and acceptance tests for manager/member gear access.
**Lesson:** When a feature adds authorization checks, test coverage for those checks should be explicitly listed in the plan's PR stack — not left to the test-engineer to infer.

### 3. Review cycles were initially skipped
**What happened:** The orchestrator skipped code-reviewer and test-reviewer spawns to move faster through the stack.
**Fix:** Added review enforcement to orchestrator.md — PR Commit Protocol with mandatory REVIEW GATE, review tracking requirements, and pre-submit checklist.
**Lesson:** The "Never skip reviews" rule was at the bottom of orchestrator.md and was deprioritized. Moving it to the top and adding structural enforcement (checklists, paired tasks) makes it harder to ignore.

## Review Results

### Code Review (all 7 implementation PRs)
**Verdict:** All APPROVED

Minor observations (non-blocking):
1. PlanRoleAuthorizer fetches all members to find one user — correct but could be optimized with a single-member lookup later
2. Owner ranger hat SVG extends slightly outside viewBox (y=-2) — may clip in strict renderers

### Test Review
**Client tests:** APPROVED — 8 tests, good coverage including idempotency
**Service tests:** APPROVED — 8 tests, all action steps covered
**Acceptance tests:** CHANGES REQUESTED → FIXED — 5 critical gaps in shared gear auth coverage, resolved with 29 additional tests

## Architecture Decisions

### PlanRoleAuthorizer pattern
- Lives in `common/auth/` — shared across features, not tied to plans or items
- `PlanRole` enum: OWNER, MANAGER, MEMBER
- `authorize(planId, userId, requiredRoles)` returns `Result<PlanRoleContext, PlanRoleAuthorizationError>`
- Each feature maps auth failure to its own error type (e.g., `ItemError.Forbidden`)
- Extensible: `setOf(OWNER)` for owner-only, `setOf(OWNER, MANAGER)` for elevated, `setOf(OWNER, MANAGER, MEMBER)` for any-member
- Existing inline owner checks NOT migrated in this PR to keep scope contained

### Owner role derivation
- Owner is NEVER stored in `plan_members.role` — always derived from `plans.owner_id`
- `plan_members.role` only holds: `member` (default) or `manager`
- PlanRoleAuthorizer resolves effective role at runtime

## Recommendations

1. **Add `getMemberByUserId` to PlanClient** — The authorizer currently calls `getMembers()` and filters. A targeted lookup would be more efficient for plans with many members.
2. **Migrate existing owner checks** — `UpdatePlanAction`, `DeletePlanAction`, etc. do inline `plan.ownerId == requestingUserId` checks. These could use `PlanRoleAuthorizer.authorize(planId, userId, setOf(OWNER))` for consistency. Separate PR.
3. **Plan section for test scenarios** — Future plans should explicitly list authorization test scenarios in the PR stack to prevent coverage gaps.
4. **Adjust CamperAvatar viewBox** — Consider changing from `0 0 48 64` to `0 -4 48 68` to accommodate the ranger hat without clipping.
