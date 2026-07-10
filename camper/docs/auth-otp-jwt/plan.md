# Plan: auth-otp-jwt

## Feature Summary

Replace the existing trust-based authentication (X-User-Id header) with email OTP + JWT token authentication. Users authenticate via a 6-digit one-time password sent to their email, receiving short-lived JWT access tokens (15 min) and long-lived refresh tokens (7 days). A Spring filter validates JWTs on every request, including a user existence check. Internal database UUIDs are replaced with random external IDs in all API responses. The React frontend is updated with a new OTP login flow, token lifecycle management, and Bearer auth.

---

## Entities & Data Model

### AuthOtpCode (new)
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() |
| `email` | VARCHAR(255) | NOT NULL (normalized) |
| `code_hash` | VARCHAR(64) | NOT NULL (SHA-256 hex of 6-digit code) |
| `expires_at` | TIMESTAMPTZ | NOT NULL (created_at + 5 min) |
| `used_at` | TIMESTAMPTZ | NULL (set on verify) |
| `attempt_count` | INTEGER | NOT NULL DEFAULT 0 (incremented on failed verify, max 5) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### AuthRefreshToken (new)
| Field | Type | Constraints |
|-------|------|-------------|
| `id` | UUID | PK, DEFAULT gen_random_uuid() |
| `user_id` | UUID | NOT NULL FK -> users(id) ON DELETE CASCADE |
| `family_id` | UUID | NOT NULL (shared across a rotation chain; new on each login) |
| `token_hash` | VARCHAR(255) | NOT NULL (SHA-256 of opaque token) |
| `expires_at` | TIMESTAMPTZ | NOT NULL (created_at + 7 days) |
| `revoked_at` | TIMESTAMPTZ | NULL (set on logout/new login) |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT now() |

### Users table (modified)
| Field | Type | Constraints |
|-------|------|-------------|
| `external_id` | UUID | NOT NULL DEFAULT gen_random_uuid(), UNIQUE |

### JWT Access Token (not persisted)
- Claims: `sub` = externalId (UUID — the only user identifier in the token), `email`, `iat`, `exp` (15 min)
- No internal user ID in token — the filter resolves internal ID via DB lookup on the `sub` (externalId)
- Signed with HMAC-SHA256 using `JWT_SECRET` env var (falls back to `JWT_PREVIOUS_SECRET` for rotation)

---

## API Surface

### New Endpoints

| Method | Path | Description | Request Body | Success Response |
|--------|------|-------------|--------------|------------------|
| POST | `/api/auth/otp/request` | Request OTP code | `{ "email": "..." }` | 200 `{ "message": "OTP sent" }` |
| POST | `/api/auth/otp/verify` | Verify OTP, get tokens | `{ "email": "...", "code": "123456" }` | 200 `{ "accessToken": "...", "refreshToken": "...", "user": {...} }` |
| POST | `/api/auth/token/refresh` | Refresh access token | `{ "refreshToken": "..." }` | 200 `{ "accessToken": "...", "refreshToken": "..." }` |
| POST | `/api/auth/logout` | Revoke refresh token | `{ "refreshToken": "..." }` | 204 No Content |

### Removed Endpoints

| Method | Path | Reason |
|--------|------|--------|
| POST | `/api/auth` | Replaced by OTP flow |
| POST | `/api/users` | User creation now happens via OTP verify |

### Modified Endpoints (all existing)

All existing endpoints change from `@RequestHeader("X-User-Id")` to JWT-based auth:
- JWT filter extracts internal `userId` from token, sets as request attribute
- Controllers use `@RequestAttribute("userId")` instead of `@RequestHeader("X-User-Id")`
- Response DTOs return `external_id` instead of internal UUID for all user ID fields

### Path Parameters Accepting External IDs

These endpoints accept user IDs in path params or request bodies. They must accept external_id and resolve to internal:

| Endpoint | Parameter | Current | New |
|----------|-----------|---------|-----|
| `PATCH /api/plans/{planId}/members/{userId}/role` | `userId` path param | internal UUID | external_id, resolved via `userClient.getByExternalId()` |
| `DELETE /api/plans/{planId}/members/{memberId}` | `memberId` path param | internal UUID | external_id, resolved via `userClient.getByExternalId()` |
| `DELETE /api/plans/{planId}/assignments/{id}/members/{memberUserId}` | `memberUserId` path param | internal UUID | external_id, resolved via `userClient.getByExternalId()` |
| `POST /api/plans/{planId}/assignments/{id}/members` | `userId` in body | internal UUID | external_id, resolved in action |
| `PUT /api/plans/{planId}/assignments/{id}/owner` | `newOwnerId` in body | internal UUID | external_id, resolved in action |
| `GET /api/items?ownerType=user&ownerId={id}` | `ownerId` query param | internal UUID | external_id, resolved in action |

---

## Database Changes

### V038__create_auth_otp_codes.sql
```sql
CREATE TABLE IF NOT EXISTS auth_otp_codes (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    code_hash     VARCHAR(64)  NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    used_at       TIMESTAMPTZ,
    attempt_count INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_otp_codes_lookup
    ON auth_otp_codes (email, code_hash, expires_at);
```

### V039__create_auth_refresh_tokens.sql
```sql
CREATE TABLE IF NOT EXISTS auth_refresh_tokens (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id  UUID         NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_user_id
    ON auth_refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_token_hash
    ON auth_refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_auth_refresh_tokens_family_id
    ON auth_refresh_tokens (family_id);
```

### V040__add_external_id_to_users.sql
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS external_id UUID NOT NULL DEFAULT gen_random_uuid();

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_external_id ON users (external_id);
```

### Schema files to create/update
- `databases/camper-db/schema/tables/038_auth_otp_codes.sql`
- `databases/camper-db/schema/tables/039_auth_refresh_tokens.sql`
- `databases/camper-db/schema/tables/002_users.sql` (add external_id column)

### Rollback files
- `databases/camper-db/migrations/rollback/R038__drop_auth_otp_codes.sql`
- `databases/camper-db/migrations/rollback/R039__drop_auth_refresh_tokens.sql`
- `databases/camper-db/migrations/rollback/R040__drop_external_id_from_users.sql`

### Seed data update
- `databases/camper-db/seed/dev_seed.sql` — add explicit `external_id` values for Alice, Bob, Charlie for test stability

---

## Client Interfaces

### New: auth-client

**Package:** `com.acme.clients.authclient`

**Interface: `AuthClient`**
```kotlin
interface AuthClient {
    /** Store a new OTP code hash for the given email. */
    fun createOtp(param: CreateOtpParam): Result<AuthOtpCode, AppError>

