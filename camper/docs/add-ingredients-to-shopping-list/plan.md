# Plan: Add Ingredients to Shopping List

## Feature Summary

Add the ability for users to manually add individual items to a meal plan's shopping list. Manual items exist alongside recipe-calculated items but do not scale with party size, survive resets (quantity_purchased resets to 0), and can be individually removed. Two flavors: **ingredient-based** (user picks from ingredients table, provides quantity + unit) and **free-form** (user types a text description, stored as quantity 1 with no unit, categorized as "misc").

---

## Entities

### New: `shopping_list_manual_items`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| `id` | `UUID` | PK, DEFAULT `gen_random_uuid()` | |
| `meal_plan_id` | `UUID` | FK -> `meal_plans(id)` ON DELETE CASCADE, NOT NULL | |
| `ingredient_id` | `UUID` | FK -> `ingredients(id)` ON DELETE CASCADE, NULLABLE | NULL for free-form items |
| `description` | `TEXT` | NULLABLE | For free-form items only (e.g., "paper plates") |
| `quantity` | `NUMERIC` | NOT NULL, DEFAULT 1 | User-specified quantity (always 1 for free-form) |
| `unit` | `VARCHAR(20)` | NULLABLE | NULL for free-form items |
| `quantity_purchased` | `NUMERIC` | NOT NULL, DEFAULT 0 | Tracks how much has been purchased |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `now()` | |

**Constraints:**
- `CHECK (ingredient_id IS NOT NULL OR description IS NOT NULL)` -- at least one must be set
- `CHECK (quantity > 0)` -- quantity must be positive
- `CHECK (quantity_purchased >= 0)` -- purchased cannot be negative
- `UNIQUE (meal_plan_id, ingredient_id, unit) WHERE ingredient_id IS NOT NULL` -- prevent duplicate ingredient+unit combos
- No uniqueness constraint on free-form items

---

## API Surface

### New Endpoints

| Method | Path | Description | Request Body | Response |
|---|---|---|---|---|
| `POST` | `/api/meal-plans/{id}/shopping-list/items` | Add manual item | `AddManualItemRequest` | 201 + `ManualItemResponse` |
| `DELETE` | `/api/meal-plans/{id}/shopping-list/items/{itemId}` | Remove manual item | — | 204 |

### Modified Endpoints

| Method | Path | Change |
|---|---|---|
| `GET` | `/api/meal-plans/{id}/shopping-list` | Response items gain `source`, `manualItemId`, `description` fields; manual items merged into categories |
| `PATCH` | `/api/meal-plans/{id}/shopping-list` | Request accepts `manualItemId` as alternative to `ingredientId` |
| `DELETE` | `/api/meal-plans/{id}/shopping-list` | Also resets `quantity_purchased` to 0 on manual items (does not delete them) |

### Request/Response Shapes

**AddManualItemRequest** (new):
```kotlin
data class AddManualItemRequest(
    val ingredientId: UUID? = null,
    val description: String? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
)
```
- Ingredient-based: `ingredientId` required, `quantity` required, `unit` required. `description` must be null.
- Free-form: `description` required. `ingredientId`, `quantity`, `unit` must be null.

**ManualItemResponse** (new):
```kotlin
data class ManualItemResponse(
    val id: UUID,
    val ingredientId: UUID?,
    val ingredientName: String?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
    val quantityPurchased: BigDecimal,
    val status: String,
    val category: String,
)
```

**ShoppingListItemResponse** (modified -- 3 new fields):
```kotlin
data class ShoppingListItemResponse(
    val ingredientId: UUID?,          // was non-nullable UUID -- now nullable for free-form
    val ingredientName: String?,      // was non-nullable String -- now nullable for free-form
    val description: String?,         // NEW: text for free-form items, null otherwise
    val quantityRequired: BigDecimal,
    val quantityPurchased: BigDecimal,
    val unit: String?,                // was non-nullable String -- now nullable for free-form
    val status: String,
    val usedInRecipes: List<String>,
    val source: String,               // NEW: "recipe" or "manual"
    val manualItemId: UUID?,          // NEW: UUID for manual items, null for recipe items
)
```

