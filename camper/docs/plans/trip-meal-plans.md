# Trip Meal Plans

## Summary

Add meal planning to trips using the existing recipe catalog. A meal plan organizes recipes into numbered days and meal types (breakfast, lunch, dinner, snack). Meal plans can exist as reusable templates or be bound to a specific trip. Trip meal plans can be created from templates, and trip meal plans can be saved back as new templates.

Recipes are **referenced, not copied** — a meal plan stores pointers to recipes. Ingredient data is always read live from the recipe catalog, so updating a recipe automatically reflects in any meal plan that uses it.

The shopping list is **fully computed at read time** — no stored quantities. The system aggregates all recipe ingredients (scaled by servings), converts compatible units, and groups by ingredient and unit. The only stored data is **what the user has purchased** (`shopping_list_purchases`). This means changes to recipes, servings, or headcount are immediately reflected in the required quantities, and any shortfall vs. purchased amounts is visible instantly.

## Entities

### MealPlan

The top-level container. Can be a **template** (no trip association) or **trip-bound** (linked to a plan).

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| plan_id | UUID? | FK → plans.id CASCADE, UNIQUE when not null. Null = template |
| name | VARCHAR(255) | NOT NULL |
| servings | INT | NOT NULL, CHECK > 0. Target servings for scaling |
| scaling_mode | VARCHAR(20) | NOT NULL, CHECK IN ('fractional', 'round_up'). Default 'fractional' |
| is_template | BOOLEAN | NOT NULL, DEFAULT false |
| source_template_id | UUID? | FK → meal_plans.id SET NULL. Tracks which template this was copied from |
| created_by | UUID | FK → users.id RESTRICT |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Invariants:**
- If `is_template = true`, then `plan_id` must be null
- If `plan_id` is not null, then `is_template` must be false
- A plan can have at most one meal plan (enforced by UNIQUE on plan_id WHERE plan_id IS NOT NULL)

### MealPlanDay

A numbered day within a meal plan.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_id | UUID | FK → meal_plans.id CASCADE |
| day_number | INT | NOT NULL, CHECK > 0 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Invariants:**
- UNIQUE(meal_plan_id, day_number) — no duplicate day numbers within a meal plan

### MealPlanRecipe

A recipe placed into a specific meal slot on a specific day. Duplicate recipes (same recipe on different meals or days) are distinct records. References the recipe by ID — ingredient data is always loaded live from the recipe catalog.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_day_id | UUID | FK → meal_plan_days.id CASCADE |
| meal_type | VARCHAR(20) | NOT NULL, CHECK IN ('breakfast', 'lunch', 'dinner', 'snack') |
| recipe_id | UUID | FK → recipes.id CASCADE |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Notes:**
- No `base_servings` snapshot — always read live from `recipes.base_servings`
- Deleting a recipe cascades to remove it from all meal plans

### ShoppingListPurchase

Tracks **only what the user has purchased** per ingredient per unit per meal plan. Required quantities are never stored — always computed live from recipe data.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_id | UUID | FK → meal_plans.id CASCADE |
| ingredient_id | UUID | FK → ingredients.id CASCADE |
| unit | VARCHAR(20) | NOT NULL |
| quantity_purchased | NUMERIC | NOT NULL, CHECK >= 0 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Invariants:**
- UNIQUE(meal_plan_id, ingredient_id, unit) — one purchase entry per ingredient per unit per meal plan

**Notes:**
- The `unit` here is the **resolved unit** after unit conversion (e.g., if a recipe uses tbsp and another uses cups, they both resolve to cups)
- When the user "checks off" an ingredient, the UI sets `quantity_purchased = computed_quantity_required` for that row
- Purchase records may become orphaned if all recipes using that ingredient are removed — these show as "no longer needed"

## Unit Conversion

A pure-logic library (`libs/meal-plan-calculator`) handles unit conversion and shopping list computation.

### Unit Categories

Units fall into three categories:

| Group | Units | Compatible? |
|-------|-------|-------------|
| **Volume** | tsp, tbsp, cup, ml, l | Yes — all convertible to each other |
| **Weight** | g, kg, oz, lb | Yes — all convertible to each other |
| **Count** | pieces, whole, bunch, can, clove, pinch, slice, sprig | No — each is its own type |

### Conversion Rules

