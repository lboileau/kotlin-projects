# camper-service

API service for camping trip planning — user registration, authentication, plan management, and assignment management.

## Package
`com.acme.services.camperservice`

## Architecture
- Spring Boot 3.4.3 application on port 8080
- Consumes `world-client`, `user-client`, `plan-client`, `item-client`, `itinerary-client`, and `assignment-client` for data access
- Database: `camper-db` (port 5433, database `camper_db`)
- **WebSocket:** STOMP-over-WebSocket at `/ws` for live updates. `PlanEventPublisher` broadcasts `PlanUpdateMessage(resource, action)` to `/topic/plans/{planId}` after successful mutations.

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
- **Model:** `Plan(id, name, visibility, ownerId, createdAt, updatedAt, isMember=true)`, `PlanMember(planId, userId, username?, createdAt)`
- **DTOs:** `CreatePlanRequest(name)`, `UpdatePlanRequest(name, visibility?)`, `AddMemberRequest(email)`, `PlanResponse(..., isMember)`, `PlanMemberResponse(...)`
- **Error:** `PlanError` sealed class — `NotFound`, `NotOwner`, `AlreadyMember`, `NotMember`, `Invalid`
- **Service params:** `CreatePlanParam(name, userId)`, `GetPlansParam(userId)`, `UpdatePlanParam(planId, name, visibility?, userId)`, `DeletePlanParam(planId, userId)`, `GetPlanMembersParam(planId)`, `AddPlanMemberParam(planId, email)`, `RemovePlanMemberParam(planId, userId, requestingUserId)`
- **Actions:**
  - `CreatePlanAction`: Creates private plan, auto-adds creator as member
  - `GetPlansAction`: Merges user's plans + public plans (deduped), marks non-member public plans with `isMember=false`
  - `UpdatePlanAction`: Owner-only, supports updating name and/or visibility
  - `DeletePlanAction`: Owner-only
  - `GetPlanMembersAction`: Lists plan members, enriches with username from UserClient
  - `AddPlanMemberAction`: Uses userClient.getOrCreate then adds member
  - `RemovePlanMemberAction`: Self-remove or owner can remove
