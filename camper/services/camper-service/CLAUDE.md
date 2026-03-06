# camper-service

API service for camping trip planning — user registration, authentication, and plan management.

## Package
`com.acme.services.camperservice`

## Architecture
- Spring Boot 3.4.3 application on port 8080
- Consumes `world-client`, `user-client`, and `plan-client` for data access
- Database: `camper-db` (port 5433, database `camper_db`)

## Features

### World (`features/world/`)
- **Model:** `World(id, name, greeting, createdAt, updatedAt)`
- **DTOs:** `CreateWorldRequest(name, greeting)`, `UpdateWorldRequest(name?, greeting?)`, `WorldResponse(id, name, greeting, createdAt, updatedAt)`
- **Error:** `WorldError` sealed class — `NotFound(entityId)`, `AlreadyExists(name)`, `Invalid(field, reason)`
- **Service params:** `GetWorldByIdParam(id)`, `GetAllWorldsParam(limit?, offset?)`, `CreateWorldParam(name, greeting)`, `UpdateWorldParam(id, name?, greeting?)`, `DeleteWorldParam(id)`
- **Validations:** 1:1 with actions in `validations/`
  - `ValidateCreateWorld`: name must not be blank, greeting must not be blank
  - `ValidateUpdateWorld`: name must not be blank (if provided), greeting must not be blank (if provided)
  - `ValidateGetWorldById`, `ValidateGetAllWorlds`, `ValidateDeleteWorld`: default (return `success(Unit)`)
- **Actions:** 1:1 with service methods in `actions/`
  - `GetWorldByIdAction`, `GetAllWorldsAction`, `CreateWorldAction`, `UpdateWorldAction`, `DeleteWorldAction`
- **Service:** `WorldService` facade (no `@Service`, wired via `@Configuration` bean)
- **Routes:**
  - `GET /api/worlds/{id}`
  - `GET /api/worlds`
  - `POST /api/worlds`
  - `PUT /api/worlds/{id}`
  - `DELETE /api/worlds/{id}`

### User (`features/user/`)
- **Model:** `User(id, email, username?, createdAt, updatedAt)`
- **DTOs:** `CreateUserRequest(email, username?)`, `AuthRequest(email)`, `UpdateUserRequest(username)`, `UserResponse(id, email, username?, createdAt, updatedAt)`, `AuthResponse(id, email, username?)`
- **Error:** `UserError` sealed class — `NotFound(email)`, `Invalid(field, reason)`, `Forbidden(userId)`
- **Service params:** `GetUserByIdParam(userId)`, `CreateUserParam(email, username?)`, `AuthenticateUserParam(email)`, `UpdateUserParam(userId, username, requestingUserId)`
- **Actions:**
  - `GetUserByIdAction`: Fetches user by UUID
  - `CreateUserAction`: Idempotent — checks getByEmail first, returns existing if found
  - `AuthenticateUserAction`: Looks up user by email
  - `UpdateUserAction`: Checks userId == requestingUserId for authorization
- **Service:** `UserService` facade
- **Routes:**
  - `GET /api/users/{userId}` — get user by ID
  - `POST /api/users` — register (idempotent, returns 201)
  - `POST /api/auth` — authenticate by email
  - `PUT /api/users/{userId}` — update username (requires `X-User-Id` header)

### Plan (`features/plan/`)
- **Model:** `Plan(id, name, visibility, ownerId, createdAt, updatedAt)`, `PlanMember(planId, userId, username?, createdAt)`
- **DTOs:** `CreatePlanRequest(name)`, `UpdatePlanRequest(name)`, `AddMemberRequest(email)`, `PlanResponse(...)`, `PlanMemberResponse(...)`
- **Error:** `PlanError` sealed class — `NotFound`, `NotOwner`, `AlreadyMember`, `NotMember`, `Invalid`
- **Service params:** `CreatePlanParam(name, userId)`, `GetPlansParam(userId)`, `UpdatePlanParam(planId, name, userId)`, `DeletePlanParam(planId, userId)`, `GetPlanMembersParam(planId)`, `AddPlanMemberParam(planId, email)`, `RemovePlanMemberParam(planId, userId, requestingUserId)`
- **Actions:**
  - `CreatePlanAction`: Creates private plan, auto-adds creator as member
  - `GetPlansAction`: Merges user's plans + public plans (deduped)
  - `UpdatePlanAction`: Owner-only
  - `DeletePlanAction`: Owner-only
  - `GetPlanMembersAction`: Lists plan members, enriches with username from UserClient
  - `AddPlanMemberAction`: Uses userClient.getOrCreate then adds member
  - `RemovePlanMemberAction`: Self-remove or owner can remove
- **Service:** `PlanService` facade (takes PlanClient + UserClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/plans` — create plan (201)
  - `GET /api/plans` — list plans
  - `PUT /api/plans/{planId}` — update plan name (owner only)
  - `DELETE /api/plans/{planId}` — delete plan (owner only, 204)
  - `GET /api/plans/{planId}/members` — list members
  - `POST /api/plans/{planId}/members` — add member by email (201)
  - `DELETE /api/plans/{planId}/members/{memberId}` — remove member (204)

## Key Patterns
- `WorldMapper.fromClient()` / `UserMapper` / `PlanMapper` adapt client models to service models
- Service param objects for all service calls
- Action classes validate, convert params, call client
- `GlobalExceptionHandler` catches `RuntimeException::class`
- `ResultExtensions.kt` maps errors to HTTP responses
- Trust-based auth via `X-User-Id` header (no tokens/passwords)

## Configuration
- `WorldClientConfig` — creates client via factory function
- `UserClientConfig` — creates user client via factory function
- `PlanClientConfig` — creates plan client via factory function
- `WorldServiceConfig` — wires service via `@Configuration` bean
- `UserServiceConfig` — wires UserService
- `PlanServiceConfig` — wires PlanService (takes PlanClient + UserClient)

## Testing
- **Unit:** `WorldServiceTest`, `UserServiceTest`, `PlanServiceTest` use FakeClient from testFixtures
- **Acceptance:** `WorldAcceptanceTest`, `UserAcceptanceTest`, `PlanAcceptanceTest` with `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- **Fixtures:** `WorldFixture`, `UserFixture`, `PlanFixture` use direct SQL for test setup
- **Clean slate:** Tables truncated via `@BeforeEach`