    /** Find a valid (unexpired, unused, under max attempts) OTP matching email + code hash. */
    fun findValidOtp(param: FindValidOtpParam): Result<AuthOtpCode?, AppError>

    /** Increment the attempt_count on an OTP record (called on failed verify). */
    fun incrementOtpAttemptCount(param: IncrementOtpAttemptCountParam): Result<Unit, AppError>

    /** Mark an OTP as used (set used_at). */
    fun markOtpUsed(param: MarkOtpUsedParam): Result<Unit, AppError>

    /** Store a new refresh token hash with family_id. */
    fun createRefreshToken(param: CreateRefreshTokenParam): Result<AuthRefreshToken, AppError>

    /** Find a valid (unexpired, unrevoked) refresh token by hash. */
    fun findValidRefreshToken(param: FindValidRefreshTokenParam): Result<AuthRefreshToken?, AppError>

    /** Revoke a single refresh token by ID. */
    fun revokeRefreshToken(param: RevokeRefreshTokenParam): Result<Unit, AppError>

    /** Revoke all refresh tokens for a user. */
    fun revokeAllUserRefreshTokens(param: RevokeAllUserRefreshTokensParam): Result<Unit, AppError>

    /** Revoke all refresh tokens in a token family (reuse detection). */
    fun revokeTokenFamily(param: RevokeTokenFamilyParam): Result<Unit, AppError>
}
```

**Models:**
```kotlin
// model/AuthOtpCode.kt
data class AuthOtpCode(
    val id: UUID,
    val email: String,
    val codeHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
    val attemptCount: Int,
    val createdAt: Instant
)

// model/AuthRefreshToken.kt
data class AuthRefreshToken(
    val id: UUID,
    val userId: UUID,
    val familyId: UUID,
    val tokenHash: String,
    val expiresAt: Instant,
    val revokedAt: Instant?,
    val createdAt: Instant
)
```

**Params:**
```kotlin
// api/AuthClientParams.kt
data class CreateOtpParam(val email: String, val codeHash: String, val expiresAt: Instant)
data class FindValidOtpParam(val email: String, val codeHash: String)
data class IncrementOtpAttemptCountParam(val id: UUID)
data class MarkOtpUsedParam(val id: UUID)
data class CreateRefreshTokenParam(val userId: UUID, val familyId: UUID, val tokenHash: String, val expiresAt: Instant)
data class FindValidRefreshTokenParam(val tokenHash: String)
data class RevokeRefreshTokenParam(val id: UUID)
data class RevokeAllUserRefreshTokensParam(val userId: UUID)
data class RevokeTokenFamilyParam(val familyId: UUID)
```

**Directory structure:**
```
clients/auth-client/
├── build.gradle.kts
└── src/
    ├── main/kotlin/com/acme/clients/authclient/
    │   ├── api/
    │   │   ├── AuthClient.kt              # Interface
    │   │   └── AuthClientParams.kt        # All param data classes
    │   ├── model/
    │   │   ├── AuthOtpCode.kt
    │   │   └── AuthRefreshToken.kt
    │   ├── internal/
    │   │   ├── JdbiAuthClient.kt          # Facade
    │   │   ├── adapters/
    │   │   │   ├── AuthOtpCodeRowAdapter.kt
    │   │   │   └── AuthRefreshTokenRowAdapter.kt
    │   │   ├── operations/
    │   │   │   ├── CreateOtp.kt
    │   │   │   ├── FindValidOtp.kt
    │   │   │   ├── IncrementOtpAttemptCount.kt
    │   │   │   ├── MarkOtpUsed.kt
    │   │   │   ├── CreateRefreshToken.kt
    │   │   │   ├── FindValidRefreshToken.kt
    │   │   │   ├── RevokeRefreshToken.kt
    │   │   │   ├── RevokeAllUserRefreshTokens.kt
    │   │   │   └── RevokeTokenFamily.kt
    │   │   └── validations/
    │   │       ├── ValidateCreateOtp.kt
    │   │       ├── ValidateFindValidOtp.kt
    │   │       ├── ValidateIncrementOtpAttemptCount.kt
    │   │       ├── ValidateMarkOtpUsed.kt
    │   │       ├── ValidateCreateRefreshToken.kt
    │   │       ├── ValidateFindValidRefreshToken.kt
    │   │       ├── ValidateRevokeRefreshToken.kt
    │   │       ├── ValidateRevokeAllUserRefreshTokens.kt
    │   │       └── ValidateRevokeTokenFamily.kt
    │   └── AuthClientFactory.kt           # createAuthClient()
    └── testFixtures/kotlin/com/acme/clients/authclient/
        └── fake/
            └── FakeAuthClient.kt
```

### Modified: user-client

**New methods on `UserClient` interface:**
```kotlin
/** Look up a user by external_id. */
fun getByExternalId(param: GetByExternalIdParam): Result<User, AppError>

