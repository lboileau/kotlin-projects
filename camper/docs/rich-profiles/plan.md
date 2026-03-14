# Rich Profiles — Feature Plan

## Feature Summary

Enrich user profiles with three new capabilities: **dietary restrictions** (multi-select from a fixed list), **experience level** (single-select enum), and **customized avatars** (deterministic procedural generation from a stored seed). All three are user-level settings managed through the existing profile update flow. The avatar seed is auto-generated from the user's name on registration and can be re-randomized on demand.

A **first-time profile setup modal** automatically opens when a user visits a plan page and has not yet completed their profile. The modal presents dietary restrictions, experience level, and avatar customization. Once submitted, the `profile_completed` flag is set to `true` and the modal never appears again.

**Linear ticket:** LBO-7 — Personal Profiles

---

## Entities

### User (existing — extended)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `UUID` | PK (existing) |
| `email` | `VARCHAR(255)` | Existing |
| `username` | `VARCHAR(100)` | Existing, nullable |
| `experience_level` | `VARCHAR(20)` | New, nullable. One of: `beginner`, `intermediate`, `advanced`, `expert` |
| `avatar_seed` | `VARCHAR(64)` | New, nullable. Hex string used to generate avatar |
| `profile_completed` | `BOOLEAN` | New, NOT NULL, DEFAULT false. Set to true after first profile setup via modal |
| `created_at` | `TIMESTAMPTZ` | Existing |
| `updated_at` | `TIMESTAMPTZ` | Existing |

### UserDietaryRestriction (new junction table)

| Field | Type | Notes |
|-------|------|-------|
| `user_id` | `UUID` | PK part 1, FK → users(id) ON DELETE CASCADE |
| `restriction` | `VARCHAR(30)` | PK part 2. One of: `gluten_free`, `nut_allergy`, `vegetarian`, `vegan`, `lactose_intolerant`, `shellfish_allergy`, `halal`, `kosher` |
| `created_at` | `TIMESTAMPTZ` | DEFAULT now() |

Composite PK: `(user_id, restriction)` — same pattern as `plan_members`.

---

## Database Changes

### Migration V027 — Add experience_level, avatar_seed, and profile_completed to users

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS experience_level VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_seed VARCHAR(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS profile_completed BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE users ADD CONSTRAINT ck_users_experience_level
    CHECK (experience_level IS NULL OR experience_level IN ('beginner', 'intermediate', 'advanced', 'expert'));
```

### Migration V028 — Create user_dietary_restrictions table

```sql
CREATE TABLE IF NOT EXISTS user_dietary_restrictions (
    user_id     UUID        NOT NULL,
    restriction VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, restriction),
    CONSTRAINT fk_user_dietary_restrictions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_dietary_restrictions_restriction CHECK (
        restriction IN ('gluten_free', 'nut_allergy', 'vegetarian', 'vegan', 'lactose_intolerant', 'shellfish_allergy', 'halal', 'kosher')
    )
);

CREATE INDEX IF NOT EXISTS idx_user_dietary_restrictions_user_id ON user_dietary_restrictions (user_id);
```

### Schema files

- `schema/tables/002_users.sql` — updated with new columns (`experience_level`, `avatar_seed`, `profile_completed`)
- `schema/tables/027_user_dietary_restrictions.sql` — new table

### Seed data

Add to `dev_seed.sql`:
- Alice: `vegetarian`, experience `intermediate`, avatar seed derived from "Alice", `profile_completed = true`
- Bob: `gluten_free`, `nut_allergy`, experience `advanced`, avatar seed derived from "Bob", `profile_completed = true`
- Charlie: no dietary restrictions, experience `beginner`, avatar seed derived from "Charlie", `profile_completed = false` (to test modal behavior)

---

## API Surface

### Updated Endpoints

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| `PUT` | `/api/users/{userId}` | Update profile | `UpdateUserRequest` (extended) | `UserResponse` (extended) |
| `GET` | `/api/users/{userId}` | Get user profile | — | `UserResponse` (extended) |
| `POST` | `/api/users` | Create user | `CreateUserRequest` (unchanged) | `UserResponse` (extended) |
| `POST` | `/api/auth` | Authenticate | `AuthRequest` (unchanged) | `AuthResponse` (extended) |
| `GET` | `/api/plans/{planId}/members` | List plan members | — | `PlanMemberResponse[]` (extended with avatar) |

### New Endpoints

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| `POST` | `/api/users/{userId}/randomize-avatar` | Re-randomize avatar seed | — | `UserResponse` |
| `GET` | `/api/users/{userId}/avatar` | Get computed avatar for user | — | `AvatarResponse` |

---

## Client Interface Changes

### Extended User Model

```kotlin
// clients/user-client/src/main/kotlin/.../model/User.kt
data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val experienceLevel: String?,       // New
    val avatarSeed: String?,            // New
    val profileCompleted: Boolean,      // New
    val dietaryRestrictions: List<String>, // New
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Extended Parameters

