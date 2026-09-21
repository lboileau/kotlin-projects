# recipe-favorites — implementation plan

> **AMENDMENT (owner, 2026-09-21, mid-build) — read before the frontend sections.** `handoff.md` "Decisions already made by the owner" 1–3 were replaced after the owner saw the first design, and they SUPERSEDE anything in this plan that contradicts them: (1) the Show filter is a **dropdown** inline with Import / New, not a second chip row; (2) **no heart button on list rows** — a read-only `♥ N` only; (3) on the recipe page the heart sits **right of the title** (new `PageHero` `action` prop) and the link under the title reads **count + truncated who** ("♥ 3 · You, Alice and 1 other ›", `formatFavouritedBy`, `useRecipeFavorites`, optimistic "You"). Backend, DB, client and API sections of this plan are unaffected. Also: the stack's base is now `main` (PR #315 merged as `2f14042`), not `meal-app-frontend`.


Source of truth for requirements: `docs/recipe-favorites/handoff.md`. The six decisions in its
"Decisions already made by the owner" section are settled and are not revisited here.

Branch base: `meal-app-frontend` @ `14347d3`. Every PR stacks on that branch, not `main`.

**Spelling rule, applied throughout this plan:** `favourite` in anything a person reads (UI copy,
route path segments the user sees, doc prose); `favorite` in code identifiers, table/column names,
API paths and JSON field names.

---

## 1. Feature summary

Any signed-in user can favourite any recipe they can see (published recipes, plus their own
drafts). A new `recipe_favorites` table stores one row per (recipe, user). `RecipeResponse` and
`RecipeDetailResponse` gain two computed fields — `favoriteCount` and `favoritedByMe` (relative to
the `X-User-Id` caller) — filled from a single batched client read per request so the list endpoint
stays O(1) queries. Three new endpoints favourite, un-favourite (both idempotent) and list who
favourited a recipe. The webapp replaces the recipe list's `Mine` switch with a single-choice
`Show` chip row (`All · Mine · ♥ Favourites · ♥ My favourites`), puts a heart on every list row and
on the recipe page, and opens a `/recipes/:recipeId/favourites` sheet listing the people who
favourited it. The toggle is optimistic with targeted rollback and per-recipe network
serialisation. No STOMP topic is added.

---

## 2. Decisions made in this plan

These were open in the handoff, or arose from reading the code. They are decisions, not questions.

1. **Un-favourite returns `200` with a body**, not `204` — the handoff specifies a response body
   for `DELETE /api/recipes/{id}/favorite`, and a body needs a 200.
2. **Hidden recipe → `404`, never `403`.** Someone else's draft is indistinguishable from a
   non-existent recipe for the favourite endpoints. `RecipeError.NotFound` already maps to 404
   (`common/error/ResultExtensions.kt:256`). **No new `RecipeError` variants are needed.**
3. **`GetRecipeAction` is NOT retrofitted with the visibility rule.** Today `GET /api/recipes/{id}`
   returns any recipe by id regardless of status or creator (it takes `userId` and ignores it —
   `RecipeDetailPage.tsx` even renders a "still a draft, shown here read-only" callout for exactly
   that case). Tightening it is a behaviour change to a shipped endpoint and is out of scope. The
   visibility rule is enforced **only in the three new favourite actions**, via a shared helper.
   Flagged as a known inconsistency; see §12.
4. **`toRecipeResponse` / `toRecipeDetailResponse` take the two new fields as REQUIRED parameters,
   with no defaults.** Defaults would let `update`, `publish` and `resolve-duplicate` silently
   return `0 / false` for a recipe that already has favourites, poisoning the frontend cache. All
   seven call sites are enumerated in §8 and each must pass a real value.
5. **Per-recipe favourite summaries are read in one batched call**, never one query per recipe.
   Signature in §6.
6. **`GET /api/recipes/{id}/favorites` enriches usernames one `UserClient.getById` per row.** This
   mirrors the established service-layer enrichment in `GetMembersAction.kt:68` and
   `GetPlanMembersAction`. It is bounded by one recipe's favouriters and is not on the list path.
   A batched user lookup is deliberately **not** introduced — `UserClient` has no batch method and
   adding one is a separate concern.
7. **`RecipeService` gains a `UserClient` constructor parameter** (4th positional, before
   `htmlFetcher`). `RecipeServiceConfig` and `RecipeServiceTest` are updated in lockstep.
8. **The recipe feature gets its first `validations/` package.** The feature currently has none
   (its existing actions validate inline). Three default validators are added, 1:1 with the three
   new actions, per the monorepo service pattern. **Existing actions are not retrofitted.**
9. **Infrastructure gap — `recipe-client` has no test source set at all.** No
   `src/test/`, no `RecipeTestDb` in `testFixtures/`, no `docker-java.properties`. It is the only
   JDBI client in the repo without one. **Decision: add the harness** (three small files) as part
   of the client-test PR. The batched aggregate SQL, `ON CONFLICT DO NOTHING` idempotency and FK
   cascade-on-recipe-delete are DB behaviours a fake cannot prove.
10. **Frontend race handling: serialise-and-coalesce per recipe id**, mirroring
    `queries/shopping.ts` `rowChains`. **Not** disable-while-in-flight. Rationale in §9.4.
11. **The heart is accent-coloured, not red.** `webapp/CLAUDE.md` states "One colour for the whole
    app: the accent" and reserves `color="red"` for "take it away". A red heart was considered and
    rejected on those grounds; filled vs outline carries the state.
12. **At `favoriteCount === 0` the recipe page shows the heart alone** — no count, no link, no
    "Be the first". Quietest option, per the handoff's "keep it quiet".
13. **No rollback SQL file is written.** `databases/camper-db/migrations/rollback/` stops at
    `R043`; V044 gets `R044__drop_recipe_favorites.sql` because the project convention (db-manager
    skill + `databases/camper-db/CLAUDE.md` "Adding a New Table" step 3) requires one.
14. **A `schema/tables/` file is written.** The dual-schema convention requires it; numbering
    continues from `035_ladder_votes.sql` → `036_recipe_favorites.sql`.

---

## 3. PR stack

Each PR is one concern and must leave the build green (`./gradlew build` for backend PRs;
`npm run build && npm run lint && npm run test` in `webapp/` for frontend PRs).

| # | Layer | Title | Agent |
|---|---|---|---|
| 1 | plan | `feat(recipe-favorites): plan` | architect (this document) |
| 2 | db | `feat(recipe-favorites): db contracts` | db-dev |
| 3 | client | `feat(recipe-favorites): client contracts` | kotlin-dev |
| 4 | service | `feat(recipe-favorites): service contracts` | kotlin-dev |
| 5 | client-impl | `feat(recipe-favorites): client implementation` | kotlin-dev |
| 6 | service-impl | `feat(recipe-favorites): service implementation` | kotlin-dev |
| 7 | web | `feat(recipe-favorites): webapp favourites` | web-dev |
| 8 | client-test | `feat(recipe-favorites): recipe-client integration tests` | test-engineer |
| 9 | service-test | `feat(recipe-favorites): service unit tests` | test-engineer |
| 10 | acceptance | `feat(recipe-favorites): acceptance tests` | test-engineer |
| 11 | docs | `feat(recipe-favorites): documentation updates` | doc-updater |

No `lib/` PR: there is no new Kotlin library. No new client module: `recipe-client` is extended.

### PR 2 — db contracts (db-dev)

**Created**
- `databases/camper-db/migrations/V044__create_recipe_favorites.sql`
- `databases/camper-db/migrations/rollback/R044__drop_recipe_favorites.sql`
- `databases/camper-db/schema/tables/036_recipe_favorites.sql`

**Modified**
- `databases/camper-db/seed/dev_seed.sql`
- `databases/camper-db/CLAUDE.md`

Exact contents in §5.

### PR 3 — client contracts (kotlin-dev)

Interface + params + model + `TODO()` stubs in both implementations, so the module compiles.

**Created**
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/model/RecipeFavorite.kt`
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/model/RecipeFavoriteSummary.kt`