/** Bulk lookup: internal user IDs -> external IDs. */
fun getExternalIds(param: GetExternalIdsParam): Result<Map<UUID, UUID>, AppError>
```

**New params:**
```kotlin
data class GetByExternalIdParam(val externalId: UUID)
data class GetExternalIdsParam(val internalIds: Set<UUID>)
```

**Modified model — `User`:**
```kotlin
data class User(
    val id: UUID,
    val externalId: UUID,          // NEW
    val email: String,
    val username: String?,
    val experienceLevel: String? = null,
    val avatarSeed: String? = null,
    val profileCompleted: Boolean = false,
    val dietaryRestrictions: List<String> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant
)
```

**Constructor call-site cascade for `User` model change:**

Adding `externalId` to `User` breaks every call site that constructs a `User`. These files must ALL be updated:

| File | Location |
|------|----------|
| `clients/user-client/src/main/kotlin/.../internal/adapters/UserRowAdapter.kt` | Row mapping |
| `clients/user-client/src/testFixtures/kotlin/.../fake/FakeUserClient.kt` | Fake user creation, seed() |
| `services/camper-service/src/main/kotlin/.../features/user/mapper/UserMapper.kt` | `fromClient()` |
| `services/camper-service/src/test/kotlin/.../features/user/service/UserServiceTest.kt` | Test user construction |
| `services/camper-service/src/test/kotlin/.../fixtures/UserFixture.kt` | Test fixture |
| Any other test file that constructs `User` directly |

**New operations in user-client:**
- `internal/operations/GetUserByExternalId.kt`
- `internal/operations/GetExternalIds.kt`
- `internal/validations/ValidateGetUserByExternalId.kt`
- `internal/validations/ValidateGetExternalIds.kt`

**Modified operations:**
- All existing operations that SELECT from users must include `external_id` in SELECT

---

## Service Layer

### New: Auth Feature (`features/auth/`)

**Package:** `com.acme.services.camperservice.features.auth`

**AuthService:**
```kotlin
class AuthService(
    authClient: AuthClient,
    userClient: UserClient,
    emailClient: EmailClient,
    jwtProvider: JwtProvider
) {
    fun requestOtp(param: RequestOtpParam): Result<Unit, AuthError>
    fun verifyOtp(param: VerifyOtpParam): Result<OtpVerifyResult, AuthError>
    fun refreshToken(param: RefreshTokenParam): Result<TokenPair, AuthError>
    fun logout(param: LogoutParam): Result<Unit, AuthError>
}
```

**Service params:**
```kotlin
// params/AuthServiceParams.kt
data class RequestOtpParam(val email: String)
data class VerifyOtpParam(val email: String, val code: String)
data class RefreshTokenParam(val refreshToken: String)
data class LogoutParam(val refreshToken: String)
```

**Service models:**
```kotlin
// model/OtpVerifyResult.kt
data class OtpVerifyResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User   // service-layer User model (from features/user/model/)
)

// model/TokenPair.kt
data class TokenPair(
    val accessToken: String,
    val refreshToken: String
)
```

**Error type:**
```kotlin
// error/AuthError.kt
sealed class AuthError(override val message: String) : AppError {
    data class InvalidOtp(val email: String) : AuthError("Invalid or expired code")
    data class OtpMaxAttempts(val email: String) : AuthError("Too many failed attempts")
    data class InvalidRefreshToken(val reason: String = "Invalid or expired refresh token") : AuthError(reason)
    data class InvalidRequest(val field: String, val reason: String) : AuthError("Invalid $field: $reason")
}
```

**Actions:**
```kotlin
// actions/RequestOtpAction.kt
// - Validates email not blank
// - Normalizes email
// - In-memory rate limiting: max 3 OTP requests per email per 10 minutes
//   (ConcurrentHashMap<String, List<Instant>> keyed by normalized email;
//    prune expired entries on check; return success silently if rate limited
//    to prevent enumeration — do NOT return an error)
// - Generates random 6-digit code via TokenGenerator.generateOtpCode()
// - Hashes code via TokenGenerator.hashToken() before storing
// - Stores OTP hash via authClient.createOtp() with 5-min expiry
// - Looks up user by email; if exists, sends email via emailClient with plaintext code
// - If user doesn't exist, still returns success (prevent enumeration)
// - Always returns Result.Success(Unit)

// actions/VerifyOtpAction.kt
// - Validates email and code not blank
// - Normalizes email
// - Hashes incoming code via TokenGenerator.hashToken()
// - Finds valid OTP via authClient.findValidOtp(email, codeHash)
//   (query: WHERE email = ? AND code_hash = ? AND expires_at > now() AND used_at IS NULL)
// - If not found -> AuthError.InvalidOtp
// - If found but attemptCount >= 5 -> AuthError.InvalidOtp (brute-force protection)
// - If code_hash doesn't match (found by email only but hash differs):
//   call authClient.incrementOtpAttemptCount(), return AuthError.InvalidOtp
// - Marks OTP as used (set used_at)
// - Gets or creates user via userClient.getOrCreate()
//   NOTE: getOrCreate must handle concurrent creation (the existing implementation
//   chains getByEmail then create with ConflictError catch — verify this is sufficient)
// - Revokes all existing refresh tokens for user
// - Generates new family_id (UUID.randomUUID()) for this login session
// - Generates opaque refresh token (256-bit random), stores SHA-256 hash with family_id
// - Generates JWT access token via jwtProvider.createAccessToken(user.externalId, email)
// - Returns OtpVerifyResult

// actions/RefreshTokenAction.kt
// - Validates refreshToken not blank
// - Hashes incoming token (SHA-256)
// - Finds valid refresh token via authClient.findValidRefreshToken()
// - If not found:
//   Check if the token_hash matches a REVOKED token (reuse detection).
//   If so, revoke the entire token family via authClient.revokeTokenFamily(familyId).
//   Return AuthError.InvalidRefreshToken either way.
// - Revokes the used refresh token (rotation)
// - Looks up user by ID to verify still exists
// - Generates new refresh token + stores hash with SAME family_id (inherited from old token)
// - Generates new JWT access token
// - Returns TokenPair

// actions/LogoutAction.kt
// - Hashes incoming token
// - Revokes refresh token if found (authClient.revokeRefreshToken or find+revoke)
// - Always returns success (don't leak validity)
```

**Validations (1:1 with actions):**
- `validations/ValidateRequestOtp.kt` — email not blank (rate limiting is in the action, not validation)
- `validations/ValidateVerifyOtp.kt` — email not blank, code not blank, code is exactly 6 digits numeric
- `validations/ValidateRefreshToken.kt` — refreshToken not blank
- `validations/ValidateLogout.kt` — refreshToken not blank

**DTOs:**
```kotlin
// dto/AuthDtos.kt
data class OtpRequestRequest(val email: String)
data class OtpRequestResponse(val message: String = "OTP sent")

data class OtpVerifyRequest(val email: String, val code: String)
data class OtpVerifyResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserResponse
)
data class AuthUserResponse(
    val id: UUID,          // external_id
    val email: String,
    val username: String?,
    val avatarSeed: String?,
    val profileCompleted: Boolean,
    val avatar: AvatarResponse?
)

data class TokenRefreshRequest(val refreshToken: String)
data class TokenRefreshResponse(
    val accessToken: String,
    val refreshToken: String
)

