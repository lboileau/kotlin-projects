# world-client

Data access client for the worlds table CRUD operations.

## Package
`com.acme.clients.worldclient`

## Public API (`WorldClient` interface)
- `getById(GetByIdParam)` — Retrieve a single world by its unique identifier
- `getList(GetListParam)` — Retrieve a list of worlds with optional limit/offset
- `create(CreateWorldParam)` — Create a new world with name and greeting
- `update(UpdateWorldParam)` — Update an existing world (null fields left unchanged)
- `delete(DeleteWorldParam)` — Delete a world by its unique identifier

## Model
```kotlin
data class World(
    val id: UUID,
    val name: String,
    val greeting: String,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

## Database
- Database: `camper-db` (port 5433, database `camper_db`)
- Table: `worlds`
- Key constraints: `uq_worlds_name` (unique name)

## Architecture
- **Facade pattern:** `JdbiWorldClient` delegates to individual operation classes
- **Validation classes:** 1:1 with operations in `internal/validations/`
- **Parameter objects:** All methods take dedicated data class params
- **Row adapter:** `WorldRowAdapter` maps ResultSet to `World`
- **Factory:** `createWorldClient()` creates the client (reads DB config from env vars)

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` for missing entities
- `ConflictError` for duplicate constraint violations

## Testing
- Integration tests: Testcontainers PostgreSQL + migrations via `WorldTestDb`
- `FakeWorldClient` (testFixtures) for consumer testing — references actual validators
- `WorldTestDb` (testFixtures) wraps `MigrationRunner` from database module