```kotlin
// clients/user-client/src/main/kotlin/.../api/UserClientParams.kt

// Existing — unchanged
data class GetByIdParam(val id: UUID)
data class GetByEmailParam(val email: String)
data class GetOrCreateUserParam(val email: String, val username: String? = null)

// Extended
data class CreateUserParam(val email: String, val username: String? = null, val avatarSeed: String? = null)
data class UpdateUserParam(
    val id: UUID,
    val username: String,
    val experienceLevel: String? = null,    // New, nullable = clear
    val avatarSeed: String? = null,         // New, nullable = no change (see below)
    val profileCompleted: Boolean? = null,  // New, null = no change, true = mark completed
    val dietaryRestrictions: List<String>? = null  // New, null = no change, empty = clear all
)
```

> **Note on avatarSeed in UpdateUserParam:** `null` means "don't change", a value means "set to this". Avatar seed is never cleared — only replaced.

### New Client Methods

```kotlin
// clients/user-client/src/main/kotlin/.../api/UserClient.kt
interface UserClient {
    // Existing
    fun getById(param: GetByIdParam): Result<User, AppError>
    fun getByEmail(param: GetByEmailParam): Result<User, AppError>
    fun create(param: CreateUserParam): Result<User, AppError>
    fun getOrCreate(param: GetOrCreateUserParam): Result<User, AppError>
    fun update(param: UpdateUserParam): Result<User, AppError>

    // New
    /** Get dietary restrictions for a user */
    fun getDietaryRestrictions(param: GetDietaryRestrictionsParam): Result<List<String>, AppError>

    /** Replace all dietary restrictions for a user (idempotent) */
    fun setDietaryRestrictions(param: SetDietaryRestrictionsParam): Result<List<String>, AppError>
}
```

### New Parameters

```kotlin
data class GetDietaryRestrictionsParam(val userId: UUID)
data class SetDietaryRestrictionsParam(val userId: UUID, val restrictions: List<String>)
```

### New Client Operations

| Operation | File | Description |
|-----------|------|-------------|
| `GetDietaryRestrictions` | `internal/operations/GetDietaryRestrictions.kt` | SELECT from `user_dietary_restrictions` WHERE user_id |
| `SetDietaryRestrictions` | `internal/operations/SetDietaryRestrictions.kt` | DELETE all + INSERT new (in transaction) |

### New Validations

| Validation | File | Rules |
|------------|------|-------|
| `ValidateGetDietaryRestrictions` | `internal/validations/ValidateGetDietaryRestrictions.kt` | userId must not be null |
| `ValidateSetDietaryRestrictions` | `internal/validations/ValidateSetDietaryRestrictions.kt` | Each restriction must be in allowed set |
| `ValidateUpdateUser` (extended) | `internal/validations/ValidateUpdateUser.kt` | Add: experienceLevel must be in allowed set if provided |

### UserRowAdapter Changes