- **Compatible units** (volume↔volume, weight↔weight): convert to a common unit, sum, then use `bestFit` to find a clean display unit
- **Incompatible units**: cannot convert. Same ingredient in volume + weight (or volume + count) → separate shopping list rows. E.g., "1.5 cups carrots" + "2 whole carrots" = two rows
- **Count units**: each count unit is its own type. "2 cloves garlic" + "1 whole garlic" = two rows (clove ≠ whole)

### Best-Fit Display Unit

`bestFit(quantity, unit)` scales up through compatible units until reaching the smallest whole number or clean fraction (1/4, 1/2, 3/4):
- 48 tsp → 1 cup
- 6 tbsp → 6 tbsp (0.375 cups isn't a clean fraction, so stay at tbsp)
- 1500 ml → 1.5 l
- 750 g → 750 g (0.75 kg is a clean fraction → could go either way, prefer the more readable one)

## Scaling Logic

Scaling is computed in the `libs/meal-plan-calculator` library.

- **Scale factor** = `meal_plan.servings / recipe.base_servings` (read live from the recipe)
- **Fractional mode**: `scale_factor` used as-is (exact ratio, may produce decimals). E.g., 6 people / 4-serving recipe = 1.5x multiplier
- **Round-up mode**: `ceil(scale_factor)` rounded up to the nearest whole number. This means quantities are always a whole-number multiple of the recipe's base. E.g., 6 people / 4-serving recipe → ceil(1.5) = 2x multiplier (scales to 8 servings worth)

Scaled quantity for a single recipe ingredient:
- **Fractional**: `quantity * (meal_plan.servings / recipe.base_servings)`
- **Round-up**: `quantity * ceil(meal_plan.servings / recipe.base_servings)`

## Shopping List Computation

The shopping list is **fully computed at read time**. The flow:

1. Load all `meal_plan_recipes` → get distinct `recipe_id`s
2. Load `recipe_ingredients` for each recipe (only approved, with resolved `ingredient_id`)
3. Scale each ingredient quantity using the meal plan's servings and scaling mode
4. Group by `ingredient_id`, then within each group:
   - Partition into compatible unit subgroups (e.g., volume vs count)
   - Within each subgroup, convert all to a common unit, sum, then `bestFit` the total
   - Each subgroup becomes one shopping list row
5. For each `(ingredient_id, resolved_unit)`, look up the corresponding `shopping_list_purchases` row
6. Compute status per row:
   - `quantity_purchased >= quantity_required > 0` → **Done** (green)
   - `quantity_purchased < quantity_required` → **"X more needed"** (yellow/orange)
   - `quantity_required = 0, quantity_purchased > 0` → **"No longer needed"** (red)
   - `quantity_purchased = 0` → **Not purchased**

### Example

Trip has 6 people, scaling mode = fractional. Recipes:
- **Chili** (base 4 servings): 1 cup carrots, 2 whole onions
- **Stir Fry** (base 2 servings): 2 tbsp carrots, 1 whole carrot, 3 whole onions

Scaling: Chili scale = 6/4 = 1.5x, Stir Fry scale = 6/2 = 3x

Carrots (volume group): 1 cup × 1.5 = 1.5 cups, 2 tbsp × 3 = 6 tbsp = 0.375 cups → **total: 1.875 cups**
Carrots (count group): 1 whole × 3 = **3 whole**
Onions (count group): 2 whole × 1.5 = 3, 3 whole × 3 = 9 → **total: 12 whole**

Shopping list shows:
- Carrots: 1.875 cups | 3 whole (two rows)
- Onions: 12 whole (one row)

## Recipe Completion

A `MealPlanRecipe` is considered **fully purchased** when every one of its recipe's approved ingredients (with a resolved `ingredient_id`) has `quantity_purchased >= quantity_required` in the shopping list for the corresponding `(ingredient_id, unit)` pair.

This is computed at read time — no stored field.

## API Surface

### Meal Plans

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | /api/meal-plans | Create meal plan | `{ name, servings, scalingMode, isTemplate, planId? }` | 201 MealPlan |
| GET | /api/meal-plans/{id} | Get meal plan with full details (days, meals, recipes, live ingredients) | — | 200 MealPlanDetail |
| GET | /api/meal-plans?planId={planId} | Get meal plan for a trip | — | 200 MealPlanDetail? |
| GET | /api/meal-plans/templates | List template meal plans | — | 200 List\<MealPlan\> |
| PUT | /api/meal-plans/{id} | Update meal plan settings | `{ name?, servings?, scalingMode? }` | 200 MealPlan |
| DELETE | /api/meal-plans/{id} | Delete meal plan | — | 204 |
| POST | /api/meal-plans/{id}/copy-to-trip | Copy template to a trip | `{ planId, servings? }` | 201 MealPlanDetail |
| POST | /api/meal-plans/{id}/save-as-template | Save trip meal plan as template | `{ name }` | 201 MealPlan |

### Days

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | /api/meal-plans/{id}/days | Add a day | `{ dayNumber }` | 201 MealPlanDay |
| DELETE | /api/meal-plans/{mealPlanId}/days/{dayId} | Remove a day (cascades recipes) | — | 204 |

### Recipes on Meals

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | /api/meal-plans/{mealPlanId}/days/{dayId}/recipes | Add recipe to a meal on a day | `{ mealType, recipeId }` | 201 MealPlanRecipeDetail |
| DELETE | /api/meal-plan-recipes/{mealPlanRecipeId} | Remove recipe from meal | — | 204 |

### Shopping List

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| GET | /api/meal-plans/{id}/shopping-list | Get computed shopping list with purchase status | — | 200 ShoppingList |
| PATCH | /api/meal-plans/{id}/shopping-list | Update purchased quantity for an ingredient/unit | `{ ingredientId, unit, quantityPurchased }` | 200 ShoppingListPurchase |
| DELETE | /api/meal-plans/{id}/shopping-list | Reset all purchases (delete all purchase records) | — | 204 |

**Total: 14 endpoints**

### Response Shapes

#### MealPlan (summary)
```json
{
  "id": "uuid",
  "planId": "uuid | null",
  "name": "string",
  "servings": 4,
  "scalingMode": "round_up",
  "isTemplate": false,
  "sourceTemplateId": "uuid | null",
  "createdBy": "uuid",
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

#### MealPlanDetail (full)
```json
{
  "id": "uuid",
  "planId": "uuid | null",
  "name": "string",
  "servings": 4,
  "scalingMode": "round_up",
  "isTemplate": false,
  "sourceTemplateId": "uuid | null",
  "createdBy": "uuid",
  "days": [
    {
      "id": "uuid",
      "dayNumber": 1,
      "meals": {
        "breakfast": [
          {
            "id": "uuid",
            "recipeId": "uuid",
            "recipeName": "Pancakes",
            "baseServings": 4,
            "scaleFactor": 1.0,
            "isFullyPurchased": true,
            "ingredients": [
              {
                "recipeIngredientId": "uuid",
                "ingredientId": "uuid",
                "ingredientName": "Flour",
                "category": "pantry",
                "quantity": 2.0,
                "scaledQuantity": 2.0,
                "unit": "cup"
              }
            ]
          }
        ],
        "lunch": [],
        "dinner": [],
        "snack": []
      }
    }
  ],
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

#### ShoppingList
```json
{
  "mealPlanId": "uuid",
  "servings": 4,
  "scalingMode": "round_up",
  "totalItems": 12,
  "fullyPurchasedCount": 5,
  "categories": [
    {
      "category": "produce",
      "items": [
        {
          "ingredientId": "uuid",
          "ingredientName": "Onion",
          "quantityRequired": 6.0,
          "quantityPurchased": 6.0,
          "unit": "whole",
          "status": "done",
          "usedInRecipes": ["Chili", "Pasta Sauce"]
        }
      ]
    }
  ]
}
```

#### ShoppingListPurchase
```json
{
  "id": "uuid",
  "mealPlanId": "uuid",
  "ingredientId": "uuid",
  "unit": "cup",
  "quantityPurchased": 2.0,
  "createdAt": "instant",
  "updatedAt": "instant"
}
```

**Shopping list item statuses:**
- `"done"` — `quantity_purchased >= quantity_required > 0`
- `"more_needed"` — `quantity_purchased > 0 && quantity_purchased < quantity_required` (delta shown in UI)
- `"not_purchased"` — `quantity_purchased = 0 && quantity_required > 0`
- `"no_longer_needed"` — `quantity_required = 0 && quantity_purchased > 0`

## Database Changes

### New Tables (4)

1. **meal_plans** — meal plan container (template or trip-bound)
2. **meal_plan_days** — numbered days within a meal plan
3. **meal_plan_recipes** — recipe references placed on a day/meal
4. **shopping_list_purchases** — purchase tracking per ingredient per unit per meal plan (only stores what was bought)

### Migrations

- `V018__create_meal_plans.sql`
- `V019__create_meal_plan_days.sql`
- `V020__create_meal_plan_recipes.sql`
- `V021__create_shopping_list_purchases.sql`

### Indexes

- `meal_plans(plan_id)` — lookup by trip
- `meal_plans(is_template)` — filter templates
- `meal_plan_days(meal_plan_id)` — list days for a plan
- `meal_plan_recipes(meal_plan_day_id)` — list recipes for a day
- `meal_plan_recipes(recipe_id)` — find meal plan recipes by recipe
- `shopping_list_purchases(meal_plan_id)` — list purchases for a plan

## Library

### meal-plan-calculator

New library module: `libs/meal-plan-calculator`
Package: `com.acme.libs.mealplancalculator`

Pure logic — no I/O, no Spring dependencies.

#### Unit Conversion

```kotlin
object UnitConverter {
    /** Convert a quantity from one unit to another. Returns null if units are incompatible. */
    fun convert(quantity: BigDecimal, sourceUnit: String, targetUnit: String): BigDecimal?

    /** Check if two units can be converted between each other. */
    fun areCompatible(unitA: String, unitB: String): Boolean

    /**
     * Find the best-fit unit for a quantity — scales up through compatible units
     * until reaching the smallest whole number or clean fraction (1/4, 1/2, 3/4).
     * E.g., 48 tsp → 1 cup, 6 tbsp → 6 tbsp (0.375 cups isn't clean).
     */
    fun bestFit(quantity: BigDecimal, unit: String): Pair<BigDecimal, String>
}
```

Internally backed by a map of unit→unit conversion factors and sets of compatible units. Volume units (tsp, tbsp, cup, ml, l) are compatible with each other. Weight units (g, kg, oz, lb) are compatible with each other. Count units (pieces, whole, bunch, can, clove, pinch, slice, sprig) are each their own type — not convertible.

#### Shopping List Calculator

```kotlin
data class ScaledIngredient(
    val ingredientId: UUID,
    val recipeIngredientId: UUID,
    val recipeName: String,
    val quantity: BigDecimal,
    val unit: String,
)

data class ShoppingListRow(
    val ingredientId: UUID,
    val ingredientName: String,
    val category: String,
    val quantityRequired: BigDecimal,
    val unit: String,
    val usedInRecipes: List<String>,
)

object ShoppingListCalculator {
    fun scaleIngredients(
        recipeIngredients: List<RecipeIngredientWithMeta>,
        servings: Int,
        baseServings: Int,
        scalingMode: ScalingMode,
    ): List<ScaledIngredient>

    fun aggregateShoppingList(
        scaledIngredients: List<ScaledIngredient>,
        ingredientLookup: Map<UUID, IngredientInfo>,
    ): List<ShoppingListRow>
}
```

## Client Interface

### meal-plan-client

New client module: `clients/meal-plan-client`
Package: `com.acme.clients.mealplanclient`

#### Models

```kotlin
data class MealPlan(id, planId?, name, servings, scalingMode, isTemplate, sourceTemplateId?, createdBy, createdAt, updatedAt)
data class MealPlanDay(id, mealPlanId, dayNumber, createdAt, updatedAt)
data class MealPlanRecipe(id, mealPlanDayId, mealType, recipeId, createdAt, updatedAt)
data class ShoppingListPurchase(id, mealPlanId, ingredientId, unit, quantityPurchased: BigDecimal, createdAt, updatedAt)
```

#### Interface

```kotlin
interface MealPlanClient {
    // Meal plans
    fun create(param: CreateMealPlanParam): Result<MealPlan, AppError>
    fun getById(param: GetByIdParam): Result<MealPlan, AppError>
    fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError>
    fun getTemplates(): Result<List<MealPlan>, AppError>
    fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError>
    fun delete(param: DeleteMealPlanParam): Result<Unit, AppError>

    // Days
    fun addDay(param: AddDayParam): Result<MealPlanDay, AppError>
    fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError>
    fun removeDay(param: RemoveDayParam): Result<Unit, AppError>

    // Recipes
    fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError>
    fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError>
    fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError>
    fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError>

    // Shopping list purchases
    fun getPurchases(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError>
    fun upsertPurchase(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError>
    fun deletePurchases(param: DeletePurchasesParam): Result<Unit, AppError>
}
```

**Notes:**
- No recipe ingredient operations on this client — ingredient data is read from the existing `RecipeClient`
- Shopping list computation (scaling, unit conversion, aggregation) lives in `libs/meal-plan-calculator`
- `upsertPurchase` uses ON CONFLICT on `(meal_plan_id, ingredient_id, unit)` to create or update

## Service Layer

### Feature: meal-plan

Package: `com.acme.services.camperservice.features.mealplan`

#### Actions

| Action | Description |
|--------|-------------|
| CreateMealPlanAction | Validate and create a new meal plan (template or trip-bound) |
| GetMealPlanDetailAction | Fetch meal plan with all days, recipes, live ingredients from recipe catalog, compute scaling |
| GetMealPlanByPlanIdAction | Get the meal plan for a specific trip |
| GetTemplatesAction | List all template meal plans |
| UpdateMealPlanAction | Update name, servings, scaling mode |
| DeleteMealPlanAction | Delete a meal plan (cascades everything) |
| CopyToTripAction | Deep-copy a template meal plan to a trip (days, recipes — no purchases) |
| SaveAsTemplateAction | Deep-copy a trip meal plan as a new template (days, recipes — no purchases) |
| AddDayAction | Add a numbered day to a meal plan |
| RemoveDayAction | Remove a day (cascade deletes recipes) |
| AddRecipeToMealAction | Add a recipe reference to a day/meal |
| RemoveRecipeFromMealAction | Remove a recipe from a meal |
| GetShoppingListAction | Compute shopping list live: load recipes → scale → convert units → aggregate → join with purchases → return with statuses |
| UpdatePurchaseAction | Upsert a purchase record for an ingredient/unit |
| ResetPurchasesAction | Delete all purchase records for a meal plan |

#### Error Types

```kotlin
sealed class MealPlanError {
    data class MealPlanNotFound(val id: UUID) : MealPlanError()
    data class DayNotFound(val id: UUID) : MealPlanError()
    data class RecipeNotFound(val id: UUID) : MealPlanError()
    data class DuplicateDayNumber(val dayNumber: Int) : MealPlanError()
    data class PlanAlreadyHasMealPlan(val planId: UUID) : MealPlanError()
    data class NotATemplate(val id: UUID) : MealPlanError()
    data class IsATemplate(val id: UUID) : MealPlanError()
}
```

## PR Stack

| # | Branch Suffix | Title | Description |
|---|--------------|-------|-------------|
| 1 | plan | feat(meal-plans): plan | This document |
| 2 | db-contracts | feat(meal-plans): db contracts | Schema files + migration SQL for 4 tables |
| 3 | lib-contracts | feat(meal-plans): lib contracts | meal-plan-calculator module setup, interfaces, types |
| 4 | client-contracts | feat(meal-plans): client contracts | MealPlanClient interface, param objects, model types, fake stubs |
| 5 | service-contracts | feat(meal-plans): service contracts | DTOs, error types, action signatures, controller routes (501s) |
| 6 | db-impl | feat(meal-plans): db implementation | Seed data, verify migrations |
| 7 | lib-impl | feat(meal-plans): lib implementation | Unit conversion + shopping list calculator logic |
| 8 | client-impl | feat(meal-plans): client implementation | JDBI operations, row adapters, factory, fake client |
| 9 | service-impl | feat(meal-plans): service implementation | Actions, service facade, controller, error mapping, config |
| 10 | lib-tests | feat(meal-plans): lib tests | Unit tests for unit conversion and shopping list calculator |
| 11 | client-tests | feat(meal-plans): client tests | Integration tests with Testcontainers |
| 12 | service-tests | feat(meal-plans): service tests | Unit tests with fake client |
| 13 | acceptance-tests | feat(meal-plans): acceptance tests | End-to-end API tests |
| 14 | docs | feat(meal-plans): update documentation and skills | CLAUDE.md, README updates, skill updates |

## Testing Strategy

The calculation logic in this feature — unit conversion, scaling, aggregation, and purchase status derivation — is the core of the shopping list experience. Bugs here directly affect the user's grocery trip. **Testing this layer thoroughly is critical.**

### Lib Tests (meal-plan-calculator) — highest priority

This is where the most complex logic lives and where the most things can go wrong. These tests must be **exhaustive**.

#### UnitConverter

- **Every supported conversion pair**: tsp↔tbsp, tbsp↔cup, cup↔ml, ml↔l, and all reverse directions. Same for weight: g↔kg, g↔oz, oz↔lb, etc.
- **Incompatible conversions**: volume↔weight returns null, volume↔count returns null, different count types return null (clove↔whole)
- **areCompatible**: true for all volume pairs, all weight pairs. False across groups. False for count↔count of different types.
- **bestFit edge cases**:
  - Clean conversions: 48 tsp → 1 cup, 3 tsp → 1 tbsp, 1000 ml → 1 l
  - Stays put when conversion isn't clean: 6 tbsp → 6 tbsp (not 0.375 cups)
  - Clean fractions: 8 tbsp → 0.5 cup, 6 tsp → 2 tbsp
  - Large quantities: 5000 ml → 5 l
  - Small quantities: 0.25 tsp → 0.25 tsp (no smaller unit)
  - Quantities that are already best-fit: 2 cups → 2 cups
- **Precision**: BigDecimal rounding behavior across all conversions. No floating point drift.

#### ShoppingListCalculator — scaleIngredients

- **Fractional scaling**: 6 servings / 4 base = 1.5x. Verify all ingredient quantities multiplied by 1.5
- **Round-up scaling**: 6/4 → ceil(1.5) = 2x. Verify quantities doubled.
- **Round-up exact fit**: 8/4 = 2x exactly. ceil(2) = 2. No unnecessary rounding.
- **Scale factor < 1**: 2 servings / 4 base = 0.5x fractional, ceil(0.5) = 1x round-up
- **Scale factor = 1**: no change to quantities
- **Large scale factors**: 20 servings / 2 base = 10x
- **Multiple recipes with different base servings**: recipe A (base 4) and recipe B (base 2) in the same meal plan with 6 servings — different scale factors per recipe

#### ShoppingListCalculator — aggregateShoppingList

- **Same ingredient, same unit**: 2 cups flour (recipe A) + 1 cup flour (recipe B) → 3 cups flour
- **Same ingredient, compatible units**: 1 cup carrots + 2 tbsp carrots → single row in best-fit unit
- **Same ingredient, incompatible units**: 1 cup carrots + 2 whole carrots → two separate rows
- **Same ingredient, three unit groups**: 1 cup carrots + 500g carrots + 2 whole carrots → three rows
- **Single recipe, single ingredient**: simplest case, no aggregation needed
- **Many recipes, same ingredient**: 5+ recipes all using onions in compatible units → one row with correct total
- **Empty meal plan**: no recipes → empty shopping list
- **Recipe with no approved ingredients**: skipped in aggregation
- **Recipe ingredients with null ingredient_id**: excluded (only approved with resolved ingredient_id)

#### Purchase status derivation

- **Done**: purchased >= required > 0
- **More needed**: 0 < purchased < required (verify delta = required - purchased)
- **Not purchased**: purchased = 0, required > 0
- **No longer needed**: required = 0, purchased > 0 (recipe was removed after purchasing)
- **Edge: purchased exactly equals required**: status is "done", not "more needed"
- **Edge: no purchase record exists**: treated as purchased = 0

#### Integration scenarios (end-to-end calculator)

These test the full pipeline: raw recipe ingredients → scale → convert → aggregate → join purchases → statuses.

- **Headcount increase after purchase**: buy 3 cups flour, then servings goes from 4 to 8 → required doubles, status becomes "more needed" with correct delta
- **Recipe removed after purchase**: buy ingredients, remove recipe → "no longer needed" on those items
- **Recipe added after partial purchase**: existing purchases still apply to the new totals
- **Scaling mode change**: switch from fractional to round-up mid-plan — all quantities recompute
- **Same recipe on multiple days**: Pancakes on day 1 breakfast AND day 3 breakfast — ingredients counted twice
- **Same recipe on same day, different meals**: Pancakes for breakfast AND snack — ingredients counted twice

### Service Tests

- Each action's happy path and error cases
- **GetShoppingListAction**: verify it correctly orchestrates data loading from clients, passes to calculator, joins with purchases, returns correct response shape
- Verify purchase UPSERT behavior (create if not exists, update if exists)
- Verify cascade behavior: deleting a meal plan clears purchases

### Client Tests

- Standard CRUD operations for meal_plans, meal_plan_days, meal_plan_recipes, shopping_list_purchases
- UPSERT on `(meal_plan_id, ingredient_id, unit)` conflict
- CASCADE deletes: delete meal plan → purchases gone. Delete recipe → meal_plan_recipes gone.
- Foreign key constraints: cannot create meal_plan_recipe with invalid recipe_id

### Acceptance Tests

- Full API workflow: create meal plan → add days → add recipes → get shopping list → update purchases → verify statuses
- Shopping list reflects live recipe changes (update a recipe's ingredient quantity via recipe API, then re-fetch shopping list)
- Headcount change flow: update servings → shopping list quantities change → purchase shortfalls visible
- Template copy: copy template to trip → shopping list computable on new trip plan (no purchases carried over)

## Open Questions

None — all requirements clarified with user.
