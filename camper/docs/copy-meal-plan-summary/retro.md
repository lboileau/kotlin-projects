# Retrospective — copy-meal-plan-summary

## Feature Summary

Added a "Copy Summary" button to the `MealPlanModal` Overview view that builds a plain-text summary of the trip's meal plan recipes (one per unique recipe, deduplicated by `recipeId`) and copies it to the user's clipboard. The summary includes recipe names and optional web links formatted as:

```
Recipe Name
https://example.com/recipe

Another Recipe
```

This required a single new field, `recipeWebLink: String?`, on the existing `MealPlanRecipeDetailResponse` DTO — no new entities, no new endpoints, no DB migration. The field flows from the existing `Recipe.webLink` through the service layer via `recipeClient.getById()`.

## PR Stack

1. **Plan** — Architecture design and change surface specification (6 files across backend/frontend, 2 call sites, scope correction about JDBI wiring)
2. **Service DTO + mapper** — Added `recipeWebLink` to `MealPlanRecipeDetailResponse`; wired `recipe.webLink` through `MealPlanDetailBuilder` and `AddRecipeToMealAction`; updated `MealPlanFixture.insertRecipe` fixture signature
3. **Webapp** — Mirrored TS interface; created pure `buildMealPlanSummary` function; added Copy Summary button in Overview view with Copied!/Copy failed feedback states
4. **Unit tests** — Added vitest scaffolding (`vitest.config.ts`, package.json updates); created `mealPlanSummary.test.ts` with 8 test cases covering dedup, ordering, link/no-link, empty
5. **Acceptance tests** — Added test in `MealPlanAcceptanceTest` verifying `GET /api/meal-plans/{id}` returns `recipeWebLink` for recipes with and without links
6. **Documentation** — Updated `services/camper-service/CLAUDE.md` and `webapp/CLAUDE.md` to document the new field in `MealPlanRecipeDetailResponse`

## What Went Well

### Execution
- **Zero review cycles.** All four review passes (code-reviewer PR 2, code-reviewer PR 3, test-reviewer PR 4, test-reviewer PR 5) returned APPROVED on first submission. No rework needed.
- **Plan accuracy.** The architect's line number predictions were accurate to within 1-2 lines. File list was complete; no surprises during implementation.
- **Tight scope.** Feature was genuinely small — ~50 non-test production lines of code + 130 lines of test — which made review fast and risk low.
- **Fixture hygiene.** The fixture parameter-binding order was correctly anticipated in the plan (noting that `web_link` was a hardcoded NULL before, not a parameter), allowing the test-engineer to extend it cleanly.

### Team Coordination
- **Scope correction preempted confusion.** The plan's note that "no meal-plan-client changes are needed" — explaining why the handoff was wrong about JDBI wiring — prevented implementation guesswork.
- **Single-PR decision was sound.** Collapsing the handoff's separate "contracts" and "impl" PRs into one avoided introducing an intermediate regression that the second PR would have to fix.
- **Shared builder centralization.** Discovering that `MealPlanRecipeDetailResponse` is constructed in exactly 2 places via the centralized `MealPlanDetailBuilder` made tracing the required changes trivial.

## What Surprised Us

### Technical Surprises
1. **Incorrect handoff claim.** The handoff stated "wire it from the joined `recipes.web_link` column in the meal-plan-client JDBI read path," which was wrong. The data actually flows via `recipeClient.getById()` (already called at both construction sites), so no meal-plan-client changes were needed. The plan corrected this. Lesson: handoff authors should grep-verify data-flow claims before writing them.
2. **Webapp framework gap.** The webapp had no test runner configured (no `vitest`, `jest`, or `.test.*` files). Adding vitest was a pre-condition for PR 4. The architect's plan correctly identified this as an open question but set a default. Test-reviewer confirmed the setup was correct.
3. **TypeScript verbatimModuleSyntax strictness.** During webapp build, `import { MealPlanDetailResponse }` (non-type form) failed with TS1484 when running `npm run build` (which invokes `tsc -b`), but the same import passed `npx tsc --noEmit`. This means the two type-checking gates enforce different rules. The test-engineer discovered this during the first build attempt.
4. **Node modules not pre-installed in worktree.** Running `npm install` was required before running webapp tests/build in the fresh worktree. Plan didn't flag this.

### Process Surprises
- **Handoff inconsistency on disabled vs hidden.** The handoff said both "disabled" and "hidden" in different sentences for the zero-recipes case. The plan defaulted to "hidden" (cleaner UX). No one disagreed, and implementation proceeded.
- **Test tsconfig coupling.** New test files are picked up by the app's single `tsconfig.json` by default, so a type mismatch in a test helper (e.g., using wrong fixture field names) fails the production build. This is expected for minimal setup but worth documenting.

