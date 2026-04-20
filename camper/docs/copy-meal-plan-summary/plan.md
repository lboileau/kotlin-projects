# Plan — copy-meal-plan-summary

## Feature summary
Add a "Copy Summary" action to the `MealPlanModal` Overview view that writes a deduplicated, plain-text list of the meal plan's recipes (name + optional web link, separated by a blank line) to the user's clipboard. Requires a single new field, `recipeWebLink: String?`, on the existing `MealPlanRecipeDetailResponse` so the frontend can emit links without a second round-trip to the recipe endpoint. No new entities, no new endpoints, no DB migration.

---

## API surface

### `MealPlanRecipeDetailResponse` — Kotlin

**Before** (`services/camper-service/.../mealplan/dto/MealPlanResponses.kt:47`):
```kotlin
data class MealPlanRecipeDetailResponse(
    val id: UUID,
    val recipeId: UUID,
    val recipeName: String,
    val baseServings: Int,
    val scaleFactor: BigDecimal,
    val isFullyPurchased: Boolean,
    val ingredients: List<MealPlanIngredientResponse>
)
```

**After** — `recipeWebLink: String?` inserted between `recipeName` and `baseServings` (per handoff ordering):
```kotlin
data class MealPlanRecipeDetailResponse(
    val id: UUID,
    val recipeId: UUID,
    val recipeName: String,
    val recipeWebLink: String?,
    val baseServings: Int,
    val scaleFactor: BigDecimal,
    val isFullyPurchased: Boolean,
    val ingredients: List<MealPlanIngredientResponse>
)
```

### `MealPlanRecipeDetailResponse` — TypeScript

**Before** (`webapp/src/api/client.ts:270`):
```ts
export interface MealPlanRecipeDetailResponse {
  id: string;
  recipeId: string;
  recipeName: string;
  baseServings: number;
  scaleFactor: number;
  isFullyPurchased: boolean;
  ingredients: MealPlanIngredientResponse[];
}
```

**After**:
```ts
export interface MealPlanRecipeDetailResponse {
  id: string;
  recipeId: string;
  recipeName: string;
  recipeWebLink: string | null;
  baseServings: number;
  scaleFactor: number;
  isFullyPurchased: boolean;
  ingredients: MealPlanIngredientResponse[];
}
```

### Endpoints that return this DTO
`MealPlanRecipeDetailResponse` is always returned nested inside `MealPlanDetailResponse` → `MealPlanDayResponse` → `MealsByTypeResponse`, OR returned directly by `AddRecipeToMealAction`. All endpoints inherit the field automatically via the DTO.

| Endpoint | Returns | Wiring location |
|---|---|---|
| `GET /api/meal-plans/{id}` | `MealPlanDetailResponse` | `MealPlanDetailBuilder.buildRecipeDetail` |
| `GET /api/meal-plans?planId={planId}` | `MealPlanDetailResponse` | `MealPlanDetailBuilder.buildRecipeDetail` |
| `POST /api/meal-plans/{id}/copy-to-trip` | `MealPlanDetailResponse` | `MealPlanDetailBuilder.buildRecipeDetail` (reused) |
| `POST /api/meal-plans/{id}/save-as-template` | `MealPlanDetailResponse` | `MealPlanDetailBuilder.buildRecipeDetail` (reused) |
| `POST /api/meal-plans/{mealPlanId}/days/{dayId}/recipes` | `MealPlanRecipeDetailResponse` (single) | `AddRecipeToMealAction` |

