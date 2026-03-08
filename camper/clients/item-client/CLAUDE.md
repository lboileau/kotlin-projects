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

## Database
- Database: `camper-db` (port 5433, database `camper_db`)
- Table: `items`

## Architecture
- **Facade pattern:** `JdbiItemClient` delegates to individual operation classes
- **Validation classes:** 1:1 with operations in `internal/validations/`
- **Parameter objects:** All methods take dedicated data class params
- **Row adapter:** `ItemRowAdapter` maps ResultSet to `Item`
- **Factory:** `createItemClient()` creates the client (reads DB config from env vars)

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` for missing entities
- `ValidationError` for invalid input (blank name, quantity <= 0, planId/userId constraint)

## Testing
- `FakeItemClient` (testFixtures) for consumer testing — references actual validators
- In-memory `ConcurrentHashMap` storage with `reset()` and `seedItem()` helpers
