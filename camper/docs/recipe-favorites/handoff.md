# Orchestrator Handoff

## Workflow
feature-build

## Project Path
/Users/louisboileau/Development/kotlin-projects/camper

## Feature Name
recipe-favorites

## Plan
to be created by architect

## Feature Description

People can favourite a recipe. A recipe shows how many people have favourited it, and tapping that count shows their names. The recipe list gets two new filters: **Favourites** (every recipe that anyone has favourited) and **My favourites** (recipes the signed-in user has favourited).

Full stack: one new table, recipe-client + camper-service changes, and webapp changes. Base: `main` — PR #315 (`meal-app-frontend`) was merged as `2f14042` while this build was under way; the stack was started on `meal-app-frontend` @ `14347d3` (an ancestor of `main`, identical tree) and is to be restacked onto `main`.

**Spelling:** "favourite" (British) in everything the user reads; `favorite` (American) in code, table/column names, API paths and JSON fields.

### Decisions already made by the owner (do not re-open)

> **Amended 2026-09-21, mid-build, by the owner after seeing the first design — and amended a second time the same day (decisions 2 and 3, marked below) after the owner saw the reworked UI running.** Decisions 1–3 below replace the originals (a second chip row `All · Mine · ♥ Favourites · ♥ My favourites`; a tappable heart on every list row; a count-only "N people ›" link on the recipe page). Backend, database, client and API are unchanged by the amendment. The first webapp pass was built to the original decisions and must be reworked to these.