```kotlin
object UserRowAdapter {
    fun fromResultSet(rs: ResultSet): User = User(
        id = rs.getObject("id", UUID::class.java),
        email = rs.getString("email"),
        username = rs.getString("username"),
        experienceLevel = rs.getString("experience_level"),   // New
        avatarSeed = rs.getString("avatar_seed"),             // New
        profileCompleted = rs.getBoolean("profile_completed"), // New
        dietaryRestrictions = emptyList(),                     // Populated separately
        createdAt = rs.getTimestamp("created_at").toInstant(),
        updatedAt = rs.getTimestamp("updated_at").toInstant()
    )
}
```

> Dietary restrictions are loaded separately (via join or second query in GetUserById/GetUserByEmail) and merged into the User model. The adapter handles the flat row; the operation enriches it.

### Enrichment Strategy

Operations `GetUserById` and `GetUserByEmail` will be updated to:
1. Query the user row
2. Query `user_dietary_restrictions` WHERE `user_id = :id`
3. Return `User` with populated `dietaryRestrictions`

This avoids complex joins and keeps the adapter simple.

### Updated Client Operations

**`CreateUser`** — Extended to include `avatar_seed` in the INSERT statement when provided via `CreateUserParam.avatarSeed`.

**`UpdateUser`** — Extended SQL to conditionally update `experience_level`, `avatar_seed`, and `profile_completed` alongside `username`. Fields with `null` param values are not included in the UPDATE (no change). `profile_completed` is one-way: only updated when `true` (ignores `null` and `false`).

### Avatar Seed Lifecycle

The avatar seed can be set at several points depending on the user creation flow:

1. **Direct registration** (`POST /api/users` → `CreateUserAction`): Service generates seed via `AvatarGenerator.seedFromName(username ?: email)` and passes it in `CreateUserParam.avatarSeed`.

2. **Invite flow** (`getOrCreate` → stub user created without seed): The invited user is created with `avatarSeed = null`. When they later register via `POST /api/users`, `CreateUserAction` finds the existing user (idempotent), updates their username, AND generates an avatar seed at this point via the client `update` call.

3. **Re-randomize** (`POST /api/users/{userId}/randomize-avatar`): `RandomizeAvatarAction` generates a new seed via `AvatarGenerator.randomSeed()` and updates via client.

4. **Profile setup modal**: Does not generate a new seed — the user already has one from registration (step 1 or 2). The modal only allows randomizing via the "Randomize" button.

> **Edge case:** If a user somehow has no seed (e.g., pre-existing users before migration), the response will have `avatarSeed: null` and `avatar: null`. The frontend should handle this gracefully by showing a default/placeholder avatar.

### FakeUserClient Changes

Extend `FakeUserClient` store to include `profileCompleted` and `dietaryRestrictions`. Add in-memory storage (`ConcurrentHashMap<UUID, List<String>>`) for dietary restrictions. Implement `getDietaryRestrictions` and `setDietaryRestrictions`. Handle `profileCompleted` one-way logic in `update`.

---

## Lib: Avatar Generator

**Module:** `libs/avatar-generator`
**Package:** `com.acme.libs.avatargenerator`

### Purpose

Pure-logic library that deterministically generates avatar properties from a seed string. No I/O.

### Model

```kotlin
// libs/avatar-generator/src/main/kotlin/.../model/Avatar.kt
data class Avatar(
    val hairStyle: HairStyle,
    val hairColor: HairColor,
    val skinColor: SkinColor,
    val clothingStyle: ClothingStyle,
    val pantsColor: PantsColor,
    val shirtColor: ShirtColor
)

enum class HairStyle {
    SHORT, LONG, CURLY, WAVY, BUZZ, MOHAWK, PONYTAIL, BUN
}

enum class HairColor {
    BLACK, BROWN, BLONDE, RED, GRAY, WHITE, AUBURN, PLATINUM
}

enum class SkinColor {
    LIGHT, FAIR, MEDIUM, OLIVE, TAN, BROWN, DARK, DEEP
}

enum class ClothingStyle {
    CASUAL, SPORTY, OUTDOORSY, FORMAL, RUGGED, BOHEMIAN, PREPPY, MINIMALIST
}

enum class PantsColor {
    BLACK, NAVY, KHAKI, OLIVE, BROWN, GRAY, DENIM, CHARCOAL
}

enum class ShirtColor {
    RED, BLUE, GREEN, YELLOW, ORANGE, PURPLE, WHITE, TEAL
}
```