## Plan vs Reality

**Predictions that held:**
- Both call-site updates required (`MealPlanDetailBuilder.buildRecipeDetail` line ~141, `AddRecipeToMealAction` line ~95) ✅
- `recipe.webLink` in scope at both sites ✅
- Only two constructor call sites exist (grepped) ✅
- `MealPlanFixture.insertRecipe` needed a signature bump with an optional `webLink` parameter ✅
- No meal-plan-client changes required ✅

**Predictions that diverged:**
- Handoff claimed JDBI join wiring was needed ❌ (corrected by plan before implementation)
- Plan rated scope as "tiny"; actual diff was even smaller (no client changes, no new endpoints) ✅

## Recommendations for the System

### Skill Updates (web-manager / webapp CLAUDE.md)

1. **Add vitest scaffolding guidance.** Webapp lacks test infrastructure. Future features that need unit tests should reference vitest setup steps (or defer to separate "add testing" PR).
2. **Clarify canonical type-check gate.** Document that `npm run build` (not `npx tsc --noEmit`) is the authoritative type-check gate for this project because `verbatimModuleSyntax` is enabled and `tsc -b` enforces it, whereas `--noEmit` does not.
3. **Fresh-worktree npm install requirement.** Flag that new worktrees may need `npm install` before running tests/build commands.

### Handoff Authoring (orchestrator / team lead guidance)

1. **Grep-verify data-flow claims.** Before writing "wire it from X column in Y client," verify the claim by searching for actual read paths in the codebase. Handoff authors should include grep output or at least confirm the path exists.
2. **Enumerate service layer vs. client layer clearly.** Handoff was ambiguous about whether data flows through JDBI (client layer) or service layer composition. Clarify which client methods are called and which data is already fetched.

### Plan Authoring (architect feedback)

1. **Continue including "what we decided NOT to do and why" sections.** The plan's note explaining why no meal-plan-client changes were needed preempted confusion and demonstrated architectural understanding. This is valuable.
2. **Call out framework/infrastructure gaps upfront.** The vitest gap should have been surfaced at the plan stage with a decision (add or skip). The plan did flag it as an "open question," which is good.

### Nice-to-haves (non-blocking)

1. **Consider adding `getMealPlanDetail` test helper.** The pattern `exchange(...).body.to(MealPlanDetailResponse::class.java)` repeats ~10 times across acceptance tests. A helper would improve readability (e.g., `getMealPlanDetail(planId)`).
2. **Consider `tsconfig.test.json` if tests grow.** If webapp tests expand significantly, separating test config from app config prevents accidental coupling (type errors in tests breaking prod build).

## Metrics

- **PRs:** 6 (plan, service DTO, webapp, unit tests, acceptance tests, docs)
- **Production code changed:** ~50 lines (2 field passes + 1 fixture parameter + 1 DTO field)
- **Test code added:** ~130 lines (8 unit tests + 1 acceptance test)
- **Dependencies added:** `vitest@^3.2`, `@vitest/ui` (optional)
- **Review cycles:** 0 (all PRs approved first submission)
- **Build status:** ✅ Full `./gradlew clean build` passes (50s). Webapp: 8/8 unit tests pass, `npm run build` clean.
- **Time to ship:** 6 PRs, 1 review round each, no rework

## Phase 10 TODO List (Retro Follow-up PR)

The Phase 10 retro follow-up PR should implement the following skill updates:

1. **`/web-manager` skill**
   - Add section: "Testing — Unit Tests with Vitest"
     - Link to webapp `package.json` vitest + config example
     - Note: `npm run build` is the canonical type-check gate (not `npx tsc --noEmit`)
     - Flag: fresh worktrees may need `npm install` before running test/build
   - Expand "Conventions" to mention vitest as the testing framework (even though webapp didn't have it before)

2. **`webapp/CLAUDE.md`** (in the project itself)
   - Add new "Testing" section under "Running"
     - Clarify `npm run build` is the canonical gate
     - Document vitest as the test runner
     - Note `environment: 'node'` is appropriate for pure functions

3. **Handoff authoring guidance** (if maintained in `.claude/skills/build-feature/`)
   - Add requirement: "Grep-verify any data-flow claims (e.g., 'wire from X column in Y client') before including them"
   - Example: show the grep output and confirm the read path exists

4. **Architect plan authoring guidance** (if maintained)
   - Reinforce: always include "what we decided NOT to do and why" sections when constraining scope
   - Flag infrastructure gaps (missing test framework, etc.) as explicit decisions in the plan, not open questions left to implementers

These updates are bundled in a single "skill & docs" PR per usual Phase 10 pattern (no code changes, only docs/skill updates).