data class LogoutRequest(val refreshToken: String)
```

**Controller:**
```kotlin
// controller/AuthController.kt
@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/otp/request")
    fun requestOtp(@RequestBody request: OtpRequestRequest): ResponseEntity<Any>
    // -> 200 OtpRequestResponse

    @PostMapping("/otp/verify")
    fun verifyOtp(@RequestBody request: OtpVerifyRequest): ResponseEntity<Any>
    // -> 200 OtpVerifyResponse or 401

    @PostMapping("/token/refresh")
    fun refreshToken(@RequestBody request: TokenRefreshRequest): ResponseEntity<Any>
    // -> 200 TokenRefreshResponse or 401

    @PostMapping("/logout")
    fun logout(@RequestBody request: LogoutRequest): ResponseEntity<Any>
    // -> 204 No Content
}
```

**Auth directory structure:**
```
features/auth/
├── actions/
│   ├── RequestOtpAction.kt
│   ├── VerifyOtpAction.kt
│   ├── RefreshTokenAction.kt
│   └── LogoutAction.kt
├── validations/
│   ├── ValidateRequestOtp.kt
│   ├── ValidateVerifyOtp.kt
│   ├── ValidateRefreshToken.kt
│   └── ValidateLogout.kt
├── controller/
│   └── AuthController.kt
├── dto/
│   └── AuthDtos.kt
├── error/
│   └── AuthError.kt
├── model/
│   ├── OtpVerifyResult.kt
│   └── TokenPair.kt
├── params/
│   └── AuthServiceParams.kt
└── service/
    └── AuthService.kt
```

### New: JWT Infrastructure (`common/auth/`)

**JwtProvider:**
```kotlin
// common/auth/JwtProvider.kt
class JwtProvider(
    private val secret: String,
    private val previousSecret: String?    // For zero-downtime key rotation
) {
    /**
     * Create a JWT with sub=externalId (the only user identifier in the token).
     * No internal user ID is included — the filter resolves it via DB lookup.
     */
    fun createAccessToken(externalId: UUID, email: String): String

    /**
     * Validate and decode a JWT. Tries current secret first, falls back to
     * previousSecret if set (enables zero-downtime key rotation).
     * Future improvement: migrate to RS256/ES256 asymmetric keys for
     * independent signing/verification and easier rotation.
     */
    fun validateAndDecode(token: String): Result<JwtClaims, JwtError>
}

data class JwtClaims(
    val externalId: UUID,   // from "sub" claim
    val email: String
)

sealed class JwtError(override val message: String) : AppError {
    data object Expired : JwtError("Token expired")
    data object Invalid : JwtError("Invalid token")
}
```

**JwtAuthenticationFilter:**
```kotlin
// common/auth/JwtAuthenticationFilter.kt
class JwtAuthenticationFilter(
    private val jwtProvider: JwtProvider,
    private val userClient: UserClient
) : OncePerRequestFilter() {
    // Skips: /api/auth/otp/*, /api/auth/token/refresh, /api/auth/logout,
    //        /api/webhooks/*, /actuator/*, static resources
    // Extracts Bearer token from Authorization header
    // Validates JWT via jwtProvider — gets JwtClaims(externalId, email)
    // Looks up user by externalId via userClient.getByExternalId() to:
    //   (a) verify user still exists (deleted account -> 401)
    //   (b) resolve the internal user ID for downstream use
    // Sets request attributes: "userId" (internal UUID), "userExternalId" (external UUID)
    // On failure: returns 401 JSON response
}
```

**TokenGenerator:**
```kotlin
// common/auth/TokenGenerator.kt
object TokenGenerator {
    /** Generate a cryptographically random 256-bit opaque token, returned as hex string. */
    fun generateRefreshToken(): String

    /** SHA-256 hash a token string. */
    fun hashToken(token: String): String

