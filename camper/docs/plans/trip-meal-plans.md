# Trip Meal Plans

## Summary

Add meal planning to trips using the existing recipe catalog. A meal plan organizes recipes into numbered days and meal types (breakfast, lunch, dinner, snack). Meal plans can exist as reusable templates or be bound to a specific trip. Trip meal plans can be created from templates, and trip meal plans can be saved back as new templates.

Recipes are **referenced, not copied** — a meal plan stores pointers to recipes and their servings information. Ingredient data is always read live from the recipe catalog, so updating a recipe automatically reflects in any meal plan that uses it.

Shopping lists are **generated on explicit user request**. When generated, the system aggregates all recipe ingredients (scaled by servings), grouped by category, and creates shopping list items with quantity tracking. If recipes in the meal plan change after generation, the shopping list is marked stale — prompting the user to regenerate (but never auto-regenerating).

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
| shopping_list_stale | BOOLEAN | NOT NULL, DEFAULT false. Set true when recipes are added/removed after shopping list generation |
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

### ShoppingListItem

Tracks quantity and purchase status per recipe ingredient per meal plan. Generated on explicit user request via the generate endpoint — not auto-created when recipes are added.

| Field | Type | Constraints |
|-------|------|-------------|
| id | UUID | PK, auto-generated |
| meal_plan_id | UUID | FK → meal_plans.id CASCADE |
| recipe_ingredient_id | UUID | FK → recipe_ingredients.id CASCADE |
| quantity_required | NUMERIC | NOT NULL, CHECK >= 0. Scaled quantity at time of generation |
| quantity_purchased | NUMERIC | NOT NULL, DEFAULT 0, CHECK >= 0 |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Invariants:**
- UNIQUE(meal_plan_id, recipe_ingredient_id) — one entry per recipe ingredient per meal plan

**Notes:**
- `quantity_required` is computed during generation: `recipe_ingredient.quantity * scale_factor`, summed across all meal plan recipes that use the same recipe ingredient
- `quantity_purchased` defaults to 0. The UI sets it to `quantity_required` on "check" (but the API accepts any value)
- An item is considered fully purchased when `quantity_purchased >= quantity_required`
- When headcount changes and the user regenerates, `quantity_required` updates and the UI can flag items where `quantity_purchased < quantity_required`

## Scaling Logic

Scaling is applied at **read time** (service layer) for display, and at **generation time** for shopping list quantities.

- **Scale factor** = `meal_plan.servings / recipe.base_servings` (read live from the recipe)
- **Fractional mode**: `scale_factor` used as-is (exact ratio, may produce decimals). E.g., 6 people / 4-serving recipe = 1.5x multiplier
- **Round-up mode**: `ceil(scale_factor)` rounded up to the nearest whole number. This means quantities are always a whole-number multiple of the recipe's base. E.g., 6 people / 4-serving recipe → ceil(1.5) = 2x multiplier (scales to 8 servings worth)

Scaled quantity for a single recipe ingredient:
- **Fractional**: `quantity * (meal_plan.servings / recipe.base_servings)`
- **Round-up**: `quantity * ceil(meal_plan.servings / recipe.base_servings)`

Shopping list totals are computed by summing scaled quantities across all meal plan recipes, grouped by `recipe_ingredient_id`.

## Recipe Completion

A `MealPlanRecipe` is considered **fully purchased** when every one of its recipe's approved ingredients (with a resolved `ingredient_id`) has a corresponding `shopping_list_items` entry where `quantity_purchased >= quantity_required`.

This is computed at read time — no stored field. Only meaningful when a shopping list has been generated.

## Shopping List Lifecycle

1. User adds recipes to meal plan days (no shopping list yet)
2. User explicitly hits **"Generate Shopping List"** → `POST /api/meal-plans/{id}/shopping-list/generate`
   - System reads all recipes in the meal plan, scales ingredients, aggregates by `recipe_ingredient_id`
   - Creates/replaces `shopping_list_items` with computed `quantity_required`, resets `quantity_purchased` to 0
   - Sets `shopping_list_stale = false` on the meal plan
3. User checks off items (PATCH updates `quantity_purchased`)
4. If recipes are added/removed from the meal plan, or servings/scaling mode changes:
   - Set `meal_plan.shopping_list_stale = true`
   - UI shows a prompt: "Shopping list is out of date. Regenerate?"
   - System does **NOT** auto-regenerate
5. User hits "Regenerate" → same as step 2 (replaces all items, resets purchases)

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
| POST | /api/meal-plans/{id}/shopping-list/generate | Generate (or regenerate) shopping list | — | 201 ShoppingList |
| GET | /api/meal-plans/{id}/shopping-list | Get aggregated shopping list | — | 200 ShoppingList |
| PATCH | /api/meal-plans/{id}/shopping-list/{recipeIngredientId} | Update purchased quantity | `{ quantityPurchased }` | 200 ShoppingListItem |
| DELETE | /api/meal-plans/{id}/shopping-list | Delete all shopping list items | — | 204 |