### Generator

```kotlin
// libs/avatar-generator/src/main/kotlin/.../AvatarGenerator.kt
object AvatarGenerator {
    /**
     * Generate an avatar deterministically from a seed string.
     * Same seed always produces the same avatar.
     */
    fun generate(seed: String): Avatar

    /**
     * Generate a seed from a user's name.
     * Used for initial avatar seed on registration.
     */
    fun seedFromName(name: String): String

    /**
     * Generate a random seed.
     * Used when the user re-randomizes their avatar.
     */
    fun randomSeed(): String
}
```

### Algorithm

1. **Seed → Hash:** SHA-256 hash of the seed string → 32 bytes (64 hex chars)
2. **Hash → Components:** Each component uses a different segment of the hash:
   - Bytes 0-3 → `hairStyle` (mod 8)
   - Bytes 4-7 → `hairColor` (mod 8)
   - Bytes 8-11 → `skinColor` (mod 8)
   - Bytes 12-15 → `clothingStyle` (mod 8)
   - Bytes 16-19 → `pantsColor` (mod 8)
   - Bytes 20-23 → `shirtColor` (mod 8)
3. **seedFromName:** SHA-256 of lowercase-trimmed name, return hex string
4. **randomSeed:** `UUID.randomUUID().toString()` (fed into generate, which hashes it)

Each enum has exactly 8 values. 4 bytes mod 8 gives uniform distribution. Total combinations: 8^6 = 262,144 unique avatars.

**Determinism guarantee:** SHA-256 is deterministic. Same seed → same hash → same indices → same avatar.
**Uniqueness guarantee:** SHA-256 collision resistance ensures different seeds produce different hashes (and thus different avatars with overwhelmingly high probability).

---

## Service Layer Changes

### Extended Service Model

```kotlin
// services/camper-service/.../features/user/model/User.kt
data class User(
    val id: UUID,
    val email: String,
    val username: String?,
    val experienceLevel: String?,           // New
    val avatarSeed: String?,                // New
    val profileCompleted: Boolean,          // New
    val dietaryRestrictions: List<String>,   // New
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### Extended Plan Member Model

```kotlin
// services/camper-service/.../features/plan/model/Plan.kt (PlanMember class)
data class PlanMember(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val role: String,
    val avatarSeed: String?,                // New — from user lookup
    val createdAt: Instant
)
```

### Extended DTOs

```kotlin
// User Request DTOs
data class UpdateUserRequest(
    val username: String,
    val experienceLevel: String? = null,        // New
    val dietaryRestrictions: List<String>? = null, // New, null = no change
    val profileCompleted: Boolean? = null       // New, set to true on first profile setup
)

// User Response DTOs
data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val experienceLevel: String?,               // New
    val avatarSeed: String?,                    // New
    val profileCompleted: Boolean,              // New
    val dietaryRestrictions: List<String>,       // New
    val avatar: AvatarResponse?,                // New — computed from seed
    val createdAt: Instant,
    val updatedAt: Instant
)

data class AuthResponse(
    val id: UUID,
    val email: String,
    val username: String?,
    val avatarSeed: String?,                    // New
    val profileCompleted: Boolean,              // New — frontend uses this to show/hide setup modal
    val avatar: AvatarResponse?                 // New
)

data class AvatarResponse(
    val hairStyle: String,
    val hairColor: String,
    val skinColor: String,
    val clothingStyle: String,
    val pantsColor: String,
    val shirtColor: String
)

