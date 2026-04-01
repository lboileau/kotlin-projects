# Gear Pack Grouping — Feature Plan

## Summary

When a gear pack is applied to a plan, the created items should carry a reference back to the gear pack so the frontend can visually group them together. Items within a gear pack group remain fully editable (name, quantity, packed, delete). Users can also add new items directly into an existing gear pack group by selecting the gear pack as a "category" option in the add-item form. This is a lightweight data-threading + UI enhancement — no new endpoints, no new CRUD for gear packs.

## Entities

### items (existing table — modify)

Add one nullable column:

| Field        | Type | Notes                                            |
|--------------|------|--------------------------------------------------|
| gear_pack_id | UUID | Nullable FK to `gear_packs(id) ON DELETE SET NULL` |

When a gear pack is deleted (or hasn't been applied), `gear_pack_id` is `NULL` and the item displays as ungrouped (today's behavior).

### gear_packs (existing table — no changes)

Read-only usage: JOIN to resolve `gear_pack_name` in item queries.

## API Surface

### Modified Endpoints

No new endpoints. Four existing endpoints are modified.

| Method | Path | Change |
|--------|------|--------|
| POST | `/api/items` | Add optional `gearPackId: UUID?` to request body |
| PUT | `/api/items/{id}` | Add optional `gearPackId: UUID?` to request body |
| GET | `/api/items?ownerType=&ownerId=` | Response includes `gearPackId` and `gearPackName` |
| GET | `/api/items/{id}` | Response includes `gearPackId` and `gearPackName` |
| POST | `/api/gear-packs/{id}/apply` | (internal) Pass `gearPackId` when creating items |

### Request Body Changes

**`POST /api/items`** — `CreateItemRequest`
```json
{
  "name": "Spatula",
  "category": "kitchen",
  "quantity": 1,
  "packed": false,
  "ownerType": "plan",
  "ownerId": "uuid",
  "gearPackId": "uuid"         // NEW — optional
}
```

**`PUT /api/items/{id}`** — `UpdateItemRequest`
```json
{
  "name": "Spatula",
  "category": "kitchen",
  "quantity": 1,
  "packed": false,
  "gearPackId": "uuid"         // NEW — optional, explicit null clears it
}
```

Note: `gearPackId` in the update request uses a wrapper to distinguish "not provided" (keep existing) from "explicitly null" (clear it). In practice, the frontend always sends `gearPackId` on update — either the existing value or `null` when the user changes category away from the gear pack.

### Response Body Changes

**`ItemResponse`** — all GET endpoints
```json
{
  "id": "uuid",
  "planId": "uuid",
  "userId": null,
  "name": "Spatula",
  "category": "kitchen",
  "quantity": 1,
  "packed": false,
  "gearPackId": "uuid",           // NEW — nullable
  "gearPackName": "Cooking Equipment",  // NEW — nullable, resolved via JOIN
  "createdAt": "...",
  "updatedAt": "..."
}
```

## Database Changes

### Migration: V033__add_gear_pack_id_to_items.sql

```sql
ALTER TABLE items ADD COLUMN IF NOT EXISTS gear_pack_id UUID;

ALTER TABLE items ADD CONSTRAINT fk_items_gear_pack
    FOREIGN KEY (gear_pack_id) REFERENCES gear_packs (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_items_gear_pack_id ON items (gear_pack_id);
```

### Rollback: R033__remove_gear_pack_id_from_items.sql

```sql
DROP INDEX IF EXISTS idx_items_gear_pack_id;
ALTER TABLE items DROP CONSTRAINT IF EXISTS fk_items_gear_pack;
ALTER TABLE items DROP COLUMN IF EXISTS gear_pack_id;
```

### Schema File Update: 005_items.sql

Add `gear_pack_id` column, FK constraint, and index to the current-state DDL.

## Client Interface Changes

### Module: `clients/item-client`

#### Model: `Item` — add field

