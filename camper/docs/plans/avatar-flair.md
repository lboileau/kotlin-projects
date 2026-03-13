# Avatar Flair & Role-Based Access Control

## Feature Summary

Add a role system to plan members so the plan owner can promote members to "camp manager". Camp managers gain shared-gear editing rights alongside the owner. Additionally, give the trip owner and camp managers visual flair on the campsite scene: the owner gets a ranger hat and compass accessory on their CamperAvatar SVG, while managers get a compass/badge accessory (less prominent). This combines Linear tickets LBO-10 (Shared camp gear access control) and LBO-11 (Group leader avatar flair).

## Entities

### Modified: `plan_members` table
| Column | Type | Constraint | Notes |
|--------|------|------------|-------|
| `role` | `VARCHAR(20)` | `NOT NULL DEFAULT 'member'` | New column. Values: `member`, `manager` |

- CHECK constraint: `ck_plan_members_role CHECK (role IN ('member', 'manager'))`
- Owner role is NOT stored here — derived from `plans.owner_id`

### Modified: `PlanMember` (client model)
```kotlin
data class PlanMember(
    val planId: UUID,
    val userId: UUID,
    val role: String,       // NEW — "member" or "manager"
    val createdAt: Instant
)
```

### Modified: `PlanMember` (service model)
```kotlin
data class PlanMember(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val role: String,       // NEW — "member" or "manager"
    val createdAt: Instant
)
```

### Modified: `PlanMemberResponse` (DTO)
```kotlin
data class PlanMemberResponse(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val role: String,       // NEW
    val createdAt: Instant
)
```

### Modified: `PlanMember` (TypeScript interface)
```typescript
export interface PlanMember {
  planId: string;
  userId: string;
  username: string | null;
  email: string | null;
  invitationStatus: string | null;
  role: string;             // NEW — "member" | "manager"
  createdAt: string;
}
```

## API Surface

| Method | Path | Auth | Description | Request Body | Response |
|--------|------|------|-------------|--------------|----------|
| `PATCH` | `/api/plans/{planId}/members/{userId}/role` | `X-User-Id` (owner only) | Promote/demote a member | `{ "role": "manager" \| "member" }` | `PlanMemberResponse` (200) |
| `GET` | `/api/plans/{planId}/members` | `X-User-Id` | List members (existing) | — | Now includes `role` field |

No other endpoint changes. The shared gear permission check is frontend-only today (checking `currentUserId === planOwnerId`). We update this to also allow managers.

## Database Changes

### Migration V024: Add role to plan_members

```sql
-- V024__add_role_to_plan_members.sql
ALTER TABLE plan_members ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'member';
ALTER TABLE plan_members ADD CONSTRAINT ck_plan_members_role CHECK (role IN ('member', 'manager'));
```

### Rollback

```sql
-- R024__add_role_to_plan_members.sql
ALTER TABLE plan_members DROP CONSTRAINT IF EXISTS ck_plan_members_role;
ALTER TABLE plan_members DROP COLUMN IF EXISTS role;
```

### Schema update (schema/tables/004_plan_members.sql)

```sql
CREATE TABLE IF NOT EXISTS plan_members (
    plan_id    UUID         NOT NULL,
    user_id    UUID         NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'member',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    PRIMARY KEY (plan_id, user_id),
    CONSTRAINT fk_plan_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_plan_members_role CHECK (role IN ('member', 'manager'))
);
CREATE INDEX IF NOT EXISTS idx_plan_members_user_id ON plan_members (user_id);
```

### Seed data update

Add a manager role to at least one existing seed member for dev testing.

## Client Interface

### New param
```kotlin
/** Parameter for updating a member's role. */
data class UpdateMemberRoleParam(val planId: UUID, val userId: UUID, val role: String)
```

### New method on `PlanClient`
```kotlin
/** Update a plan member's role. */
fun updateMemberRole(param: UpdateMemberRoleParam): Result<PlanMember, AppError>
```

### Modified: `PlanMemberRowAdapter`
Add `role` column mapping:
```kotlin
fun fromResultSet(rs: ResultSet) = PlanMember(
    planId = rs.getObject("plan_id", UUID::class.java),
    userId = rs.getObject("user_id", UUID::class.java),
    role = rs.getString("role"),
    createdAt = rs.getTimestamp("created_at").toInstant()
)
```

### Modified: `GetPlanMembers` operation
Update SQL to include `role`:
```sql
SELECT plan_id, user_id, role, created_at
FROM plan_members
WHERE plan_id = :planId
ORDER BY created_at
```

### Modified: `AddPlanMember` operation
Update INSERT to include `role` (default 'member'):
```sql
INSERT INTO plan_members (plan_id, user_id, role) VALUES (:planId, :userId, 'member')
```
(The default is also handled by the DB, but be explicit.)

### New operation: `UpdateMemberRole`
```sql
UPDATE plan_members SET role = :role WHERE plan_id = :planId AND user_id = :userId
RETURNING plan_id, user_id, role, created_at
```

### New validation: `ValidateUpdateMemberRole`
- `role` must be `member` or `manager`
- `planId` must not be null
- `userId` must not be null