**Modified**
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/api/RecipeClient.kt` — 4 new methods with KDoc
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/api/RecipeClientParams.kt` — 4 new param data classes
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/internal/JdbiRecipeClient.kt` — 4 overrides, body `TODO("Implementation in client-impl PR")`
- `clients/recipe-client/src/testFixtures/kotlin/com/acme/clients/recipeclient/fake/FakeRecipeClient.kt` — 4 overrides, same `TODO()`

### PR 4 — service contracts (kotlin-dev)

DTO/param/mapper/wiring changes, plus `TODO()` action stubs. **This PR changes the shape of
`RecipeResponse`/`RecipeDetailResponse` and the signatures of both mappers, so it must update every
call site in the same PR** (see §8).

**Created**
- `.../features/recipe/actions/RecipeVisibility.kt`
- `.../features/recipe/actions/FavoriteRecipeAction.kt` (stub)
- `.../features/recipe/actions/UnfavoriteRecipeAction.kt` (stub)
- `.../features/recipe/actions/ListRecipeFavoritesAction.kt` (stub)
- `.../features/recipe/validations/ValidateFavoriteRecipe.kt`
- `.../features/recipe/validations/ValidateUnfavoriteRecipe.kt`
- `.../features/recipe/validations/ValidateListRecipeFavorites.kt`

(all under `services/camper-service/src/main/kotlin/com/acme/services/camperservice/`)

**Modified**
- `.../features/recipe/dto/RecipeResponses.kt` — 2 fields on each of 2 DTOs, 2 new response DTOs
- `.../features/recipe/mapper/RecipeMapper.kt` — 2 changed signatures, 2 new mapper functions
- `.../features/recipe/params/RecipeServiceParams.kt` — 3 new params
- `.../features/recipe/service/RecipeService.kt` — `userClient` param, 3 new actions + 3 facade methods
- `.../features/recipe/controller/RecipeController.kt` — 3 new routes
- `.../config/RecipeServiceConfig.kt` — inject `UserClient`
- `.../features/recipe/actions/ListRecipesAction.kt` — pass through the new fields
- `.../features/recipe/actions/GetRecipeAction.kt`
- `.../features/recipe/actions/CreateRecipeAction.kt`
- `.../features/recipe/actions/UpdateRecipeAction.kt`
- `.../features/recipe/actions/PublishRecipeAction.kt`
- `.../features/recipe/actions/ResolveDuplicateAction.kt`
- `.../features/recipe/actions/ImportRecipeAction.kt`
- `services/camper-service/src/test/kotlin/.../features/recipe/RecipeServiceTest.kt` — `RecipeService(...)` construction gains `FakeUserClient` (named arguments); keeps the existing 35 tests compiling

### PR 5 — client implementation (kotlin-dev)

**Created**
- `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/internal/adapters/RecipeFavoriteRowAdapter.kt`
- `.../internal/adapters/RecipeFavoriteSummaryRowAdapter.kt`
- `.../internal/operations/AddRecipeFavorite.kt`
- `.../internal/operations/RemoveRecipeFavorite.kt`
- `.../internal/operations/GetRecipeFavorites.kt`
- `.../internal/operations/GetRecipeFavoriteSummaries.kt`
- `.../internal/validations/ValidateAddRecipeFavorite.kt`
- `.../internal/validations/ValidateRemoveRecipeFavorite.kt`
- `.../internal/validations/ValidateGetRecipeFavorites.kt`
- `.../internal/validations/ValidateGetRecipeFavoriteSummaries.kt`

**Modified**
- `.../internal/JdbiRecipeClient.kt` — replace the 4 `TODO()`s with delegation
- `.../testFixtures/.../fake/FakeRecipeClient.kt` — replace the 4 `TODO()`s with **real in-memory behaviour** (§6.5). No stubs survive this PR.
- `clients/recipe-client/CLAUDE.md` — new methods, model, table

### PR 6 — service implementation (kotlin-dev)

**Modified**
- `.../features/recipe/actions/FavoriteRecipeAction.kt`
- `.../features/recipe/actions/UnfavoriteRecipeAction.kt`
- `.../features/recipe/actions/ListRecipeFavoritesAction.kt`
- `.../features/recipe/actions/ListRecipesAction.kt`
- `.../features/recipe/actions/GetRecipeAction.kt`
- `.../features/recipe/actions/UpdateRecipeAction.kt`
- `.../features/recipe/actions/PublishRecipeAction.kt`
- `.../features/recipe/actions/ResolveDuplicateAction.kt`
- `.../features/recipe/actions/ImportRecipeAction.kt`
- `.../features/recipe/actions/CreateRecipeAction.kt`

(PR 4 stubs them to `TODO()` / `0, false` placeholders; PR 6 makes them correct. If the developer
prefers, PR 4 may leave `ListRecipesAction`/`GetRecipeAction` returning `0, false` and PR 6 wires
the real summary reads — the split is at the developer's discretion so long as PR 4 compiles and
PR 6 is correct.)

### PR 7 — webapp favourites (web-dev)

Detailed in §9.

**Created**
- `webapp/src/lib/recipeFavorites.ts`
- `webapp/src/lib/recipeFavorites.test.ts`
- `webapp/src/pages/recipes/FavouritedBySheet.tsx`
- `webapp/src/pages/recipes/FavouritedBySheet.css`

**Modified**
- `webapp/src/api/recipes.ts`
- `webapp/src/queries/recipes.ts`
- `webapp/src/pages/recipes/RecipesPage.tsx`
- `webapp/src/pages/recipes/RecipesPage.css`
- `webapp/src/pages/recipes/RecipeDetailPage.tsx`
- `webapp/src/pages/recipes/RecipeDetailPage.css`
- `webapp/src/pages/recipes/index.ts`
- `webapp/src/router.tsx`
- `webapp/CLAUDE.md` — two "Non-obvious decisions" entries

### PR 8 — recipe-client integration tests (test-engineer)

**Created**
- `clients/recipe-client/src/testFixtures/kotlin/com/acme/clients/recipeclient/test/RecipeTestDb.kt`
- `clients/recipe-client/src/test/resources/docker-java.properties` (`api.version=1.44`)
- `clients/recipe-client/src/test/kotlin/com/acme/clients/recipeclient/JdbiRecipeClientFavoritesTest.kt`

No `build.gradle.kts` change is needed — `clients/recipe-client/build.gradle.kts` already declares
every Testcontainers / JUnit / AssertJ / postgres dependency and the
`systemProperty("project.root", ...)` that `MigrationRunner` needs.

### PR 9 — service unit tests (test-engineer)

**Modified**
- `services/camper-service/src/test/kotlin/.../features/recipe/RecipeServiceTest.kt` — a new `@Nested inner class Favorites`

### PR 10 — acceptance tests (test-engineer)

**Modified**
- `services/camper-service/src/test/kotlin/.../features/recipe/acceptance/fixture/RecipeFixture.kt` — `insertFavorite(...)`, `truncateAll()` lists `recipe_favorites` first
- `services/camper-service/src/test/kotlin/.../features/recipe/acceptance/RecipeAcceptanceTest.kt` — a new `@Nested inner class Favorites`

### PR 11 — documentation (doc-updater)

**Modified**
- `services/camper-service/CLAUDE.md` — Recipe feature: new routes, actions, DTO fields, `UserClient` dependency; Configuration: `RecipeServiceConfig`
- `clients/recipe-client/CLAUDE.md` (if PR 5 left anything)
- `databases/camper-db/CLAUDE.md` (if PR 2 left anything)
- `camper/CLAUDE.md` — recipe-client one-liner gains `recipe_favorites`
- `docs/recipe-favorites/retro.md` — written by the orchestrator from the agents' retros

---

## 4. Entities

### `RecipeFavorite` (new)

| field | type | notes |
|---|---|---|
| `id` | `UUID` | PK |
| `recipeId` | `UUID` | → `recipes.id`, `ON DELETE CASCADE` |
| `userId` | `UUID` | → `users.id`, `ON DELETE CASCADE` |
| `createdAt` | `Instant` | |

Unique on `(recipeId, userId)`. No `updated_at` — rows are only inserted and deleted (same
reasoning as `ladder_votes`).

### `RecipeFavoriteSummary` (new, read model — not a table)

| field | type |
|---|---|
| `recipeId` | `UUID` |
| `favoriteCount` | `Int` |
| `favoritedByMe` | `Boolean` |

### `Recipe` (existing, unchanged)

`clients/recipe-client/.../model/Recipe.kt` is **not modified**. The two new fields are computed at
the service boundary and live only on the response DTOs.

---

## 5. Database

### `databases/camper-db/migrations/V044__create_recipe_favorites.sql`

Verified against the real column types: `recipes.id UUID PRIMARY KEY` (V014), `users.id UUID
PRIMARY KEY` (V002). Style matches `V043__create_ladder_votes.sql` (aligned FK block, `IF NOT
EXISTS` everywhere, index on its own line after the table).

```sql
CREATE TABLE IF NOT EXISTS recipe_favorites (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_recipe_favorites_recipe_user UNIQUE (recipe_id, user_id),
    CONSTRAINT fk_recipe_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_favorites_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_recipe_favorites_user_id ON recipe_favorites (user_id);
```

Differences from the handoff's draft, and why: the `UNIQUE` constraint is listed **before** the FKs
to match `V043`'s ordering; otherwise identical. `uq_recipe_favorites_recipe_user` backs the
`(recipe_id, ...)`-leading lookups used by both reads, so no separate `idx_..._recipe_id` is
created. `ON DELETE CASCADE` on `user_id` is required for the repo-wide `TRUNCATE users CASCADE`
fixture pattern (see §11) and matches `ladder_votes`.

### `databases/camper-db/schema/tables/036_recipe_favorites.sql`

Byte-identical to the migration file above (the creation-migration convention).

### `databases/camper-db/migrations/rollback/R044__drop_recipe_favorites.sql`

```sql
DROP TABLE IF EXISTS recipe_favorites CASCADE;
```

### `databases/camper-db/seed/dev_seed.sql`

Append a new section after the "Recipe ingredients" block. Uses the existing seeded users — Alice
`d3bbef22-…`, Bob `e4ccf033-…`, Charlie `f5dda144-…` (**Charlie's `username` is NULL**, which
exercises the email fallback locally) — and the existing seeded recipes Camp Guacamole
`aa140000-0001-…`, Trail Tacos `aa140000-0002-…`, Campfire Chili `aa140000-0003-…`.

```sql
-- ============================================================
-- Recipe favourites
-- ============================================================
-- Guacamole has three (Charlie has no username, so the API falls back to his
-- email); Trail Tacos has one; Campfire Chili has none, so every state of the
-- Show chip row has something to show locally.

INSERT INTO recipe_favorites (id, recipe_id, user_id, created_at)
VALUES
    ('aa160000-0001-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now()),
    ('aa160000-0002-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now()),
    ('aa160000-0003-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'f5dda144-e150-9d4d-00b2-b00e118d5f66', now()),
    ('aa160000-0004-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now())
ON CONFLICT (recipe_id, user_id) DO NOTHING;
```

The conflict target is the **natural key**, not `id` — matching how this file already guards
`worlds` (`ON CONFLICT (name)`), `users` (`ON CONFLICT (email)`) and `ingredients`
(`ON CONFLICT (name)`). `ON CONFLICT (id)` would not protect the row against
`uq_recipe_favorites_recipe_user`: un-favourite Camp Guacamole as Alice locally and re-favourite it,
and the row comes back with a fresh random `id`. A re-seed would then miss the `id` conflict, hit
the unique constraint, and — since the seed script runs without `ON_ERROR_STOP` — abort that one
multi-row statement and silently drop **all four** demo favourites. Caught in review of PR 2.

### `databases/camper-db/CLAUDE.md`

Add a `### recipe_favorites` schema block (the DDL above), three `## Relationships` bullets
(`recipe_favorites.recipe_id → recipes.id` CASCADE, `.user_id → users.id` CASCADE), and two
`## Invariants` bullets: "A user can favourite a given recipe at most once (enforced by
`uq_recipe_favorites_recipe_user`)." and "`recipe_favorites` has no `updated_at` — rows are only
inserted and deleted."

---

## 6. Client — `recipe-client`

Package root `com.acme.clients.recipeclient`.

### 6.1 Models

`model/RecipeFavorite.kt`
```kotlin
package com.acme.clients.recipeclient.model

import java.time.Instant
import java.util.UUID

data class RecipeFavorite(
    val id: UUID,
    val recipeId: UUID,
    val userId: UUID,
    val createdAt: Instant
)
```

`model/RecipeFavoriteSummary.kt`
```kotlin
package com.acme.clients.recipeclient.model

import java.util.UUID

/**
 * Per-recipe favourite totals for one caller. Produced by a single batched
 * read across many recipe ids — never one query per recipe.
 */
data class RecipeFavoriteSummary(
    val recipeId: UUID,
    val favoriteCount: Int,
    val favoritedByMe: Boolean
)
```

### 6.2 Params — appended to `api/RecipeClientParams.kt`

```kotlin
/** Parameter for favouriting a recipe on behalf of a user. Idempotent. */
data class AddRecipeFavoriteParam(val recipeId: UUID, val userId: UUID)

/** Parameter for removing a user's favourite of a recipe. Idempotent. */
data class RemoveRecipeFavoriteParam(val recipeId: UUID, val userId: UUID)

/** Parameter for listing who favourited a recipe, oldest first. */
data class GetRecipeFavoritesParam(val recipeId: UUID)

/**
 * Parameter for the batched favourite summary read.
 *
 * [recipeIds] may be empty — the operation then returns an empty list without
 * touching the database (an empty `IN ()` is a SQL syntax error).
 */
data class GetRecipeFavoriteSummariesParam(val recipeIds: List<UUID>, val userId: UUID)
```

### 6.3 Interface — appended to `api/RecipeClient.kt`

```kotlin
/**
 * Favourite a recipe on behalf of a user. Idempotent: favouriting an already
 * favourited recipe succeeds and changes nothing. Does not check whether the
 * recipe is visible to the user — that is the caller's responsibility.
 */
fun addFavorite(param: AddRecipeFavoriteParam): Result<Unit, AppError>

/**
 * Remove a user's favourite of a recipe. Idempotent: removing a favourite
 * that was never there succeeds.
 */
fun removeFavorite(param: RemoveRecipeFavoriteParam): Result<Unit, AppError>

/** Who favourited a recipe, oldest first. Empty when nobody has. */
fun getFavorites(param: GetRecipeFavoritesParam): Result<List<RecipeFavorite>, AppError>

/**
 * Favourite count and "did this user favourite it" for many recipes in one
 * read — the list endpoint's answer to N+1.
 *
 * Only recipes with at least one favourite appear in the result; an id the
 * caller asked about that nobody has favourited is simply absent, and callers
 * must default it to `favoriteCount = 0, favoritedByMe = false`.
 */
fun getFavoriteSummaries(param: GetRecipeFavoriteSummariesParam): Result<List<RecipeFavoriteSummary>, AppError>
```

### 6.4 Operations and validations

Each operation instantiates its paired validator and calls it first, as every other operation in
this client does. All four validators are **default validators** — `execute()` logs a failure and
`validate()` returns `success(Unit)`. Nothing here is validatable from the param alone (the
visibility rule needs the fetched recipe and lives in the service).

| Operation file | Validator file |
|---|---|
| `internal/operations/AddRecipeFavorite.kt` | `internal/validations/ValidateAddRecipeFavorite.kt` |
| `internal/operations/RemoveRecipeFavorite.kt` | `internal/validations/ValidateRemoveRecipeFavorite.kt` |
| `internal/operations/GetRecipeFavorites.kt` | `internal/validations/ValidateGetRecipeFavorites.kt` |
| `internal/operations/GetRecipeFavoriteSummaries.kt` | `internal/validations/ValidateGetRecipeFavoriteSummaries.kt` |

**`AddRecipeFavorite`** — idempotency is the DB's job, so the unique constraint never raises:
```sql
INSERT INTO recipe_favorites (recipe_id, user_id)
VALUES (:recipeId, :userId)
ON CONFLICT (recipe_id, user_id) DO NOTHING
```
Returns `success(Unit)` regardless of the affected row count. `id` and `created_at` come from
their column defaults.

**`RemoveRecipeFavorite`**:
```sql
DELETE FROM recipe_favorites WHERE recipe_id = :recipeId AND user_id = :userId
```
Returns `success(Unit)` regardless of the affected row count — deleting nothing is success, not
`NotFoundError`. This is the one place this client deliberately departs from `DeleteRecipe`'s
"0 rows → NotFoundError" shape; the KDoc says so.

**`GetRecipeFavorites`**:
```sql
SELECT id, recipe_id, user_id, created_at
FROM recipe_favorites
WHERE recipe_id = :recipeId
ORDER BY created_at, id
```
`id` is the tiebreaker so a seeded batch inserted in the same transaction (identical `now()`) has a
stable order. Mapped by `RecipeFavoriteRowAdapter`.

**`GetRecipeFavoriteSummaries`** — the batched read. **Guard first:**
```kotlin
if (param.recipeIds.isEmpty()) return success(emptyList())
```
then one query:
```sql
SELECT recipe_id,
       COUNT(*)::int                       AS favorite_count,
       BOOL_OR(user_id = :userId)          AS favorited_by_me
FROM recipe_favorites
WHERE recipe_id IN (<recipeIds>)
GROUP BY recipe_id
```
bound with `.bindList("recipeIds", param.recipeIds.distinct())` and `.bind("userId", param.userId)`.
The `<recipeIds>` placeholder is JDBI's default `DefinedAttributeTemplateEngine` list syntax,
already used in this repo at `ingredient-client/.../FindIngredientsByNames.kt:28`,
`itinerary-client/.../GetLinksByEventIds.kt:28` and `meal-plan-client/.../DuplicateMealPlan.kt:85`.
Mapped by `RecipeFavoriteSummaryRowAdapter` (`rs.getObject("recipe_id", UUID::class.java)`,
`rs.getInt("favorite_count")`, `rs.getBoolean("favorited_by_me")`).

**`internal/JdbiRecipeClient.kt`** — four fields and four overrides, in the existing style:
```kotlin
private val addRecipeFavorite = AddRecipeFavorite(jdbi)
private val removeRecipeFavorite = RemoveRecipeFavorite(jdbi)
private val getRecipeFavorites = GetRecipeFavorites(jdbi)
private val getRecipeFavoriteSummaries = GetRecipeFavoriteSummaries(jdbi)

override fun addFavorite(param: AddRecipeFavoriteParam): Result<Unit, AppError> = addRecipeFavorite.execute(param)
override fun removeFavorite(param: RemoveRecipeFavoriteParam): Result<Unit, AppError> = removeRecipeFavorite.execute(param)
override fun getFavorites(param: GetRecipeFavoritesParam): Result<List<RecipeFavorite>, AppError> = getRecipeFavorites.execute(param)
override fun getFavoriteSummaries(param: GetRecipeFavoriteSummariesParam): Result<List<RecipeFavoriteSummary>, AppError> = getRecipeFavoriteSummaries.execute(param)
```

### 6.5 `FakeRecipeClient` — required behaviour

By the end of PR 5 the fake must be **real in-memory logic, not stubs**. It is what every service
unit test runs against.

Add a store beside `recipes` and `ingredients`:
```kotlin
private val favorites = ConcurrentHashMap<Pair<UUID, UUID>, RecipeFavorite>()   // (recipeId, userId) -> row
```
A map keyed by the pair is the in-memory form of `uq_recipe_favorites_recipe_user` and makes both
mutations idempotent for free.

| method | required behaviour |
|---|---|
| `addFavorite` | run the validator; `favorites.putIfAbsent(recipeId to userId, RecipeFavorite(UUID.randomUUID(), recipeId, userId, Instant.now()))`; always `success(Unit)`. Re-favouriting must **not** change the existing row's `createdAt` (ordering in the names sheet must be stable). Does **not** require the recipe to exist — the service checks that. |
| `removeFavorite` | run the validator; `favorites.remove(recipeId to userId)`; always `success(Unit)`, whether or not a row was there. |
| `getFavorites` | run the validator; `favorites.values.filter { it.recipeId == param.recipeId }.sortedWith(compareBy({ it.createdAt }, { it.id }))`; `success(list)`. |
| `getFavoriteSummaries` | run the validator; if `param.recipeIds.isEmpty()` return `success(emptyList())`; group `favorites.values.filter { it.recipeId in param.recipeIds.toSet() }` by `recipeId` and emit one `RecipeFavoriteSummary(recipeId, rows.size, rows.any { it.userId == param.userId })` per group. **Recipes with no favourites must be absent from the result**, exactly like the SQL — otherwise the fake would hide a missing default in the service. |

Also extend the existing helpers:
- `reset()` must clear `favorites`.
- `delete(param)` must also do `favorites.keys.removeIf { it.first == param.id }` — this is the
  in-memory stand-in for the FK `ON DELETE CASCADE`, and the service test for cascade-on-delete
  depends on it.
- new `fun seedFavorites(vararg entities: RecipeFavorite) = entities.forEach { favorites[it.recipeId to it.userId] = it }`.

---

## 7. Service — `camper-service`, `features/recipe/`

### 7.1 Visibility rule

`features/recipe/actions/RecipeVisibility.kt` (an internal object beside the actions, the same
placement as the existing `HtmlFetcher`):

```kotlin
package com.acme.services.camperservice.features.recipe.actions

import com.acme.clients.recipeclient.model.Recipe as ClientRecipe
import java.util.UUID

/**
 * A recipe is visible to a user when it is published, or when they created it
 * (their own draft). Someone else's draft is treated as not existing at all —
 * the favourite endpoints answer 404, never 403, so the existence of another
 * person's draft is never disclosed.
 *
 * Deliberately NOT applied to GetRecipeAction: `GET /api/recipes/{id}` has
 * always returned any recipe by id (RecipeDetailPage even renders a read-only
 * banner for someone else's draft). Tightening that is a behaviour change to a
 * shipped endpoint and is out of this feature's scope.
 */
internal object RecipeVisibility {
    fun isVisibleTo(recipe: ClientRecipe, userId: UUID): Boolean =
        recipe.status == "published" || recipe.createdBy == userId
}
```

Every one of the three new actions begins with the same four lines:

```kotlin
val recipe = when (val result = recipeClient.getById(GetByIdParam(param.recipeId))) {
    is Result.Success -> result.value
    is Result.Failure -> when (result.error) {
        is NotFoundError -> return Result.Failure(RecipeError.NotFound(param.recipeId))
        else -> return Result.Failure(RecipeError.Invalid("recipe", result.error.message))
    }
}
if (!RecipeVisibility.isVisibleTo(recipe, param.userId)) {
    return Result.Failure(RecipeError.NotFound(param.recipeId))
}
```

### 7.2 DTOs — `features/recipe/dto/RecipeResponses.kt`

`RecipeResponse` and `RecipeDetailResponse` each gain two fields, placed **after `theme` and before
`createdAt`** (declaration order is the JSON field order):

```kotlin
data class RecipeResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val duplicateOfId: UUID?,
    val meal: String?,
    val theme: String?,
    val favoriteCount: Int,
    val favoritedByMe: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class RecipeDetailResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val webLink: String?,
    val baseServings: Int,
    val status: String,
    val createdBy: UUID,
    val duplicateOf: RecipeResponse?,
    val ingredients: List<RecipeIngredientResponse>,
    val meal: String?,
    val theme: String?,
    val favoriteCount: Int,
    val favoritedByMe: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

Two new response DTOs in the same file:

```kotlin
/** Answer to PUT/DELETE /api/recipes/{id}/favorite. */
data class RecipeFavoriteStatusResponse(
    val recipeId: UUID,
    val favoriteCount: Int,
    val favoritedByMe: Boolean
)

/** One row of GET /api/recipes/{id}/favorites. `username` falls back to email. */
data class RecipeFavoriteUserResponse(
    val userId: UUID,
    val username: String,
    val favoritedAt: Instant
)
```

### 7.3 Mapper — `features/recipe/mapper/RecipeMapper.kt`

Two changed signatures (**new parameters are required — no defaults**, decision §2.4):

```kotlin
fun toRecipeResponse(
    recipe: ClientRecipe,
    favoriteCount: Int,
    favoritedByMe: Boolean
): RecipeResponse = RecipeResponse(..., favoriteCount = favoriteCount, favoritedByMe = favoritedByMe, ...)

fun toRecipeDetailResponse(
    recipe: ClientRecipe,
    duplicateOf: RecipeResponse?,
    ingredients: List<RecipeIngredientResponse>,
    favoriteCount: Int,
    favoritedByMe: Boolean
): RecipeDetailResponse = ...
```

Two new mapper functions in the same object:

```kotlin
fun toFavoriteStatusResponse(summary: RecipeFavoriteSummary): RecipeFavoriteStatusResponse =
    RecipeFavoriteStatusResponse(summary.recipeId, summary.favoriteCount, summary.favoritedByMe)

fun toFavoriteUserResponse(favorite: ClientRecipeFavorite, username: String): RecipeFavoriteUserResponse =
    RecipeFavoriteUserResponse(userId = favorite.userId, username = username, favoritedAt = favorite.createdAt)
```

### 7.4 Getting the fields without N+1 — the one shared shape

Every action that needs the fields does exactly this, once:

```kotlin
val summaries = when (val result = recipeClient.getFavoriteSummaries(
    GetRecipeFavoriteSummariesParam(recipeIds = ids, userId = param.userId)
)) {
    is Result.Success -> result.value.associateBy { it.recipeId }
    is Result.Failure -> return Result.Failure(RecipeError.Invalid("favorites", result.error.message))
}
fun countOf(id: UUID) = summaries[id]?.favoriteCount ?: 0
fun mineOf(id: UUID) = summaries[id]?.favoritedByMe ?: false
```

Per action, `ids` is:

| action | `ids` | notes |
|---|---|---|
| `ListRecipesAction` | `visible.map { it.id }` | **one** call after the existing two `getAll` calls. Total client calls stays at 3, independent of list length. Skip the call when `visible` is empty. |
| `GetRecipeAction` | `listOfNotNull(recipe.id, recipe.duplicateOfId)` | one call covers both the recipe and its nested `duplicateOf` |
| `ImportRecipeAction` | `listOfNotNull(finalRecipe.id, finalRecipe.duplicateOfId)` | the new draft is genuinely 0/false, but going through the same call keeps one shape and covers `duplicateOf` |
| `UpdateRecipeAction` | `listOf(param.recipeId)` | after the update succeeds |
| `PublishRecipeAction` | `listOf(param.recipeId)` | after the update succeeds |
| `ResolveDuplicateAction` | `listOf(param.recipeId)` | `NOT_DUPLICATE` branch only; `USE_EXISTING` returns `null` |
| `CreateRecipeAction` | — | **no call.** A recipe that was created microseconds ago has no favourites; pass `favoriteCount = 0, favoritedByMe = false` literally, with a one-line comment saying why. |

The count read is not in the same transaction as the write, so a concurrent favourite by another
user can land between them. That is accepted: the numbers are advisory and the frontend refetches
on focus. No locking.

### 7.5 Service params — appended to `features/recipe/params/RecipeServiceParams.kt`

```kotlin
data class FavoriteRecipeParam(val recipeId: UUID, val userId: UUID)
data class UnfavoriteRecipeParam(val recipeId: UUID, val userId: UUID)
data class ListRecipeFavoritesParam(val recipeId: UUID, val userId: UUID)
```

### 7.6 Validations — `features/recipe/validations/` (new package)

Three default validators, 1:1 with the three new actions. Each follows the standard
`execute()` → log on failure → `private fun validate()` → `success(Unit)` shape, with
`Result<Unit, RecipeError>` as the return type:

- `ValidateFavoriteRecipe.kt`
- `ValidateUnfavoriteRecipe.kt`
- `ValidateListRecipeFavorites.kt`

Existing recipe actions are **not** retrofitted.

### 7.7 Actions

**`FavoriteRecipeAction(recipeClient: RecipeClient)`** →
`Result<RecipeFavoriteStatusResponse, RecipeError>`
1. `validate.execute(param)`; return on failure.
2. Fetch + visibility (§7.1).
3. `recipeClient.addFavorite(AddRecipeFavoriteParam(param.recipeId, param.userId))` — failure →
   `RecipeError.Invalid("favorite", error.message)`.
4. `getFavoriteSummaries(listOf(param.recipeId), param.userId)` → `RecipeMapper.toFavoriteStatusResponse`,
   defaulting a missing row to `RecipeFavoriteSummary(param.recipeId, 0, false)`.

**`UnfavoriteRecipeAction(recipeClient)`** → identical, with `removeFavorite`.

**`ListRecipeFavoritesAction(recipeClient: RecipeClient, userClient: UserClient)`** →
`Result<List<RecipeFavoriteUserResponse>, RecipeError>`
1. `validate.execute(param)`.
2. Fetch + visibility.
3. `recipeClient.getFavorites(GetRecipeFavoritesParam(param.recipeId))` — already oldest-first.
4. For each row, resolve the display name with the **exact** `GetMembersAction.kt:68` rule:
   ```kotlin
   private fun displayName(userId: UUID): String =
       when (val result = userClient.getById(UserGetByIdParam(userId))) {
           is Result.Success -> result.value.username ?: result.value.email
           is Result.Failure -> userId.toString()
       }
   ```
   A user-lookup failure degrades to the id rather than failing the whole request, as it does for
   plan members.
5. Map to `RecipeFavoriteUserResponse`, preserving the order.

### 7.8 Errors

**No new `RecipeError` variants.** A hidden or missing recipe is `RecipeError.NotFound` → 404; a
client failure is `RecipeError.Invalid` → 400. `common/error/ResultExtensions.kt` already has the
`RecipeError.toResponseEntity()` mapping and the `@JvmName("recipeResultToResponseEntity")`
`Result` overload — **no change to `ResultExtensions.kt` is required.**

### 7.9 Service facade — `features/recipe/service/RecipeService.kt`

```kotlin
class RecipeService(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    recipeScraperClient: RecipeScraperClient,
    userClient: UserClient,
    htmlFetcher: HtmlFetcher = defaultHtmlFetcher()
) {
    ...
    private val favoriteRecipe = FavoriteRecipeAction(recipeClient)
    private val unfavoriteRecipe = UnfavoriteRecipeAction(recipeClient)
    private val listRecipeFavorites = ListRecipeFavoritesAction(recipeClient, userClient)

    fun favorite(param: FavoriteRecipeParam) = favoriteRecipe.execute(param)
    fun unfavorite(param: UnfavoriteRecipeParam) = unfavoriteRecipe.execute(param)
    fun listFavorites(param: ListRecipeFavoritesParam) = listRecipeFavorites.execute(param)
}
```
`userClient` is the **4th positional parameter, before `htmlFetcher`** (which has a default and must
stay last). Both existing construction sites are listed in §8.

### 7.10 Wiring — `config/RecipeServiceConfig.kt`

```kotlin
@Bean
fun recipeService(
    recipeClient: RecipeClient,
    ingredientClient: IngredientClient,
    recipeScraperClient: RecipeScraperClient,
    userClient: UserClient
): RecipeService = RecipeService(recipeClient, ingredientClient, recipeScraperClient, userClient)
```
A `UserClient` bean already exists (`UserClientConfig`), so no new config class is needed.
`services/camper-service/build.gradle.kts` already has `implementation(project(":clients:user-client"))`
and `testImplementation(testFixtures(project(":clients:user-client")))` — **no build file change.**

### 7.11 Controller — `features/recipe/controller/RecipeController.kt`

```kotlin
@PutMapping("/{id}/favorite")
fun favorite(
    @PathVariable id: UUID,
    @RequestHeader("X-User-Id") userId: UUID
): ResponseEntity<Any> {
    logger.info("PUT /api/recipes/{}/favorite", id)
    return recipeService.favorite(FavoriteRecipeParam(recipeId = id, userId = userId))
        .toResponseEntity { it }
}

@DeleteMapping("/{id}/favorite")
fun unfavorite(
    @PathVariable id: UUID,
    @RequestHeader("X-User-Id") userId: UUID
): ResponseEntity<Any> {
    logger.info("DELETE /api/recipes/{}/favorite", id)
    return recipeService.unfavorite(UnfavoriteRecipeParam(recipeId = id, userId = userId))
        .toResponseEntity { it }
}

@GetMapping("/{id}/favorites")
fun favorites(
    @PathVariable id: UUID,
    @RequestHeader("X-User-Id") userId: UUID
): ResponseEntity<Any> {
    logger.info("GET /api/recipes/{}/favorites", id)
    return recipeService.listFavorites(ListRecipeFavoritesParam(recipeId = id, userId = userId))
        .toResponseEntity { it }
}
```

**Route-collision check:** `/{id}/favorite` and `/{id}/favorites` are distinct literal segments and
do not collide with the existing `/{id}/ingredients`, `/{id}/publish` or `/{id}/resolve-duplicate`.
No WebSocket publish — recipes have no STOMP topic (handoff decision 5).

### 7.12 API surface

| Method | Path | Success | Body |
|---|---|---|---|
| `PUT` | `/api/recipes/{id}/favorite` | `200` | `RecipeFavoriteStatusResponse` |
| `DELETE` | `/api/recipes/{id}/favorite` | `200` | `RecipeFavoriteStatusResponse` |
| `GET` | `/api/recipes/{id}/favorites` | `200` | `RecipeFavoriteUserResponse[]` |

All three: `404 NOT_FOUND` when the recipe does not exist or is another user's draft;
`400 BAD_REQUEST` on an unexpected client failure. All require `X-User-Id`.

`GET /api/recipes` and `GET /api/recipes/{id}` gain `favoriteCount` and `favoritedByMe`. So do the
`RecipeResponse`/`RecipeDetailResponse` bodies of create, update, import, publish and
resolve-duplicate.

---

## 8. Cascade impact — every file that constructs a changed type

`RecipeResponse` and `RecipeDetailResponse` are constructed **only** inside `RecipeMapper`
(verified: `grep -rn "RecipeResponse(\|RecipeDetailResponse(" --include="*.kt"` returns only
`RecipeMapper.kt:62` plus unrelated `MealPlanRecipeDetailResponse` hits). Tests **deserialise**
these DTOs via Jackson rather than constructing them, so no test constructor call sites exist.

The real cascade is the two **mapper signature changes**. Every call site, with what it must pass:

| # | File:line | Call | Must pass |
|---|---|---|---|
| 1 | `features/recipe/actions/ListRecipesAction.kt:29` | `toRecipeResponse` | `countOf(it.id)`, `mineOf(it.id)` from the batched read |
| 2 | `features/recipe/actions/GetRecipeAction.kt:44` | `toRecipeResponse` (nested `duplicateOf`) | summary for `duplicateOfId` |
| 3 | `features/recipe/actions/GetRecipeAction.kt:57` | `toRecipeDetailResponse` | summary for `recipe.id` |
| 4 | `features/recipe/actions/ImportRecipeAction.kt:167` | `toRecipeResponse` (nested `duplicateOf`) | summary for `duplicateOfId` |
| 5 | `features/recipe/actions/ImportRecipeAction.kt:180` | `toRecipeDetailResponse` | summary for `finalRecipe.id` (0/false in practice) |
| 6 | `features/recipe/actions/CreateRecipeAction.kt:69` | `toRecipeResponse` | literal `0, false` + comment |
| 7 | `features/recipe/actions/UpdateRecipeAction.kt:38` | `toRecipeResponse` | summary for `param.recipeId` |
| 8 | `features/recipe/actions/PublishRecipeAction.kt:50` | `toRecipeResponse` | summary for `param.recipeId` |
| 9 | `features/recipe/actions/ResolveDuplicateAction.kt:32` | `toRecipeResponse` | summary for `param.recipeId` |

`RecipeService` constructor call sites (the `userClient` parameter):

| File:line | Change |
|---|---|
| `config/RecipeServiceConfig.kt` (`recipeService` bean) | add `userClient: UserClient` param and pass it |
| `src/test/kotlin/.../features/recipe/RecipeServiceTest.kt:~32` | `RecipeService(fakeRecipeClient, fakeIngredientClient, fakeScraperClient, fakeHtmlFetcher)` is **positional and will break**. Rewrite with named arguments: `RecipeService(recipeClient = fakeRecipeClient, ingredientClient = fakeIngredientClient, recipeScraperClient = fakeScraperClient, userClient = fakeUserClient, htmlFetcher = fakeHtmlFetcher)`, adding `private val fakeUserClient = FakeUserClient()` and `fakeUserClient.reset()` in `@BeforeEach`. |

`RecipeClient` interface call sites — adding four methods breaks every implementor. Both are
updated in PR 3 with `TODO("Implementation in client-impl PR")`:
- `clients/recipe-client/src/main/kotlin/.../internal/JdbiRecipeClient.kt`
- `clients/recipe-client/src/testFixtures/kotlin/.../fake/FakeRecipeClient.kt`

No other module implements `RecipeClient`.

Frontend types changing shape — `RecipeResponse` gains two **required** fields in
`webapp/src/api/recipes.ts`. Consumers that only read the type are unaffected; there are no
object-literal constructions of `RecipeResponse` in the webapp (it is only ever a server payload).
`webapp/src/lib/recipeFavorites.ts` takes a structural subtype, not `RecipeResponse` itself, so the
pure tests can build tiny fixtures without the whole DTO.

---

## 9. Frontend — `webapp/`

`webapp/CLAUDE.md` is binding. Every screen state is a URL; every row-end icon button is a
`RowActionButton`; colours are tokens only; optimistic writes roll back with a targeted inverse
edit on the current cache, never a snapshot; the only tests are pure Vitest suites in `lib/`.

### 9.1 `api/recipes.ts`

`RecipeResponse` gains, after `theme`:
```ts
  favoriteCount: number;
  favoritedByMe: boolean;
```
(`RecipeDetailResponse extends RecipeResponse`, so it inherits them.)

New types and calls:
```ts
export interface RecipeFavoriteStatusResponse {
  recipeId: string;
  favoriteCount: number;
  favoritedByMe: boolean;
}

export interface RecipeFavoriteUserResponse {
  userId: string;
  /** Their username, falling back to their email when they have none. */
  username: string;
  favoritedAt: string;
}

/** PUT /api/recipes/{id}/favorite — idempotent; 404 if the recipe isn't visible to you. */
export function favoriteRecipe(recipeId: string): Promise<RecipeFavoriteStatusResponse> {
  return request(`/api/recipes/${recipeId}/favorite`, { method: 'PUT' });
}

/** DELETE /api/recipes/{id}/favorite — idempotent; answers 200 with the new totals, not 204. */
export function unfavoriteRecipe(recipeId: string): Promise<RecipeFavoriteStatusResponse> {
  return request(`/api/recipes/${recipeId}/favorite`, { method: 'DELETE' });
}

/** GET /api/recipes/{id}/favorites — who favourited it, oldest first. */
export function getRecipeFavorites(recipeId: string): Promise<RecipeFavoriteUserResponse[]> {
  return request(`/api/recipes/${recipeId}/favorites`);
}
```

### 9.2 `lib/recipeFavorites.ts` — the pure module (all the logic worth testing)

This is where the filter predicate and both optimistic cache edits live, so the components stay
thin shells. `.test.ts` sits beside it.

```ts
export type RecipeShowFilter = 'all' | 'mine' | 'favourites' | 'my-favourites';

/** Anything with the three fields the filter and the cache edits need. */
export interface FavouritableRecipe {
  id: string;
  createdBy: string;
  favoriteCount: number;
  favoritedByMe: boolean;
}

/** Reads the `show` search param. Anything unrecognised (or absent) is 'all'. */
export function parseShowFilter(raw: string | null): RecipeShowFilter;

/**
 * The Show chip row's predicate. ANDs with the meal chips and the search box,
 * which the page applies separately.
 *   all           — everything
 *   mine          — created by the signed-in user (undefined user: nothing)
 *   favourites    — anyone has favourited it (favoriteCount > 0)
 *   my-favourites — the signed-in user has favourited it (favoritedByMe)
 */
export function matchesShowFilter(
  recipe: FavouritableRecipe,
  filter: RecipeShowFilter,
  userId: string | undefined,
): boolean;

/**
 * The whole optimistic edit, as one guarded primitive: flip `favoritedByMe`
 * to `favorited` and move the count by one — but only when that is an actual
 * change. Applying it twice, or applying it to a row the server has already
 * brought to that state, does nothing, which is what makes the rollback below
 * safe to run against the CURRENT cache rather than a snapshot.
 */
export function setFavorite<T extends FavouritableRecipe>(recipe: T, favorited: boolean): T;

/** `setFavorite` mapped over a cached list, leaving other rows identically referenced. */
export function applyFavoriteToList<T extends FavouritableRecipe>(
  list: T[] | undefined,
  recipeId: string,
  favorited: boolean,
): T[] | undefined;

/** `setFavorite` on a cached detail, when it is the recipe in question. */
export function applyFavoriteToDetail<T extends FavouritableRecipe>(
  detail: T | undefined,
  recipeId: string,
  favorited: boolean,
): T | undefined;
```

`setFavorite` clamps at zero (`Math.max(0, count - 1)`) so a stale cache can never show `-1`.

**`lib/recipeFavorites.test.ts` must cover:**
- `parseShowFilter`: `null`, each of the three valid values, `'1'` (the dead `?mine=1` value) and
  an arbitrary string → `'all'`.
- `matchesShowFilter`: all four filters × (mine / not mine) × (0 / >0 count) × (favoritedByMe
  true / false), plus `userId === undefined` under `'mine'` and `'my-favourites'`.
- `setFavorite`: false→true increments and flips; true→false decrements and flips; **true→true and
  false→false return the identical object** (no double count); count never goes below 0.
- `applyFavoriteToList`: edits the matching row only, leaves the other rows referentially equal,
  returns `undefined` for an undefined list, no-ops for an id not in the list.
- `applyFavoriteToDetail`: edits a matching detail, no-ops on a different id, `undefined` in →
  `undefined` out.
- **Apply-then-rollback round trip** — `applyFavoriteToList(applyFavoriteToList(list, id, true), id, false)`
  returns the original counts and flags. This is the property the rollback relies on.

### 9.3 `queries/recipes.ts` — keys and the toggle mutation

```ts
export const recipeFavoritesKey = (recipeId: string) => ['recipe-favorites', recipeId] as const;
const favoriteMutationKey = (recipeId: string) => ['recipe-favorite', recipeId] as const;

export function useRecipeFavorites(recipeId: string | undefined) {
  return useQuery({
    queryKey: recipeFavoritesKey(recipeId ?? ''),
    queryFn: () => getRecipeFavorites(recipeId as string),
    enabled: Boolean(recipeId),
  });
}
```

### 9.4 Rapid double-taps — **serialise and coalesce per recipe id**

**Decision: serialise-and-coalesce, not disable-while-in-flight.** Two reasons:

1. A heart is a one-tap toggle on a 44px target. Disabling it makes the second tap silently do
   nothing while the UI has already flipped — exactly the failure the shopping list solved with
   `rowChains`. The optimistic write must stay instant on every tap; only the network needs order.
2. The list renders one shared mutation instance across every row, so `isPending`/`variables`
   only ever reflect the most recent call and cannot scope a disable to the tapped row — the same
   problem `AddToPlanSheet` works around with a `pendingPlanIds` set. Chaining avoids per-row
   bookkeeping entirely.

Implementation, mirroring `queries/shopping.ts` (`rowChains` / `rowLatestPurchases`) module-level
maps, keyed by **recipe id alone** — unlike shopping rows, recipes are globally unique, so no
composite key is needed:

```ts
// Per-recipe network serialization + coalescing, the same idiom as
// `rowChains` in queries/shopping.ts. A PUT and a DELETE for the same heart
// can otherwise reach the server out of order (tap on then off quickly) and
// the last one to land wins, which may not be the one the user meant.
// `favoriteChains` serializes: a recipe's next send waits for its previous
// one to settle. `favoriteLatest` coalesces: a send superseded before its
// turn comes is skipped entirely, so only the latest desired state is ever
// sent. The optimistic cache write in onMutate is untouched by any of this —
// it is what keeps every tap instant.
const favoriteChains = new Map<string, Promise<void>>();
const favoriteLatest = new Map<string, boolean>();

function sendFavoriteInOrder(
  recipeId: string,
  favorited: boolean,
): Promise<RecipeFavoriteStatusResponse | undefined> {
  favoriteLatest.set(recipeId, favorited);
  const previousLink = favoriteChains.get(recipeId) ?? Promise.resolve();
  const thisLink = previousLink
    // A previous link's failure is reported through THAT mutate() call; it
    // must not block this recipe's future taps from ever running.
    .catch(() => undefined)
    .then(() => {
      const desired = favoriteLatest.get(recipeId);
      if (desired === undefined) return undefined;   // a later link already sent it
      favoriteLatest.delete(recipeId);
      return desired ? favoriteRecipe(recipeId) : unfavoriteRecipe(recipeId);
    });
  favoriteChains.set(recipeId, thisLink.then(() => undefined, () => undefined));
  return thisLink;
}
```

The hook:

```ts
/**
 * Optimistic favourite toggle. Writes to BOTH the list cache and the detail
 * cache at once; rolls back with the inverse edit applied to whatever is in
 * the cache at that moment (never a whole-list snapshot, which would wipe a
 * different concurrent mutation's success — see webapp/CLAUDE.md). Sends are
 * serialized and coalesced per recipe (`sendFavoriteInOrder`).
 */
export function useToggleFavorite() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: ['recipe-favorite'],
    mutationFn: ({ recipeId, favorited }: { recipeId: string; favorited: boolean }) =>
      sendFavoriteInOrder(recipeId, favorited),
    onMutate: async ({ recipeId, favorited }) => {
      await queryClient.cancelQueries({ queryKey: recipesKey });
      await queryClient.cancelQueries({ queryKey: recipeKey(recipeId) });
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        applyFavoriteToList(current, recipeId, favorited),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        applyFavoriteToDetail(current, recipeId, favorited),
      );
    },
    onError: (_err, { recipeId, favorited }) => {
      // Targeted inverse edit on the CURRENT cache. `setFavorite` is guarded,
      // so if something already brought the row back this is a no-op rather
      // than a second decrement.
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        applyFavoriteToList(current, recipeId, !favorited),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        applyFavoriteToDetail(current, recipeId, !favorited),
      );
    },
    onSuccess: (status, { recipeId }) => {
      // A coalesced link sends nothing and resolves undefined — the optimistic
      // write already says the right thing, so leave the cache alone.
      if (!status) return;
      queryClient.setQueryData<RecipeResponse[]>(recipesKey, (current) =>
        current?.map((r) =>
          r.id === recipeId
            ? { ...r, favoriteCount: status.favoriteCount, favoritedByMe: status.favoritedByMe }
            : r,
        ),
      );
      queryClient.setQueryData<RecipeDetailResponse>(recipeKey(recipeId), (current) =>
        current && current.id === recipeId
          ? { ...current, favoriteCount: status.favoriteCount, favoritedByMe: status.favoritedByMe }
          : current,
      );
    },
    onSettled: (_data, _err, { recipeId }) => {
      // `=== 1`, not `=== 0`: TanStack still counts THIS mutation as in
      // flight while its onSettled runs (see webapp/CLAUDE.md).
      if (queryClient.isMutating({ mutationKey: ['recipe-favorite'] }) === 1) {
        void queryClient.invalidateQueries({ queryKey: recipesKey });
        void queryClient.invalidateQueries({ queryKey: recipeKey(recipeId) });
      }
      // The names sheet's contents change with the caller's own row; always
      // refresh it (it is a cheap, rarely-mounted query).
      void queryClient.invalidateQueries({ queryKey: recipeFavoritesKey(recipeId) });
    },
  });
}
```

Errors surface through the global mutation error toast — no `meta.suppressErrorToast`.

### 9.5 `pages/recipes/RecipesPage.tsx` — the Show chip row

Remove: the `Switch` import, the `Text`-based `Mine` label block (lines ~167–172), and
`const mine = searchParams.get('mine') === '1'`. The old `?mine=1` value is **not** kept working
(the handoff says it need not be).

Add:
```ts
const show = parseShowFilter(searchParams.get('show'));
const hasFilters = q.trim().length > 0 || meal !== 'all' || show !== 'all';
const clearFilters = () => { setQ(''); patch({ q: null, meal: null, show: null }); };
```
`filtered` becomes:
```ts
const filtered = useMemo(() => {
  let list = recipes ?? [];
  list = list.filter((r) => matchesShowFilter(r, show, user?.id));
  if (meal !== 'all') list = list.filter((r) => r.meal === meal);
  const needle = q.trim().toLowerCase();
  if (needle) list = list.filter((r) => r.name.toLowerCase().includes(needle));
  return [...list].sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }));
}, [recipes, show, meal, q, user]);
```

A second chip row, immediately **under** the existing meal-chip row and above
`.recipes-page__actions-row`, reusing `.recipes-page__chips` / `.recipes-page__chip` verbatim (the
class already has `overflow-x: auto` and `flex-shrink: 0`, which is what keeps it from scrolling
the page at 360px):

```tsx
<div className="recipes-page__chips" role="group" aria-label="Show">
  {SHOW_CHIPS.map(({ value, label, heart }) => (
    <button
      key={value}
      type="button"
      className={`recipes-page__chip${show === value ? ' recipes-page__chip--active' : ''}`}
      aria-pressed={show === value}
      onClick={() => patch({ show: value === 'all' ? null : value })}
    >
      {heart && <HeartFilledIcon aria-hidden />}
      {label}
    </button>
  ))}