// Plan Member Response DTO (extended)
data class PlanMemberResponse(
    val planId: UUID,
    val userId: UUID,
    val username: String?,
    val email: String?,
    val invitationStatus: String?,
    val role: String,
    val avatarSeed: String?,                    // New
    val avatar: AvatarResponse?,                // New — computed from seed
    val createdAt: Instant
)
```

### Extended Service Params

```kotlin
data class UpdateUserParam(
    val userId: UUID,
    val username: String,
    val experienceLevel: String? = null,         // New
    val dietaryRestrictions: List<String>? = null, // New
    val profileCompleted: Boolean? = null,       // New — set to true on first profile setup
    val requestingUserId: UUID
)

data class RandomizeAvatarParam(val userId: UUID, val requestingUserId: UUID)  // New
data class GetAvatarParam(val userId: UUID)  // New
```

### New / Updated Actions

| Action | Description |
|--------|-------------|
| `UpdateUserAction` (extended) | Now also updates experience level, dietary restrictions, and `profileCompleted` flag via client |
| `RandomizeAvatarAction` (new) | Generates random seed via `AvatarGenerator.randomSeed()`, updates user's avatar_seed |
| `GetAvatarAction` (new) | Gets user, generates avatar from seed via `AvatarGenerator.generate()` |
| `CreateUserAction` (extended) | On create, generate initial avatar seed via `AvatarGenerator.seedFromName(username ?: email)`. For invite-flow users (existing stub found), also generate seed during the username update if `avatarSeed` is null |

### New / Updated Validations

| Validation | Rules |
|------------|-------|
| `ValidateUpdateUser` (extended) | Add: experienceLevel must be valid if provided; each dietary restriction must be valid |
| `ValidateRandomizeAvatar` (new) | userId must not be null |
| `ValidateGetAvatar` (new) | userId must not be null |

### Updated UserMapper

```kotlin
object UserMapper {
    fun fromClient(clientUser: ClientUser): User = User(
        id = clientUser.id,
        email = clientUser.email,
        username = clientUser.username,
        experienceLevel = clientUser.experienceLevel,
        avatarSeed = clientUser.avatarSeed,
        profileCompleted = clientUser.profileCompleted,
        dietaryRestrictions = clientUser.dietaryRestrictions,
        createdAt = clientUser.createdAt,
        updatedAt = clientUser.updatedAt
    )

    fun toResponse(user: User): UserResponse = UserResponse(
        id = user.id,
        email = user.email,
        username = user.username,
        experienceLevel = user.experienceLevel,
        avatarSeed = user.avatarSeed,
        profileCompleted = user.profileCompleted,
        dietaryRestrictions = user.dietaryRestrictions,
        avatar = user.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) },
        createdAt = user.createdAt,
        updatedAt = user.updatedAt
    )

    fun toAuthResponse(user: User): AuthResponse = AuthResponse(
        id = user.id,
        email = user.email,
        username = user.username,
        avatarSeed = user.avatarSeed,
        profileCompleted = user.profileCompleted,
        avatar = user.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) }
    )
}
```

### New AvatarMapper

```kotlin
object AvatarMapper {
    fun toResponse(avatar: Avatar): AvatarResponse = AvatarResponse(
        hairStyle = avatar.hairStyle.name.lowercase(),
        hairColor = avatar.hairColor.name.lowercase(),
        skinColor = avatar.skinColor.name.lowercase(),
        clothingStyle = avatar.clothingStyle.name.lowercase(),
        pantsColor = avatar.pantsColor.name.lowercase(),
        shirtColor = avatar.shirtColor.name.lowercase()
    )
}
```

### Plan Member Avatar Enrichment

**Approach:** Service-layer enrichment (no plan-client changes needed).

`GetPlanMembersAction` already calls `userClient.getById()` for each member to fetch `username` and `email`. Since the `User` client model now includes `avatarSeed`, the action simply passes it through to the `PlanMember` service model.

**Changes to `GetPlanMembersAction`:**
```kotlin
// In the member mapping loop (already fetches user via userClient.getById):
PlanMapper.fromClient(
    clientMember,
    username = user?.username,
    email = user?.email ?: invitation?.email,
    invitationStatus = invitation?.status,
    avatarSeed = user?.avatarSeed          // New — pass through from user lookup
)
```

**Changes to `PlanMapper`:**
```kotlin
fun fromClient(
    clientMember: ClientPlanMember,
    username: String? = null,
    email: String? = null,
    invitationStatus: String? = null,
    avatarSeed: String? = null              // New parameter
): PlanMember = PlanMember(
    planId = clientMember.planId,
    userId = clientMember.userId,
    username = username,
    email = email,
    invitationStatus = invitationStatus,
    role = clientMember.role,
    avatarSeed = avatarSeed,                // New
    createdAt = clientMember.createdAt
)

