# Feature: User Registration & Plans

## Summary

Add user registration (email + username, no password) with trust-based authentication, and plan creation for camping trips. Users can create plans, invite others by email, join plans, and view their plans. Plans have an owner and a public/private visibility flag.

## Entities

### users

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | PK, `DEFAULT gen_random_uuid()` |
| `email` | `VARCHAR(255)` | `NOT NULL`, `UNIQUE` |
| `username` | `VARCHAR(100)` | nullable |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |

Indexes: `idx_users_email` on `email`
Constraints: `uq_users_email` unique on `email`

### plans

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | `UUID` | PK, `DEFAULT gen_random_uuid()` |
| `name` | `VARCHAR(255)` | `NOT NULL` |
| `visibility` | `VARCHAR(10)` | `NOT NULL DEFAULT 'private'`, check `IN ('public', 'private')` |
| `owner_id` | `UUID` | `NOT NULL`, FK → `users(id)` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |
| `updated_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |

Constraints: `ck_plans_visibility` check on visibility values, `fk_plans_owner` FK to users

### plan_members

| Column | Type | Constraints |
|--------|------|-------------|
| `plan_id` | `UUID` | `NOT NULL`, FK → `plans(id) ON DELETE CASCADE` |
| `user_id` | `UUID` | `NOT NULL`, FK → `users(id)` |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL DEFAULT now()` |

PK: composite `(plan_id, user_id)`
Indexes: `idx_plan_members_user_id` on `user_id` (for "get plans by user" queries)

## API Surface