</div>
```
with a module-level constant (single choice, `all` first, `all` written as an absent param):
```ts
const SHOW_CHIPS = [
  { value: 'all',            label: 'All',            heart: false },
  { value: 'mine',           label: 'Mine',           heart: false },
  { value: 'favourites',     label: 'Favourites',     heart: true  },
  { value: 'my-favourites',  label: 'My favourites',  heart: true  },
] as const;
```

`RecipeRow` gains a heart, **before** the existing `＋` button, inside a wrapper that keeps the
count outside the 44px target:
```tsx
<span className="recipes-page__row-favourite">
  {recipe.favoriteCount > 0 && (
    <Text as="span" size="1" color="gray">{recipe.favoriteCount}</Text>
  )}
  <RowActionButton
    quiet
    aria-label={recipe.favoritedByMe ? `Remove ${recipe.name} from your favourites` : `Favourite ${recipe.name}`}
    aria-pressed={recipe.favoritedByMe}
    onClick={onToggleFavorite}
  >
    {recipe.favoritedByMe ? <HeartFilledIcon /> : <HeartIcon />}
  </RowActionButton>
</span>
```
`RowActionButton` is required by `webapp/CLAUDE.md` ("every icon button at the end of a list row
is a `RowActionButton`"); `quiet` because the heart is a secondary action beside the row's main
`＋`. **Accent, not red** (decision §2.11): filled vs outline carries the state. No
`stopPropagation` is needed — the row's navigation lives on a sibling
`<button className="recipes-page__row-open">`, not on the row container.

`RecipesPage` calls `const toggleFavorite = useToggleFavorite();` and passes
`onToggleFavorite={() => toggleFavorite.mutate({ recipeId: recipe.id, favorited: !recipe.favoritedByMe })}`.

**`pages/recipes/RecipesPage.css`:**
- **delete** the `.recipes-page__mine` rule (lines 51–56) — the switch is gone.
- `.recipes-page__actions-row`: `justify-content: space-between` → `flex-end` (Import/New are now
  the row's only children and would otherwise sit left).
- `.recipes-page__chip`: add `display: inline-flex; align-items: center; gap: 6px;` so the heart
  glyph and the label sit on one baseline.
- add:
  ```css
  .recipes-page__row-favourite {
    display: flex;
    align-items: center;
    gap: 2px;
  }
  ```

### 9.6 `pages/recipes/RecipeDetailPage.tsx`

A new block at the top of `.recipe-detail-page__body`, before the description:

```tsx
<div className="recipe-detail-page__favourite">
  <IconButton
    size="3"
    variant="soft"
    aria-label={recipe.favoritedByMe ? 'Remove from your favourites' : 'Favourite this recipe'}
    aria-pressed={recipe.favoritedByMe}
    onClick={() => toggleFavorite.mutate({ recipeId: recipe.id, favorited: !recipe.favoritedByMe })}
  >
    {recipe.favoritedByMe ? <HeartFilledIcon /> : <HeartIcon />}
  </IconButton>
  {recipe.favoriteCount > 0 && (
    <SheetLink to={`/recipes/${recipe.id}/favourites`} className="recipe-detail-page__favourite-count">
      {recipe.favoriteCount === 1 ? '1 person' : `${recipe.favoriteCount} people`} ›
    </SheetLink>
  )}