fun toResponse(member: PlanMember): PlanMemberResponse = PlanMemberResponse(
    planId = member.planId,
    userId = member.userId,
    username = member.username,
    email = member.email,
    invitationStatus = member.invitationStatus,
    role = member.role,
    avatarSeed = member.avatarSeed,         // New
    avatar = member.avatarSeed?.let { AvatarMapper.toResponse(AvatarGenerator.generate(it)) },  // New
    createdAt = member.createdAt
)
```

> **Why service-layer enrichment?** The plan-client's `PlanMember` comes from the `plan_members` junction table which only has `(plan_id, user_id, role, created_at)`. The avatar seed lives on the `users` table. Rather than adding a cross-table join in plan-client, we reuse the existing user lookup in `GetPlanMembersAction` — zero new queries, zero new client methods.

### Updated UserError

```kotlin
sealed class UserError(override val message: String) : AppError {
    data class NotFound(val email: String) : UserError("User not found: $email")
    data class Invalid(val field: String, val reason: String) : UserError("Invalid user $field: $reason")
    data class Forbidden(val userId: String) : UserError("Forbidden: user $userId cannot perform this action")
    data class RegistrationRequired(val email: String) : UserError("Please register with a trail name to continue")
    // No new error types needed — Invalid covers validation failures
}
```

### Updated Controller Routes

```kotlin
@RestController
class UserController(private val userService: UserService) {

    // Existing (unchanged signatures, extended response)
    @GetMapping("/api/users/{userId}")
    fun getById(@PathVariable userId: UUID): ResponseEntity<Any>

    @PostMapping("/api/users")
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<Any>

    @PostMapping("/api/auth")
    fun authenticate(@RequestBody request: AuthRequest): ResponseEntity<Any>

    @PutMapping("/api/users/{userId}")
    fun update(
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID,
        @RequestBody request: UpdateUserRequest  // Extended body (includes profileCompleted)
    ): ResponseEntity<Any>

    // New
    @PostMapping("/api/users/{userId}/randomize-avatar")
    fun randomizeAvatar(
        @PathVariable userId: UUID,
        @RequestHeader("X-User-Id") requestingUserId: UUID
    ): ResponseEntity<Any>

    @GetMapping("/api/users/{userId}/avatar")
    fun getAvatar(@PathVariable userId: UUID): ResponseEntity<Any>
}
```

### Service Wiring

`UserServiceConfig` updated to inject `AvatarGenerator` is not needed — it's a static `object`. The `UserService` will add:
- `fun randomizeAvatar(param: RandomizeAvatarParam): Result<User, UserError>`
- `fun getAvatar(param: GetAvatarParam): Result<AvatarResponse, UserError>`

The service already takes `UserClient` — no new client dependencies needed.

---

## First-Time Profile Setup Modal

### Behavior

1. **Trigger:** When a user navigates to a plan page, the frontend checks `profileCompleted` from the auth/user response.
2. **If `profileCompleted` is `false`:** A modal automatically opens with:
   - Dietary restrictions (multi-select checkboxes)
   - Experience level (single-select radio/dropdown)
   - Avatar preview with a "Randomize" button (calls `POST /api/users/{userId}/randomize-avatar`)
3. **On submit:** The frontend calls `PUT /api/users/{userId}` with the selected values and `profileCompleted: true`.
4. **If `profileCompleted` is `true`:** The modal does not appear. The user can still edit their profile via the profile settings page.

### Data Flow

```
Plan page load → GET /api/users/{userId} or use cached auth data
  → profileCompleted === false?
    → YES: open profile setup modal
      → user fills in dietary restrictions, experience level, (optionally randomizes avatar)
      → PUT /api/users/{userId} { username, experienceLevel, dietaryRestrictions, profileCompleted: true }
      → modal closes, never shown again
    → NO: normal plan page behavior