**UpdatePurchaseRequest** (modified -- new optional field):
```kotlin
data class UpdatePurchaseRequest(
    val ingredientId: UUID? = null,        // now nullable (one of ingredientId or manualItemId required)
    val manualItemId: UUID? = null,        // NEW
    val unit: String? = null,              // now nullable (not needed for manual items)
    val quantityPurchased: BigDecimal,
)
```

---

## Database Changes

### Migration: `V030__create_shopping_list_manual_items.sql`

```sql
CREATE TABLE IF NOT EXISTS shopping_list_manual_items (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id       UUID        NOT NULL,
    ingredient_id      UUID,
    description        TEXT,
    quantity           NUMERIC     NOT NULL DEFAULT 1,
    unit               VARCHAR(20),
    quantity_purchased NUMERIC     NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_manual_items_has_ingredient_or_description
        CHECK (ingredient_id IS NOT NULL OR description IS NOT NULL),
    CONSTRAINT ck_manual_items_quantity_positive
        CHECK (quantity > 0),
    CONSTRAINT ck_manual_items_purchased_non_negative
        CHECK (quantity_purchased >= 0),
    CONSTRAINT fk_manual_items_meal_plan
        FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_manual_items_ingredient
        FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_manual_items_meal_plan_ingredient_unit
    ON shopping_list_manual_items (meal_plan_id, ingredient_id, unit)
    WHERE ingredient_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_manual_items_meal_plan_id
    ON shopping_list_manual_items (meal_plan_id);
```

### Schema file: `schema/tables/030_shopping_list_manual_items.sql`

Same content as migration (readable reference copy).

---

## Client Layer

### New model: `ShoppingListManualItem`

File: `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/model/ShoppingListManualItem.kt`

```kotlin
data class ShoppingListManualItem(
    val id: UUID,
    val mealPlanId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
    val quantityPurchased: BigDecimal,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### New param objects

Added to `MealPlanClientParams.kt`:

```kotlin
/** Parameter for adding a manual item to a shopping list. */
data class AddManualItemParam(
    val mealPlanId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
)

/** Parameter for retrieving all manual items for a meal plan. */
data class GetManualItemsParam(val mealPlanId: UUID)

/** Parameter for removing a manual item by ID. */
data class RemoveManualItemParam(val id: UUID)

/** Parameter for updating the purchased quantity of a manual item. */
data class UpdateManualItemPurchaseParam(
    val id: UUID,
    val quantityPurchased: BigDecimal,
)

/** Parameter for resetting all manual item purchases for a meal plan. */
data class ResetManualItemPurchasesParam(val mealPlanId: UUID)
```

### New methods on `MealPlanClient` interface

```kotlin
// --- Shopping List Manual Items ---

/** Add a manual item to a meal plan's shopping list. */
fun addManualItem(param: AddManualItemParam): Result<ShoppingListManualItem, AppError>

/** Retrieve all manual items for a meal plan. */
fun getManualItems(param: GetManualItemsParam): Result<List<ShoppingListManualItem>, AppError>

/** Remove a manual item by its unique identifier. Returns NotFoundError if not found. */
fun removeManualItem(param: RemoveManualItemParam): Result<Unit, AppError>