    /** Generate a random 6-digit numeric OTP code. */
    fun generateOtpCode(): String
}
```

### Modified: User Feature

**Modified `UserMapper`:**
- `toResponse()` — uses `user.externalId` for the `id` field in `UserResponse`
- `toAuthResponse()` — uses `user.externalId` for the `id` field in `AuthResponse`

**Modified service `User` model:**
```kotlin
data class User(
    val id: UUID,
    val externalId: UUID,    // NEW
    val email: String,
    // ... rest unchanged
)
```

**Removed:**
- `AuthenticateUserAction` — replaced by OTP flow
- `ValidateAuthenticateUser` — replaced by OTP flow
- `AuthenticateUserParam` — replaced by OTP flow
- `authenticate()` method on `UserService`
- `POST /api/auth` endpoint on `UserController`
- `POST /api/users` endpoint on `UserController` (create user)

**Modified `UserController`:**
- Remove `authenticate()` and `create()` methods
- Change `@RequestHeader("X-User-Id")` to `@RequestAttribute("userId")` on remaining methods

### External ID Resolution in Response DTOs

**Strategy:** Each service action that returns DTOs with user ID fields will:
1. Collect all internal user IDs from the result
2. Call `userClient.getExternalIds(GetExternalIdsParam(ids))` to get `Map<UUID, UUID>` (internal -> external)
3. Pass the map to the mapper's `toResponse()` method

**Affected mappers — all `toResponse()` methods gain a `externalIds: Map<UUID, UUID>` parameter:**

| Mapper | Method | Fields mapped |
|--------|--------|---------------|
| `UserMapper` | `toResponse(user)` | `id` -> `user.externalId` (direct, no lookup needed) |
| `UserMapper` | `toAuthResponse(user)` | `id` -> `user.externalId` (direct) |
| `PlanMapper` | `toResponse(plan, externalIds)` | `ownerId` |
| `PlanMapper` | `toResponse(member, externalIds)` | `userId`, `invitedBy` |
| `ItemMapper` | `toResponse(item, externalIds)` | `userId` |
| `AssignmentMapper` | `toResponse(assignment, externalIds)` | `ownerId` |
| `AssignmentMapper` | `toDetailResponse(detail, externalIds)` | `ownerId`, members' `userId` |
| `AssignmentMapper` | `toResponse(member, externalIds)` | `userId` |
| `RecipeMapper` | `toRecipeResponse(recipe, externalIds)` | `createdBy` |
| `RecipeMapper` | `toRecipeDetailResponse(recipe, ..., externalIds)` | `createdBy` |
| `MealPlanMapper` | `toMealPlanResponse(mealPlan, externalIds)` | `createdBy` |
| `MealPlanMapper` | `toMealPlanDetailResponse(detail, externalIds)` | `createdBy` |
| `LogBookMapper` | `toResponse(faq, externalIds)` | `askedById`, `answeredById` |
| `LogBookMapper` | `toResponse(entry, externalIds)` | `userId` |
| `GearPackMapper` | `appliedItemToResponse(item, externalIds)` | `userId` |

**Actions that need external ID resolution (need `UserClient` injected):**

| Service | Action | Why |
|---------|--------|-----|
| PlanService | GetPlansAction | PlanResponse.ownerId |
| PlanService | CreatePlanAction | PlanResponse.ownerId |
| PlanService | UpdatePlanAction | PlanResponse.ownerId |
| PlanService | GetPlanMembersAction | PlanMemberResponse.userId, invitedBy |
| PlanService | AddPlanMemberAction | PlanMemberResponse.userId, invitedBy |
| PlanService | UpdateMemberRoleAction | PlanMemberResponse.userId, invitedBy |
| ItemService | CreateItemAction, GetItemAction, GetItemsByOwnerAction, UpdateItemAction | ItemResponse.userId |
| AssignmentService | All actions returning responses | ownerId, member userId |
| RecipeService | CreateRecipeAction, GetRecipeAction, ListRecipesAction, UpdateRecipeAction, PublishRecipeAction | createdBy |
| MealPlanService | All actions returning MealPlanResponse/Detail | createdBy |
| LogBookService | All actions returning faqs/entries | askedById, answeredById, userId |
| GearPackService | ApplyGearPackAction | AppliedItemResponse.userId |

**Services that need `UserClient` added to constructor (don't already have it):**
- `ItemService` (currently takes only ItemClient)
- `RecipeService` (currently takes RecipeClient + IngredientClient + RecipeScraperClient)
- `MealPlanService` (currently takes MealPlanClient + RecipeClient + IngredientClient)
- `LogBookService` (currently takes LogBookClient + PlanRoleAuthorizer)
- `GearPackService` (currently takes GearPackClient + ItemClient + PlanRoleAuthorizer)

**Services that already have `UserClient`:**
- `PlanService` (has UserClient)
- `AssignmentService` (has UserClient)
- `UserService` (has UserClient)

### External ID Resolution for Incoming Requests

For endpoints where external_id comes in via path params or request body and must be resolved to internal:

**New utility in `common/auth/`:**
```kotlin
// common/auth/ExternalIdResolver.kt
class ExternalIdResolver(private val userClient: UserClient) {
    fun resolve(externalId: UUID): Result<UUID, AppError>  // returns internal ID
}
```

**Controllers that need resolution:**
- `PlanController.updateMemberRole()` — `userId` path param
- `PlanController.removeMember()` — `memberId` path param
- `AssignmentController.removeMember()` — `memberUserId` path param
- `AssignmentController.addMember()` — `userId` in request body
- `AssignmentController.transferOwnership()` — `newOwnerId` in request body
- `ItemController.getByOwner()` — `ownerId` query param when ownerType=user

### Controller X-User-Id -> JWT Migration

**All controllers change from:**
```kotlin
@RequestHeader("X-User-Id") userId: UUID
```
**To:**
```kotlin
@RequestAttribute("userId") userId: UUID
```

**Affected controllers (every method that uses `@RequestHeader("X-User-Id")`):**
- `UserController` (2 methods)
- `PlanController` (8 methods)
- `ItemController` (5 methods)
- `ItineraryController` (5 methods)
- `AssignmentController` (8 methods)
- `GearPackController` (3 methods)
- `GearSyncController` (1 method)
- `RecipeController` (10+ methods)
- `IngredientController` (4 methods)
- `MealPlanController` (10+ methods)
- `LogBookController` (8 methods)

**Not affected (no auth):**
- `WorldController`
- `WebhookController`

### Spring Configuration Changes

**New config beans:**

```kotlin
// config/JwtConfig.kt
@Configuration
class JwtConfig {
    @Bean
    fun jwtProvider(
        @Value("\${jwt.secret:default-dev-secret-min-32-bytes-long!!}") secret: String,
        @Value("\${jwt.previous-secret:}") previousSecret: String,
        environment: Environment
    ): JwtProvider {
        // Fail hard if JWT_SECRET is not explicitly set in non-dev profiles
        val activeProfiles = environment.activeProfiles.toSet()
        val isDevProfile = activeProfiles.intersect(setOf("dev", "local", "test")).isNotEmpty()
        if (!isDevProfile && secret == "default-dev-secret-min-32-bytes-long!!") {
            throw IllegalStateException("JWT_SECRET must be set in non-dev profiles")
        }
        return JwtProvider(secret, previousSecret.ifBlank { null })
    }
}

// config/AuthClientConfig.kt
@Configuration
class AuthClientConfig {
    @Bean
    fun authClient(): AuthClient = createAuthClient()
}

// config/AuthServiceConfig.kt
@Configuration
class AuthServiceConfig {
    @Bean
    fun authService(
        authClient: AuthClient,
        userClient: UserClient,
        emailClient: EmailClient,
        jwtProvider: JwtProvider
    ): AuthService = AuthService(authClient, userClient, emailClient, jwtProvider)
}

// config/JwtFilterConfig.kt
@Configuration
class JwtFilterConfig {
    @Bean
    fun jwtAuthenticationFilter(jwtProvider: JwtProvider, userClient: UserClient): FilterRegistrationBean<JwtAuthenticationFilter> {
        val registration = FilterRegistrationBean(JwtAuthenticationFilter(jwtProvider, userClient))
        registration.addUrlPatterns("/api/*")
        registration.order = 1
        return registration
    }
}

// config/ExternalIdResolverConfig.kt
@Configuration
class ExternalIdResolverConfig {
    @Bean
    fun externalIdResolver(userClient: UserClient): ExternalIdResolver = ExternalIdResolver(userClient)
}
```

**Modified config beans (add UserClient dependency):**
- `ItemServiceConfig` — `fun itemService(itemClient: ItemClient, userClient: UserClient)`
- `RecipeServiceConfig` — add `userClient: UserClient`
- `MealPlanServiceConfig` — add `userClient: UserClient`
- `LogBookServiceConfig` — add `userClient: UserClient`
- `GearPackServiceConfig` — add `userClient: UserClient`

**application.yml addition:**
```yaml
jwt:
  secret: ${JWT_SECRET:default-dev-secret-min-32-bytes-long!!}
  previous-secret: ${JWT_PREVIOUS_SECRET:}
