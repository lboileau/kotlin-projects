# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/add-ingredents-to-shopping-list/camper

## Feature Name
add-ingredients-to-shopping-list

## Plan
to be created by architect

## Feature Description

Add the ability for users to manually add individual items to a meal plan's shopping list. Currently, shopping list items are entirely computed from recipe ingredients scaled by party size. This feature introduces **manual items** that exist alongside calculated items but behave differently:

- **Do not scale** with party size — the user sets an explicit quantity.
- **Survive resets** — resetting the shopping list resets their purchased quantity to 0 but does not remove them.
- **Can be removed** — users can delete manual items. This removal capability is exclusive to manual items (recipe-calculated items cannot be removed).

### Two flavors of manual items

1. **Ingredient-based**: User picks from the existing `ingredients` table (typeahead search) or creates a new ingredient using the same creation flow as the recipes/ingredients page. User provides a quantity and unit (e.g., "2 lb ground beef").

2. **Free-form**: User types a freeform text description (e.g., "paper plates" or "3 bags of ice"). Always stored as quantity 1 with no unit. The text description captures everything including any implicit quantity/unit the user intends. Free-form items should be categorized as `misc`.

### Purchase tracking

Manual items use the same purchase model as calculated items:
- `quantityRequired` = the user-specified quantity (or 1 for free-form)
- `quantityPurchased` = tracked via the existing purchase flow
- Status derivation (DONE / MORE_NEEDED / NOT_PURCHASED) works identically
- Reset sets `quantityPurchased` back to 0, keeps the item and its `quantityRequired`

## Entities

### New: `shopping_list_manual_items`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | UUID | PK | |
| meal_plan_id | UUID | FK → meal_plans(id) ON DELETE CASCADE, NOT NULL | |
| ingredient_id | UUID | FK → ingredients(id), NULLABLE | NULL for free-form items |
| description | TEXT | NULLABLE | For free-form items only (e.g., "paper plates") |
| quantity | NUMERIC | NOT NULL, DEFAULT 1 | User-specified quantity (always 1 for free-form) |
| unit | VARCHAR(20) | NULLABLE | NULL for free-form items |
| quantity_purchased | NUMERIC | NOT NULL, DEFAULT 0 | Tracks how much has been purchased |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() | |

**Constraints:**
- CHECK: either `ingredient_id` is NOT NULL (ingredient-based) or `description` is NOT NULL (free-form) — at least one must be set
- UNIQUE on `(meal_plan_id, ingredient_id, unit)` WHERE `ingredient_id IS NOT NULL` — prevent duplicate ingredient+unit combos
- No uniqueness constraint on free-form items (user can add multiple distinct free-form items)

**Design note:** Purchase tracking is embedded in this table (via `quantity_purchased`) rather than using `shopping_list_purchases`, because manual items have a 1:1 relationship between the item and its purchase state. The existing `shopping_list_purchases` table is for computed/aggregated recipe ingredients which have no stored "item" row.

## API Surface

### New endpoints

**Add manual item to shopping list**
```
POST /api/meal-plans/{id}/shopping-list/items
```

Request body (ingredient-based):
```json
{
  "ingredientId": "uuid",
  "quantity": 2,
  "unit": "lb"
}
```

Request body (free-form):
```json
{
  "description": "paper plates"
}
```

Response (201):
```json
{
  "id": "uuid",
  "ingredientId": "uuid-or-null",
  "ingredientName": "ground beef",
  "description": null,
  "quantity": 2,
  "unit": "lb",
  "quantityPurchased": 0,
  "status": "not_purchased",
  "category": "meat"
}
```

For free-form items, `ingredientId`, `ingredientName`, and `unit` are null; `category` is `"misc"`; `quantity` is 1.

**Remove manual item from shopping list**
```
DELETE /api/meal-plans/{id}/shopping-list/items/{itemId}
```

Response: 204 No Content

This only works for manual items. If the itemId doesn't correspond to a manual item, return 404.

### Modified endpoints

**GET /api/meal-plans/{id}/shopping-list**

The response should include manual items merged into the existing category-based structure. Each `ShoppingListItemResponse` needs a new field to distinguish item types:

```json
{
  "ingredientId": "uuid-or-null",
  "ingredientName": "ground beef",
  "description": null,
  "quantityRequired": 2,
  "quantityPurchased": 0,
  "unit": "lb",
  "status": "not_purchased",
  "usedInRecipes": [],
  "source": "manual",
  "manualItemId": "uuid"
}
```

New fields:
- `source`: `"recipe"` for calculated items, `"manual"` for manual items
- `manualItemId`: UUID of the manual item (null for recipe items) — needed for the DELETE endpoint
- `description`: text description for free-form items (null otherwise)

For free-form items, add a `"misc"` category to group them.

**Note on aggregation:** If a manual item has the same `ingredientId` + compatible unit as a calculated item, they should appear as **separate rows** (not aggregated together). This keeps the manual item independently trackable and removable. The `source` field distinguishes them.

**PATCH /api/meal-plans/{id}/shopping-list** (update purchase)

Extend to support updating purchases on manual items. The request should accept `manualItemId` as an alternative to `ingredientId`:

```json
{
  "manualItemId": "uuid",
  "quantityPurchased": 2
}
```

The existing `ingredientId`-based flow continues to work for recipe-calculated items.

**DELETE /api/meal-plans/{id}/shopping-list** (reset)

Modified behavior:
- Still deletes all `shopping_list_purchases` records (for recipe-calculated items)
- For manual items: resets `quantity_purchased` to 0 but does NOT delete the rows

## Database Changes

- One new table: `shopping_list_manual_items` (described above)
- No changes to existing tables

## Special Considerations

- **No scaling**: Manual items are never passed through `ShoppingListCalculator`. They bypass the scale/aggregate pipeline entirely.
- **Category for free-form**: Free-form items always use category `"misc"`. Ingredient-based manual items use the ingredient's category from the `ingredients` table.
- **Template behavior**: When a meal plan is copied to a trip (`copy-to-trip`) or saved as a template (`save-as-template`), manual shopping list items should NOT be copied. They are trip-specific shopping additions, not part of the meal plan structure.
- **Ingredient deletion**: If an ingredient referenced by a manual item is deleted, the CASCADE or application logic should handle cleanup. Follow the same pattern as `recipe_ingredients`.
- **Validation**: Validate that `unit` is a recognized unit from the existing unit system. Validate that `quantity` is positive.

## Notes

- The `shopping_list_manual_items` table embeds purchase tracking directly (via `quantity_purchased`) rather than using the separate `shopping_list_purchases` table. This is intentional — manual items have a concrete stored row to attach state to, unlike computed items which only exist at read time.
- The existing `ShoppingListCalculator` in `libs/meal-plan-calculator` should not need changes. Manual items are merged into the response at the service/action layer after calculation.
