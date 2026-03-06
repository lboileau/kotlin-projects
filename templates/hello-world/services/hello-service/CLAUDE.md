# hello-service

API service for world CRUD operations.

## Package
`com.example.services.hello`

## Architecture
- Spring Boot 3.4.3 application on port 8080
- Consumes `world-client` for data access
- Database: `hello-world-db` (port 5433, database `hello_world_db`)

## Features

### World (`features/world/`)
- **Model:** World(id, name, greeting, createdAt, updatedAt)
- **DTOs:** CreateWorldRequest(name, greeting), UpdateWorldRequest(name?, greeting?), WorldResponse(id, name, greeting, createdAt, updatedAt)
- **Error:** `WorldError` sealed class — NotFound(entityId), AlreadyExists(name), Invalid(field, reason)
- **Service params:** GetWorldByIdParam(id), GetAllWorldsParam(limit?, offset?), CreateWorldParam(name, greeting), UpdateWorldParam(id, name?, greeting?), DeleteWorldParam(id)
- **Validations:** 1:1 with actions in `validations/`
- **Actions:** 1:1 with service methods in `actions/`
- **Service:** `WorldService` facade (no `@Service`, wired via `@Configuration` bean)
- **Routes:** GET /api/worlds/{id}, GET /api/worlds, POST /api/worlds, PUT /api/worlds/{id}, DELETE /api/worlds/{id}

## Key Patterns
- `WorldMapper.fromClient()` adapts client model to service model
- Service param objects for all service calls
- Action classes validate, convert params, call client
- `GlobalExceptionHandler` catches `RuntimeException::class`
- `ResultExtensions.kt` maps errors to HTTP responses

## Configuration
- `WorldClientConfig` — creates client via factory function
- `WorldServiceConfig` — wires service via `@Configuration` bean

## Testing
- **Unit:** `WorldServiceTest` uses `FakeWorldClient` from testFixtures
- **Acceptance:** `WorldAcceptanceTest` with `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- **Fixture:** `WorldFixture` uses direct SQL for test setup
- **Clean slate:** Tables truncated via `@BeforeEach`
