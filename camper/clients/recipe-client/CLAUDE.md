# Recipe Client — AI Context

## Overview
JDBI data access client for recipes, recipe ingredients, and recipe favourites.

## Package
`com.acme.clients.recipeclient`

## Public API

### RecipeClient (interface)
- **Recipes**: create, getById, getAll, update, delete, findByWebLink, findSimilarByName
- **Recipe ingredients**: addIngredient, addIngredients, getIngredients, updateIngredient, removeIngredient, findIngredientsByIngredientId
- **Favourites**: addFavorite, removeFavorite, getFavorites, getFavoriteSummaries

All operations return `Result<T, AppError>`.

### Models
- `Recipe` — a cooking recipe (`status` is `draft` / `published`, `duplicateOfId` links a suspected duplicate to its original)
- `RecipeIngredient` — one ingredient line on a recipe, with its parse/match status
- `RecipeFavorite` — one `(recipeId, userId)` favourite, with `id` and `createdAt`. No `updatedAt`: rows are only inserted and deleted
- `RecipeFavoriteSummary(recipeId, favoriteCount, favoritedByMe)` — read model, not a table. Produced by the batched aggregate below

### Factory
- `createRecipeClient()` — creates a JDBI-backed client using DB env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`)

### Fake (testFixtures)
- `FakeRecipeClient` — in-memory fake for unit testing. `reset()`, `seed(...)`, `seedIngredients(...)`, `seedFavorites(...)`

## Database
- Database: `camper_db` (port 5433)
- Tables: `recipes`, `recipe_ingredients`, `recipe_favorites`
- Key constraints: `uq_recipe_favorites_recipe_user UNIQUE (recipe_id, user_id)`;
  `recipe_favorites.recipe_id → recipes.id` and `.user_id → users.id`, both `ON DELETE CASCADE`

## Favourites — non-obvious behaviour

These four are contracts, not implementation details. Tests depend on each of them, and the fake
replicates all of them.

1. **`addFavorite` is idempotent via the database.** `ON CONFLICT (recipe_id, user_id) DO NOTHING`,
   so the unique constraint never raises and re-favouriting leaves the existing row — and therefore
   its `created_at` — untouched. Success is returned regardless of the affected row count. `id` and
   `created_at` come from their column defaults. The fake uses `putIfAbsent` for the same reason.
2. **`removeFavorite` returns success on 0 deleted rows.** This is the one place this client
   deliberately departs from the `DeleteRecipe` / `RemoveRecipeIngredient` shape, which answers
   `NotFoundError` when nothing matched. Un-favouriting is specified as idempotent, so
   un-favouriting something that was never favourited is a no-op, not an error.
3. **`getFavorites` orders by `created_at, id`.** `id` is the tiebreaker so a batch inserted in one
   transaction — identical `now()` for every row — still comes back in a stable order.
4. **`getFavoriteSummaries` omits recipes with zero favourites.** It is the list endpoint's answer
   to N+1: one `GROUP BY` over `recipe_id IN (<recipeIds>)` covers the whole page. A `GROUP BY`
   yields no row for an empty group, so an id the caller asked about that nobody has favourited is
   simply **absent** — never zero-filled. Callers must default a missing id to
   `favoriteCount = 0, favoritedByMe = false`. The fake omits them too, on purpose: zero-filling in
   the fake would hide a missing default in the service. An empty `recipeIds` short-circuits to an
   empty list before touching the database, because an empty `IN ()` is a SQL syntax error.
   The count is `COUNT(*)::int` — a bare `COUNT(*)` is `bigint` and `rs.getInt` would narrow it
   silently — and the flag is `BOOL_OR(user_id = :userId)`.

The client does **not** check whether a recipe is visible to the user before favouriting it. That
rule (published, or your own draft) lives in the service layer's `RecipeVisibility`.

## Architecture
- **Facade pattern:** `JdbiRecipeClient` delegates to individual operation classes
- **Validation classes:** 1:1 with operations in `internal/validations/`. The four favourite
  validators are default validators — nothing about a favourite is checkable from the param alone
- **Parameter objects:** All methods take dedicated data class params
- **Row adapters:** `RecipeRowAdapter`, `RecipeIngredientRowAdapter`, `RecipeFavoriteRowAdapter`,
  `RecipeFavoriteSummaryRowAdapter`

## Error Handling
- Returns `Result<T, AppError>` — never throws for expected failures
- `NotFoundError` for missing entities, `ConflictError` for duplicate `web_link`
- Favourite mutations never return either: both are idempotent

## Gradle Module
`:clients:recipe-client`
