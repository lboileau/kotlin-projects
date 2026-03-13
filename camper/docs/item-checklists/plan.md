# Feature Plan: Item Checklists (Gear)

## Summary

Add item checklists to the camper app. Items represent gear, food, and supplies that can be tracked per plan or per user. Each item has a name, category, quantity, and packed status. Ownership is modeled with separate nullable FK columns (`plan_id`, `user_id`) with a CHECK constraint ensuring exactly one is set. This is extensible — adding a new owner type (e.g., `meal`) requires only a new nullable FK column and an updated CHECK constraint via migration.

## Entities

### Item

| Field | Type | Notes |
|-------|------|-------|
| `id` | UUID | PK, `gen_random_uuid()` |
| `plan_id` | UUID (nullable) | FK → `plans(id)` ON DELETE CASCADE |
| `user_id` | UUID (nullable) | FK → `users(id)` ON DELETE CASCADE |
| `name` | VARCHAR(255) | NOT NULL |
| `category` | VARCHAR(50) | NOT NULL, no DB-level validation |
| `quantity` | INTEGER | NOT NULL, DEFAULT 1 |
| `packed` | BOOLEAN | NOT NULL, DEFAULT false |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| `updated_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Constraints:**
- `chk_items_single_owner`: CHECK that exactly one of `plan_id`, `user_id` is NOT NULL
- Indexes: `idx_items_plan_id`, `idx_items_user_id`

**Known categories:** `canoe`, `kitchen`, `camp`, `personal`, `misc`, `food_item` — but any string is accepted.

## API Surface

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| `POST` | `/api/items` | Create an item | `CreateItemRequest` | `201` + `ItemResponse` |
| `GET` | `/api/items?ownerType={type}&ownerId={id}` | List items by owner | — | `200` + `List<ItemResponse>` |
| `GET` | `/api/items/{id}` | Get item by ID | — | `200` + `ItemResponse` |
| `PUT` | `/api/items/{id}` | Update item | `UpdateItemRequest` | `200` + `ItemResponse` |
| `DELETE` | `/api/items/{id}` | Delete item | — | `204` |

All endpoints require `X-User-Id` header.

### DTOs

**CreateItemRequest:**
```json
{
  "name": "Tent",
  "category": "camp",
  "quantity": 1,
  "packed": false,
  "ownerType": "plan",
  "ownerId": "uuid"
}
```

**UpdateItemRequest:**
```json
{
  "name": "Tent (4-person)",
  "category": "camp",
  "quantity": 1,
  "packed": true
}
```

**ItemResponse:**
```json
{
  "id": "uuid",
  "planId": "uuid-or-null",
  "userId": "uuid-or-null",
  "name": "Tent",
  "category": "camp",
  "quantity": 1,
  "packed": false,
  "createdAt": "2026-03-07T...",
  "updatedAt": "2026-03-07T..."
}
```

## Database Changes

### New Table: `items`

Migration: `V005__create_items.sql`

```sql
CREATE TABLE items (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id    UUID        REFERENCES plans(id) ON DELETE CASCADE,
    user_id    UUID        REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    category   VARCHAR(50)  NOT NULL,
    quantity   INTEGER      NOT NULL DEFAULT 1,
    packed     BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_items_single_owner CHECK (
        (plan_id IS NOT NULL AND user_id IS NULL) OR
        (plan_id IS NULL AND user_id IS NOT NULL)
    )
);

CREATE INDEX idx_items_plan_id ON items(plan_id);
CREATE INDEX idx_items_user_id ON items(user_id);
```

## Client Interface

### Module: `clients/item-client`

**Package:** `com.acme.clients.itemclient`

**ItemClient interface:**

| Method | Param | Return |
|--------|-------|--------|
| `create(param)` | `CreateItemParam` | `Result<Item, AppError>` |
| `getById(param)` | `GetByIdParam` | `Result<Item, AppError>` |
| `getByPlanId(param)` | `GetByPlanIdParam` | `Result<List<Item>, AppError>` |
| `getByUserId(param)` | `GetByUserIdParam` | `Result<List<Item>, AppError>` |
| `update(param)` | `UpdateItemParam` | `Result<Item, AppError>` |
| `delete(param)` | `DeleteItemParam` | `Result<Unit, AppError>` |

**Model:**
```kotlin
data class Item(
    val id: UUID,
    val planId: UUID?,
    val userId: UUID?,
    val name: String,
    val category: String,
    val quantity: Int,
    val packed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
```

**Param Objects:**
- `CreateItemParam(planId: UUID?, userId: UUID?, name: String, category: String, quantity: Int, packed: Boolean)`
- `GetByIdParam(id: UUID)`
- `GetByPlanIdParam(planId: UUID)`
- `GetByUserIdParam(userId: UUID)`
- `UpdateItemParam(id: UUID, name: String, category: String, quantity: Int, packed: Boolean)`
- `DeleteItemParam(id: UUID)`

## Service Layer

### Feature: `item` in `camper-service`

**Package:** `com.acme.services.camperservice.features.item`

**ItemError sealed class:**
- `NotFound(itemId: UUID)` → 404
- `Invalid(field: String, reason: String)` → 400
- Companion: `fromClientError(AppError): ItemError`

**Actions:**
- `CreateItemAction(itemClient)` — validate → create item
- `GetItemAction(itemClient)` — get by ID
- `GetItemsByOwnerAction(itemClient)` — get by owner (plan or user)
- `UpdateItemAction(itemClient)` — validate → update item
- `DeleteItemAction(itemClient)` — delete item

**Service Params:**
- `CreateItemParam(name, category, quantity, packed, ownerType, ownerId, requestingUserId)`
- `GetItemParam(id, requestingUserId)`
- `GetItemsByOwnerParam(ownerType, ownerId, requestingUserId)`
- `UpdateItemParam(id, name, category, quantity, packed, requestingUserId)`
- `DeleteItemParam(id, requestingUserId)`

**ItemService:** facade composing the actions above.

**ItemController:** `@RestController @RequestMapping("/api/items")`

## PR Stack

| # | Branch Suffix | Title | Description |
|---|---------------|-------|-------------|
| 1 | plan | `feat(item-checklists): plan` | This plan document |
| 2 | db-contracts | `feat(item-checklists): db contracts` | Schema file + migration DDL for `items` table |
| 3 | client-contracts | `feat(item-checklists): client contracts` | ItemClient interface, params, model, stub fake |
| 4 | service-contracts | `feat(item-checklists): service contracts` | DTOs, ItemError, action signatures, controller stubs |
| 5 | db-impl | `feat(item-checklists): db implementation` | Seed data, verify migrations runnable |
| 6 | client-impl | `feat(item-checklists): client implementation` | Operations, adapters, factory, fake |
| 7 | service-impl | `feat(item-checklists): service implementation` | Actions, service, controller, wiring |
| 8 | client-tests | `feat(item-checklists): client tests` | Integration tests for ItemClient |
| 9 | service-tests | `feat(item-checklists): service tests` | Unit tests with FakeItemClient |
| 10 | acceptance-tests | `feat(item-checklists): acceptance tests` | End-to-end API tests |
| 11 | docs | `feat(item-checklists): update documentation` | CLAUDE.md, README updates |

## Open Questions

None — all design decisions resolved.
