# Trip Meal Plans

## Summary

Add meal planning to trips using the existing recipe catalog. A meal plan organizes recipes into numbered days and meal types (breakfast, lunch, dinner, snack). Meal plans can exist as reusable templates or be bound to a specific trip. Trip meal plans can be created from templates, and trip meal plans can be saved back as new templates. The system computes a shopping list by aggregating all recipe ingredients (scaled by servings), grouped by category. Users can mark ingredients as purchased, and recipe completion status is derived from whether all of a recipe's ingredients have been purchased.

## Entities

### MealPlan

The top-level container. Can be a **template** (no trip association) or **trip-bound** (linked to a plan).

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| plan_id | UUID? | FK → plans.id CASCADE, UNIQUE when not null. Null = template |
| name | VARCHAR(255) | NOT NULL |
| servings | INT | NOT NULL, CHECK > 0. Target servings for scaling |
| scaling_mode | VARCHAR(20) | NOT NULL, CHECK IN ('fractional', 'round_up'). Default 'round_up' |
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

A recipe placed into a specific meal slot on a specific day. Duplicate recipes (same recipe on different meals or days) are distinct records.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_day_id | UUID | FK → meal_plan_days.id CASCADE |
| meal_type | VARCHAR(20) | NOT NULL, CHECK IN ('breakfast', 'lunch', 'dinner', 'snack') |
| recipe_id | UUID | FK → recipes.id RESTRICT |
| base_servings | INT | NOT NULL, CHECK > 0. Snapshot of recipe.base_servings at time of addition |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

### MealPlanRecipeIngredient

A copy of a recipe's ingredients at the time the recipe was added to the meal plan. Quantities are stored unscaled (original recipe quantities). Scaling is computed at read time.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_recipe_id | UUID | FK → meal_plan_recipes.id CASCADE |
| ingredient_id | UUID | FK → ingredients.id RESTRICT |
| quantity | NUMERIC | NOT NULL, CHECK > 0 |
| unit | VARCHAR(20) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Notes:**
- Only approved recipe_ingredients with a resolved ingredient_id are copied
- Quantities are the original recipe quantities — scaling is applied at read time using `meal_plan.servings / meal_plan_recipe.base_servings`

### ShoppingListItem

Tracks purchase status per ingredient per meal plan.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_id | UUID | FK → meal_plans.id CASCADE |
| ingredient_id | UUID | FK → ingredients.id RESTRICT |
| purchased | BOOLEAN | NOT NULL, DEFAULT false |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Invariants:**
- UNIQUE(meal_plan_id, ingredient_id) — one purchase status per ingredient per meal plan

## Scaling Logic

Scaling is applied at **read time** (service layer), not stored in the DB.

- **Scale factor** = `meal_plan.servings / meal_plan_recipe.base_servings`
- **Fractional mode**: `scaled_quantity = quantity * scale_factor` (exact, may produce decimals)
- **Round-up mode**: `scaled_quantity = quantity * ceil(scale_factor)` (round the multiplier up to nearest whole number, then multiply)

Shopping list totals are computed by summing all scaled `meal_plan_recipe_ingredients` grouped by `ingredient_id`.

## Recipe Completion

A `MealPlanRecipe` is considered **fully purchased** when every one of its `meal_plan_recipe_ingredients` has its `ingredient_id` marked as `purchased = true` in the `shopping_list_items` table for that meal plan.

This is computed at read time — no stored field.

## API Surface

### Meal Plans

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | /api/meal-plans | Create meal plan | `{ name, servings, scalingMode, isTemplate, planId? }` | 201 MealPlan |
| GET | /api/meal-plans/{id} | Get meal plan with full details (days, meals, recipes, ingredients) | — | 200 MealPlanDetail |
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
| DELETE | /api/meal-plans/{mealPlanId}/days/{dayId} | Remove a day (cascades recipes & ingredients) | — | 204 |

### Recipes on Meals

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | /api/meal-plans/{mealPlanId}/days/{dayId}/recipes | Add recipe to a meal on a day | `{ mealType, recipeId }` | 201 MealPlanRecipeDetail |
| DELETE | /api/meal-plan-recipes/{mealPlanRecipeId} | Remove recipe from meal | — | 204 |