```

### Backend Impact

- **No new endpoints needed.** The existing `PUT /api/users/{userId}` handles all profile fields including `profileCompleted`.
- **`profileCompleted` is one-way:** Once set to `true`, it should not be set back to `false`. The `UpdateUser` client operation only updates `profile_completed` when the param value is `true` (ignores `null` and `false`).
- **`AuthResponse` includes `profileCompleted`** so the frontend has the flag available immediately after login without an extra API call.

---

## PR Stack

| # | Tag | Branch | Title | Description |
|---|-----|--------|-------|-------------|
| 1 | `[plan]` | `feat/rich-profiles/plan` | feat(rich-profiles): plan | This document |
| 2 | `[db]` | `feat/rich-profiles/db` | feat(rich-profiles): db contracts | Schema files for updated `users` table and new `user_dietary_restrictions` table |
| 3 | `[client]` | `feat/rich-profiles/client` | feat(rich-profiles): client contracts | Extended `UserClient` interface, params, model with new fields. Placeholder operations |
| 4 | `[lib]` | `feat/rich-profiles/lib` | feat(rich-profiles): lib contracts | `avatar-generator` module with `AvatarGenerator`, `Avatar` model, and enums |
| 5 | `[service]` | `feat/rich-profiles/service` | feat(rich-profiles): service contracts | Extended DTOs, params, actions, mapper, controller routes, plan member avatar enrichment. Placeholder action bodies |
| 6 | `[db-impl]` | `feat/rich-profiles/db-impl` | feat(rich-profiles): db implementation | Migration V027 (experience_level, avatar_seed, profile_completed), V028 (dietary restrictions table), updated schema, seed data |
| 7 | `[client-impl]` | `feat/rich-profiles/client-impl` | feat(rich-profiles): client implementation | Implement all client operations, validations, row adapter changes, fake client |
| 8 | `[lib-impl]` | `feat/rich-profiles/lib-impl` | feat(rich-profiles): lib implementation | Implement `AvatarGenerator.generate()`, `seedFromName()`, `randomSeed()` with SHA-256 |
| 9 | `[service-impl]` | `feat/rich-profiles/service-impl` | feat(rich-profiles): service implementation | Implement all actions, mapper, controller wiring, plan member avatar enrichment |
| 10 | `[client-test]` | `feat/rich-profiles/client-test` | feat(rich-profiles): client tests | Integration tests for new/updated user-client operations with Testcontainers |
| 11 | `[service-test]` | `feat/rich-profiles/service-test` | feat(rich-profiles): service tests | Unit tests for actions using FakeUserClient, including plan member avatar enrichment |
| 12 | `[acceptance]` | `feat/rich-profiles/acceptance` | feat(rich-profiles): acceptance tests | End-to-end API tests for profile update, dietary restrictions, avatar endpoints, plan member avatars |
| 13 | `[fe]` | `feat/rich-profiles/fe` | feat(rich-profiles): webapp | Frontend changes: profile page (dietary restrictions, experience level, avatar display/randomize), plan member avatars in member lists, first-time profile setup modal (auto-opens on plan page when `profileCompleted` is false, sets flag to true on submit) |
| 14 | `[docs]` | `feat/rich-profiles/docs` | feat(rich-profiles): documentation updates | Update CLAUDE.md files with new feature documentation |