```kotlin
data class Item(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID?,       // NEW
    val gearPackName: String?,   // NEW — resolved via LEFT JOIN
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

#### Params: `CreateItemParam` — add field

```kotlin
data class CreateItemParam(
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,   // NEW — optional, defaults to null
)
```

#### Params: `UpdateItemParam` — add field

```kotlin
data class UpdateItemParam(
    val id: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,   // NEW — nullable
)
```

#### Row Adapter: `ItemRowAdapter` — add fields

```kotlin
object ItemRowAdapter {
    fun fromResultSet(rs: ResultSet): Item = Item(
        // ... existing fields ...
        gearPackId = rs.getObject("gear_pack_id", UUID::class.java),
        gearPackName = rs.getString("gear_pack_name"),  // from LEFT JOIN alias
        // ... timestamps ...
    )
}
```

#### Operation: `CreateItem` — thread `gearPackId`

Add `gear_pack_id` to the INSERT statement and bind `gearPackId` from param. Include `gear_pack_id` in the returned `Item` object.

#### Operation: `UpdateItem` — thread `gearPackId`

Add `gear_pack_id = :gearPackId` to the UPDATE SET clause. Bind `param.gearPackId` (which may be null to clear the field).

#### Operations: All SELECT queries — add LEFT JOIN

All operations that SELECT from `items` (`GetItemById`, `GetItemsByPlanId`, `GetItemsByPlanIdAndUserId`, `GetItemsByUserId`) must be updated to:

```sql
SELECT i.id, i.plan_id, i.user_id, i.name, i.category, i.quantity, i.packed,
       i.gear_pack_id, gp.name AS gear_pack_name,
       i.created_at, i.updated_at
