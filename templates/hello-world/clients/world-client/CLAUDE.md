# world-client

Data access client for the worlds table CRUD operations.

## Package
`com.example.clients.worldclient`

## Public API (`WorldClient` interface)
- `getById(GetByIdParam)` — retrieve a single world by UUID
- `getList(GetListParam)` — retrieve a list of worlds, ordered by name (supports limit/offset)
- `create(CreateWorldParam)` — create a new world with name and greeting
- `update(UpdateWorldParam)` — update an existing world (null fields left unchanged)
- `delete(DeleteWorldParam)` — delete a world by UUID

## Model
```kotlin
data class World(id: UUID, name: String, greeting: String, createdAt: Instant, updatedAt: Instant)
```

## Database
- Database: `hello-world-db` (port 5433, database `hello_world_db`)
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
- `ValidationError` for invalid input (blank name/greeting)

## Testing
- Integration tests: Testcontainers PostgreSQL + migrations via `WorldTestDb`
- `FakeWorldClient` (testFixtures) for consumer testing — references actual validators
- `WorldTestDb` (testFixtures) wraps `MigrationRunner` from database module
