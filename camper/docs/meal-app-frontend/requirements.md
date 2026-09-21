# Meal app frontend — requirements

Status: requirements gathered 2026-09-19. Planning has not started.

## Goal

Replace the current camping webapp with a dedicated, mobile-first meal planning app: recipes, ingredients, meal plans and shopping lists. The frontend is written from scratch. The existing frontend is a reference for what exists, not a design to copy.

## Decisions made

| Topic | Decision |
|---|---|
| Location | The new app replaces `webapp/`. The old camping UI stays in git history. Dockerfile, dev scripts and `WebConfig.kt` stay as they are (npm, `package-lock.json`, `build` script, output to `webapp/dist`). |
| Backend | Unchanged, with two additive exceptions. (1) `GET /api/meal-plans?createdBy={userId}` to list a user's meal plans; `created_by` is already stored and indexed. (2) Live sync: after every successful meal plan mutation (recipes added or removed, check-offs, ad hoc items, purchase reset, rename, servings, delete) publish a `{ resource, action }` message to a new per-meal-plan topic `/topic/meal-plans/{mealPlanId}`, following the `LadderEventPublisher` pattern. |
| Backend changes | Allowed where they clearly help the frontend (decided after the first rounds, which assumed a frozen backend). The plan lists the ones taken and the ones deferred. |
| Live sync | Whole plan. The frontend subscribes to the open plan's topic and refetches what is on screen. The user's own optimistic changes are unaffected. |
| Testing | No new tests, frontend or backend. The type-checked build (`npm run build`) and the existing backend suite are the gates. |
| Cutover | Built on one long-lived branch and merged once at the end. Main stays deployable with the old UI throughout. |
| Look | Light theme only. Clean, simple and crisp. Accent colour: a light pastel purple. |
| Global delete | Accepted as is: any user can delete any recipe or ingredient. |
| UI library | Radix UI Themes. |
| Plan shape | A plan is a flat list of recipes. Stored under day 1 in a single meal slot so the backend is unchanged. |
| Plan ownership | The Plans home lists only plans the signed-in user created. Any plan is readable and editable by link (the backend has no permission checks). |
| Recipe scope | Shared library: everyone's published recipes plus the user's own drafts, filtered client-side. A "my recipes" filter. |
| Ingredients | Managed inside the Recipes tab, not a separate tab. |
| Plan reuse | A Duplicate action, orchestrated client-side with existing endpoints. No template concept in the UI. |
| Offline | Plain responsive web app. No install support, no offline mode. |
| Sign-in | Email-only with the `X-User-Id` header, as the backend provides. Unauthenticated users always land on sign-in first. |

## Defaults taken (not explicitly confirmed)

- The selected plan id is in the URL (`/plans/:id`, `/plans/:id/shopping`). The Shopping tab opens the last-selected plan, remembered per device.
- TanStack Query for caching, optimistic updates with rollback, and refetch on window focus. Live sync messages invalidate the relevant queries.
- The list endpoint also returns existing trip-bound plans with several days and meal types. They are flattened into one recipe list for display; new additions go under day 1. Templates are filtered out of the Plans list.
- The copy-summary action carries over as a Share or Copy action on the plan.
- A recipe appears at most once in a plan; servings handles scaling.
- One servings stepper per plan (the backend has no per-recipe servings).
- Shopping check-off stays all-or-nothing per row.
- Account screen: name and sign out only. Avatars, experience level and dietary restrictions are dropped from the UI.
- Category and unit lists are hardcoded from `V013__create_ingredients.sql` (no endpoint exposes them).

## Product requirements

### Navigation
- Persistent bottom tab bar, iOS style: Recipes, Plans, Shopping.
- Home after sign-in is the user's meal plans.
- Every screen, modal and sheet has a URL. Back, forward, reload and deep links all restore the exact state.

### Plans
- List my plans; create, rename, delete, duplicate.
- Plan detail: see its recipes, add recipes, remove recipes, tap a recipe to open the full recipe.
- Plan-level servings.

### Shopping list
- Shows the list for the selected plan, grouped by category, with progress.
- Check-off is optimistic and feels native; the request runs in the background and rolls back on failure.
- Adding an ad hoc item is type-and-enter. It defaults to category misc, quantity 1 (the backend's free-form mode), and the add field stays open for rapid entry of several items.
- Each item shows which recipes it belongs to (`usedInRecipes`: names only, no ids or per-recipe amounts).
- Remove manual items; reset all purchases.

### Recipes
- List with search and meal/theme filters; detail; create; edit; delete; import from URL; resolve duplicates; publish.
- Creating and importing wait for the server (not optimistic).

### Ingredients — all four flows need redesign
- Review after import: a real picker for existing ingredients, fast bulk accept, clear handling of ingredients with no suggested match.
- Adding while writing a recipe: search, and create-in-place when missing, without leaving the flow.
- Editing ingredients on an existing recipe: quantities, units and ingredient, in the edit flow.
- Managing the ingredient list: create several, rename, recategorise, delete. Deleting an ingredient sends every recipe that uses it back to draft, so it needs a clear warning.
- One shared ingredient picker/creator component replaces today's five separate implementations.

### Interaction principles
- Mobile first: thumb-reachable actions, large touch targets, sheets rather than centred dialogs.
- Optimistic where the outcome is predictable (check-off, add/remove ad hoc item, add/remove recipe from plan, rename). Not optimistic where the server response matters (create or import recipe, create ingredient, publish).
- Fast navigation and streamlined repetitive tasks.

## Known backend constraints to design around

- Recipe and ingredient data is global and unprotected; any user can edit or delete anything. The UI hides edit actions on other people's recipes, but this is cosmetic.
- Deleting a recipe removes it from every meal plan that uses it.
- Free-form shopping items cannot carry a quantity, unit or category. Those need an ingredient-backed item.
- Shopping list rows are keyed by ingredient and unit; one ingredient can produce several rows, and a row's display unit can change as the plan changes.
- `GET /api/recipes` and `GET /api/ingredients` return everything, unpaginated. Search and filtering are client-side.
- Meal plan detail and shopping list endpoints are slow on large plans (N+1 queries). Design loading states for them.
- `GET /api/meal-plans?planId=` returns 200 with a `null` body when nothing matches.
- A standalone non-template meal plan (`planId` null, `isTemplate` false) is allowed by the schema but has no acceptance test, and none will be added. Verify it by hand early in the build.
- No recipe images, structured steps, or cook times exist in the data model.

## Open for planning

- Route map and which screens are sheets versus pages.
- The exact Radix accent scale for the pastel purple, and whether it has enough contrast for buttons and the active tab.
- Build order on the long-lived branch.