FROM items i
LEFT JOIN gear_packs gp ON gp.id = i.gear_pack_id
WHERE ...
```

#### Fake: `FakeItemClient` — thread `gearPackId`

Update `create()` to store `gearPackId` from param. Update `update()` to persist `gearPackId`. Update `seedItem()` (no change needed — it already accepts `Item` objects which will have the new field).

## Service Layer Changes

### Feature: `features/item/`

#### Service Model: `Item` — add fields

```kotlin
data class Item(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID?,       // NEW
    val gearPackName: String?,   // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

#### DTOs: `CreateItemRequest` — add field

```kotlin
data class CreateItemRequest(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val ownerType: String,
    val ownerId: UUID,
    val planId: UUID? = null,
    val gearPackId: UUID? = null,   // NEW
)
```

#### DTOs: `UpdateItemRequest` — add field

```kotlin
data class UpdateItemRequest(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID? = null,   // NEW
)
```

#### DTOs: `ItemResponse` — add fields

```kotlin
data class ItemResponse(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID?,       // NEW
    val gearPackName: String?,   // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

#### Service Params: `CreateItemParam` — add field

```kotlin
data class CreateItemParam(
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val ownerType: String,
    val ownerId: UUID,
    val planId: UUID? = null,
    val requestingUserId: UUID,
    val gearPackId: UUID? = null,   // NEW
)
```

#### Service Params: `UpdateItemParam` — add field

```kotlin
data class UpdateItemParam(
    val id: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val requestingUserId: UUID,
    val gearPackId: UUID? = null,   // NEW
)
```

#### Mapper: `ItemMapper` — thread new fields

```kotlin
object ItemMapper {
    fun fromClient(clientItem: ClientItem): Item = Item(
        // ... existing fields ...
        gearPackId = clientItem.gearPackId,
        gearPackName = clientItem.gearPackName,
        // ... timestamps ...
    )

    fun toResponse(item: Item): ItemResponse = ItemResponse(
        // ... existing fields ...
        gearPackId = item.gearPackId,
        gearPackName = item.gearPackName,
        // ... timestamps ...
    )
}
```

#### Action: `CreateItemAction` — thread `gearPackId`

Pass `gearPackId = param.gearPackId` when constructing `ClientCreateItemParam`.

#### Action: `UpdateItemAction` — thread `gearPackId`

Pass `gearPackId = param.gearPackId` when constructing `ClientUpdateItemParam`.

#### Controller: `ItemController` — thread `gearPackId`

In `create()`: pass `gearPackId = request.gearPackId` to the service param.
In `update()`: pass `gearPackId = request.gearPackId` to the service param.

### Feature: `features/gearpack/`

#### Action: `ApplyGearPackAction` — pass `gearPackId`

When creating items in the loop, add `gearPackId = param.gearPackId` to the `ClientCreateItemParam`:

```kotlin
val createResult = itemClient.create(
    ClientCreateItemParam(
        planId = param.planId,
        userId = null,
        name = packItem.name,
        category = packItem.category,
        quantity = finalQuantity,
        packed = false,
        gearPackId = param.gearPackId,   // NEW — the gear pack's own ID
    )
)
```

#### Model: `AppliedItem` — add field

```kotlin
data class AppliedItem(
    val id: UUID,
    val planId: UUID,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID?,       // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

#### DTOs: `AppliedItemResponse` — add field

```kotlin
data class AppliedItemResponse(
    val id: UUID,
    val planId: UUID,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val gearPackId: UUID?,       // NEW
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

### No new validations needed

The `gearPackId` field is optional and nullable. The FK constraint in the database handles referential integrity. No service-layer or client-layer validation is required beyond what the DB enforces.

## Frontend Plan

### API Types: `Item` — add fields

```typescript
export interface Item {
  id: string;
  planId: string | null;
  userId: string | null;
  name: string;
  category: string;
  quantity: number;
  packed: boolean;
  gearPackId: string | null;       // NEW
  gearPackName: string | null;     // NEW
  createdAt: string;
  updatedAt: string;
}
```

### API Methods: `createItem` / `updateItem` — thread `gearPackId`

```typescript
createItem(data: {
  name: string; category: string; quantity: number; packed: boolean;
  ownerType: string; ownerId: string; planId?: string;
  gearPackId?: string | null;   // NEW
}): Promise<Item>

updateItem(itemId: string, data: {
  name: string; category: string; quantity: number; packed: boolean;
  gearPackId?: string | null;   // NEW
}): Promise<Item>
```

### GearModal Changes

#### 1. Group items by gear pack

In `ChecklistSection`, after grouping by category, add a higher-level grouping by `gearPackId`:

- Items with the same non-null `gearPackId` are rendered inside a `GearPackGroup` wrapper component that shows a light label with the gear pack name and a subtle border.
- Items with `gearPackId = null` render exactly as today (grouped by category only).
- Within each gear pack group, items are still sub-grouped by category.

#### 2. GearPackGroup component (new)

```
<div class="gear-pack-group">
  <div class="gear-pack-group-label">
    <PackIcon packName={gearPackName} />
    <span>Cooking Equipment</span>
    <span class="gear-pack-group-count">8 items</span>
  </div>
  {/* Category sub-groups with items inside */}
</div>
```

Styling: light background tint, subtle left border, slightly indented. Reuse `PackIcon` from existing `GearPacksPanel` component.

#### 3. Category dropdown: add gear pack options

In `AddItemForm` and the inline edit `ItemRow`, extend the category `<select>` to include gear pack groups currently present on the plan:

- Derive the set of active gear packs from the items list: `items.filter(i => i.gearPackId).reduce(...)` to get unique `{ gearPackId, gearPackName }` pairs.
- Add an `<optgroup label="Gear Packs">` section at the end of the dropdown with entries like `"Cooking Equipment"`.
- When a gear pack is selected as category: set `gearPackId` to the pack's ID and keep `category` as the item's actual category (default to the first category or let the user pick).

**Revised approach:** Since `category` and `gearPackId` are independent fields, the dropdown should remain a pure category selector. Instead, add a separate "Gear Pack" dropdown (only shown when gear packs exist on the plan) that lets the user optionally assign the new item to a gear pack group. This avoids conflating the category concept with gear pack grouping.

#### 4. Category change clears gearPackId

When editing an item that belongs to a gear pack group, if the user changes the category, the frontend does NOT automatically clear `gearPackId`. The gear pack grouping is independent of category — items stay in their gear pack group regardless of category. Users can explicitly remove an item from a gear pack group via the gear pack dropdown (set to "None").

#### 5. CRUD callbacks — thread `gearPackId`

In `makePlanCrud` and `makeMemberCrud`:
- `onAdd`: pass `gearPackId` if the user selected a gear pack.
- `onUpdate`: preserve the existing `gearPackId` when updating, unless the user explicitly changed it.
- `onToggle`: preserve `gearPackId` (no change to packed toggle behavior).

### No new routes

Gear pack grouping is entirely within the existing GearModal on PlanPage.

## PR Stack

All PRs stack on top of `03-16-fix_gear-packs_retro_improvements_agent_lifecycle_and_workflow_fixes` (PR #208).

| # | Type | Branch | Base | Title | Description |
|---|------|--------|------|-------|-------------|
| 1 | plan | `feat-gear-pack-grouping` | `03-16-fix_gear-packs_retro_improvements_agent_lifecycle_and_workflow_fixes` | feat(gear-pack-grouping): plan | This plan document |
| 2 | db | `feat-gear-pack-grouping-db` | `feat-gear-pack-grouping` | feat(gear-pack-grouping): database | V033 migration adding `gear_pack_id` column to items table, schema file update |
| 3 | client | `feat-gear-pack-grouping-client` | `feat-gear-pack-grouping-db` | feat(gear-pack-grouping): client changes | Add `gearPackId`/`gearPackName` to Item model, params, row adapter, operations, fake |
| 4 | service | `feat-gear-pack-grouping-service` | `feat-gear-pack-grouping-client` | feat(gear-pack-grouping): service changes | Thread `gearPackId` through DTOs, params, mapper, actions, controller; update ApplyGearPackAction |
| 5 | webapp | `feat-gear-pack-grouping-webapp` | `feat-gear-pack-grouping-service` | feat(gear-pack-grouping): webapp | GearPackGroup component, item grouping in ChecklistSection, gear pack dropdown in AddItemForm, API type updates |
| 6 | client-test | `feat-gear-pack-grouping-client-test` | `feat-gear-pack-grouping-webapp` | feat(gear-pack-grouping): client tests | Integration tests for gear_pack_id in create/update/getByPlanId |
| 7 | service-test | `feat-gear-pack-grouping-service-test` | `feat-gear-pack-grouping-client-test` | feat(gear-pack-grouping): service tests | Unit tests for gearPackId threading in create/update actions, ApplyGearPackAction passing gearPackId |
| 8 | acceptance | `feat-gear-pack-grouping-acceptance` | `feat-gear-pack-grouping-service-test` | feat(gear-pack-grouping): acceptance tests | E2E tests: create item with gearPackId, update item gearPackId, apply gear pack returns gearPackId, list items includes gearPackName |
| 9 | docs | `feat-gear-pack-grouping-docs` | `feat-gear-pack-grouping-acceptance` | feat(gear-pack-grouping): documentation | CLAUDE.md updates for item-client, camper-service, webapp |

## Open Questions

1. **Migration number conflicts:** The gear-packs stack has V030-V032 at the same numbers as existing itinerary/shopping-list migrations. When the gear-packs stack merges to main, these will need renumbering. The new V033 migration assumes the gear-packs stack migrations (V030-V032) are already applied. If renumbering happens before this feature lands, V033 will also need renumbering.

2. **Gear pack dropdown UX:** The plan describes a separate optional "Gear Pack" dropdown alongside the category dropdown. An alternative is to skip the "add to gear pack" capability entirely for V1, since the primary use case is items created via "Apply Gear Pack" which automatically get `gearPackId`. Adding items to a gear pack manually is a nice-to-have. **Recommendation:** Implement the separate dropdown for V1 — it is low effort and completes the feature story.
