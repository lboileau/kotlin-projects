# Meal Plan Client — AI Context

## Overview
JDBI data access client for meal plans, meal plan days, meal plan recipes, and shopping list purchases.

## Package
`com.acme.clients.mealplanclient`

## Public API

### MealPlanClient (interface)
- **Meal Plans**: create, getById, getByPlanId, getTemplates, getByCreatedBy, getMine, update, delete, duplicate
- **Access**: getAccess (resolves `created_by` + membership in one statement, for the service-layer authorizer)
- **Days**: addDay, getDays, removeDay
- **Recipes**: addRecipe, getRecipesByDayId, getRecipesByMealPlanId, removeRecipe, getMealPlanIdForRecipe, removeRecipeFromPlan, addRecipeToPlanIfAbsent
- **Shopping List Purchases**: getPurchases, upsertPurchase, deletePurchases
- **Shopping List Manual Items**: addManualItem, getManualItems, removeManualItem, updateManualItemPurchase, resetManualItemPurchases
- **Sharing & Members**: getOrCreateBackingPlan (lazy, race-safe), addMember, getMembers, removeMember

All operations return `Result<T, AppError>`.

### Models
- `MealPlan` — meal plan container (template or trip-bound). Every read resolves `recipeCount`, `memberCount` (distinct people with access via the backing plan, not counting the owner), and `ownerName` (owner's username, falling back to email) via subselects/join in the shared `MealPlanSelect.COLUMNS` SQL fragment (`internal/adapters/MealPlanRowAdapter.kt`) — so every caller gets these for free without extra round trips. `ownerName` is resolved with a direct `JOIN`/subselect on the `users` table, the same cross-client-table pattern `item-client` uses for `gearPackName` (LEFT JOIN `gear_packs`).
- `MealPlanDay` — numbered day within a meal plan
- `MealPlanRecipe` — recipe reference placed on a day/meal
- `MealPlanAccess(createdBy, isMember)` — the two facts needed to resolve a caller's role on a meal plan
- `MealPlanMember(userId, createdAt)` — a person with access via the backing plan (its `owner_id` or a `plan_members` row), excluding the meal plan's own owner; username/email enrichment happens at the service layer via `UserClient`, not here
- `MealPlanMemberRemoval` — outcome enum for `removeMember`: `REMOVED`, `NOT_A_MEMBER`, `IS_BACKING_PLAN_OWNER`
- `ShoppingListPurchase` — purchase tracking per ingredient per unit per meal plan

### Sharing — no dedicated schema; reuses `plans` / `plan_members`
Decided 2026-09-19 (see `docs/meal-app-frontend/sharing.md`) after an earlier design using a `meal_plans.share_token` column and a `meal_plan_members` table was rejected — this project takes no new migrations. A meal plan is shared **through a trip plan** that backs it:
- `meal_plans.plan_id` (existing column, nullable, unique per trip) points at a `plans` row. The share token *is* that plan's id (a UUID, shown as a string) — there's no separate token column.
- "Members" of a meal plan are the people with access to its backing plan: that plan's `owner_id` plus every `plan_members.user_id`, minus the meal plan's own `created_by` (who's always separately "the owner"). A meal plan with `plan_id IS NULL` has no members — `member_count = 0` falls out of the same subselect naturally, since both branches of its `UNION` match nothing.
- `getOrCreateBackingPlan` lazily creates the backing plan on first call: it holds a `FOR UPDATE` lock on the meal plan row for the whole check-and-create (same pattern as `AddRecipeToPlanIfAbsent`), so two concurrent first calls serialise on the lock — no orphaned `plans` row is ever created, and both calls return the same id. The new `plans` row (name = meal plan name, owner = meal plan's `created_by`, `visibility = 'private'`) gets its owner inserted into `plan_members` too, mirroring `CreatePlanAction`, so `plan_members`-keyed queries (like `GetPlansByUserId`) also find it.
- `addMember` idempotently inserts into the backing plan's `plan_members` (role `'member'`) — unless the user is already the backing plan's `owner_id`, in which case they have implicit access and nothing is inserted.
- `removeMember` deletes from `plan_members`; removing the backing plan's `owner_id` isn't possible this way (there's no row to delete) and is reported via `MealPlanMemberRemoval.IS_BACKING_PLAN_OWNER` so the service layer can surface `400` instead of a silent no-op.
- Looking a meal plan up by its share token is just `getByPlanId` on the parsed UUID — no separate lookup operation.
- Deleting a meal plan does **not** delete its backing `plans` row (accepted: a backing plan can't be told apart from a real trip once created).
- No reset-token operation — resetting would need a token column, which this design deliberately avoids.

### Factory
- `createMealPlanClient()` — creates a JDBI-backed client using DB env vars

### Fake (testFixtures)
- `FakeMealPlanClient` — in-memory fake for unit testing

## Gradle Module
`:clients:meal-plan-client`