```

### ResultExtensions.kt Changes

Add `AuthError` mapping:
```kotlin
fun AuthError.toResponseEntity(): ResponseEntity<Any> = when (this) {
    is AuthError.InvalidOtp -> ResponseEntity.status(401)
        .body(ApiResponse.ErrorBody("INVALID_OTP", message))
    is AuthError.OtpMaxAttempts -> ResponseEntity.status(429)
        .body(ApiResponse.ErrorBody("TOO_MANY_ATTEMPTS", message))
    is AuthError.InvalidRefreshToken -> ResponseEntity.status(401)
        .body(ApiResponse.ErrorBody("INVALID_REFRESH_TOKEN", message))
    is AuthError.InvalidRequest -> ResponseEntity.status(400)
        .body(ApiResponse.ErrorBody("BAD_REQUEST", message))
}
```

---

## Webapp Changes

### AuthContext Rewrite (`src/context/AuthContext.tsx`)

**New interface:**
```typescript
interface AuthState {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (accessToken: string, refreshToken: string, user: User) => void;
    logout: () => void;
    getAccessToken: () => Promise<string | null>;
}
```

**Behavior:**
- Access token stored in memory (module-level variable, not state — avoids re-renders)
- Refresh token stored in localStorage (treat as opaque — never parse or inspect it on the frontend)
- On mount: if refresh token in localStorage, call `/api/auth/token/refresh` to get new access token
- `isLoading` is true during initial token refresh attempt
- `getAccessToken()` checks if current token is expired (decode JWT exp claim client-side), refreshes if needed
- `logout()`: calls `/api/auth/logout` with refresh token, then clears ALL auth state:
  - Set in-memory access token to null
  - `localStorage.removeItem('refreshToken')`
  - `localStorage.removeItem('user')` (remove any legacy keys too: `userId`)
  - Set user state to null
  - Navigate to `/login`

### API Client Rewrite (`src/api/client.ts`)

**Changes to `request()` function:**
- Remove `X-User-Id` header injection
- Add `Authorization: Bearer <accessToken>` header (get token from AuthContext's `getAccessToken()`)
- On 401 response: attempt token refresh, retry original request once
- If refresh also fails: trigger logout

**New API methods:**
```typescript
requestOtp(email: string): Promise<{ message: string }>
verifyOtp(email: string, code: string): Promise<{ accessToken: string; refreshToken: string; user: User }>
refreshToken(refreshToken: string): Promise<{ accessToken: string; refreshToken: string }>
logout(refreshToken: string): Promise<void>
```

**Removed API methods:**
- `login(email)` (old POST /api/auth)
- `register(email, username?)` (old POST /api/users)

### LoginPage Rewrite (`src/pages/LoginPage.tsx`)

**New flow:**
1. **Email step:** User enters email, clicks "Continue"
2. **OTP step:** 6-digit code input appears (same page, not a new route)
3. On OTP verify success: `login(accessToken, refreshToken, user)` via AuthContext
4. If user has no username (`!user.profileCompleted`): redirect to profile setup
5. Otherwise: redirect to "/"

**Components:**
- Reuse existing `LoginPage` component, add OTP step state
- New `OtpInput` sub-component for the 6-digit code entry
- Remove Sign In / Register tab toggle

### Other Frontend Changes

- Remove `localStorage.getItem('userId')` / `localStorage.setItem('userId', ...)` usage
- User ID comparisons (`plan.ownerId === user.id`) continue to work since both now use external_id
- TypeScript `User` interface unchanged in shape (the `id` field just contains external_id now)

---

## PR Stack

### PR 1: [plan] feat(auth-otp-jwt): plan
**Files:**
- `camper/docs/auth-otp-jwt/plan.md` (this file)

---

### PR 2: [db] feat(auth-otp-jwt): database migrations
**Files created:**
- `databases/camper-db/migrations/V038__create_auth_otp_codes.sql`
- `databases/camper-db/migrations/V039__create_auth_refresh_tokens.sql`
- `databases/camper-db/migrations/V040__add_external_id_to_users.sql`
- `databases/camper-db/migrations/rollback/R038__drop_auth_otp_codes.sql`
- `databases/camper-db/migrations/rollback/R039__drop_auth_refresh_tokens.sql`
- `databases/camper-db/migrations/rollback/R040__drop_external_id_from_users.sql`
- `databases/camper-db/schema/tables/038_auth_otp_codes.sql`
- `databases/camper-db/schema/tables/039_auth_refresh_tokens.sql`

**Files modified:**
- `databases/camper-db/schema/tables/002_users.sql` (add external_id)
- `databases/camper-db/seed/dev_seed.sql` (add external_id values for test users)
- `databases/camper-db/CLAUDE.md` (add new table schemas)

---

### PR 3: [client] feat(auth-otp-jwt): client contracts
New auth-client interface + user-client interface additions. No implementations.

**Files created:**
- `clients/auth-client/build.gradle.kts`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/api/AuthClient.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/api/AuthClientParams.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/model/AuthOtpCode.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/model/AuthRefreshToken.kt`

**Files modified:**
- `camper/settings.gradle.kts` (add `:clients:auth-client`)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/api/UserClient.kt` (add `getByExternalId`, `getExternalIds`)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/api/UserClientParams.kt` (add `GetByExternalIdParam`, `GetExternalIdsParam`)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/model/User.kt` (add `externalId` field)

---

### PR 4: [service] feat(auth-otp-jwt): service contracts
Auth feature types, error, DTOs, updated response DTOs for external_id, JWT types.

**Files created:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/error/AuthError.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/dto/AuthDtos.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/params/AuthServiceParams.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/model/OtpVerifyResult.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/model/TokenPair.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/JwtClaims.kt` (JwtClaims(externalId, email) + JwtError)

**Files modified (response DTO external_id change — `UUID` user ID fields remain `UUID` type, values change at mapping time, but DTOs are unchanged in shape):**
- No DTO structural changes needed. The DTOs already use `UUID` type for user ID fields. The mapping from internal -> external happens in the mapper layer, not the DTO definition.

---

### PR 5: [client-impl] feat(auth-otp-jwt): client implementations
Auth-client JDBI implementation + user-client changes for external_id.