### Shopping List

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| GET | /api/meal-plans/{id}/shopping-list | Get aggregated shopping list | — | 200 ShoppingList |
| PATCH | /api/meal-plans/{id}/shopping-list/{ingredientId} | Toggle purchased status | `{ purchased }` | 200 ShoppingListItem |
| DELETE | /api/meal-plans/{id}/shopping-list | Reset all purchased statuses | — | 204 |

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
            "isFullyPurchased": true,
            "ingredients": [
              {
                "id": "uuid",
                "ingredientId": "uuid",
                "ingredientName": "Flour",
                "category": "pantry",
                "quantity": 2.0,
                "scaledQuantity": 2.0,
                "unit": "cup",
                "purchased": true
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
  "totalIngredients": 12,
  "purchasedCount": 5,
  "categories": [
    {
      "category": "produce",
      "items": [
        {
          "ingredientId": "uuid",
          "ingredientName": "Onion",
          "totalQuantity": 6.0,
          "unit": "whole",
          "purchased": false,
          "usedInRecipes": ["Chili", "Pasta Sauce"]
        }
      ]
    }
  ]
}
```

## Database Changes

### New Tables (5)

1. **meal_plans** — meal plan container (template or trip-bound)
2. **meal_plan_days** — numbered days within a meal plan
3. **meal_plan_recipes** — recipe instances placed on a day/meal
4. **meal_plan_recipe_ingredients** — copied ingredients per recipe instance
5. **shopping_list_items** — purchase tracking per ingredient per meal plan

### Migrations

- `V018__create_meal_plans.sql`
- `V019__create_meal_plan_days.sql`
- `V020__create_meal_plan_recipes.sql`
- `V021__create_meal_plan_recipe_ingredients.sql`
- `V022__create_shopping_list_items.sql`

### Indexes

- `meal_plans(plan_id)` — lookup by trip
- `meal_plans(is_template)` — filter templates
- `meal_plan_days(meal_plan_id)` — list days for a plan
- `meal_plan_recipes(meal_plan_day_id)` — list recipes for a day
- `meal_plan_recipe_ingredients(meal_plan_recipe_id)` — list ingredients for a recipe
- `shopping_list_items(meal_plan_id)` — list items for a plan

## Client Interface

### meal-plan-client

New client module: `clients/meal-plan-client`
Package: `com.acme.clients.mealplanclient`

#### Models

```kotlin
data class MealPlan(id, planId?, name, servings, scalingMode, isTemplate, sourceTemplateId?, createdBy, createdAt, updatedAt)
data class MealPlanDay(id, mealPlanId, dayNumber, createdAt, updatedAt)
data class MealPlanRecipe(id, mealPlanDayId, mealType, recipeId, baseServings, createdAt, updatedAt)
data class MealPlanRecipeIngredient(id, mealPlanRecipeId, ingredientId, quantity: BigDecimal, unit, createdAt, updatedAt)
data class ShoppingListItem(id, mealPlanId, ingredientId, purchased, createdAt, updatedAt)
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

    // Recipe ingredients
    fun addRecipeIngredients(param: AddRecipeIngredientsParam): Result<List<MealPlanRecipeIngredient>, AppError>
    fun getRecipeIngredients(param: GetRecipeIngredientsParam): Result<List<MealPlanRecipeIngredient>, AppError>
    fun getRecipeIngredientsByMealPlanId(param: GetRecipeIngredientsByMealPlanIdParam): Result<List<MealPlanRecipeIngredient>, AppError>

    // Shopping list
    fun getShoppingListItems(param: GetShoppingListItemsParam): Result<List<ShoppingListItem>, AppError>
    fun upsertShoppingListItem(param: UpsertShoppingListItemParam): Result<ShoppingListItem, AppError>
    fun deleteShoppingListItems(param: DeleteShoppingListItemsParam): Result<Unit, AppError>
}
```

## Service Layer

### Feature: meal-plan

Package: `com.acme.services.camperservice.features.mealplan`

#### Actions

| Action | Description |
|--------|-------------|
| CreateMealPlanAction | Validate and create a new meal plan (template or trip-bound) |
| GetMealPlanDetailAction | Fetch meal plan with all days, recipes, ingredients, compute scaling and purchase status |
| GetMealPlanByPlanIdAction | Get the meal plan for a specific trip |
| GetTemplatesAction | List all template meal plans |
| UpdateMealPlanAction | Update name, servings, scaling mode |
| DeleteMealPlanAction | Delete a meal plan |
| CopyToTripAction | Deep-copy a template meal plan to a trip (days, recipes, ingredients, shopping list) |
| SaveAsTemplateAction | Deep-copy a trip meal plan as a new template |
| AddDayAction | Add a numbered day to a meal plan |
| RemoveDayAction | Remove a day (cascade deletes recipes, ingredients) |
| AddRecipeToMealAction | Add a recipe to a day/meal, copy its ingredients, upsert shopping list items |
| RemoveRecipeFromMealAction | Remove a recipe from a meal, clean up orphaned shopping list items |
| GetShoppingListAction | Aggregate all ingredients with scaling, group by category, include purchase status and recipe references |
| TogglePurchasedAction | Toggle purchased status for an ingredient in the shopping list |
| ResetShoppingListAction | Reset all purchased statuses to false |

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
    data class IngredientNotFound(val id: UUID) : MealPlanError()
}
```

## PR Stack

| # | Branch Suffix | Title | Description |
|---|--------------|-------|-------------|
| 1 | plan | feat(meal-plans): plan | This document |
| 2 | db-contracts | feat(meal-plans): db contracts | Schema files + migration SQL for 5 tables |
| 3 | client-contracts | feat(meal-plans): client contracts | MealPlanClient interface, param objects, model types, fake stubs |
| 4 | service-contracts | feat(meal-plans): service contracts | DTOs, error types, action signatures, controller routes (501s) |
| 5 | db-impl | feat(meal-plans): db implementation | Seed data, verify migrations |
| 6 | client-impl | feat(meal-plans): client implementation | JDBI operations, row adapters, factory, fake client |
| 7 | service-impl | feat(meal-plans): service implementation | Actions, service facade, controller, error mapping, config |
| 8 | client-tests | feat(meal-plans): client tests | Integration tests with Testcontainers |
| 9 | service-tests | feat(meal-plans): service tests | Unit tests with fake client |
| 10 | acceptance-tests | feat(meal-plans): acceptance tests | End-to-end API tests |
| 11 | docs | feat(meal-plans): update documentation and skills | CLAUDE.md, README updates, skill updates |

## Open Questions

None — all requirements clarified with user.