### Data source
`Recipe.webLink` already exists on the model returned by `recipeClient.getById(...)` (`clients/recipe-client/.../model/Recipe.kt:10`). Both code paths that construct `MealPlanRecipeDetailResponse` already fetch the full `Recipe` before building the response, so **no meal-plan-client changes are needed** — the service layer simply passes `recipe.webLink` through. (This is a scope correction against the handoff's "wire it from the joined `recipes.web_link` column in the meal-plan-client JDBI read path" — the join already happens at the service layer via the recipe-client round-trip.)

---

## Impacted files (grouped by PR)

### PR 2 — Service contracts + implementation (merged — see "PR stack" rationale)

**Files that must be touched for compilation to pass:**

1. `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/dto/MealPlanResponses.kt`
   - Add `recipeWebLink: String?` to `MealPlanRecipeDetailResponse` (line 47).

2. `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/MealPlanDetailBuilder.kt`
   - `buildRecipeDetail` — line 141 constructor: add `recipeWebLink = recipe.webLink,` after `recipeName`. (`recipe` is already in scope from the `recipeClient.getById()` call on line 97.)

3. `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/AddRecipeToMealAction.kt`
   - Line 95 constructor: add `recipeWebLink = recipe.webLink,` after `recipeName`. (`recipe` is already in scope from the `recipeClient.getById()` call on line 36.)

**Grep verification** (`MealPlanRecipeDetailResponse\(`):
```
services/camper-service/.../dto/MealPlanResponses.kt:47             (definition)
services/camper-service/.../actions/MealPlanDetailBuilder.kt:141   (call site #1)
services/camper-service/.../actions/AddRecipeToMealAction.kt:95    (call site #2)
```
No other constructor call sites exist. `FakeMealPlanClient` does NOT construct `MealPlanRecipeDetailResponse` (it's a service-layer DTO, not a client-layer type) — verified by grep.

4. `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/mealplan/acceptance/fixture/MealPlanFixture.kt`
   - `insertRecipe(...)` currently hard-codes `web_link = NULL` (line 67–68). Add an optional `webLink: String? = null` parameter and bind it into the SQL. Needed so the acceptance test can insert a recipe with a link. **This is a fixture-signature change only** — existing callers (all of which rely on the default) keep compiling.

### PR 3 — Webapp

5. `webapp/src/api/client.ts`
   - Add `recipeWebLink: string | null;` to `MealPlanRecipeDetailResponse` (after `recipeName`, line 273).

6. `webapp/src/lib/mealPlanSummary.ts` *(new file)*
   - Export `buildMealPlanSummary(mealPlan: MealPlanDetailResponse): string` — pure function. Returns empty string when no recipes.

7. `webapp/src/components/MealPlanModal.tsx`
   - Import `buildMealPlanSummary`.
   - Add Copy Summary button inside the `.mp-template-links` branch (lines 718–736) — i.e., alongside "Save as Template" / "Load from Template" — as a new `<button className="mp-template-link">` after the load-from-template button (see "Open questions" for placement confirmation).
   - Hide the button when `recipeCount === 0` (derived by flattening `mealPlan.days[*].meals.{breakfast,lunch,dinner,snack}`).
   - Copy handler: `navigator.clipboard.writeText(buildMealPlanSummary(mealPlan))`; on success set label to "Copied!" for 2000 ms; on rejection set label to "Copy failed" for 2000 ms.
   - State: `const [copyState, setCopyState] = useState<'idle' | 'copied' | 'failed'>('idle')` scoped in `OverviewView`.
   - Thread `mealPlan` access (already in scope in `OverviewView`).

8. `webapp/src/components/MealPlanModal.css`
   - Optionally add a separator rule (`<span className="mp-template-sep">|</span>`) between the existing templates links and the Copy Summary button to match the existing pattern. No new class names required if the existing `.mp-template-link` / `.mp-template-sep` styles are reused.

### PR 4 — Unit tests (webapp)

9. `webapp/package.json`
   - Add `vitest` + `@vitest/ui` (optional) to `devDependencies` and `"test": "vitest run"` + `"test:watch": "vitest"` scripts. **See open question #1** — there is no existing test framework in the webapp; this is new infrastructure.

10. `webapp/vitest.config.ts` *(new file)* — minimal config, `environment: 'node'` (builder is pure, no DOM needed).

11. `webapp/src/lib/mealPlanSummary.test.ts` *(new file)* — vitest suite, cases enumerated below.

### PR 5 — Acceptance tests (backend)

12. `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/mealplan/acceptance/MealPlanAcceptanceTest.kt`
   - New `@Nested` class or new `@Test` inside an existing nested class (follow existing pattern — e.g., under the `GET /api/meal-plans/{id}` block). Test: insert two recipes — one with `webLink = "https://example.com/chili"`, one with `webLink = null` — add both to a meal plan, `GET /api/meal-plans/{id}`, assert `recipeWebLink` matches.

### PR 6 — Docs

13. `services/camper-service/CLAUDE.md` — update the `MealPlanRecipeDetailResponse` DTO description line to include `recipeWebLink`.
14. `webapp/CLAUDE.md` — same mirror in the "API Layer" interface list.
15. Optional: `docs/copy-meal-plan-summary/retro.md` (the retro output written by doc-updater).

### Unit/service tests unaffected
`MealPlanServiceTest` asserts `recipeName` but does not construct `MealPlanRecipeDetailResponse` directly — it reads through `MealPlanDetailBuilder`. It will pick up the new field automatically; assertions that only check existing fields continue to pass.

---

## PR stack

**Decision: collapse the handoff's PR 2 ("service contracts") and PR 3 ("service + client implementation") into a single PR.** Rationale: the contract change is a single optional field with two trivial pass-through call-site updates (`recipe.webLink` is already in scope at both sites). Splitting them would require either (a) a stub PR that sets `recipeWebLink = null` at both call sites — which introduces a regression for the second PR to un-regress, or (b) landing the DTO field with no call-site updates, which won't compile. Neither offers any value when the implementation is two lines. Additionally, there is no separate client-layer change (the meal-plan-client does not join on `recipes`; `Recipe.webLink` arrives via `recipeClient.getById`).

Final stack:

1. `feat(copy-meal-plan-summary): plan` — this document.
2. `feat(copy-meal-plan-summary): service DTO + mapper` — adds `recipeWebLink` to `MealPlanRecipeDetailResponse` and wires `recipe.webLink` through both construction sites. Updates `MealPlanFixture.insertRecipe` to accept an optional `webLink` parameter (needed for PR 5).
3. `feat(copy-meal-plan-summary): webapp` — mirrors TS interface, adds pure `buildMealPlanSummary` in `webapp/src/lib/mealPlanSummary.ts`, adds Copy Summary button in Overview view with Copied!/Copy failed feedback.
4. `feat(copy-meal-plan-summary): unit tests` — vitest scaffolding (package.json, vitest.config.ts) + `mealPlanSummary.test.ts` covering dedup, ordering, link/no-link, empty.
5. `feat(copy-meal-plan-summary): acceptance tests` — backend test proving `GET /api/meal-plans/{id}` returns `recipeWebLink` correctly for recipes with and without a link.
6. `feat(copy-meal-plan-summary): update documentation and skills` — CLAUDE.md updates + retro.

---

## Test plan

### Unit — `webapp/src/lib/mealPlanSummary.test.ts`
Fixture helper (local to the test file): `makeRecipe(id, name, webLink = null)` returning a minimal `MealPlanRecipeDetailResponse`.

Cases:
1. **Empty meal plan (no days)** — returns `""`.
2. **Meal plan with days but no recipes on any meal** — returns `""`.
3. **Single recipe with no web link** — returns `"Pancakes"` (no trailing newline, no blank line after).
4. **Single recipe with web link** — returns `"Campfire Chili\nhttps://example.com/chili"`.
5. **Two distinct recipes, mix of link/no-link** — matches the handoff example exactly:
   ```
   Campfire Chili
   https://example.com/chili
   
   Pancakes
   ```
6. **Deduplication by `recipeId`** — same recipe across breakfast/lunch + across day 1 / day 2 → appears exactly once.
7. **Deduplication by `recipeId` (not `recipeName`)** — two recipes sharing a name but different ids → both appear.
8. **Ordering** — walks day order ascending (as given by the response); within a day, `breakfast → lunch → dinner → snack`; within a meal, response order.

### Acceptance — `MealPlanAcceptanceTest`
Fixture setup (inside the test):
- Create user + plan (existing helpers).
- Insert two ingredients (`insertIngredient` already exists).
- Insert two recipes via `fixture.insertRecipe(..., webLink = "https://example.com/chili")` and `fixture.insertRecipe(..., webLink = null)`.
- Insert recipe ingredients (existing helper).
- Create meal plan, add day 1, add both recipes to different meals (direct SQL via fixture).

Assertions:
- `GET /api/meal-plans/{id}` → 200.
- Response contains two recipe entries.
- Recipe A: `recipeWebLink == "https://example.com/chili"`.
- Recipe B: `recipeWebLink == null`.

---

## Open questions

1. **Webapp unit-test framework.** The webapp currently has no test runner configured (no `vitest`, no `jest`, no test files anywhere in `webapp/`). Adding one is scope creep beyond this feature. Options:
   - (a) Add vitest as part of this feature's PR 4 (≈10 lines of config).
   - (b) Skip the webapp unit test and rely on the acceptance test + manual QA.
   - (c) Port the builder to a Kotlin lib and test it there (rejected — the builder runs entirely client-side, no value in cross-module coupling).
   **Default if unanswered: (a)** — add vitest. The builder has real branching (dedup, ordering, link/no-link) and deserves a test, and the infrastructure is cheap.

2. **Button placement.** Handoff says "near the existing template actions in the `.mp-plan-template-row` / `.mp-template-links` area." Two concrete options:
   - (a) Append inside `.mp-template-links` after "Load from Template", using the existing `.mp-template-link` + `.mp-template-sep` pattern (all three read as sibling underline links, visually cohesive).
   - (b) A separate row underneath template actions.
   **Default if unanswered: (a)** — a third dashed-underline link with a `|` separator. Matches the existing visual language.

3. **Disabled vs. hidden when meal plan has 0 recipes.** Handoff says both "disabled" and "hidden" in different sentences. **Default if unanswered: hidden.** Rationale: the Overview view already has a create form / empty state when the meal plan is empty — a greyed-out button in that empty state adds visual clutter. When `mealPlan` exists but has no recipes scheduled yet, a disabled button risks looking broken; hidden is cleaner.

4. **Exact button copy.** Handoff says "Copy Summary" (title case). Default: **"Copy Summary"** for idle, **"Copied!"** for success, **"Copy failed"** for error.

---

## Cascade impact checklist

Cross-checked against the architect's "Model Changes Checklist":

| # | Item | Affected here? |
|---|---|---|
| 1 | Model definition | ✅ `MealPlanRecipeDetailResponse` (service DTO only; no client model changes) |
| 2 | Row adapter | ❌ No meal-plan-client read path touches this type |
| 3 | SQL queries | ❌ No new joins (data comes via existing `recipeClient.getById`) |
| 4 | Fake client | ❌ `FakeMealPlanClient` doesn't touch service DTOs |
| 5 | Operations | ❌ no INSERT/UPDATE/DELETE — read-only field |
| 6 | Mappers | ✅ `MealPlanDetailBuilder.buildRecipeDetail`, `AddRecipeToMealAction` |
| 7 | Test fixtures | ✅ `MealPlanFixture.insertRecipe` — add optional `webLink` parameter |
| 8 | Service unit tests | ❌ Existing assertions read fields by name; new field defaults through |
| 9 | Integration tests | ❌ — |
| 10 | Acceptance tests | ✅ New assertion on the new field |
| 11 | All other constructor call sites | ✅ Grepped — 2 sites (see PR 2 section) |

Frontend cascade: `MealPlanRecipeDetailResponse` TS interface is the only type that changes. `MealPlanModal.tsx` is the only consumer that needs the new field; other usages (if any) continue to compile because the field is non-optional but purely additive.

---

## Notes for implementers

- **No DB migration.** `recipes.web_link` has existed since the recipe feature shipped.
- **Do not** modify `clients/meal-plan-client/` — the field flows through the service layer, not the meal-plan-client.
- **Do not** introduce a mapper helper "for symmetry" — the two call sites each have `recipe` in local scope; a one-line field assignment is the right size.
- **Kotlin data class ordering matters** for positional construction. The two existing call sites use named arguments, so the field insert position (between `recipeName` and `baseServings`) won't break them, but double-check after editing.
- **Clipboard:** `navigator.clipboard.writeText` returns a `Promise<void>` that rejects if the secure-context check fails or if the document is not focused. Wrap in `.then()/.catch()` — don't assume success.