</div>
```
At `favoriteCount === 0` the heart stands alone — no count, no link, no "Be the first" (decision
§2.12). The count is a `SheetLink`, so the sheet is a URL like every other sheet in this app.

**`pages/recipes/RecipeDetailPage.css`** — add:
```css
.recipe-detail-page__favourite {
  display: flex;
  align-items: center;
  gap: 10px;
}

.recipe-detail-page__favourite-count {
  color: var(--accent-11);
  font-size: 14px;
  text-decoration: none;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
}
```

### 9.7 `pages/recipes/FavouritedBySheet.tsx` (new)

Modelled on `AddToPlanSheet.tsx` for states and shape.

```tsx
export function FavouritedBySheet() {
  const { recipeId } = useParams<{ recipeId: string }>();
  const sheet = useSheet(`/recipes/${recipeId}`);
  const { data: people, isLoading, isError, refetch } = useRecipeFavorites(recipeId);
  const hasData = !!people;
  ...
  return (
    <Sheet {...sheet.sheetProps} title="Favourited by">
      {/* loading: <Skeleton height="48px" aria-hidden /> inside aria-busy */}
      {/* error with no data: <QueryErrorState message="Couldn't load who favourited this." onRetry={() => void refetch()} /> */}
      {/* empty: <Text as="p" color="gray" size="2">No one has favourited this yet.</Text> */}
      {/* list: one row per person, name left, relative/short date right */}
    </Sheet>
  );
}
```
Rows are plain non-interactive `<li>`s (there is nothing to tap through to). The date is rendered
with `new Date(person.favoritedAt).toLocaleDateString()`.
`FavouritedBySheet.css` holds `.favourited-by-sheet__list` (column flex, gap 4px) and
`.favourited-by-sheet__row` (space-between, min-height 44px).

### 9.8 Routing

`webapp/src/pages/recipes/index.ts` — append:
```ts
export { FavouritedBySheet } from './FavouritedBySheet';
```

`webapp/src/router.tsx` — add the lazy import beside the other recipe ones (line ~38):
```ts
const FavouritedBySheet = lazy(() => import('./pages/recipes').then((m) => ({ default: m.FavouritedBySheet })));
```
and the child route in the `recipes/:recipeId` block (after line 123):
```ts
{ path: 'favourites', element: <FavouritedBySheet /> },
```
Full path: `/recipes/:recipeId/favourites`. It is a child of the recipe page, so it renders through
that page's existing `<Outlet/>` and closes back to the recipe.

### 9.9 `webapp/CLAUDE.md` — two "Non-obvious decisions" entries

1. **The recipe list's Show chip row replaced the Mine switch** — one single-choice row
   (`All · Mine · ♥ Favourites · ♥ My favourites`) in a single `show` search param, absent meaning
   all; it ANDs with the meal chips and the search box, all three filtering client-side on the one
   `['recipes']` payload. The old `?mine=1` is deliberately not still accepted. The chips reuse
   `.recipes-page__chips`, which scrolls itself rather than the page at 360px.
2. **The favourite toggle is optimistic, per-recipe serialized and coalesced** (`queries/recipes.ts`
   `favoriteChains` / `favoriteLatest`, the same idiom as `shopping.ts` `rowChains` but keyed by
   recipe id alone, since recipes are globally unique). It writes both the list and the detail
   cache, and rolls back with a guarded inverse edit on the current cache — `setFavorite` in
   `lib/recipeFavorites.ts` is a no-op when the row is already in the target state, so a rollback
   can never double-decrement. The heart is accent-coloured, not red: red means "take it away" in
   this app and the accent is the app's one colour.

---

## 10. Tests

### 10.1 `recipe-client` integration tests — PR 8 (Testcontainers)

`clients/recipe-client/src/testFixtures/kotlin/com/acme/clients/recipeclient/test/RecipeTestDb.kt` —
a copy of `PlanTestDb.kt`'s shape, delegating to `com.acme.databases.camperdb.MigrationRunner`.

`clients/recipe-client/src/test/resources/docker-java.properties` — `api.version=1.44`.

`clients/recipe-client/src/test/kotlin/com/acme/clients/recipeclient/JdbiRecipeClientFavoritesTest.kt` —
`@Testcontainers` PostgreSQL 16, `RecipeTestDb.cleanAndMigrate(...)` once, `DB_URL`/`DB_USER`/
`DB_PASSWORD` system properties then `createRecipeClient()`. `@BeforeEach`:
```sql
TRUNCATE TABLE recipe_favorites, recipe_ingredients, recipes, ingredients, users CASCADE
```
Tests (the DB-shaped behaviour a fake cannot prove):
- `addFavorite inserts a row`
- `addFavorite twice is idempotent and leaves one row` — and the second call must not move
  `created_at`
- `removeFavorite deletes the row`
- `removeFavorite succeeds when there is no favourite` (0 rows affected is success, not NotFound)
- `getFavorites returns favourites oldest first`
- `getFavorites returns empty for a recipe nobody favourited`
- `getFavoriteSummaries counts favourites across several users`
- `getFavoriteSummaries sets favoritedByMe only for the asking user` — the same fixture read twice
  with two different `userId`s
- `getFavoriteSummaries omits recipes with no favourites` (the caller-defaults contract)
- `getFavoriteSummaries returns empty list for empty recipeIds without querying`
- `deleting a recipe cascades its favourites away` — `client.delete(...)`, then a direct
  `SELECT count(*) FROM recipe_favorites WHERE recipe_id = ?` is 0
- `deleting a user cascades their favourites away` — direct `DELETE FROM users WHERE id = ?` after
  removing their recipes, then count is 0

### 10.2 Service unit tests — PR 9 (`FakeRecipeClient` + `FakeUserClient`)

New `@Nested inner class Favorites` in `RecipeServiceTest.kt`:
- `favorite returns count 1 and favoritedByMe true`
- `favorite twice is idempotent and count stays 1`
- `unfavorite returns count 0 and favoritedByMe false`
- `unfavorite when never favourited succeeds with count 0`
- `favorite counts favourites across three users`
- `favoritedByMe is true for the favouriting user and false for another caller` — same recipe, two
  `ListRecipesParam`/`GetRecipeParam` callers
- `favorite returns NotFound for a missing recipe`
- `favorite returns NotFound for another user's draft`
- `favorite succeeds on the caller's own draft`
- `listFavorites returns names oldest first`
- `listFavorites falls back to email when the user has no username` — seed a `FakeUserClient` user
  with `username = null`