/** Update the purchased quantity of a manual item. Returns NotFoundError if not found. */
fun updateManualItemPurchase(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError>

/** Reset quantity_purchased to 0 for all manual items in a meal plan. */
fun resetManualItemPurchases(param: ResetManualItemPurchasesParam): Result<Unit, AppError>
```

### New internal files (client implementation)

| File | Purpose |
|---|---|
| `internal/adapters/ShoppingListManualItemRowAdapter.kt` | Maps ResultSet -> `ShoppingListManualItem` |
| `internal/operations/AddManualItem.kt` | INSERT with UUID generation |
| `internal/operations/GetManualItems.kt` | SELECT WHERE meal_plan_id, ORDER BY created_at |
| `internal/operations/RemoveManualItem.kt` | DELETE WHERE id, check rows affected for NotFound |
| `internal/operations/UpdateManualItemPurchase.kt` | UPDATE quantity_purchased + updated_at WHERE id |
| `internal/operations/ResetManualItemPurchases.kt` | UPDATE SET quantity_purchased = 0 WHERE meal_plan_id |
| `internal/validations/ValidateAddManualItem.kt` | quantity > 0, unit in VALID_UNITS (if ingredient-based), ingredient_id or description set |
| `internal/validations/ValidateGetManualItems.kt` | No-op |
| `internal/validations/ValidateRemoveManualItem.kt` | No-op |
| `internal/validations/ValidateUpdateManualItemPurchase.kt` | quantityPurchased >= 0 |
| `internal/validations/ValidateResetManualItemPurchases.kt` | No-op |

### Fake updates

`FakeMealPlanClient` gains:
- `private val manualItems = ConcurrentHashMap<UUID, ShoppingListManualItem>()`
- Implementations of all 5 new methods with in-memory logic
- `fun seedManualItems(vararg entities: ShoppingListManualItem)` helper
- `resetManualItemPurchases` updates all items for the meal plan to `quantityPurchased = 0`
- `addManualItem` enforces the partial unique index (meal_plan_id, ingredient_id, unit) for ingredient-based items

### JdbiMealPlanClient updates

Wire the 5 new operations into the facade, delegating to each operation's `execute()`.

---

## Service Layer

### New service params

Added to `MealPlanServiceParams.kt`:

```kotlin
data class AddManualItemParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val ingredientId: UUID?,
    val description: String?,
    val quantity: BigDecimal?,
    val unit: String?,
)

data class RemoveManualItemParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val itemId: UUID,
)
```

### Modified service params

`UpdatePurchaseParam` gains `manualItemId: UUID?`:

```kotlin
data class UpdatePurchaseParam(
    val mealPlanId: UUID,
    val userId: UUID,
    val ingredientId: UUID?,       // was non-nullable -- now nullable
    val manualItemId: UUID?,       // NEW
    val unit: String?,             // was non-nullable -- now nullable
    val quantityPurchased: BigDecimal,
)
```

### New error type

Added to `MealPlanError`:

```kotlin
data class ManualItemNotFound(val id: UUID) : MealPlanError("Manual item not found: $id")
data class DuplicateManualItem(val ingredientId: UUID, val unit: String) : MealPlanError("Manual item already exists for ingredient $ingredientId with unit $unit")
```

### New error-to-HTTP mapping

In `ResultExtensions.kt`, add to `MealPlanError.toResponseEntity()`:

```kotlin
is MealPlanError.ManualItemNotFound -> ResponseEntity.status(404)
    .body(ApiResponse.ErrorBody("NOT_FOUND", message))
is MealPlanError.DuplicateManualItem -> ResponseEntity.status(409)
    .body(ApiResponse.ErrorBody("CONFLICT", message))
