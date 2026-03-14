# Rich Profiles — Retrospective

**Date:** 2026-03-14
**Linear ticket:** LBO-7 — Personal Profiles
**PRs:** #171–#183 (13-PR stack)

---

## Build Summary

12 implementation PRs + 1 plan PR built across the full stack, adding 3 profile capabilities:
- **Dietary Restrictions** — multi-select from 8 values, junction table
- **Experience Level** — single-select from 4 values
- **Customized Avatar** — deterministic SHA-256 procedural generation, new `libs/avatar-generator` module
- **Profile Completed flag** — one-way boolean for first-time setup modal
- **Plan member avatar enrichment** — service-layer enrichment, zero new client methods

Test coverage: 24 client integration tests, 20 service unit tests, 43 acceptance tests.

---

## Issues Caught by Reviewers

### 1. Fake/JDBI Behavioral Divergence (test-reviewer)
**Problem:** `FakeUserClient.getDietaryRestrictions()` returned `NotFoundError` for nonexistent users, but the real JDBI client returned an empty list. This divergence could cause service-level tests using the fake to mask production bugs.
**Fix:** Updated FakeUserClient to return `success(emptyList())` for nonexistent users, matching JDBI behavior.
**Takeaway:** Fakes must match real client behavior exactly, including edge cases for nonexistent entities. Consider adding divergence checks to the test review checklist.

### 2. False-Positive Test (test-reviewer)
**Problem:** The invite-flow avatar seed test created a stub user via the service (which generated an email-based seed), then claimed to test the "null seed" branch. The `avatarSeed == null` condition was never actually exercised.
**Fix:** Used `fakeUserClient.seed()` to create a stub with explicitly null `avatarSeed`, properly exercising the null branch.
**Takeaway:** When testing conditional branches, verify the test actually reaches the branch. A test that passes trivially without exercising the target code is worse than no test — it gives false confidence.

### 3. Weak Test Assertions (test-reviewer)
**Problem:** `getAvatar` test asserted `isNotEmpty()` on avatar fields instead of specific expected values. Would pass even if the wrong avatar was generated.
**Fix:** Computed expected avatar via `AvatarGenerator.generate()` and asserted exact field matches.
**Takeaway:** Prefer specific value assertions over existence checks. `isNotNull()`/`isNotEmpty()` proves the pipeline ran but not that it ran correctly.

### 4. Missing Acceptance Test Coverage (test-reviewer)
**Problem:** Initial acceptance tests covered pre-existing endpoints well but had zero tests for all three new endpoints (`randomize-avatar`, `avatar`, profile update with new fields).
**Fix:** Added 20+ acceptance tests covering new endpoints, error paths, and null-vs-empty dietary restriction semantics.
**Takeaway:** Acceptance test coverage must be reviewed against the API surface — new endpoints need explicit tests, not just extended existing ones.

---

## Workflow Notes

### Worktree Isolation Hiccup
**Problem:** Agents spawned with `isolation: "worktree"` produced changes in temporary worktrees that were cleaned up when the agent completed, losing all work. Had to re-spawn agents without isolation.
**Resolution:** Switched to shared-worktree approach with stash/branch management. This worked reliably but required careful coordination when multiple agents modified files in the same worktree.
**Recommendation:** For future builds, use non-isolated agents and coordinate via branches. Only use worktree isolation for truly independent, read-only research tasks.

### Parallel Contract PRs
**What worked:** DB, client, lib, and service contracts were built in parallel (after the user suggested more parallelization). The agents worked on independent modules and their outputs were committed sequentially in stack order.
**What didn't work as well:** Branch management with multiple agents sharing a worktree required stashing, cherry-picking, and rebasing — error-prone and time-consuming.

### Test Reviewers Add Real Value
All 4 issues above were caught by test reviewers, not by the build or by developers. Two were real correctness bugs (Fake/JDBI divergence, false-positive test) that would have been silent in production. **Never skip review cycles.**

---

## Recommendations

### For Immediate Follow-Up
1. **Backfill avatar seeds for pre-existing users** — Users created before V027 have `avatarSeed = null` and `avatar = null`. A data migration should generate seeds for these users using `seedFromName(username ?: email)`.
2. **Frontend PR** — Profile page (dietary restrictions, experience level, avatar display/randomize), plan member avatars, first-time profile setup modal.

### For Future Builds
1. **Fake divergence testing** — When extending a client interface, explicitly verify that FakeClient edge-case behavior matches the real JDBI implementation (especially for nonexistent entities, empty results, null handling).
2. **Acceptance test completeness** — Review acceptance tests against the full API surface diff, not just the feature description. Every new endpoint and every new field on existing endpoints needs explicit coverage.
3. **Agent coordination** — Prefer sequential agent spawning for code that shares files. Parallel agents work best for independent modules (e.g., libs vs clients vs DB).