**Files created:**
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/AuthClientFactory.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/JdbiAuthClient.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/adapters/AuthOtpCodeRowAdapter.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/adapters/AuthRefreshTokenRowAdapter.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/CreateOtp.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/FindValidOtp.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/IncrementOtpAttemptCount.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/MarkOtpUsed.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/CreateRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/FindValidRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/RevokeRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/RevokeAllUserRefreshTokens.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/operations/RevokeTokenFamily.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateCreateOtp.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateFindValidOtp.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateIncrementOtpAttemptCount.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateMarkOtpUsed.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateCreateRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateFindValidRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateRevokeRefreshToken.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateRevokeAllUserRefreshTokens.kt`
- `clients/auth-client/src/main/kotlin/com/acme/clients/authclient/internal/validations/ValidateRevokeTokenFamily.kt`
- `clients/auth-client/src/testFixtures/kotlin/com/acme/clients/authclient/fake/FakeAuthClient.kt`
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/GetUserByExternalId.kt`
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/GetExternalIds.kt`
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/validations/ValidateGetUserByExternalId.kt`
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/validations/ValidateGetExternalIds.kt`

**Files modified:**
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/JdbiUserClient.kt` (add new operations, wire into facade)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/adapters/UserRowAdapter.kt` (add `external_id` column mapping)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/GetUserById.kt` (add `external_id` to SELECT)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/GetUserByEmail.kt` (add `external_id` to SELECT)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/CreateUser.kt` (add `external_id` to INSERT/SELECT)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/GetOrCreateUser.kt` (passes through, no change needed)
- `clients/user-client/src/main/kotlin/com/acme/clients/userclient/internal/operations/UpdateUser.kt` (add `external_id` to SELECT)
- `clients/user-client/src/testFixtures/kotlin/com/acme/clients/userclient/fake/FakeUserClient.kt` (add `externalId` to fake User creation, add new methods)

---

### PR 6: [service-impl] feat(auth-otp-jwt): service implementation
Auth feature, JWT filter, controller migration, external ID mapping — the core implementation PR.

**Files created:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/actions/RequestOtpAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/actions/VerifyOtpAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/actions/RefreshTokenAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/actions/LogoutAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/validations/ValidateRequestOtp.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/validations/ValidateVerifyOtp.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/validations/ValidateRefreshToken.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/validations/ValidateLogout.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/controller/AuthController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/auth/service/AuthService.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/JwtProvider.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/JwtAuthenticationFilter.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/TokenGenerator.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/ExternalIdResolver.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/JwtConfig.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/AuthClientConfig.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/AuthServiceConfig.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/JwtFilterConfig.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/ExternalIdResolverConfig.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/auth/WebSocketAuthInterceptor.kt`

**Files modified — WebSocket auth:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/WebSocketConfig.kt` (register `WebSocketAuthInterceptor` on STOMP CONNECT; reject unauthenticated connections; JWT passed as query param `?token=` on `/ws` endpoint or in CONNECT frame `Authorization` header)

**Files modified — auth endpoint removal:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/controller/UserController.kt` (remove `authenticate()`, `create()` endpoints; change `@RequestHeader` -> `@RequestAttribute`)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/service/UserService.kt` (remove `authenticate()` method)

**Files deleted:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/actions/AuthenticateUserAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/validations/ValidateAuthenticateUser.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/actions/CreateUserAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/validations/ValidateCreateUser.kt`

