# Feature: Assignments (Tent & Canoe)

## Summary

Add tent and canoe assignments to plans. Each assignment has a name, type, max occupancy, an owner, and a list of assigned users. Users can be assigned to at most one tent and one canoe per plan. Assignments cascade-delete with their plan, and when a user is deleted their memberships are removed and any assignments they own transfer to the plan owner.

## Entities

### Assignment

| Field | Type | Notes |
|-------|------|-------|
| id | UUID | PK |
| plan_id | UUID | FK → plans, ON DELETE CASCADE |
| name | VARCHAR(255) | Required, non-blank |
| type | VARCHAR(10) NOT NULL | 'tent' or 'canoe' |
| max_occupancy | INT NOT NULL | Default: tent=4, canoe=2 |
| owner_id | UUID NOT NULL | FK → users |
| created_at | TIMESTAMPTZ | |
| updated_at | TIMESTAMPTZ | |

Constraints:
- `UNIQUE(plan_id, name, type)` — names unique per type within a plan
- `CHECK(type IN ('tent', 'canoe'))`
- `CHECK(max_occupancy > 0)`

### Assignment Member

| Field | Type | Notes |
|-------|------|-------|
| assignment_id | UUID | FK → assignments, ON DELETE CASCADE |
| user_id | UUID | FK → users, ON DELETE CASCADE |
| plan_id | UUID | FK → plans, ON DELETE CASCADE |
| assignment_type | VARCHAR(10) | 'tent' or 'canoe' |
| created_at | TIMESTAMPTZ | |

Constraints:
- `PK(assignment_id, user_id)`
- `UNIQUE(plan_id, user_id, assignment_type)` — one tent + one canoe per user per plan

### DB Trigger: Transfer Ownership on User Delete

When a user is deleted, any assignments they own should have `owner_id` set to the plan's `owner_id`. Implemented as a `BEFORE DELETE ON users` trigger.

## API Surface

All endpoints nested under `/api/plans/{planId}/assignments`. All require `X-User-Id` header.

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|-------------|----------|
| POST | `/assignments` | Create assignment | `CreateAssignmentRequest` | 201 `AssignmentResponse` |
| GET | `/assignments` | List assignments (optional `?type=tent\|canoe`) | — | 200 `List<AssignmentResponse>` |
| GET | `/assignments/{id}` | Get assignment with members | — | 200 `AssignmentDetailResponse` |
| PUT | `/assignments/{id}` | Update name/max_occupancy | `UpdateAssignmentRequest` | 200 `AssignmentResponse` |
| DELETE | `/assignments/{id}` | Delete assignment | — | 204 |
| POST | `/assignments/{id}/members` | Add member | `AddAssignmentMemberRequest` | 201 `AssignmentMemberResponse` |
| DELETE | `/assignments/{id}/members/{userId}` | Remove member | — | 204 |
| PUT | `/assignments/{id}/owner` | Transfer ownership | `TransferOwnershipRequest` | 200 `AssignmentResponse` |

### Request/Response Shapes

```
CreateAssignmentRequest { name: String, type: String, maxOccupancy: Int? }
UpdateAssignmentRequest { name: String?, maxOccupancy: Int? }
AddAssignmentMemberRequest { userId: UUID }
TransferOwnershipRequest { newOwnerId: UUID }

AssignmentResponse { id, planId, name, type, maxOccupancy, ownerId, createdAt, updatedAt }
AssignmentDetailResponse { id, planId, name, type, maxOccupancy, ownerId, members: List<AssignmentMemberResponse>, createdAt, updatedAt }
AssignmentMemberResponse { assignmentId, userId, username?, createdAt }
```

### Authorization Rules

| Action | Who |
|--------|-----|
| Create assignment | Any user |
| View assignments | Any user |
| Edit assignment (name, maxOccupancy) | Assignment owner or plan owner |
| Delete assignment | Assignment owner or plan owner |
| Add member | Any user |
| Remove member | Self-remove only; assignment owner or plan owner can remove anyone |
| Transfer ownership | Assignment owner or plan owner |

