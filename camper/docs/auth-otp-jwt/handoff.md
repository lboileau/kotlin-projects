# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects-worktrees/auth-dry-run/camper

## Feature Name
auth-otp-jwt

## Plan
to be created by architect

## Feature Description
Replace the existing trust-based authentication (X-User-Id header) with a proper email OTP + JWT token system. Currently, the frontend sends an X-User-Id header that the backend blindly trusts. The new system will:

1. Authenticate users via a 6-digit one-time password (OTP) sent to their email
2. Issue short-lived JWT access tokens (15 min) and long-lived refresh tokens (7 days)
3. Validate JWTs on every API request via a Spring filter, including user existence check on every request
4. Support token refresh without re-authentication
5. Revoke all existing refresh tokens when a new session is created
6. Update the React frontend to handle the full token lifecycle
7. Introduce external user IDs — stop exposing internal database UUIDs in API responses

There are NO passwords — authentication is always via email OTP.

## Entities

### AuthOtpCode
- `id` UUID PK
- `email` VARCHAR(255) NOT NULL (normalized, lowercase, dots stripped from local part — match existing V010 normalization)
- `code` VARCHAR(6) NOT NULL (6-digit numeric)
- `expires_at` TIMESTAMPTZ NOT NULL (created_at + 5 minutes)
- `used_at` TIMESTAMPTZ NULL (set when OTP is successfully verified)
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- Index on (email, code, expires_at) for lookup

### AuthRefreshToken
- `id` UUID PK
- `user_id` UUID NOT NULL FK → users(id) ON DELETE CASCADE
- `token_hash` VARCHAR(255) NOT NULL (SHA-256 hash of the opaque refresh token)
- `expires_at` TIMESTAMPTZ NOT NULL (created_at + 7 days)
- `revoked_at` TIMESTAMPTZ NULL (set on logout or new login)
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- Index on (user_id) for revocation queries
- Index on (token_hash) for lookup

### Users table changes (existing table)
- Add `external_id` UUID NOT NULL DEFAULT gen_random_uuid() — a separate public-facing UUID distinct from the internal PK `id`
- Add UNIQUE index on `external_id`
- Backfill existing rows with random UUIDs
- This is the ONLY user identifier ever exposed in API responses. Internal `id` stays in the DB and backend only.

### JWT Access Token (not stored in DB)
- Claims: `sub` = user_id (internal UUID for backend use), `email`, `iat`, `exp` (15 min)
- Signed with HMAC-SHA256 using a server-side secret (`JWT_SECRET` env var)
- Validated by Spring filter on every request + user existence verified against DB

## API Surface

### POST /api/auth/otp/request
Request: `{ "email": "user@example.com" }`
Response: `{ "message": "OTP sent" }` (always 200, even if email not found — prevents enumeration)
Behavior:
- Normalize email (lowercase, strip dots from local part before @)
- Generate random 6-digit code
- Store in auth_otp_codes with 5-min expiry
- Send email via existing EmailClient with subject "Your Camper login code" and HTML body containing the code
- If user doesn't exist, still send a 200 but don't send an email (prevent enumeration)

### POST /api/auth/otp/verify
Request: `{ "email": "user@example.com", "code": "123456" }`
Response: `{ "accessToken": "eyJ...", "refreshToken": "opaque-token", "user": { "id": "<external_id>", "email": "...", "username": "..." } }`
Error: 401 `{ "error": "invalid_otp", "message": "Invalid or expired code" }`
Behavior:
- Look up unexpired, unused OTP matching email + code
- If not found or expired → 401
- Mark OTP as used (set used_at)
- Look up user by normalized email
- If user doesn't exist → create user with email (username null, same as current register flow)
- Revoke all existing refresh tokens for this user (set revoked_at = now())
- Generate new refresh token (random 256-bit value), store SHA-256 hash in auth_refresh_tokens
- Generate JWT access token with user_id and email claims
- Return tokens + user object