- **Service:** `PlanService` facade (takes PlanClient + UserClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/plans` — create plan (201)
  - `GET /api/plans` — list plans
  - `PUT /api/plans/{planId}` — update plan name and/or visibility (owner only)
  - `DELETE /api/plans/{planId}` — delete plan (owner only, 204)
  - `GET /api/plans/{planId}/members` — list members
  - `POST /api/plans/{planId}/members` — add member by email (201)
  - `DELETE /api/plans/{planId}/members/{memberId}` — remove member (204)

### Item (`features/item/`)
- **Model:** `Item(id, planId?, userId?, name, category, quantity, packed, createdAt, updatedAt)`
- **DTOs:** `CreateItemRequest(name, category, quantity, packed, ownerType, ownerId)`, `UpdateItemRequest(name, category, quantity, packed)`, `ItemResponse(...)`
- **Error:** `ItemError` sealed class — `NotFound(itemId)`, `Invalid(field, reason)`
- **Service params:** `CreateItemParam(name, category, quantity, packed, ownerType, ownerId, requestingUserId)`, `GetItemParam(id, requestingUserId)`, `GetItemsByOwnerParam(ownerType, ownerId, requestingUserId)`, `UpdateItemParam(id, name, category, quantity, packed, requestingUserId)`, `DeleteItemParam(id, requestingUserId)`
- **Actions:**
  - `CreateItemAction`: Validates, converts ownerType/ownerId to planId/userId, creates item
  - `GetItemAction`: Fetches item by ID
  - `GetItemsByOwnerAction`: Lists items by plan or user owner
  - `UpdateItemAction`: Updates item fields
  - `DeleteItemAction`: Deletes item
- **Service:** `ItemService` facade (takes ItemClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/items` — create item (201)
  - `GET /api/items?ownerType={type}&ownerId={id}` — list items by owner
  - `GET /api/items/{id}` — get item by ID
  - `PUT /api/items/{id}` — update item
  - `DELETE /api/items/{id}` — delete item (204)
- **Polymorphic ownership:** Items belong to either a plan or a user via nullable FK columns (`plan_id`, `user_id`) with a DB CHECK constraint. Categories are free-form strings (no DB validation). Known categories: canoe, kitchen, camp, personal, misc, food_item.

### Itinerary (`features/itinerary/`)
- **Model:** `Itinerary(id, planId, createdAt, updatedAt)`, `ItineraryEvent(id, itineraryId, title, description?, details?, eventAt, createdAt, updatedAt)`
- **DTOs:** `AddEventRequest(title, description?, details?, eventAt)`, `UpdateEventRequest(title, description?, details?, eventAt)`, `ItineraryResponse(id, planId, events, createdAt, updatedAt)`, `ItineraryEventResponse(id, itineraryId, title, description?, details?, eventAt, createdAt, updatedAt)`
- **Error:** `ItineraryError` sealed class — `PlanNotFound(planId)`, `NotFound(planId)`, `EventNotFound(eventId)`, `Invalid(field, reason)`
- **Service params:** `GetItineraryParam(planId)`, `DeleteItineraryParam(planId)`, `AddEventParam(planId, title, description?, details?, eventAt)`, `UpdateEventParam(planId, eventId, title, description?, details?, eventAt)`, `DeleteEventParam(planId, eventId)`
- **Actions:**
  - `GetItineraryAction`: Fetches itinerary by planId with events ordered by eventAt
  - `DeleteItineraryAction`: Deletes itinerary and all events (cascade)
  - `AddEventAction`: Auto-creates itinerary if none exists, then adds event
  - `UpdateEventAction`: Updates event fields
  - `DeleteEventAction`: Deletes a single event
- **Service:** `ItineraryService` facade (takes ItineraryClient + PlanClient)
- **Routes:** (all require `X-User-Id` header)
  - `GET /api/plans/{planId}/itinerary` — get itinerary with events
  - `DELETE /api/plans/{planId}/itinerary` — delete itinerary (204)
  - `POST /api/plans/{planId}/itinerary/events` — add event (201, auto-creates itinerary)
  - `PUT /api/plans/{planId}/itinerary/events/{eventId}` — update event
  - `DELETE /api/plans/{planId}/itinerary/events/{eventId}` — delete event (204)

### Assignment (`features/assignment/`)
- **Model:** `Assignment(id, planId, name, type, maxOccupancy, ownerId, createdAt, updatedAt)`, `AssignmentMember(assignmentId, userId, username?, createdAt)`, `AssignmentDetail(id, planId, name, type, maxOccupancy, ownerId, members, createdAt, updatedAt)`
- **DTOs:** `CreateAssignmentRequest(name, type, maxOccupancy?)`, `UpdateAssignmentRequest(name?, maxOccupancy?)`, `AddAssignmentMemberRequest(userId)`, `TransferOwnershipRequest(newOwnerId)`, `AssignmentResponse(...)`, `AssignmentDetailResponse(...)`, `AssignmentMemberResponse(...)`
- **Error:** `AssignmentError` sealed class — `NotFound`, `NotOwner`, `Invalid`, `AtCapacity`, `AlreadyAssigned`, `AlreadyMember`, `CannotRemoveOwner`, `PlanNotFound`, `DuplicateName`
- **Service params:** `CreateAssignmentParam(planId, name, type, maxOccupancy?, userId)`, `GetAssignmentsParam(planId, type?)`, `GetAssignmentParam(assignmentId)`, `UpdateAssignmentParam(assignmentId, name?, maxOccupancy?, userId)`, `DeleteAssignmentParam(assignmentId, userId)`, `AddAssignmentMemberParam(assignmentId, memberUserId, userId)`, `RemoveAssignmentMemberParam(assignmentId, memberUserId, userId)`, `TransferOwnershipParam(assignmentId, newOwnerId, userId)`
- **Actions:**
  - `CreateAssignmentAction`: Creates assignment within a plan, owner auto-added as member
  - `GetAssignmentsAction`: Lists assignments for a plan, optionally filtered by type
  - `GetAssignmentAction`: Gets single assignment with member details
  - `UpdateAssignmentAction`: Owner-only update of name/maxOccupancy
  - `DeleteAssignmentAction`: Owner-only deletion
  - `AddAssignmentMemberAction`: Adds member to assignment (capacity/uniqueness checks)
  - `RemoveAssignmentMemberAction`: Self-remove or owner can remove (owner cannot be removed)
  - `TransferOwnershipAction`: Owner-only transfer of ownership to another member
- **Service:** `AssignmentService` facade (takes AssignmentClient + UserClient)
- **Routes:** (all require `X-User-Id` header)
  - `POST /api/plans/{planId}/assignments` — create assignment (201)
  - `GET /api/plans/{planId}/assignments` — list assignments (optional `?type=` filter)
  - `GET /api/plans/{planId}/assignments/{assignmentId}` — get assignment detail
  - `PUT /api/plans/{planId}/assignments/{assignmentId}` — update assignment (owner only)
  - `DELETE /api/plans/{planId}/assignments/{assignmentId}` — delete assignment (owner only, 204)
  - `POST /api/plans/{planId}/assignments/{assignmentId}/members` — add member (201)
  - `DELETE /api/plans/{planId}/assignments/{assignmentId}/members/{memberUserId}` — remove member (204)
  - `PUT /api/plans/{planId}/assignments/{assignmentId}/owner` — transfer ownership

## Key Patterns
- `WorldMapper.fromClient()` / `UserMapper` / `PlanMapper` / `ItemMapper` / `ItineraryMapper` / `AssignmentMapper` adapt client models to service models
- Service param objects for all service calls
- Action classes validate, convert params, call client
- `GlobalExceptionHandler` catches `RuntimeException::class`
- `ResultExtensions.kt` maps errors to HTTP responses
- Trust-based auth via `X-User-Id` header (no tokens/passwords)
- `PlanEventPublisher` broadcasts WebSocket events after successful controller mutations
- Controllers inject `PlanEventPublisher` and call `publishUpdate(planId, resource, action)` after `Result.Success`

## Configuration
- `WorldClientConfig` — creates client via factory function
- `UserClientConfig` — creates user client via factory function
- `PlanClientConfig` — creates plan client via factory function
- `ItemClientConfig` — creates item client via factory function
- `ItineraryClientConfig` — creates itinerary client via factory function
- `WorldServiceConfig` — wires service via `@Configuration` bean
- `UserServiceConfig` — wires UserService
- `PlanServiceConfig` — wires PlanService (takes PlanClient + UserClient)
- `ItemServiceConfig` — wires ItemService (takes ItemClient)
- `ItineraryServiceConfig` — wires ItineraryService (takes ItineraryClient + PlanClient)
- `AssignmentClientConfig` — creates assignment client via factory function
- `AssignmentServiceConfig` — wires AssignmentService (takes AssignmentClient + UserClient)
- `WebSocketConfig` — STOMP endpoint `/ws`, topic broker `/topic`, app prefix `/app`

## Testing
- **Unit:** `WorldServiceTest`, `UserServiceTest`, `PlanServiceTest`, `ItemServiceTest`, `ItineraryServiceTest`, `AssignmentServiceTest` use FakeClient from testFixtures
- **Acceptance:** `WorldAcceptanceTest`, `UserAcceptanceTest`, `PlanAcceptanceTest`, `ItemAcceptanceTest`, `ItineraryAcceptanceTest`, `AssignmentAcceptanceTest` with `@SpringBootTest(RANDOM_PORT)` + Testcontainers
- **Fixtures:** `WorldFixture`, `UserFixture`, `PlanFixture`, `ItemFixture`, `ItineraryFixture`, `AssignmentFixture` use direct SQL for test setup
- **WebSocket:** `WebSocketIntegrationTest` verifies controllers publish STOMP messages via broker channel interceptor
- **Clean slate:** Tables truncated via `@BeforeEach`