```

### New actions

**AddManualItemAction** (`actions/AddManualItemAction.kt`):
1. Validate params (ValidateAddManualItem)
2. Verify meal plan exists
3. If ingredient-based: verify ingredient exists via `ingredientClient.getById()`; get category from ingredient
4. Convert to `ClientAddManualItemParam` (set quantity=1 for free-form)
5. Call `mealPlanClient.addManualItem()`
6. Map to `ManualItemResponse` (resolve ingredientName and category)
7. On conflict (duplicate ingredient+unit): return `DuplicateManualItem` error

**RemoveManualItemAction** (`actions/RemoveManualItemAction.kt`):
1. Validate params (ValidateRemoveManualItem)
2. Verify meal plan exists
3. Call `mealPlanClient.removeManualItem()`
4. Map NotFoundError -> `ManualItemNotFound`

### New validations

**ValidateAddManualItem**:
- Either `ingredientId` or `description` must be provided (not both, not neither)
- If ingredient-based: `quantity` must be non-null and > 0, `unit` must be non-null and in VALID_UNITS
- If free-form: `quantity`, `unit`, `ingredientId` must all be null
- `description` if provided must not be blank, max 500 chars

**ValidateRemoveManualItem**: No-op (pass-through)

### Modified actions

**GetShoppingListAction** -- major changes:
1. After computing recipe-based items (existing flow, unchanged), load manual items via `mealPlanClient.getManualItems()`
2. For each ingredient-based manual item: look up ingredient name and category from `ingredientInfoMap` (or load via `ingredientClient.getById()`)
3. For each free-form manual item: use category `"misc"`, `ingredientName` = null, `description` = the item's description
4. Convert each manual item to `ShoppingListItemResponse` with:
   - `source = "manual"`, `manualItemId = item.id`
   - `quantityRequired = item.quantity`, `quantityPurchased = item.quantityPurchased`
   - `unit = item.unit` (null for free-form)
   - `usedInRecipes = emptyList()`
   - Status derived via `PurchaseStatus.derive(item.quantity, item.quantityPurchased)`
5. All existing recipe-based items get `source = "recipe"`, `manualItemId = null`, `description = null`
6. Merge manual items into `allItems` before grouping by category
7. Free-form items grouped under `"misc"` category

**UpdatePurchaseAction** -- branching logic:
1. Validate: exactly one of `ingredientId` or `manualItemId` must be non-null
2. If `manualItemId` is provided:
   - Call `mealPlanClient.updateManualItemPurchase(UpdateManualItemPurchaseParam(id = manualItemId, quantityPurchased = param.quantityPurchased))`
   - Map NotFoundError -> `ManualItemNotFound`
   - Return `ShoppingListPurchaseResponse` adapted from the manual item
3. If `ingredientId` is provided:
   - Existing flow unchanged (upsert into shopping_list_purchases)

**ResetPurchasesAction** -- additional call:
1. Existing: `mealPlanClient.deletePurchases()` (clears recipe purchases)
2. New: `mealPlanClient.resetManualItemPurchases()` (sets quantity_purchased=0 on all manual items)

**CopyToTripAction** -- no changes needed. Manual items are NOT copied (they belong to shopping_list_manual_items which is keyed on meal_plan_id; the new meal plan gets a new ID, so no manual items carry over).

**SaveAsTemplateAction** -- no changes needed. Same reasoning.

### New request DTO

In `MealPlanRequests.kt`:

```kotlin
data class AddManualItemRequest(
    val ingredientId: UUID? = null,
    val description: String? = null,
    val quantity: BigDecimal? = null,
    val unit: String? = null,
)
```

### New response DTO

In `MealPlanResponses.kt`:

```kotlin
data class ManualItemResponse(
    val id: UUID,
    val ingredientId: UUID?,
    val ingredientName: String?,
    val description: String?,
    val quantity: BigDecimal,
    val unit: String?,
    val quantityPurchased: BigDecimal,
    val status: String,
    val category: String,
)
```

### Modified response DTOs

`ShoppingListItemResponse` -- add 3 fields, make 3 fields nullable:

```kotlin
data class ShoppingListItemResponse(
    val ingredientId: UUID?,          // nullable (was non-nullable)
    val ingredientName: String?,      // nullable (was non-nullable)
    val description: String?,         // NEW
    val quantityRequired: BigDecimal,
    val quantityPurchased: BigDecimal,
    val unit: String?,                // nullable (was non-nullable)
    val status: String,
    val usedInRecipes: List<String>,
    val source: String,               // NEW
    val manualItemId: UUID?,          // NEW
)
```

`UpdatePurchaseRequest` -- add optional manualItemId, make existing fields nullable:

```kotlin
data class UpdatePurchaseRequest(
    val ingredientId: UUID? = null,
    val manualItemId: UUID? = null,
    val unit: String? = null,
    val quantityPurchased: BigDecimal,
)
```

### Controller changes

In `MealPlanController.kt`, add two new endpoints:

```kotlin
/** POST /api/meal-plans/{id}/shopping-list/items -- Add manual item */
@PostMapping("/{id}/shopping-list/items")
fun addManualItem(
    @PathVariable id: UUID,
    @RequestHeader("X-User-Id") userId: UUID,
    @RequestBody request: AddManualItemRequest,
): ResponseEntity<Any>