- `listFavorites falls back to the user id when the user lookup fails` — a favouriter id the fake
  user client does not know
- `listFavorites returns NotFound for another user's draft`
- `list returns favoriteCount and favoritedByMe per recipe`
- `list returns zero and false for a recipe nobody favourited` — proves the missing-summary default
- `get returns favoriteCount and favoritedByMe`
- `update returns the recipe's real favourite count, not zero` — the trap decision §2.4 exists to
  prevent
- `publish returns the recipe's real favourite count`
- `create returns zero and false`
- `deleting a recipe removes its favourites` — via the fake's cascade behaviour (§6.5)

### 10.3 Acceptance tests — PR 10 (Testcontainers + `TestRestTemplate`)

`RecipeFixture.kt`:
```kotlin
fun insertFavorite(
    id: UUID = UUID.randomUUID(),
    recipeId: UUID,
    userId: UUID,
    createdAt: Instant = Instant.now()
): UUID {
    jdbcTemplate.update(
        "INSERT INTO recipe_favorites (id, recipe_id, user_id, created_at) VALUES (?, ?, ?, ?)",
        id, recipeId, userId, java.sql.Timestamp.from(createdAt)
    )
    return id
}
```
and `truncateAll()` becomes
```kotlin
jdbcTemplate.execute("TRUNCATE TABLE recipe_favorites, recipe_ingredients, recipes, ingredients, users CASCADE")
```
(`recipe_favorites` listed first, child-first, matching `ActivityLadderFixture`. See §11 — this is
for intent, not necessity.)