### POST /api/auth/token/refresh
Request: `{ "refreshToken": "opaque-token" }`
Response: `{ "accessToken": "eyJ...", "refreshToken": "new-opaque-token" }`
Error: 401 `{ "error": "invalid_refresh_token", "message": "Invalid or expired refresh token" }`
Behavior:
- Hash incoming token, look up in auth_refresh_tokens
- If not found, expired, or revoked → 401
- Revoke the used refresh token (rotate on use)
- Generate new refresh token + store hash
- Generate new JWT access token
- Return both (token rotation for security)

### POST /api/auth/logout
Request: `{ "refreshToken": "opaque-token" }`
Response: 204 No Content
Behavior:
- Hash incoming token, revoke it if found (set revoked_at)
- Always return 204 (don't leak whether token was valid)

### All existing endpoints
- Remove X-User-Id header handling
- Require Authorization: Bearer <jwt> header
- JWT filter extracts user_id from token claims, verifies user still exists in DB, and sets it on SecurityContext / request attribute
- Existing code that reads userId from request continues to work but now gets it from the validated JWT instead of the trusted header
- All response DTOs that currently expose internal user UUIDs must be updated to use external_id instead (see External User ID section below)

## Database Changes

### New migration V038__create_auth_otp_codes.sql
```sql
CREATE TABLE auth_otp_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_otp_codes_lookup ON auth_otp_codes (email, code, expires_at);
```

### New migration V039__create_auth_refresh_tokens.sql
```sql
CREATE TABLE auth_refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_auth_refresh_tokens_user_id ON auth_refresh_tokens (user_id);
CREATE INDEX idx_auth_refresh_tokens_token_hash ON auth_refresh_tokens (token_hash);
```

### New migration V040__add_external_id_to_users.sql
```sql
-- Add external_id column with default for new rows
ALTER TABLE users ADD COLUMN external_id UUID NOT NULL DEFAULT gen_random_uuid();

-- Backfill existing rows (each gets a unique random UUID)
-- The DEFAULT already handles this for NOT NULL columns in Postgres 11+,
-- but if needed: UPDATE users SET external_id = gen_random_uuid() WHERE external_id IS NULL;

CREATE UNIQUE INDEX idx_users_external_id ON users (external_id);
```

### Table-level permissions
Create a restricted DB role for the auth tables. The application's main DB role should NOT have direct access to auth tables — only the auth-client code path should query them. Implementation detail: the architect should decide whether to use a separate Postgres role with GRANT/REVOKE, or application-level enforcement via the client abstraction boundary.

## Special Considerations

### Existing EmailClient integration
- The email client is at `clients/email-client/` with interface `EmailClient.send(SendEmailParam)`
- `SendEmailParam(to: String, subject: String, html: String)`
- Has Resend production impl, NoOp for local dev, Fake for tests
- Use this directly for sending OTP emails — no new email infrastructure needed

### Existing user model
- Users table (V002) has: id, email, username (nullable), created_at, updated_at
- Email normalization (V010): lowercase + strip dots from local part
- Users without a username are considered "invited but not registered" — the OTP verify endpoint should handle this by creating the user if needed (same as current register flow)
- The existing registration flow (set username) should still work after auth — it's a profile update, not an auth concern

### JWT secret management
- `JWT_SECRET` environment variable (minimum 256-bit / 32-byte hex string)
- For local dev, use a hardcoded default in application config
- For production, must be set via environment

### Frontend changes (React)

#### Login flow update
1. LoginPage currently has Sign In (email) and Register (email + username) tabs
2. New flow: User enters email → clicks "Continue" → OTP screen appears → enters 6-digit code → authenticated
3. If user has no username after OTP verify, redirect to a profile setup / username step (reuse existing register logic)
4. The OTP screen should be a new component shown after email submission, not a separate page/route

#### AuthContext update
- Store access token in memory (not localStorage — more secure against XSS)
- Store refresh token in localStorage (needed to persist across page refreshes)
- On mount: if refresh token exists in localStorage, call /api/auth/token/refresh to get new access token
- Expose: `{ user, isAuthenticated, isLoading, login, logout, getAccessToken }`
- `getAccessToken()` returns current token or refreshes if expired (check exp claim client-side)

#### API client update
- Replace `X-User-Id` header with `Authorization: Bearer <accessToken>`
- On 401 response: attempt token refresh, retry original request once
- If refresh also fails: logout and redirect to /login

#### External user ID transition
- The frontend currently stores `user.id` (internal UUID) and uses it for comparisons like `plan.ownerId === user.id`
- After the change, all IDs returned from the API will be external_id — the frontend doesn't need to know about internal vs external, it just uses whatever the API returns
- TypeScript interfaces in `client.ts` don't change shape — the values just become external IDs
- This should be a transparent change for the frontend

### JWT filter — user existence check
The Spring `OncePerRequestFilter` that validates JWTs must also:
1. Extract `sub` (internal user_id) from the JWT claims
2. Look up the user in the DB to confirm they still exist (cache this for the duration of the request)
3. If the user doesn't exist (deleted account), return 401 even though the JWT is technically valid
4. Set the internal user_id AND the user's external_id on the request context so downstream code has access to both
5. Controllers continue using the internal user_id for DB operations; response mapping uses external_id

### External User ID — full audit of affected response DTOs
Internal user UUIDs (the `id` PK from the `users` table) are currently leaked in many API responses. All of these must be converted to use `external_id` instead. The backend continues to use internal IDs for all DB operations — the mapping happens at the response DTO layer only.

**Affected response DTOs and their fields (all must map internal → external):**

| DTO | Fields to convert |
|-----|-------------------|
| `UserResponse` | `id` |
| `AuthResponse` (new OTP verify response) | `user.id` |
| `PlanResponse` | `ownerId` |
| `PlanMemberResponse` | `userId`, `invitedBy` |
| `ItemResponse` | `userId` |
| `AppliedItemResponse` (gear pack) | `userId` |
| `AssignmentResponse` | `ownerId` |
| `AssignmentDetailResponse` | `ownerId` |
| `AssignmentMemberResponse` | `userId` |
| `LogBookFaqResponse` | `askedById`, `answeredById` |
| `LogBookJournalEntryResponse` | `userId` |
| `MealPlanResponse` | `createdBy` |
| `MealPlanDetailResponse` | `createdBy` |
| `RecipeResponse` | `createdBy` |
| `RecipeDetailResponse` | `createdBy` |

**Implementation approach:**
- The user-client should expose a bulk lookup method: `getExternalIds(internalIds: Set<UUID>): Map<UUID, UUID>` (internal → external mapping)
- Actions/Service layer resolves external IDs before building response DTOs
- Alternatively, the architect may decide to join external_id in existing client queries — either approach is acceptable
- The frontend currently stores and references user IDs from API responses (e.g., comparing `plan.ownerId === user.id` to check ownership). These will seamlessly work with external IDs since they're just comparing opaque UUIDs.

**Request path — incoming user ID references:**
- `DELETE /api/plans/{planId}/members/{memberId}` — the `memberId` path param is currently an internal user UUID. Must accept external_id instead and resolve to internal.
- `DELETE /api/plans/{planId}/assignments/{assignmentId}/members/{memberUserId}` — same: accept external_id, resolve to internal.
- `PATCH /api/plans/{planId}/members/{userId}/role` — same.
- `POST /api/plans/{planId}/members` — the request body includes user identification (by email for invites), so this likely doesn't need changes.
- `GET /api/items?ownerId={id}` — if ownerId refers to a user, must accept external_id.
- Any other endpoint where a user UUID appears in path params or query params must be audited.

### Migration path for existing sessions
- Existing users in localStorage with X-User-Id will get a 401 on first request after deploy
- The 401 handler will fail to refresh (no refresh token), triggering logout → redirect to login
- Users simply log in again with email OTP — seamless transition
- No data migration needed for existing users (they already have email in the users table)

## Notes
- No rate limiting on OTP requests for v1 — can be added later
- No password support — auth is always email OTP
- The existing `/api/auth` POST endpoint (email-only login) and `/api/users` POST (register) should be deprecated/removed once the new auth endpoints are live
- The PlanRoleAuthorizer and plan-level authorization remain unchanged — they already work with internal user_id, which will now come from JWT instead of header
- External user IDs are a security best practice: internal auto-increment or sequential UUIDs can leak information about user count/creation order. External IDs are random and opaque.
- The dev seed data should be updated to include external_id values for seeded users (the DEFAULT gen_random_uuid() handles this automatically, but explicit values may be useful for test stability)