/** DELETE /api/meal-plans/{id}/shopping-list/items/{itemId} -- Remove manual item */
@DeleteMapping("/{id}/shopping-list/items/{itemId}")
fun removeManualItem(
    @PathVariable id: UUID,
    @PathVariable itemId: UUID,
    @RequestHeader("X-User-Id") userId: UUID,
): ResponseEntity<Any>
```

Modify `updatePurchase` to pass `manualItemId` through.

### MealPlanService changes

Add two new methods and their action wiring:

```kotlin
private val addManualItem = AddManualItemAction(mealPlanClient, ingredientClient)
private val removeManualItem = RemoveManualItemAction(mealPlanClient)

fun addManualItem(param: AddManualItemParam) = addManualItem.execute(param)
fun removeManualItem(param: RemoveManualItemParam) = removeManualItem.execute(param)
```

### Validation changes

`ValidateUpdatePurchase` -- updated to validate that exactly one of `ingredientId` or `manualItemId` is provided. When `ingredientId` is used, `unit` must also be non-null.

---

## Integration with Existing Code

### ShoppingListCalculator
No changes. Manual items bypass the calculator entirely. They are merged at the action layer after `aggregateShoppingList()` returns.

### Reset endpoint
`ResetPurchasesAction` makes two calls:
1. `mealPlanClient.deletePurchases(...)` -- existing (clears recipe purchase records)
2. `mealPlanClient.resetManualItemPurchases(...)` -- new (sets quantity_purchased=0)

### Purchase endpoint
`UpdatePurchaseAction` branches on `manualItemId` vs `ingredientId`:
- `manualItemId` present -> `mealPlanClient.updateManualItemPurchase()`
- `ingredientId` present -> `mealPlanClient.upsertPurchase()` (existing)

### Copy-to-trip / Save-as-template
No changes needed. These deep-copy days and recipes to a new meal_plan_id. Manual items are keyed on the source meal_plan_id and are not carried over. This is correct per requirements.

### Category grouping
Free-form manual items use category `"misc"`. Ingredient-based manual items use the ingredient's category from the `ingredients` table. Both merge into the same `categories` list as recipe items.

### Orphaned purchase handling
Manual items have their own `quantity_purchased` and are never in `shopping_list_purchases`, so no interaction with orphan detection. The orphan logic only applies to recipe-based purchases.

---

## PR Stack

### PR 1: `[plan]` feat(shopping-list): plan for manual items
- `camper/docs/add-ingredients-to-shopping-list/plan.md` (this file)

### PR 2: `[db]` feat(shopping-list): manual items table
**Files to create:**
- `databases/camper-db/migrations/V030__create_shopping_list_manual_items.sql`
- `databases/camper-db/schema/tables/030_shopping_list_manual_items.sql`

### PR 3: `[client]` feat(shopping-list): manual items client contracts
**Files to create:**
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/model/ShoppingListManualItem.kt`

**Files to modify:**
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/api/MealPlanClient.kt` -- add 5 new method signatures
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/api/MealPlanClientParams.kt` -- add 5 new param classes