New `@Nested inner class Favorites` in `RecipeAcceptanceTest.kt`, all using the existing
`entityWithUser(...)` helper for the `X-User-Id` header:
- `PUT favorite returns 200 with count 1 and favoritedByMe true`
- `PUT favorite twice returns 200 and count stays 1`
- `DELETE favorite returns 200 with count 0 and favoritedByMe false`
- `DELETE favorite when never favourited returns 200 with count 0`
- `PUT favorite returns 404 for an unknown recipe id`
- `PUT favorite returns 404 for another user's draft`
- `PUT favorite returns 200 on the caller's own draft`
- `GET favorites returns 404 for another user's draft`
- `GET favorites returns people oldest first`
- `GET favorites returns the email when the user has no username` — `fixture.insertUser(username = null)`
- `GET favorites returns an empty list when nobody has favourited`
- `GET recipes returns favoriteCount and favoritedByMe per recipe`
- `GET recipes reports favoritedByMe differently for two callers` — same fixture, two
  `X-User-Id`s, asserting the same recipe's `favoritedByMe` differs
- `GET recipe detail returns favoriteCount and favoritedByMe`
- `PUT recipe returns the current favourite count` — favourite, then edit the name, assert the
  count is still 1 in the update response (the decision §2.4 regression test at the HTTP level)
