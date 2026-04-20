# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/copy-meal-plan-summary/camper

## Feature Name
copy-meal-plan-summary

## Plan
to be created by architect

## Feature Description
Add a "Copy Summary" action to the MealPlanModal that builds a plain-text summary of the trip's meal plan and copies it to the clipboard so the user can paste it elsewhere (email, chat, notes, etc.).

**Text format:**
- One entry per unique recipe used in the meal plan (deduplicated — each recipe appears once regardless of how many times it is scheduled or across which meal types).
- Entry shape:
  - Line 1: recipe name
  - Line 2: recipe web link (only if the recipe has a `webLink`)
- Entries are separated by one blank line.
- No header, no meal-plan name, no day/meal-type labels.

**Example output (two recipes, one with a link, one without):**
```
Campfire Chili
https://example.com/chili

Pancakes
```

**UX:**
- A "Copy Summary" button/link lives in the Overview view of `MealPlanModal`, near the existing template actions ("Save as Template" / "Load from Template") in the `.mp-plan-template-row` / `.mp-template-links` area.
- Only shown when a meal plan exists (i.e., not on the empty-state create form).
- On click: builds the text client-side, writes it to the clipboard via `navigator.clipboard.writeText(...)`, and gives brief visual feedback (e.g., the button label flips to "Copied!" for ~2s then reverts). If the clipboard API fails, show a subtle error state ("Copy failed") for ~2s.
- Button is disabled / hidden if the meal plan has zero recipes (nothing to copy).

**Ordering:**
- Preserve insertion order as encountered when walking days → meal types (breakfast/lunch/dinner/snack) → recipes. On first occurrence, include the recipe; skip subsequent duplicates.

## Entities
No new entities. Uses existing:
- `MealPlan` / `MealPlanDay` / `MealPlanRecipe` (for the list of scheduled recipes)
- `Recipe` (for the `webLink`)

## API Surface
One small backend change:

**Augment `MealPlanRecipeDetailResponse`** to include the recipe's `webLink`:
- Current shape (see `camper/services/camper-service/.../mealplan/.../MealPlanRecipeDetailResponse.kt` and the webapp mirror in `webapp/src/api/client.ts:270`):
  ```
  { id, recipeId, recipeName, baseServings, scaleFactor, isFullyPurchased, ingredients }
  ```
- New shape:
  ```
  { id, recipeId, recipeName, recipeWebLink: String?, baseServings, scaleFactor, isFullyPurchased, ingredients }
  ```
- The value comes from the joined `recipes.web_link` column. All existing meal-plan detail endpoints that return this shape should include it:
  - `GET /api/meal-plans?planId=...`
  - `GET /api/meal-plans/:id`
  - Any other path that returns `MealPlanDetailResponse` (template preview, copy-to-trip response, etc.)
- The frontend `MealPlanRecipeDetailResponse` TypeScript interface in `webapp/src/api/client.ts` must mirror the change (add `recipeWebLink: string | null`).

No new endpoints. No changes to request shapes.

## Database Changes
None. The `recipes.web_link` column already exists; we just need to include it in the joined read path for meal plan recipes.

## Special Considerations
- **Clipboard API:** `navigator.clipboard.writeText` requires a secure context (HTTPS or localhost). Both dev (`http://localhost:3000`) and prod (`https://...railway.app`) qualify. No fallback needed.
- **Deduplication key:** deduplicate by `recipeId` (not by name), since two different recipes could share a name.
- **Recipes with no web link:** emit only the name line. Do not emit an empty second line — the separator between entries is a single blank line regardless.
- **Empty meal plan (no recipes scheduled at all):** the button should be disabled or hidden; no copy action performed.
- **Live updates:** meal-plan recipe changes already trigger a WebSocket notification (`resource: "mealplan"`). The MealPlanModal already refetches on the `mealplan` event, so the summary will reflect the freshest state on next click with no extra wiring.
- **No i18n/localization** — app is English-only.
- **Testing:**
  - Unit test the summary builder (pure function over `MealPlanDetailResponse`): dedup, ordering, link/no-link formatting, empty plan.
  - Acceptance test the backend change: `GET /api/meal-plans/:id` returns `recipeWebLink` populated for recipes that have a web link, `null` for those that don't.

## Notes
- Scope is deliberately tiny: one backend field added to an existing response, one button + one pure text builder in `MealPlanModal.tsx`.
- The webapp already loads both the meal plan detail and the full recipe list, so an alternative purely-frontend implementation (cross-reference `recipes[].webLink` by `recipeId`) was considered and rejected — the user preferred embedding `webLink` in the meal-plan response for a cleaner frontend.
- Follow the standard feature-build flow: architect plan → db-dev (no-op here, skip) → kotlin-dev (response DTO + mapper + any JDBI row mapping) → web-dev (button + builder) → test-engineer → code-reviewer → test-reviewer → doc-updater.
