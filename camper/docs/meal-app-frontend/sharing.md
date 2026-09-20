# Private meal plans and share links — contract

Decided 2026-09-19. Lands in PR #315. Both backend and frontend are built against this document.

**No database migration.** Sharing reuses the existing trip tables (`plans`, `plan_members`). A meal plan is shared through a trip plan that backs it.

## Rules

- A meal plan is private. A caller can use a plan only as its **owner** (`meal_plans.created_by`) or as a **member**. Anyone else gets `403 FORBIDDEN`. A plan id that does not exist is still `404`.
- Members of a meal plan are the people on its backing trip plan: `meal_plans.plan_id` points at a `plans` row, and that trip's `owner_id` plus every `plan_members.user_id` are members. A meal plan with no `plan_id` is owner-only. Meal plans that already belong to a camping trip are therefore shared with that trip's people, with no backfill.
- One reusable share link per plan. The token in the link is the backing trip plan's id, a random UUID that the app shows nowhere else. Opening the link while signed in makes the caller a member.
- A share link only works for a backing plan that the share flow itself created. Those rows are marked by name: `plans.name = 'meal-plan-share:' || <mealPlanId>`. A meal plan that belongs to a real camping trip cannot be link-shared (`409`), and a real trip's id is never accepted as a token (`404`). Otherwise sharing a meal plan would hand out the whole trip, and anyone who learned a trip id could join it. People on a real trip still have member access to its meal plan, as before.
- A meal plan can only be created on, or copied to, a trip the caller belongs to (`403` otherwise).
- There is no "reset link" (it would need a token column). Known limitation: a removed member who kept the link can rejoin.
- Members can do everything except the owner actions.
- Owner only: rename the plan, delete the plan, remove another member.
- Members can: read the plan and its shopping list, change servings and scaling mode, add and remove recipes, all shopping list actions, duplicate the plan (the copy is theirs alone and unshared), get the share link, see the members, and leave the plan.
- Identity is still the `X-User-Id` header. This stops access through the app and by link; it is not proof against a forged header.
- Templates (`is_template = true`) follow the same rule. `GET /api/meal-plans/templates` returns only the caller's own templates.
- Deleting a meal plan leaves its backing trip plan row behind. Accepted; a backing plan cannot be told apart from a real trip.
- The existing email invite (`POST /api/plans/{tripId}/members`) is not used by the meal app: its email links to the trip, not the meal plan.

## API

All need `X-User-Id`. Errors use the existing `{ code, message }` envelope.

### Changed

- Every existing meal plan endpoint checks access: `GET/PUT/DELETE /api/meal-plans/{id}`, days, per-day recipes, `POST|DELETE /api/meal-plans/{id}/recipes…`, `duplicate`, `save-as-template`, `copy-to-trip`, all `shopping-list` endpoints, and `DELETE /api/meal-plan-recipes/{id}` (check against the owning plan). `403 FORBIDDEN` for a non-member.
- `PUT /api/meal-plans/{id}`: a member may send `servings` and `scalingMode`; any request from a member that includes `name`, changed or not, is `403`.
- `DELETE /api/meal-plans/{id}`: owner only.
- `GET /api/meal-plans?createdBy={id}`: `403` unless `createdBy` equals the caller.
- `GET /api/meal-plans?planId={tripId}`: returns the plan only if the caller has access, else `403`.
- `POST /api/meal-plans` with a `planId`, and `POST /api/meal-plans/{id}/copy-to-trip`: the caller must be the target trip's owner, manager or member, else `403`.
- Child ids are scoped to the plan in the URL: removing a day, adding a recipe to a day, removing a manual shopping item and updating a manual item's purchase all return `404` when the day or item belongs to a different meal plan.
- `MealPlanResponse` and `MealPlanDetailResponse` gain:
  - `role`: `"owner"` or `"member"` (the caller's role)
  - `memberCount`: number of people with access, not counting the owner
  - `ownerName`: the owner's username (or email when there is no username)

### New

| Method and path | Who | Response |
|---|---|---|
| `GET /api/meal-plans/mine` | anyone | `MealPlanResponse[]`: plans the caller owns plus plans shared with them, newest updated first. Includes templates (the frontend filters). |
| `GET /api/meal-plans/{id}/share` | owner or member | `200 { "token": "…" }`. On the first call for an unshared plan, creates a private backing trip plan (marker name, owned by the meal plan's owner) and attaches it, under a row lock so concurrent first calls return the same token. `409 CONFLICT` when the plan belongs to a real trip. |
| `POST /api/meal-plan-invites/{token}/accept` | anyone signed in | `200 { "mealPlanId", "name", "role", "alreadyMember" }`. Idempotent. The owner opening their own link gets `role: "owner", alreadyMember: true`. Unknown token, malformed token, or a real trip's id: `404 NOT_FOUND`. |
| `GET /api/meal-plans/{id}/members` | owner or member | `[{ "userId", "username", "role", "joinedAt" }]`, owner first (`joinedAt` = plan `createdAt`), then members by join time. `username` falls back to email. |
| `DELETE /api/meal-plans/{id}/members/{userId}` | owner for anyone; a member for themselves (leave) | `204`. Removing the meal plan's owner, or a legacy trip's owner: `400 BAD_REQUEST`. A member removing someone else: `403`. Not a member: `204` (idempotent). |

### Events

Published to `/topic/meal-plans/{mealPlanId}`: `{ resource: "members", action: "updated" }` after a join, a removal or a leave. Existing events are unchanged.

## Frontend

- Share link format: `{origin}/join/{token}`. Treat the token as opaque.
- The share link is fetched only when the user taps Share (the first call creates the backing plan).
- Route `/join/:token` inside the signed-in shell (so a signed-out visitor goes through sign-in and comes back): accept, remember the plan as selected, and always end on `/plans/{id}` with a toast ("You joined {name}", or "You already have this plan"), even if the user tapped elsewhere while the join was in flight. Unknown token: a "This link doesn't work" page with a button to Plans.
- Plans home and the Shopping tab's plan chooser use `GET /api/meal-plans/mine`. A shared plan shows a "Shared" badge and the owner's name. An owner's plan with members shows "Shared with N".
- Plan edit sheet: a Share section (copy link; use the native share sheet when `navigator.share` exists; a `409` shows the server's message), the members list, Remove (owner) or Leave plan (member). Rename and Delete are hidden for members. A `400` from remove shows "This person can't be removed".
- A `403` on plan detail or the shopping list shows "You don't have access to this plan" with a button to Plans, and clears the remembered plan if it matches. This also covers being removed while the page is open (the `members` event triggers a refetch).