- `DELETE recipe removes its favourites` — favourite, delete the recipe, then a direct
  `jdbcTemplate.queryForObject("SELECT count(*) FROM recipe_favorites WHERE recipe_id = ?", ...)`
  is 0 (the FK cascade, proven through the real stack)
- `POST recipe returns favoriteCount 0 and favoritedByMe false`

### 10.4 Frontend tests

`webapp/src/lib/recipeFavorites.test.ts` only — the full list is in §9.2. There is deliberately no
DOM test runner; the components are thin shells over the pure module, which is why every branch
worth testing lives there.

---

## 11. Test fixture impact — truncation

**Finding: no existing fixture's truncation order needs to change.** The reasoning, so a reviewer
can check it rather than trust it:

PostgreSQL's `TRUNCATE ... CASCADE` *"automatically truncate[s] all tables that have foreign-key
references to any of the named tables"*, regardless of each FK's `ON DELETE` action. Every fixture
in this repo that touches `users` or `recipes` already uses `CASCADE`. `recipe_favorites` has FKs to
both, so it is swept up automatically wherever either table is truncated. That is exactly the
behaviour the tests want: a clean slate.

Full audit of every truncation site (`grep -rn "TRUNCATE" --include="*.kt"`):

| File | Statement | Effect of `recipe_favorites` | Change |
|---|---|---|---|
| `services/.../features/recipe/acceptance/fixture/RecipeFixture.kt:83` | `recipe_ingredients, recipes, ingredients, users CASCADE` | cascaded clean | **list `recipe_favorites` first, for intent** |
| `services/.../features/mealplan/acceptance/fixture/MealPlanFixture.kt:185` | `… recipes, ingredients, plan_members, plans, users CASCADE` | cascaded clean | none |
| `services/.../features/user/acceptance/fixture/UserFixture.kt:78` | `user_dietary_restrictions, plan_members, plans, users CASCADE` | cascaded clean | none |
| `services/.../features/plan/acceptance/fixture/PlanFixture.kt:51` | `plan_members, plans, users CASCADE` | cascaded clean | none |
| `services/.../features/item/acceptance/fixture/ItemFixture.kt:75` | `items, plan_members, plans, users CASCADE` | cascaded clean | none |
| `services/.../features/gearpack/acceptance/fixture/GearPackFixture.kt:127` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/gearsync/acceptance/fixture/GearSyncFixture.kt:116` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/assignment/acceptance/fixture/AssignmentFixture.kt:87` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/itinerary/acceptance/fixture/ItineraryFixture.kt:80` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/logbook/acceptance/fixture/LogBookFixture.kt:82` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/activityladder/acceptance/fixture/ActivityLadderFixture.kt:22` | `… users CASCADE` | cascaded clean | none |
| `services/.../features/world/acceptance/fixture/WorldFixture.kt:10` | `worlds CASCADE` | untouched | none |
| `services/.../websocket/WebSocketIntegrationTest.kt:52` | `… users CASCADE` | cascaded clean | none |
| `services/.../websocket/LadderWebSocketIntegrationTest.kt:57` | `… users CASCADE` | cascaded clean | none |
| `clients/user-client/.../JdbiUserClientTest.kt:54` | `users CASCADE` | cascaded clean | none |
| `clients/plan-client/.../JdbiPlanClientTest.kt:61` | `plan_members, plans, users CASCADE` | cascaded clean | none |
| `clients/meal-plan-client/.../MealPlanClientIntegrationTest.kt:76` | `… users CASCADE` (multi-line) | cascaded clean | none |
| `clients/item-client`, `clients/gear-pack-client`, `clients/itinerary-client`, `clients/assignment-client`, `clients/log-book-client`, `clients/activity-ladder-client` | all end in `users CASCADE` | cascaded clean | none |
| `clients/world-client`, `clients/invitation-client` | `worlds` / `invitations` only | untouched | none |

**Two rules the implementers must follow so this stays true:**

1. `recipe_favorites.user_id` **must** be `ON DELETE CASCADE`. A `RESTRICT` there would make
   `TRUNCATE users CASCADE` still work (TRUNCATE ignores the action) but would break any test or
   production path that *deletes* a user. `ladder_votes` set the same precedent for the same
   reason; `databases/camper-db/CLAUDE.md` already records it.
2. There is **no** seeded reference data in the test path — `dev_seed.sql` is never run by tests —
   so no re-seed step is needed after any truncation. The fixtures create their own users.

The **new** `clients/recipe-client` integration test is the only place a truncation statement must
be authored from scratch: `TRUNCATE TABLE recipe_favorites, recipe_ingredients, recipes,
ingredients, users CASCADE` (§10.1).

---

## 12. Flagged deviations and known inconsistencies

1. **`GET /api/recipes/{id}` still returns another user's draft** while `PUT/DELETE
   /api/recipes/{id}/favorite` and `GET /api/recipes/{id}/favorites` answer 404 for one. That is
   deliberate (decision §2.3) — tightening the detail endpoint would change shipped behaviour the
   frontend depends on (`RecipeDetailPage.tsx`'s read-only draft callout). Recorded here so a
   reviewer does not read it as an oversight.
2. **`recipe-client` gets its first test source set.** It and `ingredient-client` are the only JDBI
   clients in the repo without one; this feature fixes `recipe-client` only.
3. **The recipe feature gets its first `validations/` package** (three default validators). Its
   existing actions keep validating inline and are not retrofitted in this feature.
4. **`RemoveRecipeFavorite` returns success on 0 affected rows**, unlike every other `delete`
   operation in this client, which returns `NotFoundError`. Required by the idempotency contract;
   the KDoc must say so explicitly.
5. **`RecipeService`'s constructor grows a parameter in a non-final position** (`userClient` before
   the defaulted `htmlFetcher`). The one positional call site, `RecipeServiceTest`, is converted to
   named arguments in the same PR.
6. **`GET /api/recipes/{id}/favorites` is N+1 on `users`** by design (decision §2.6). It is bounded
   by one recipe's favouriters and is not on any list path.

---

## 13. Open questions

None. Everything the handoff left to the architect has been decided in §2. Two items a reviewer
might otherwise raise, pre-answered:

- *Should `GET /api/recipes` filter server-side by the new filters?* No — the handoff settles this:
  the filters are client-side, on the one `['recipes']` payload, exactly like `mine` and `meal`
  today. No query params are added to the API.
- *Should favouriting publish a WebSocket event?* No — handoff decision 5. Recipes have no STOMP
  topic and none is added; counts refresh on refetch and window focus.