### Validation Rules

- Assignment name must not be blank
- Assignment type must be 'tent' or 'canoe'
- max_occupancy must be > 0
- Cannot add member if assignment is at max_occupancy
- Cannot add member if user already assigned to another assignment of same type in same plan
- Assignment names unique per (plan_id, type)

## Database Changes

- New table: `assignments` (see entity above)
- New table: `assignment_members` (see entity above)
- New trigger: `transfer_assignment_ownership_on_user_delete`

## Client Interface

New client: `assignment-client` in `clients/assignment-client/`

```kotlin
interface AssignmentClient {
    fun create(param: CreateAssignmentParam): Result<Assignment, AppError>
    fun getById(param: GetByIdParam): Result<Assignment, AppError>
    fun getByPlanId(param: GetByPlanIdParam): Result<List<Assignment>, AppError>
    fun update(param: UpdateAssignmentParam): Result<Assignment, AppError>
    fun delete(param: DeleteAssignmentParam): Result<Unit, AppError>
    fun addMember(param: AddAssignmentMemberParam): Result<AssignmentMember, AppError>
    fun removeMember(param: RemoveAssignmentMemberParam): Result<Unit, AppError>
    fun getMembers(param: GetAssignmentMembersParam): Result<List<AssignmentMember>, AppError>
    fun transferOwnership(param: TransferOwnershipParam): Result<Assignment, AppError>
}
```

## Service Layer

New feature: `assignment` in `services/camper-service/src/main/kotlin/.../features/assignment/`

### Actions

| Action | Validates | Calls |
|--------|-----------|-------|
| CreateAssignment | name not blank, type valid | assignmentClient.create |
| GetAssignments | — | assignmentClient.getByPlanId |
| GetAssignment | — | assignmentClient.getById + getMembers |
| UpdateAssignment | requester is owner or plan owner | assignmentClient.update |
| DeleteAssignment | requester is owner or plan owner | assignmentClient.delete |
| AddAssignmentMember | not at capacity, not already in same type | assignmentClient.addMember |
| RemoveAssignmentMember | self-remove allowed; only assignment/plan owner can remove others | assignmentClient.removeMember |
| TransferOwnership | requester is owner or plan owner | assignmentClient.transferOwnership |

### Error Types

```kotlin
sealed class AssignmentError : AppError {
    NotFound(assignmentId)
    NotOwner(assignmentId, userId)
    Invalid(field, reason)
    AtCapacity(assignmentId)
    AlreadyAssigned(userId, type, planId)
    AlreadyMember(assignmentId, userId)
    CannotRemoveOwner(assignmentId, userId)
    PlanNotFound(planId)
}
```

## PR Stack

| # | Branch | Title | Description |
|---|--------|-------|-------------|
| 1 | plan | feat(assignments): plan | This document |
| 2 | db-contracts | feat(assignments): db contracts | Schema files, migration SQL, trigger |
| 3 | client-contracts | feat(assignments): client contracts | Interface, params, models, fake stubs |
| 4 | service-contracts | feat(assignments): service contracts | DTOs, errors, action signatures, routes (501s) |
| 5 | db-impl | feat(assignments): db implementation | Seed data |
| 6 | client-impl | feat(assignments): client implementation | JDBI operations, factory, fake |
| 7 | service-impl | feat(assignments): service implementation | Actions, service, controller wiring |
| 8 | client-tests | feat(assignments): client tests | Integration tests with Testcontainers |
| 9 | service-tests | feat(assignments): service tests | Unit tests with fake client |
| 10 | acceptance | feat(assignments): acceptance tests | End-to-end API tests |
| 11 | docs | feat(assignments): documentation | Update CLAUDE.md, README |

## Open Questions

None — all requirements clarified.
