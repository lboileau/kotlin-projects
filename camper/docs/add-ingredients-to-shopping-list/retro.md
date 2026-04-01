# Retrospective: Add Ingredients to Shopping List

## Build Summary

**Feature:** Manual items on meal plan shopping lists (ingredient-based and free-form)
**Date:** 2026-03-31
**PR Stack:** #211, #215–#222, #233 (10 PRs)
**Build status:** GREEN — all tests pass

### What was built
- 1 new DB table (`shopping_list_manual_items`)
- 5 new client methods on `MealPlanClient`
- 2 new endpoints (POST add manual item, DELETE remove manual item)
- 3 modified endpoints (GET/PATCH/DELETE shopping list)
- 2 new actions (`AddManualItemAction`, `RemoveManualItemAction`)
- 3 modified actions (`GetShoppingListAction`, `UpdatePurchaseAction`, `ResetPurchasesAction`)
- 2 new error types (`ManualItemNotFound`, `DuplicateManualItem`)
- 77 new tests (26 client integration, 31 service unit, 20 acceptance)

## Review Cycles

| PR | Reviewer | Result |
|---|---|---|
| DB contracts + impl | code-reviewer | APPROVED (first pass) |
| Client contracts | code-reviewer | APPROVED (first pass) |
| Service contracts | code-reviewer | APPROVED (first pass) |
| Client implementation | code-reviewer | APPROVED (first pass) |
| Service implementation | code-reviewer | CHANGES REQUESTED → fixed → APPROVED |
| Client tests | test-reviewer | APPROVED (first pass) |
| Service + acceptance tests | test-reviewer | CHANGES REQUESTED → fixed → APPROVED |

### Issues caught in review
1. **ValidateAddManualItem missing free-form mutual exclusivity** — The validation did not reject `quantity`/`unit` when `description` was provided. A client could send `{"description": "paper plates", "quantity": 5, "unit": "kg"}` and it would pass validation, violating the free-form invariant. Fixed by adding null checks for quantity and unit in the free-form branch.
2. **Missing validation edge case tests** — Service tests lacked coverage for ingredient-based validation (null quantity, null unit, zero quantity, invalid unit) and description validation (blank, over 500 chars). Added 9 tests.

## Bugs Found

### JDBI nullable UUID binding (production bug)
- **Where:** `AddManualItem.kt` operation
- **What:** Nullable `ingredientId` was bound directly as a UUID. When null, JDBI sends it as `varchar` which causes a type mismatch error in PostgreSQL.
- **Impact:** All free-form manual items would fail in production.
- **Fix:** Use `CAST(:ingredientId AS uuid)` in SQL + `?.toString()` in Kotlin binding. This matches the existing pattern in `CreateMealPlan.kt`.
- **Caught by:** Client integration tests (Testcontainers). Unit tests with fakes would NOT have caught this.

## Key Design Decisions

1. **New table vs extending `shopping_list_purchases`** — Chose a new `shopping_list_manual_items` table because the existing table is a purchase ledger (no concept of "quantity required"), while manual items need to be the source of truth for both required and purchased quantities. One table would mean dual-purpose rows with mixed nullable semantics.

2. **Bypass ShoppingListCalculator** — Manual items are merged at the action layer after recipe-based computation. The calculator stays pure (recipe math only). This is clean and avoids coupling manual items to the scaling/aggregation pipeline.

3. **Embedded purchase tracking** — Manual items store `quantity_purchased` directly rather than using `shopping_list_purchases`, because they have a concrete stored row (unlike computed items which only exist at read time).

## Learnings Captured

| Finding | Captured in |
|---|---|
| JDBI nullable UUID binding pattern (`CAST(:param AS uuid)` + `?.toString()`) | `service-manager.md` |
| Contract PRs need stubs in both FakeClient AND JdbiClient | `service-manager.md` + `orchestrator.md` |
| Service contract PRs cascade to all callsites of modified data classes | `orchestrator.md` |

## Webapp PR

### What was built
- Add item form with two modes: ingredient-based (typeahead search + quantity + unit) and free-form (text input)
- Manual items display with "manual" badge and remove button (hover)
- Purchase toggle extended to support `manualItemId`
- Manual items kept separate in merge logic (never merged with recipe items)
- Updated empty state: "Add recipes or add items manually"

### Files changed
- `webapp/src/api/client.ts` — updated types, added API methods
- `webapp/src/components/MealPlanModal.tsx` — add item form, manual item display, remove button, purchase toggle
- `webapp/src/components/MealPlanModal.css` — ~200 lines of new styles

### Webapp retro findings
- **UNITS constant duplication** — `UNITS` array is now duplicated in `RecipesPage.tsx`, `IngredientsPage.tsx`, and `MealPlanModal.tsx`. Should be extracted to `webapp/src/lib/constants.ts`.
- **Ingredient search duplication** — Both recipes page and shopping list now need ingredient search with slightly different UIs. Consider extracting a reusable `IngredientSearch` component.
- **No WebSocket support for shopping list** — The shopping list doesn't subscribe to WebSocket updates. If another user adds a manual item, it won't appear until tab switch.
- **Empty state update** — Had to rethink the empty state since users can now add manual items without recipes.

## Recommendations for Future Builds

1. **Plan accuracy for contract PRs** — When a feature modifies existing DTOs/params, the architect should grep for all constructor callsites and list them in the plan so contract PRs don't surprise with compilation failures.
2. **Run client integration tests early** — The nullable UUID bug is invisible to fake-based unit tests. Integration tests should run immediately after client implementation.
3. **GetShoppingListAction complexity** — At ~290 lines, this action handles recipe computation, purchase matching, orphan detection, AND manual item merging. Consider extracting the manual item merge into a private helper in a future refactor.
4. **Extract shared constants** — Move `UNITS` to `webapp/src/lib/constants.ts` to eliminate cross-file duplication.
5. **Extract IngredientSearch component** — Reusable ingredient typeahead would benefit both recipes and shopping list features.
