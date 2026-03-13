# Trip Meal Plans — Build Retrospective

## Feature Summary

Added meal planning to trips with recipe catalog integration, day-by-day meal organization, auto-computed shopping lists with unit conversion, and purchase tracking. Built across 14 PRs with 270 tests.

## PR Stack

| # | PR | Title | Agent | Files | Tests |
|---|----|-------|-------|-------|-------|
| 1 | #127 | Plan | architect | 1 | — |
| 2 | #130 | DB contracts | db-dev | 14 | — |
| 3 | #131 | Lib contracts | kotlin-dev | 9 | — |
| 4 | #132 | Client contracts | kotlin-dev | 7 | — |
| 5 | #133 | Service contracts | kotlin-dev | 24 | — |
| 6 | — | DB impl (no-op) | db-dev | 0 | — |
| 7 | #134 | Lib implementation | kotlin-dev | 2 | — |
| 8 | #135 | Client implementation | kotlin-dev | 38 | — |
| 9 | #136 | Service implementation | kotlin-dev | 25 | — |
| 10 | #137 | Lib tests | test-engineer | 2 | 132 |
| 11 | #138 | Client tests | test-engineer | 4 | 44 |
| 12 | #139 | Service tests | test-engineer | 1 | 59 |
| 13 | #140 | Acceptance tests | test-engineer | 2 | 35 |
| 14 | #141 | Documentation | doc-updater | 2 | — |

## Agent Retros

### PR 2: DB Contracts (db-dev)

**Issues encountered:** None — the architect's plan matched reality perfectly for the DB layer.

**What was verified:**
- 4 migrations (V018–V021): All idempotent with `IF NOT EXISTS`
- 4 rollbacks (R018–R021): All use `DROP TABLE IF EXISTS ... CASCADE`
- Seed data: 3 meal plans, 5 days, 7 recipes, 3 purchases
- All FK references validated (users, plans, recipes, ingredients)

**Plan accuracy:** Excellent — all table definitions, constraints, indexes, and relationships matched the plan spec.

### PR 6: DB Implementation (db-dev)

**Issues encountered:** None — PR 2 was thorough and complete. No changes needed.

### PR 7: Lib Implementation (kotlin-dev)

**What was implemented:**
- UnitConverter with volume (tsp base) and weight (g base) conversion factor maps, count unit detection, bestFit with clean fraction logic
- ShoppingListCalculator with per-recipe scale factors, compatible unit subgroup partitioning, and aggregation with bestFit

**Issues encountered:** None.

### PR 8: Client Implementation (kotlin-dev)

**What was implemented:**
- 4 row adapters, 16 validation classes, 16 operation classes
- JdbiMealPlanClient facade, MealPlanClientFactory
- FakeMealPlanClient with full in-memory implementation (cascade deletes, upsert, unique constraint enforcement)

**Issues encountered:** None — all patterns from recipe-client applied cleanly.

**Plan accuracy:** Spot-on. The interface, param objects, and model types from the contracts PR were exactly right.

### PR 9: Service Implementation (kotlin-dev)

**What was implemented:**
- All 15 actions with validate → convert → call client pattern
- MealPlanDetailBuilder shared helper for building full detail responses
- MealPlanMapper for client model → DTO conversion
- MealPlanClientConfig for factory wiring
- Full controller wiring replacing 501 stubs

**Issues encountered:**
- MealPlanError sealed class needed an `Invalid(field, reason)` variant for validation errors — not in the original plan but clearly necessary (consistent with other features)
- MealPlanClientConfig was missing — created following RecipeClientConfig pattern

**Concerns flagged:**
- GetShoppingListAction and GetMealPlanDetailAction make many sequential client calls (N+1 for recipes/ingredients). Acceptable for now but could benefit from batch loading.

### PR 10: Lib Tests (test-engineer)

**Coverage:** 132 tests (74 UnitConverter + 58 ShoppingListCalculator)

