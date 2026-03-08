# item-client

Data access client for camping trip gear/equipment items.

## Package
`com.acme.clients.itemclient`

## Public API (`ItemClient` interface)
- `create(CreateItemParam)` — Create a new item
- `getById(GetByIdParam)` — Retrieve an item by ID
- `getByPlanId(GetByPlanIdParam)` — Retrieve all items for a plan
- `getByUserId(GetByUserIdParam)` — Retrieve all items for a user
- `update(UpdateItemParam)` — Update an existing item
- `delete(DeleteItemParam)` — Delete an item by ID

## Model
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

## Status
Contracts only — interfaces, param objects, model, and stub fake. No implementations yet.

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures

## Testing
- `FakeItemClient` (testFixtures) — stub implementation, all methods throw `NotImplementedError`