### PR 4: `[service]` feat(shopping-list): manual items service contracts
**Files to modify:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/params/MealPlanServiceParams.kt` -- add `AddManualItemParam`, `RemoveManualItemParam`; modify `UpdatePurchaseParam`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/dto/MealPlanRequests.kt` -- add `AddManualItemRequest`; modify `UpdatePurchaseRequest`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/dto/MealPlanResponses.kt` -- add `ManualItemResponse`; modify `ShoppingListItemResponse`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/error/MealPlanError.kt` -- add `ManualItemNotFound`, `DuplicateManualItem`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/error/ResultExtensions.kt` -- add mapping for new error types

### PR 5: `[db-impl]` feat(shopping-list): manual items migration
Implementation of PR 2 (migration SQL is the implementation).

> **Note:** PR 2 and PR 5 are merged into a single PR since DB contracts = implementation for SQL schemas.

### PR 6: `[client-impl]` feat(shopping-list): manual items client implementation
**Files to create:**
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/adapters/ShoppingListManualItemRowAdapter.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/operations/AddManualItem.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/operations/GetManualItems.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/operations/RemoveManualItem.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/operations/UpdateManualItemPurchase.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/operations/ResetManualItemPurchases.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/validations/ValidateAddManualItem.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/validations/ValidateGetManualItems.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/validations/ValidateRemoveManualItem.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/validations/ValidateUpdateManualItemPurchase.kt`
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/validations/ValidateResetManualItemPurchases.kt`

**Files to modify:**
- `clients/meal-plan-client/src/main/kotlin/com/acme/clients/mealplanclient/internal/JdbiMealPlanClient.kt` -- wire 5 new operations
- `clients/meal-plan-client/src/testFixtures/kotlin/com/acme/clients/mealplanclient/fake/FakeMealPlanClient.kt` -- implement 5 new methods + `seedManualItems()` helper

### PR 7: `[service-impl]` feat(shopping-list): manual items service implementation
**Files to create:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/AddManualItemAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/RemoveManualItemAction.kt`

**Files to modify:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/GetShoppingListAction.kt` -- merge manual items into response
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/UpdatePurchaseAction.kt` -- branch on manualItemId
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/ResetPurchasesAction.kt` -- add resetManualItemPurchases call
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/validations/MealPlanValidations.kt` -- add `ValidateAddManualItem`, `ValidateRemoveManualItem`; modify `ValidateUpdatePurchase`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/service/MealPlanService.kt` -- add 2 new methods, wire actions
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/controller/MealPlanController.kt` -- add 2 new endpoints, modify updatePurchase

### PR 8: `[client-test]` feat(shopping-list): manual items client tests
**Files to create/modify:**
- Unit tests for each new client operation (add, get, remove, updatePurchase, resetPurchases)
- Tests for ValidateAddManualItem (ingredient-based and free-form paths, edge cases)
- Tests for the unique constraint behavior (duplicate ingredient+unit)

### PR 9: `[service-test]` feat(shopping-list): manual items service tests
**Files to modify:**
- `services/camper-service/src/test/kotlin/.../MealPlanServiceTest.kt` -- add tests for:
  - Add ingredient-based manual item
  - Add free-form manual item
  - Remove manual item (success + not found)
  - Get shopping list with manual items merged
  - Update purchase on manual item
  - Reset purchases resets manual item quantities
  - Validation: both ingredientId and description provided
  - Validation: neither provided
  - Duplicate ingredient+unit conflict

### PR 10: `[acceptance]` feat(shopping-list): manual items acceptance tests
**Files to modify:**
- `services/camper-service/src/test/kotlin/.../MealPlanAcceptanceTest.kt` -- add end-to-end tests for:
  - POST add ingredient-based manual item -> 201
  - POST add free-form manual item -> 201
  - GET shopping list includes manual items with correct source/manualItemId
  - DELETE manual item -> 204
  - PATCH purchase update on manual item
  - DELETE reset also resets manual item purchases
  - Free-form items appear in "misc" category
  - Manual items NOT copied in copy-to-trip
  - Manual items NOT copied in save-as-template
  - Duplicate ingredient+unit -> 409

---

## Open Questions

None -- the handoff document is comprehensive and all design decisions are clear.