### Modified: `FakePlanClient`
- Store `role` in the in-memory member records
- Implement `updateMemberRole` method

## Service Layer

### New action: `UpdateMemberRoleAction`
1. Validate role is `member` or `manager` (via `ValidateUpdateMemberRole`)
2. Verify the plan exists (via `planClient.getById`)
3. Verify the requesting user is the plan owner (`plan.ownerId == requestingUserId`)
4. Verify the target user is not the owner (cannot change owner's "role")
5. Verify the target user is a plan member
6. Call `planClient.updateMemberRole`
7. Return the updated member (enriched with username, email, invitation status)

### New validation: `ValidateUpdateMemberRole`
- `role` must be `member` or `manager`

### New service param
```kotlin
data class UpdateMemberRoleParam(
    val planId: UUID,
    val userId: UUID,
    val role: String,
    val requestingUserId: UUID
)
```

### New request DTO
```kotlin
data class UpdateMemberRoleRequest(val role: String)
```

### Modified: `PlanService`
Add:
```kotlin
fun updateMemberRole(param: UpdateMemberRoleParam) = updateMemberRole.execute(param)
```

### Modified: `PlanMapper`
- `fromClient` for `PlanMember`: pass through `role`
- `toResponse` for `PlanMember`: include `role`

### Modified: `PlanError`
Add:
```kotlin
data class NotMemberOrOwner(val planId: String, val userId: String) : PlanError("...")
data class CannotChangeOwnerRole(val planId: String) : PlanError("...")
```

### Modified: `PlanController`
Add endpoint:
```kotlin
@PatchMapping("/{planId}/members/{userId}/role")
fun updateMemberRole(
    @PathVariable planId: UUID,
    @PathVariable userId: UUID,
    @RequestHeader("X-User-Id") requestingUserId: UUID,
    @RequestBody request: UpdateMemberRoleRequest
): ResponseEntity<Any>
```
Publishes WebSocket event `members` / `updated` on success.

### Modified: `PlanServiceConfig`
Wire the new action and validation.

## Frontend Changes

### API Client (`api/client.ts`)
- Add `role` field to `PlanMember` interface
- Add `updateMemberRole(planId: string, userId: string, role: string): Promise<PlanMember>` method

### PlanPage.tsx
- Compute each member's effective role: `plan.ownerId === member.userId ? 'owner' : member.role === 'manager' ? 'manager' : 'member'`
- Pass `role` prop to `CamperAvatar`
- Pass `members` and `currentUserId` to enable role management UI

### CamperAvatar.tsx
- Add `role` prop: `'owner' | 'manager' | 'member'`
- **Owner flair:** Ranger hat SVG overlay on top of the hood + compass accessory at belt/side
- **Manager flair:** Compass or badge accessory (less prominent than owner, no hat)
- **Member:** No flair (current appearance)
- Flair elements should:
  - Work in both day and night modes (use appropriate colors)
  - Participate in the existing gentle-bob animation (they're inside the same SVG)
  - Not extend outside the 48x64 viewBox (or adjust viewBox if needed for the hat)
- Only render flair for non-pending members (when `!isPending`)

### GearModal.tsx
- Change shared gear edit permission from `currentUserId === planOwnerId` to also allow managers:
  ```typescript
  const currentMember = members.find(m => m.userId === currentUserId);
  const canEditShared = currentUserId === planOwnerId || currentMember?.role === 'manager';
  ```
- Pass `canEditShared` instead of `currentUserId === planOwnerId` on line 532

### Role Management UI (PlanPage.tsx — Manage Plan modal or inline)
- Owner sees a role badge/dropdown next to each member in the campfire circle or in a dedicated panel
- Simple approach: Add a small crown/badge icon on each CamperAvatar that the owner can click to toggle manager status
- Or: Add role management to the existing "Manage Plan" modal with a members list

## PR Stack

```
1.  [plan]         feat(avatar-flair): plan — role-based access control + avatar flair
2.  [db]           feat(avatar-flair): db — add role column to plan_members
3.  [client]       feat(avatar-flair): client contracts — updateMemberRole interface + role on PlanMember
4.  [service]      feat(avatar-flair): service contracts — UpdateMemberRoleAction signature, DTOs, error types
5.  [db-impl]      feat(avatar-flair): db implementation — migration, rollback, schema, seed
6.  [client-impl]  feat(avatar-flair): client implementation — UpdateMemberRole operation, updated queries, fake
7.  [service-impl] feat(avatar-flair): service implementation — action, validation, controller, config
8.  [webapp]       feat(avatar-flair): webapp — avatar flair SVG, role management UI, gear permissions
9.  [client-test]  feat(avatar-flair): client tests — integration tests for updateMemberRole + role field
10. [service-test] feat(avatar-flair): service tests — unit tests for UpdateMemberRoleAction
11. [acceptance]   feat(avatar-flair): acceptance tests — end-to-end role + permission tests
12. [docs]         feat(avatar-flair): documentation updates
```

## Open Questions

None — the feature requirements are well-defined. The plan is ready for implementation.