**Files modified — controller @RequestHeader -> @RequestAttribute migration:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/controller/PlanController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/controller/ItemController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/itinerary/controller/ItineraryController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/controller/AssignmentController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/controller/GearPackController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearsync/controller/GearSyncController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/controller/RecipeController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/controller/IngredientController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/controller/MealPlanController.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/logbook/controller/LogBookController.kt`

**Files modified — external ID path param resolution:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/controller/PlanController.kt` (removeMember, updateMemberRole: resolve external->internal)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/controller/AssignmentController.kt` (removeMember: resolve external->internal)

**Files modified — external ID in response mapping (mappers + actions):**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/mapper/UserMapper.kt` (use `externalId` for response `id`)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/user/model/User.kt` (add `externalId`)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/mapper/PlanMapper.kt` (add externalIds param to `toResponse`)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/CreatePlanAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/GetPlansAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/UpdatePlanAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/GetPlanMembersAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/AddPlanMemberAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/UpdateMemberRoleAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/plan/actions/RemovePlanMemberAction.kt` (resolve external memberId)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/mapper/ItemMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/actions/CreateItemAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/actions/GetItemAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/actions/GetItemsByOwnerAction.kt` (resolve external ownerId)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/actions/UpdateItemAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/mapper/AssignmentMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/CreateAssignmentAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/GetAssignmentsAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/GetAssignmentAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/UpdateAssignmentAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/AddAssignmentMemberAction.kt` (resolve external userId)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/RemoveAssignmentMemberAction.kt` (resolve external memberUserId)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/assignment/actions/TransferOwnershipAction.kt` (resolve external newOwnerId)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/mapper/RecipeMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/actions/CreateRecipeAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/actions/GetRecipeAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/actions/ListRecipesAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/actions/UpdateRecipeAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/actions/PublishRecipeAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/mapper/MealPlanMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/CreateMealPlanAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/GetMealPlanDetailAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/GetMealPlanByPlanIdAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/GetTemplatesAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/UpdateMealPlanAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/CopyToTripAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/SaveAsTemplateAction.kt`
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/logbook/mapper/LogBookMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/logbook/actions/` (all actions that return faqs/entries)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/mapper/GearPackMapper.kt` (add externalIds param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/actions/ApplyGearPackAction.kt`

**Files modified — ResultExtensions + service configs:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/common/error/ResultExtensions.kt` (add AuthError mapping)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/ItemServiceConfig.kt` (add userClient)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/RecipeServiceConfig.kt` (add userClient)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/MealPlanServiceConfig.kt` (add userClient)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/LogBookServiceConfig.kt` (add userClient)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/config/GearPackServiceConfig.kt` (add userClient)

**Files modified — service constructors:**
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/item/service/ItemService.kt` (add userClient param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/recipe/service/RecipeService.kt` (add userClient param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/service/MealPlanService.kt` (add userClient param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/logbook/service/LogBookService.kt` (add userClient param)
- `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/gearpack/service/GearPackService.kt` (add userClient param)

**Files modified — build + config:**
- `services/camper-service/build.gradle.kts` (add auth-client dependency, add java-jwt library)
- `services/camper-service/src/main/resources/application.yml` (add jwt.secret)

**Dependency addition:**
- `com.auth0:java-jwt:4.4.0` in `services/camper-service/build.gradle.kts`

---

### PR 7: [webapp] feat(auth-otp-jwt): webapp auth flow
Frontend OTP login, AuthContext, API client Bearer auth.

**Files modified:**
- `webapp/src/context/AuthContext.tsx` (full rewrite: token management, refresh on mount)
- `webapp/src/api/client.ts` (Bearer auth, 401 retry, new OTP endpoints, remove old auth endpoints)
- `webapp/src/pages/LoginPage.tsx` (OTP flow: email step -> code step)

**Files potentially modified (if they reference old auth methods):**
- `webapp/src/App.tsx` (if routing changes needed)
- `webapp/src/components/ProtectedRoute.tsx` (add `isLoading` handling)

---

### PR 8: [service-test] feat(auth-otp-jwt): service unit tests
Unit tests for the new auth feature + updated tests for external ID changes.

**Files created:**
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/auth/service/AuthServiceTest.kt`

**Files modified (update existing tests for external_id + @RequestAttribute):**
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/user/service/UserServiceTest.kt` (remove authenticate tests, update User construction with externalId)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/plan/service/PlanServiceTest.kt` (update for externalIds in mapper calls)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/item/service/ItemServiceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/assignment/service/AssignmentServiceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/recipe/service/RecipeServiceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/mealplan/service/MealPlanServiceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/logbook/service/LogBookServiceTest.kt` (if exists)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/gearpack/service/GearPackServiceTest.kt`

---

### PR 9: [acceptance] feat(auth-otp-jwt): acceptance tests
Full API acceptance tests for auth endpoints + updated existing tests for JWT auth.

**Files created:**
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/auth/AuthAcceptanceTest.kt`

**Files modified (all acceptance tests must switch from X-User-Id header to JWT Bearer auth):**
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/fixtures/UserFixture.kt` (add externalId, add JWT helper)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/fixtures/AuthFixture.kt` (new: helper to generate test JWTs)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/user/UserAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/plan/PlanAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/item/ItemAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/itinerary/ItineraryAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/assignment/AssignmentAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/gearpack/GearPackAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/recipe/RecipeAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/recipe/IngredientAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/mealplan/MealPlanAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/logbook/LogBookAcceptanceTest.kt` (if exists)
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/features/webhook/InviteEmailAcceptanceTest.kt`
- `services/camper-service/src/test/kotlin/com/acme/services/camperservice/websocket/WebSocketIntegrationTest.kt`

---

### PR 10: [docs] feat(auth-otp-jwt): documentation updates
Update CLAUDE.md files and project documentation.

**Files modified:**
- `camper/CLAUDE.md`
- `camper/services/camper-service/CLAUDE.md`
- `camper/databases/camper-db/CLAUDE.md`
- `camper/docs/auth-otp-jwt/retro.md` (new)

---

## Design Decisions

### 1. No Spring Security dependency
The codebase has no Spring Security. Adding it would pull in a large dependency tree and require SecurityFilterChain configuration. Instead, we use a plain `OncePerRequestFilter` registered via `FilterRegistrationBean`. This is lighter and more aligned with the existing codebase's style.

### 2. JWT library: com.auth0:java-jwt
Lightweight single-jar dependency. Alternatives like jjwt require 3 artifacts (api, impl, jackson). auth0/java-jwt has a clean API and is well-maintained.

### 3. External ID resolution at the action layer
Each action that returns DTOs with user IDs calls `userClient.getExternalIds()` to batch-resolve internal -> external mappings. This keeps mappers pure (they receive a pre-computed map) and avoids N+1 queries. The alternative (joining external_id in every client query) would require modifying every client that touches user-related tables.

### 4. Table-level access control: application-enforced
The handoff suggests considering separate Postgres roles for auth tables. We use application-level enforcement via the client abstraction boundary (only `AuthClient` queries auth tables). This is simpler, consistent with the existing pattern, and sufficient for a single-service architecture.

### 5. Access token in memory, refresh token in localStorage
Per the handoff. Access tokens in memory are more secure against XSS (not accessible via `document.cookie` or `localStorage`). Refresh tokens in localStorage survive page refreshes. This is a standard pattern.

### 6. @RequestAttribute for userId injection
The JWT filter sets `request.setAttribute("userId", ...)`. Controllers use `@RequestAttribute("userId")` instead of `@RequestHeader("X-User-Id")`. This is a clean, mechanical migration.

### 7. JWT sub claim is externalId, not internal ID
The JWT `sub` claim contains the user's `externalId` — the only user identifier in the token. No internal database ID is included. The filter already performs a per-request DB lookup to verify user existence; during that lookup it resolves the internal ID from the externalId. This means the JWT never leaks internal IDs, even if a token is intercepted.

### 8. Per-request DB lookup in JWT filter is intentional
The JWT filter performs a `userClient.getByExternalId()` call on every authenticated request to verify the user still exists and to resolve the internal ID. This is intentional: it ensures deleted accounts are immediately locked out (not just after token expiry) and avoids stale internal ID references. This is acceptable at current scale (single-digit concurrent users for a camping trip planner). If this becomes a bottleneck, add a short-lived in-memory cache (e.g., Caffeine with 30s TTL) keyed by externalId — but not for v1.

### 9. Refresh token in localStorage: accepted XSS risk
Storing the refresh token in localStorage means that an XSS vulnerability would give an attacker full account takeover (they can exfiltrate the refresh token and generate new access tokens). This is accepted for the current threat model: the app has no user-generated HTML, no third-party script injection points, and the user base is small. If hardening is needed later, migrate refresh tokens to HttpOnly cookies with SameSite=Strict + CSRF protection.

### 10. Revoke all tokens on new login is intentional
When a user logs in via OTP verify, all existing refresh tokens for that user are revoked. This means logging in on your phone logs out your laptop. This is a deliberate UX trade-off for simplicity: single active session per user avoids the complexity of multi-session management. Acceptable for a camping trip planner where users don't need simultaneous sessions.

### 11. Refresh token family_id for reuse detection
Each login creates a new `family_id`. Token rotation preserves the same `family_id`. If a revoked token from a family is presented (indicating the token was stolen and used after the legitimate user already rotated), the entire family is revoked. This limits the damage window of a stolen refresh token.

---

## Open Questions

1. **OTP email template**: The handoff specifies subject "Your Camper login code" and HTML body with the code. Should we use a simple text-only template or a styled HTML template similar to `InviteEmailTemplate`? **Recommendation:** Simple styled HTML matching the invite email aesthetic.

2. **POST /api/users removal**: The `POST /api/users` endpoint is used for registration. After OTP verify auto-creates users, this endpoint is no longer needed. However, should we keep `POST /api/users` as a profile-setup endpoint or rely solely on `PUT /api/users/{userId}`? **Recommendation:** Remove `POST /api/users` entirely; profile setup uses `PUT /api/users/{userId}` (already exists and handles username, experience level, etc.).