**Total: 15 endpoints**

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
  "shoppingListStale": false,
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
  "shoppingListStale": false,
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
  "isStale": false,
  "totalItems": 12,
  "fullyPurchasedCount": 5,
  "categories": [
    {
      "category": "produce",
      "items": [
        {
          "recipeIngredientId": "uuid",
          "ingredientId": "uuid",
          "ingredientName": "Onion",
          "quantityRequired": 6.0,
          "quantityPurchased": 6.0,
          "unit": "whole",
          "isFullyPurchased": true,
          "usedInRecipes": ["Chili", "Pasta Sauce"]
        }
      ]
    }
  ]
}
```

## Database Changes

### New Tables (4)

1. **meal_plans** — meal plan container (template or trip-bound), includes `shopping_list_stale` flag
2. **meal_plan_days** — numbered days within a meal plan
3. **meal_plan_recipes** — recipe references placed on a day/meal (no ingredient copies)
4. **shopping_list_items** — quantity tracking per recipe ingredient per meal plan (generated on request)

### Migrations

- `V018__create_meal_plans.sql`
- `V019__create_meal_plan_days.sql`
- `V020__create_meal_plan_recipes.sql`
- `V021__create_shopping_list_items.sql`

### Indexes

- `meal_plans(plan_id)` — lookup by trip
- `meal_plans(is_template)` — filter templates
- `meal_plan_days(meal_plan_id)` — list days for a plan
- `meal_plan_recipes(meal_plan_day_id)` — list recipes for a day
- `meal_plan_recipes(recipe_id)` — find meal plan recipes by recipe
- `shopping_list_items(meal_plan_id)` — list items for a plan
- `shopping_list_items(recipe_ingredient_id)` — lookup by recipe ingredient

## Client Interface

### meal-plan-client

New client module: `clients/meal-plan-client`
Package: `com.acme.clients.mealplanclient`

#### Models

```kotlin
data class MealPlan(id, planId?, name, servings, scalingMode, isTemplate, sourceTemplateId?, shoppingListStale, createdBy, createdAt, updatedAt)
data class MealPlanDay(id, mealPlanId, dayNumber, createdAt, updatedAt)
data class MealPlanRecipe(id, mealPlanDayId, mealType, recipeId, createdAt, updatedAt)
data class ShoppingListItem(id, mealPlanId, recipeIngredientId, quantityRequired: BigDecimal, quantityPurchased: BigDecimal, createdAt, updatedAt)
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

    // Shopping list
    fun getShoppingListItems(param: GetShoppingListItemsParam): Result<List<ShoppingListItem>, AppError>
    fun upsertShoppingListItems(param: UpsertShoppingListItemsParam): Result<List<ShoppingListItem>, AppError>
    fun updateShoppingListItem(param: UpdateShoppingListItemParam): Result<ShoppingListItem, AppError>
    fun deleteShoppingListItems(param: DeleteShoppingListItemsParam): Result<Unit, AppError>
}
```

**Notes:**
- No recipe ingredient operations on this client — ingredient data is read from the existing `RecipeClient`
- `upsertShoppingListItems` is used by the generate action to bulk create/replace items
- `updateShoppingListItem` is used for updating `quantity_purchased`

## Service Layer

### Feature: meal-plan

Package: `com.acme.services.camperservice.features.mealplan`

#### Actions

| Action | Description |
|--------|-------------|
| CreateMealPlanAction | Validate and create a new meal plan (template or trip-bound) |
| GetMealPlanDetailAction | Fetch meal plan with all days, recipes, live ingredients from recipe catalog, compute scaling and purchase status |
| GetMealPlanByPlanIdAction | Get the meal plan for a specific trip |
| GetTemplatesAction | List all template meal plans |
| UpdateMealPlanAction | Update name, servings, scaling mode. Marks shopping list stale if servings or scaling mode changed. |
| DeleteMealPlanAction | Delete a meal plan (cascades everything) |
| CopyToTripAction | Deep-copy a template meal plan to a trip (days, recipes — no shopping list, user generates separately) |
| SaveAsTemplateAction | Deep-copy a trip meal plan as a new template (days, recipes — no shopping list) |
| AddDayAction | Add a numbered day to a meal plan |
| RemoveDayAction | Remove a day (cascade deletes recipes). Marks shopping list stale. |
| AddRecipeToMealAction | Add a recipe reference to a day/meal. Marks shopping list stale. |
| RemoveRecipeFromMealAction | Remove a recipe from a meal. Marks shopping list stale. |
| GenerateShoppingListAction | Read all recipes in meal plan, scale ingredients, aggregate by recipe_ingredient_id, upsert shopping list items. Resets stale flag. |
| GetShoppingListAction | Read shopping list items, join with ingredient names/categories, group by category, include recipe references |
| UpdatePurchasedQuantityAction | Update quantity_purchased for a shopping list item |
| DeleteShoppingListAction | Delete all shopping list items for a meal plan |

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
    data class ShoppingListItemNotFound(val recipeIngredientId: UUID) : MealPlanError()
    data class NoRecipesInMealPlan(val mealPlanId: UUID) : MealPlanError()
}
```

## PR Stack

| # | Branch Suffix | Title | Description |
|---|--------------|-------|-------------|
| 1 | plan | feat(meal-plans): plan | This document |
| 2 | db-contracts | feat(meal-plans): db contracts | Schema files + migration SQL for 4 tables |
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