**What was tested:**
- Every supported conversion pair (volume and weight)
- All incompatible conversion combos
- bestFit: clean conversions, stays-put cases, clean fractions, large/small quantities, count units
- Scale factors: fractional, round-up, <1, =1, large, multiple recipes with different base servings
- Aggregation: same unit, compatible units, incompatible units, three unit groups, many recipes, empty input
- Full pipeline integration using the plan's exact example (Chili + Stir Fry with 6 servings)
- Purchase status derivation: all 4 statuses + edge cases

**Issues encountered:**
- bestFit is applied during aggregation, so tests must account for unit promotion (e.g., 500g → 0.5 kg)

### PR 11: Client Tests (test-engineer)

**Coverage:** 44 integration tests with Testcontainers PostgreSQL

**Production bug found and fixed:**
- `CreateMealPlan.kt` had a nullable UUID binding bug. JDBI can't infer the type when a UUID value is null and sends it as VARCHAR, causing PostgreSQL to reject it.
- **Fix:** Changed to `CAST(:planId AS uuid)` in SQL and `.bind("planId", param.planId?.toString())` — matching the pattern used in item-client's CreateItem operation.

**Recommendation:** Audit other clients for similar nullable UUID binding issues.

### PR 12: Service Tests (test-engineer)

**Coverage:** 59 unit tests with fake clients

**What was tested:**
- All 15 actions: happy paths and error cases
- GetShoppingListAction: full pipeline verification including purchase status derivation
- CopyToTrip/SaveAsTemplate: deep copy verification (days + recipes copied, purchases not copied)
- All validation errors

**Issues encountered:**
- Shopping list assertions initially failed because `aggregateShoppingList` applies `bestFit()` which converts units (e.g., 500g → 0.5kg). Fixed assertions to account for unit conversion.

### PR 13: Acceptance Tests (test-engineer)

**Coverage:** 35 end-to-end API tests

**What was tested:**
- Full workflow: create template → add days → add recipes → copy to trip → shopping list → purchases → status verification → reset
- CRUD for all entities, error cases (400, 404, 409)
- Headcount change flow (servings update → quantities change → purchase shortfalls)
- Template copy and save-as-template flows
- Detail response shape verification (meals grouped by type, ingredient scaling info)

**Issues encountered:**
- Initial butter quantity assertion assumed `8 tbsp`, but `bestFit` correctly converts to `0.5 cup`. Fixed assertion.

### PR 14: Documentation (doc-updater)

**What was updated:**
- `camper/CLAUDE.md`: project structure tree, architecture section (computed read-time data pattern)
- `camper/README.md`: features, project structure, migration count, schema diagram, constraints, full API reference

**Recommendations:**
- Consider adding WebSocket live update support for meal plan mutations (missing `PlanEventPublisher` calls — other features have this)

## Key Lessons

1. **Nullable UUID binding in JDBI** — Always use `CAST(:param AS uuid)` with `.bind("param", value?.toString())` for nullable UUID columns in INSERT statements. JDBI can't infer the type when the value is null.

2. **bestFit unit conversion in assertions** — When testing shopping list quantities, account for the fact that `aggregateShoppingList` applies `bestFit()`, which may promote units (e.g., 500g → 0.5 kg, 8 tbsp → 0.5 cup).

3. **Plan accuracy** — The architect's plan was highly accurate across all layers. The only additions needed were:
   - `MealPlanError.Invalid` variant for validation errors
   - `MealPlanClientConfig` for factory wiring
   - `MealPlanDetailBuilder` shared helper (extracted during implementation)

4. **DB contracts completeness** — PR 2 was so thorough that PR 6 (db-impl) was a no-op. Seed data, CLAUDE.md updates, and all FK references were correct from the start.

5. **N+1 query pattern** — GetShoppingListAction and GetMealPlanDetailAction make sequential client calls per recipe. This is a known trade-off for simplicity. Consider batch loading if performance becomes an issue.

6. **Missing WebSocket support** — The meal plan controller doesn't publish `PlanEventPublisher` events after mutations. Other features (items, assignments) do this for live frontend updates. This should be added as a follow-up.