| Method | Path | Description | Auth | Request Body | Response |
|--------|------|-------------|------|-------------|----------|
| `POST` | `/api/users` | Register or authenticate (if email exists, returns existing user) | No | `{ email, username }` | `201 { id, email, username, createdAt, updatedAt }` or `200` if existing |
| `POST` | `/api/auth` | Authenticate by email | No | `{ email }` | `200 { id, email, username }` |
| `POST` | `/api/plans` | Create a plan (creator auto-added as member) | `X-User-Id` | `{ name }` | `201 { id, name, visibility, ownerId, createdAt, updatedAt }` |
| `GET` | `/api/plans` | List plans (public + user's private) | `X-User-Id` | — | `200 [{ id, name, visibility, ownerId, createdAt, updatedAt }]` |
| `GET` | `/api/plans/{planId}/members` | List plan members | `X-User-Id` | — | `200 [{ id, email, username, createdAt }]` |
| `POST` | `/api/plans/{planId}/members` | Add user to plan by email (creates user if not found) | `X-User-Id` | `{ email }` | `201 { planId, userId, createdAt }` |
| `DELETE` | `/api/plans/{planId}/members/{userId}` | Remove user from plan (self or owner) | `X-User-Id` | — | `204` |
| `PUT` | `/api/users/{userId}` | Update user (username) | `X-User-Id` | `{ username }` | `200 { id, email, username, createdAt, updatedAt }` |
| `PUT` | `/api/plans/{planId}` | Update plan (name) | `X-User-Id` | `{ name }` | `200 { id, name, visibility, ownerId, createdAt, updatedAt }` |
| `DELETE` | `/api/plans/{planId}` | Delete plan (owner only, cascades members) | `X-User-Id` | — | `204` |

### Authentication

Trust-based: authenticated endpoints require an `X-User-Id` header containing a valid user UUID. No tokens or passwords.

### Authorization Rules

- **Create plan:** Any authenticated user
- **List plans:** Any authenticated user (sees public plans + private plans they belong to)
- **Get plan members:** Any authenticated user
- **Add member:** Any authenticated user (for now — no ownership check on adding)
- **Remove member:** A user can remove themselves; the plan owner can remove anyone
- **Update user:** A user can only update themselves (userId in path must match `X-User-Id`)
- **Update plan:** Plan owner only
- **Delete plan:** Plan owner only

## Client Interfaces

### user-client (`com.acme.clients.userclient`)

| Method | Params | Return |
|--------|--------|--------|
| `getById(GetByIdParam)` | `id: UUID` | `Result<User, AppError>` |
| `getByEmail(GetByEmailParam)` | `email: String` | `Result<User, AppError>` |
| `create(CreateUserParam)` | `email: String, username: String` | `Result<User, AppError>` |
| `getOrCreate(GetOrCreateUserParam)` | `email: String, username: String?` | `Result<User, AppError>` |
| `update(UpdateUserParam)` | `id: UUID, username: String` | `Result<User, AppError>` |

Model: `User(id: UUID, email: String, username: String, createdAt: Instant, updatedAt: Instant)`

### plan-client (`com.acme.clients.planclient`)

| Method | Params | Return |
|--------|--------|--------|
| `getById(GetByIdParam)` | `id: UUID` | `Result<Plan, AppError>` |
| `getByUserId(GetByUserIdParam)` | `userId: UUID` | `Result<List<Plan>, AppError>` |
| `getPublicPlans(GetPublicPlansParam)` | — | `Result<List<Plan>, AppError>` |
| `create(CreatePlanParam)` | `name: String, visibility: String, ownerId: UUID` | `Result<Plan, AppError>` |
| `update(UpdatePlanParam)` | `id: UUID, name: String` | `Result<Plan, AppError>` |
| `delete(DeletePlanParam)` | `id: UUID` | `Result<Unit, AppError>` |
| `getMembers(GetMembersParam)` | `planId: UUID` | `Result<List<PlanMember>, AppError>` |
| `addMember(AddMemberParam)` | `planId: UUID, userId: UUID` | `Result<PlanMember, AppError>` |
| `removeMember(RemoveMemberParam)` | `planId: UUID, userId: UUID` | `Result<Unit, AppError>` |

Models:
- `Plan(id: UUID, name: String, visibility: String, ownerId: UUID, createdAt: Instant, updatedAt: Instant)`
- `PlanMember(planId: UUID, userId: UUID, createdAt: Instant)`

## Service Layer

### User feature (`features/user/`)

- **Actions:** `CreateUserAction`, `AuthenticateUserAction`, `UpdateUserAction`
- **Error:** `UserError` sealed class — `NotFound(email)`, `Invalid(field, reason)`, `Forbidden(userId)`
- **DTOs:** `CreateUserRequest(email, username)`, `AuthRequest(email)`, `UpdateUserRequest(username)`, `UserResponse(id, email, username, createdAt, updatedAt)`, `AuthResponse(id, email, username)`
- **Validations:** `ValidateCreateUser` (email not blank, username not blank), `ValidateAuthenticateUser` (email not blank), `ValidateUpdateUser` (username not blank)
- **Behavior:** `CreateUserAction` checks if email already exists — if so, returns the existing user (200) instead of failing. This makes registration idempotent.

### Plan feature (`features/plan/`)

- **Actions:** `CreatePlanAction`, `GetPlansAction`, `UpdatePlanAction`, `DeletePlanAction`, `GetPlanMembersAction`, `AddPlanMemberAction`, `RemovePlanMemberAction`
- **Error:** `PlanError` sealed class — `NotFound(planId)`, `NotOwner(planId, userId)`, `AlreadyMember(planId, email)`, `NotMember(planId, userId)`, `Invalid(field, reason)`
- **DTOs:** `CreatePlanRequest(name)`, `UpdatePlanRequest(name)`, `AddMemberRequest(email)`, `PlanResponse(id, name, visibility, ownerId, createdAt, updatedAt)`, `PlanMemberResponse(planId, userId, createdAt)`
- **Service params:** `CreatePlanParam(name, userId)`, `GetPlansParam(userId)`, `UpdatePlanParam(planId, name, userId)`, `DeletePlanParam(planId, userId)`, `GetPlanMembersParam(planId)`, `AddPlanMemberParam(planId, email)`, `RemovePlanMemberParam(planId, userId, requestingUserId)`
- **Validations:** `ValidateCreatePlan` (name not blank), `ValidateUpdatePlan` (name not blank), `ValidateDeletePlan`, `ValidateGetPlans`, `ValidateGetPlanMembers`, `ValidateAddPlanMember` (email not blank), `ValidateRemovePlanMember`

## PR Stack

| # | Branch Suffix | Title | Description |
|---|--------------|-------|-------------|
| 1 | `plan` | feat(user-plans): plan | This document |
| 2 | `db-contracts` | feat(user-plans): db contracts | Schema files and migration SQL for users, plans, plan_members |
| 3 | `client-contracts` | feat(user-plans): client contracts | user-client and plan-client interfaces, params, models (no impl) |
| 4 | `service-contracts` | feat(user-plans): service contracts | DTOs, errors, action signatures, routes (return 501) |
| 5 | `db-impl` | feat(user-plans): db implementation | Seed data, verify migrations |
| 6 | `client-impl` | feat(user-plans): client implementation | Operations, adapters, factory, fakes |
| 7 | `service-impl` | feat(user-plans): service implementation | Actions, service, controller wiring |
| 8 | `client-tests` | feat(user-plans): client tests | Integration tests for user-client and plan-client |
| 9 | `service-tests` | feat(user-plans): service tests | Unit tests for user and plan services |
| 10 | `acceptance` | feat(user-plans): acceptance tests | End-to-end API tests |
| 11 | `docs` | feat(user-plans): update documentation and skills | Retrospective-driven updates |

## Open Questions

None — all decisions made:
- Visibility defaults to `private`
- Only owner can delete a plan (cascades members)
- Users can remove themselves from a plan
- Trust-based auth via `X-User-Id` header
- Plans have an `owner_id` FK to users
- `POST /api/users` with an existing email returns the existing user (idempotent registration)
- `POST /api/plans/{planId}/members` auto-creates a user if the email doesn't exist (username derived from email)
- `AddPlanMemberAction` uses `userClient.getOrCreate()` — no `UserNotFound` error possible