1. **Filter — a dropdown, not chips.** The recipe list's lone `Mine` switch is *replaced* by a single dropdown on the LEFT of the list's actions row (`.recipes-page__actions-row`), inline with Import and New: `All recipes` (default) · `Mine` · `Favourites` · `My favourites`. Single choice. It combines with the meal chips and the search box (all three AND together). The meal chip row stays exactly as it is. The owner rejected a second chip row: it took too much vertical space.
2. **Favourite button — recipe page only; the count is a pill on the row's TITLE line.** No tappable heart on list rows; rows keep their single ＋ action. Each row shows a **read-only** favourite count, only when `favoriteCount > 0`, as a small pill **on the title line, immediately after the recipe name** (the name truncates first; the ＋ stays at the far right). "Serves N" and the tags stay on the secondary line exactly as they were. The pill is **solid accent with a white heart when the viewer has favourited the recipe, soft accent (`--accent-3` / `--accent-11`) when only other people have** — so the viewer's own favourites stand out down the list. Not a button, not focusable, no pointer handlers; `role="img"` with accessible text "3 favourites" / "3 favourites, including you" (`favouritesCountLabel`). No list animation. *Second amendment, 2026-09-21: the owner first had the count in the secondary line beside "Serves", found that "the hearts, serves, and tags … are all competing for the same space", asked for the heart "on the same line as the title" and "more… exciting", briefly asked to drop "Serves", then corrected that: "keep the serves where it is — just move the heart count". Built directly by the team lead; the owner saw it on the dev server and approved it.* The owner called the row count "critical" — it is a requirement.
3. **Recipe page — a bold heart across from the title, and WHO inline.**
   - The heart toggle sits **right-justified across from the recipe title**, aligned to the title's first line. `components/PageHero.tsx` gets an optional `action?: ReactNode` rendered in a flex row with the `<h1>` (`flex: 1; min-width: 0`, the title still WRAPS as it does today — never a one-line ellipsis); other `PageHero` call sites must render identically without it. The title must not shift sideways when the heart appears after loading.
   - The toggle is `pages/recipes/FavouriteButton.tsx`, NOT an icon-only `IconButton`. *Second amendment, 2026-09-21 — the owner, on seeing the icon-only `HeartIcon` / `HeartFilledIcon` version this decision originally specified: "we need the favourited state within the recipe to be more obvious — the colour style between favourited and not is too subtle. maybe even a cooler looking heart on top of the colour change and a border".* So: a 44px round button; **not favourited** = panel background, visible 1.5px `--gray-7` border, OUTLINE heart in `--gray-11`; **favourited** = SOLID `--accent-9` disc, white filled heart, soft glow ring (`box-shadow: 0 0 0 4px var(--accent-4)`). The heart is the app's own glyph (`components/HeartGlyph.tsx`, ~22px, one shape for both states), also used small and filled in the who-line and the row pill, so a filled accent heart means "favourited" everywhere. Accent only, no red. On the tap that turns it ON, the heart pops and a ring bursts outward (CSS keyframes; guarded by click-set state cleared on `animationend`, so neither loading a favourited recipe nor a refetch replays it; none under `prefers-reduced-motion`). `aria-pressed`, aria-label "Favourite" / "Remove from favourites", `:focus-visible` outline offset so it shows against the solid fill. Built directly by the team lead; the owner saw both states on the dev server and approved them. Do not simplify it back to an icon swap.
   - Under the title, in the hero's meta area, one line that is the `SheetLink` to `/recipes/:id/favourites`: **count plus a truncated who**, e.g. `♥ 3 · You, Alice and 1 other ›`. At most 2 names then "and N other(s)"; the viewer, if they favourited it, comes first as "You"; 1 → "Alice" / "You"; 2 → "Alice and Bob" / "You and Alice"; count 0 → the line is absent (bare heart only). One line, ellipsis on overflow (usernames fall back to emails, which are long). Tapping it opens the full list sheet.
   - Names come from the existing `GET /api/recipes/{id}/favorites` through a `useRecipeFavorites(recipeId)` query shared with the sheet, enabled only when `favoriteCount > 0`; while it loads or if it fails, the line falls back to the count alone ("3 people ›"). Tapping the heart must add/remove "You" **immediately** (optimistic edit of that query's cache with the same guarded apply/rollback, invalidate on settle), not after a refetch.
   - The wording lives in a pure function (`formatFavouritedBy` in `lib/recipeFavorites.ts`) with tests: viewer-first ordering, singular/plural "other", count larger than the names returned, no names with count > 0 → count-only.
   - *(Also part of this feature, by the owner's request: the Ingredients list gets the same filter chips as the Recipes list — `components/FilterChips`, `?category=`, only categories that have ingredients. And the whole feature ships as ONE pull request against `main`; the local per-step branches are a development convenience.)*
4. **Who can favourite** — any user, any recipe they can see (published recipes, plus their own drafts). Names are visible to everyone who can see the recipe; recipes are one shared library.
5. **No live sync.** Recipes have no STOMP topic today (only `/topic/plans/{id}` and `/topic/meal-plans/{id}` exist); do not add one. Counts refresh on refetch / window focus.
6. **Optimistic toggle** with targeted rollback, per `webapp/CLAUDE.md` "Optimistic updates". The toggle is only fired from the recipe page now, but it still writes BOTH the list cache and the detail cache (the list shows the count and both filters read from it). Rapid double taps are still serialised and coalesced per recipe id.

## Entities

### RecipeFavorite (new)
- `id` UUID PK
- `recipeId` UUID → recipes.id (cascade delete)
- `userId` UUID → users.id (cascade delete)
- `createdAt` timestamp
- Unique on (recipeId, userId): one favourite per person per recipe.

### Recipe (existing — read model extended)
Service responses gain two computed fields (not stored on `recipes`):
- `favoriteCount: Int`
- `favoritedByMe: Boolean` (relative to the `X-User-Id` caller)

### User (existing, unchanged)
Names come from `users.username`, falling back to `users.email` — the same rule plan members use: `services/camper-service/src/main/kotlin/com/acme/services/camperservice/features/mealplan/actions/GetMembersAction.kt:69` (`result.value.username ?: result.value.email`), reading through `UserClient.getById` (`clients/user-client/src/main/kotlin/com/acme/clients/userclient/api/UserClient.kt:15`). `username` is nullable: `databases/camper-db/migrations/V002__create_users.sql`.

## API Surface

Identity is the `X-User-Id` header on every recipe endpoint (`features/recipe/controller/RecipeController.kt:28`, `@RequestHeader("X-User-Id") userId: UUID`). Base path `/api/recipes` (`RecipeController.kt:20`).

### New
| Method | Path | Behaviour |
|---|---|---|
| `PUT` | `/api/recipes/{id}/favorite` | Favourite as the caller. Idempotent (already favourited → still 200). Returns `{ "recipeId", "favoriteCount", "favoritedByMe": true }`. 404 if the recipe doesn't exist or isn't visible to the caller (someone else's draft). |
| `DELETE` | `/api/recipes/{id}/favorite` | Un-favourite as the caller. Idempotent. Returns `{ "recipeId", "favoriteCount", "favoritedByMe": false }`. |
| `GET` | `/api/recipes/{id}/favorites` | Who favourited it, oldest first: `[{ "userId", "username", "favoritedAt" }]`, `username` = username falling back to email. 404 as above. |

### Changed
- `GET /api/recipes` — each `RecipeResponse` gains `favoriteCount` and `favoritedByMe`. **Must not be N+1**: `ListRecipesAction` (`features/recipe/actions/ListRecipesAction.kt`) currently does two `recipeClient.getAll` calls; add one batched client read for counts + the caller's favourites across the returned recipe ids, not a query per recipe.
- `GET /api/recipes/{id}` — `RecipeDetailResponse` gains the same two fields.
- DTOs: `features/recipe/dto/RecipeResponses.kt`; mapping: `features/recipe/mapper/RecipeMapper.kt:22` (`toRecipeResponse`) and `:58` (`toRecipeDetailResponse`).
- Other endpoints that return a `RecipeResponse`/`RecipeDetailResponse` (create, update, import, publish, resolve-duplicate) must still compile and return sensible values (a new recipe is `0` / `false`).

The filters are **client-side**, like the existing `mine` and `meal` filters (`webapp/src/pages/recipes/RecipesPage.tsx`, the `filtered` memo): `Favourites` = `favoriteCount > 0`, `My favourites` = `favoritedByMe`. No filter query params on the API.

## Database Changes

New migration `databases/camper-db/migrations/V044__create_recipe_favorites.sql` (latest existing is `V043__create_ladder_votes.sql`; migrations in that folder are the single source of truth and Flyway applies them on startup):

```sql
CREATE TABLE IF NOT EXISTS recipe_favorites (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id  UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_recipe_favorites_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_favorites_user   FOREIGN KEY (user_id)   REFERENCES users (id)   ON DELETE CASCADE,
    CONSTRAINT uq_recipe_favorites_recipe_user UNIQUE (recipe_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_recipe_favorites_user_id ON recipe_favorites (user_id);
```

(The unique constraint already indexes `recipe_id`-leading lookups.) Follow the style of `V014__create_recipes.sql`. No `updated_at`: rows are only inserted and deleted. Optionally add a few favourites to `databases/camper-db/seed/dev_seed.sql` so the filters have something to show locally.

## Special Considerations

### Backend
- **Client:** extend `recipe-client` rather than adding a new client module (same aggregate, same DB). `RecipeClient` interface: `clients/recipe-client/src/main/kotlin/com/acme/clients/recipeclient/api/RecipeClient.kt`; one operation class per method under `internal/operations/`; params in `api/RecipeClientParams.kt`; **`FakeRecipeClient`** (`clients/recipe-client/src/testFixtures/kotlin/com/acme/clients/recipeclient/fake/FakeRecipeClient.kt`) must implement every new method. Suggested methods: `addFavorite`, `removeFavorite` (both idempotent — `ON CONFLICT DO NOTHING` / delete-0-rows is success), `getFavorites(recipeId)`, and a batched `getFavoriteSummaries(recipeIds, userId)` → count + favoritedByMe per recipe. Architect may rename.
- **Service:** actions + 1:1 validations per the service pattern (`features/recipe/actions/`, `RecipeService` facade, `RecipeError`). Visibility rule for all three new endpoints mirrors what the list/detail already enforce: published, or created by the caller.
- **Errors:** `Result<T, E>`, never throw for expected failures.
- **Tests:** unit tests with `FakeRecipeClient` (`services/camper-service/src/test/kotlin/.../features/recipe/RecipeServiceTest.kt` exists), client integration tests, acceptance tests under `.../features/recipe/acceptance/`. Cover: idempotent favourite/un-favourite, count across several users, `favoritedByMe` differs per caller, names fall back to email, cascade on recipe delete, 404 on another user's draft, list endpoint returns the fields.

### Frontend (read `webapp/CLAUDE.md` first — its conventions are binding)
- **API + queries:** `webapp/src/api/recipes.ts` (types + calls), `webapp/src/queries/recipes.ts` (`recipesKey` = `['recipes']` at `:22`, `recipeKey(id)` at `:23`). New `useToggleFavorite` mutation: optimistic write to BOTH the list cache and the detail cache (count ±1, flip `favoritedByMe`), **targeted rollback on the current cache, never a whole-list snapshot**, invalidate on settle. Rapid double-taps on one heart must not land out of order — reuse the serialise-and-coalesce idea from `queries/shopping.ts` (`rowChains`) or disable while in flight; architect's choice, but state it in the plan.
- **List (`pages/recipes/RecipesPage.tsx`):** replace the `Mine` switch (`searchParams.get('mine')`) with the dropdown of decision 1 — Radix Themes `Select`, size 3 to match the buttons, soft/quiet, `aria-label="Show"`; the page is not inside a Sheet, so plain `Select.Content`. URL param: a single `show` param (`mine` | `favourites` | `my-favourites`, absent = all), patched through the existing `updateParams` helper, included in `hasFilters` / `clearFilters`. Keeping the old `?mine=1` working is NOT required. Must fit at 360px with no horizontal page scroll: the trigger shrinks (`min-width: 0`, ellipsis on its value) rather than pushing Import / New off the row — check it at 360px with "My favourites" selected.
- **List row:** the read-only `♥ N` of decision 2. No heart button on rows.
- **Recipe page (`pages/recipes/RecipeDetailPage.tsx`, `components/PageHero.tsx`):** decision 3. The who-line is a `SheetLink` to a child route `/recipes/:id/favourites` rendering a `FavouritedBySheet` (uses `components/Sheet` + `useSheet('/recipes/:id')`; **every sheet is a URL, never local state**). Register it in `src/router.tsx` beside the recipe page's other children and export it from the `pages/recipes/index.ts` barrel.
- **Icons/colour:** `@radix-ui/react-icons` has `HeartIcon` and `HeartFilledIcon`. Colour with tokens only (`--accent-*` or `--red-*`; red is already imported in `main.tsx` — a *new* colour scale would need its own `tokens/colors/<name>.css` import).
- **Tests:** pure-function Vitest only, in `lib/`. Put the filter predicate and the optimistic cache edits (apply / rollback for list + detail) in a pure `lib/` module with a `.test.ts` beside it; there is deliberately no DOM test runner.
- **Docs:** add a "Non-obvious decisions" entry to `webapp/CLAUDE.md` for the dropdown replacing `Mine`, the optimistic favourite toggle, and the fact that a second chip row and a per-row heart button were both designed first and rejected by the owner (chips took too much space; favouriting belongs on the recipe page) — so a later refactor doesn't reintroduce them.
- Gates: `npm run build`, `npm run lint`, `npm run test` in `webapp/`; `./gradlew test` at the project root.

## Notes
- Scope: 1 table, 0 new client modules (recipe-client extended), 3 new endpoints + 2 changed responses — well inside the feature-size limits.
- Existing-feature check: no `favorite`/`favourite` code, table or endpoint exists anywhere in `camper/` (grep across `*.kt`, `*.sql`, `*.ts`, `*.tsx` returned nothing).
- Per the owner's standing feedback: always run code-reviewer and test-reviewer on every PR; dev agents don't run `gt submit` / `git push`; if requirements change mid-build, amend this handoff first; save `retro.md` beside this file in `docs/recipe-favorites/`.
- The dev server holds recipe imports for 5s (`webapp/src/api/recipes.ts`, dev only) — unrelated to this feature, don't remove it.
