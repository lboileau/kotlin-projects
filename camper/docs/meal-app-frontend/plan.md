# Meal app frontend — plan

Requirements: [requirements.md](requirements.md). This plan covers the route map, screen design, technical foundation, backend additions, and build order.

## 1. Stack

| Concern | Choice | Why |
|---|---|---|
| Framework | React 19 + TypeScript + Vite (kept) | Already the repo's toolchain; Dockerfile and scripts depend on it. |
| Routing | React Router 7, `createBrowserRouter`, nested routes | Sheets are child routes rendered over their parent page, so every state has a URL. |
| Server state | TanStack Query 5 | Cache, optimistic updates with rollback, refetch on focus, invalidation from sync messages. |
| UI | Radix Themes 3, plus the Radix `Dialog` primitive for bottom sheets | Themes has no bottom-sheet component; a styled `Dialog` gives focus trap, scroll lock and escape handling. |
| Icons | `@radix-ui/react-icons` | Matches the theme. |
| Live sync | `@stomp/stompjs` (kept) | Backend is STOMP over `/ws`. |
| Styling | Radix props and tokens first; CSS modules for the few custom parts (tab bar, sheet, shopping row) | No utility framework, no global CSS beyond a reset. |
| Forms | Plain React state | The forms are small; the recipe form's complexity lives in the ingredient picker. |

Removed: every existing page, component, stylesheet, Google Font and camping feature. Kept: `lib/mealPlanSummary.ts` and its existing test, `vite.config.ts` proxy, the tsconfig setup.

## 2. Theme

```tsx
<Theme appearance="light" accentColor="violet" grayColor="mauve" radius="large" scaling="100%">
```

- Pastel comes from using the low steps of the violet scale: page background white, cards and the active tab pill on `violet-2`/`violet-3`, soft buttons and badges (`variant="soft"`) as the default control style.
- Crispness comes from contrast, not colour: text in `mauve-12`, hairline `mauve-5` borders, one `variant="solid"` (`violet-9`) primary action per screen, `violet-11` for the active tab label and links.
- System font stack (Radix default). No custom fonts.
- To approve on the first built screen (sign-in plus Plans home): violet versus `purple` or `iris` as the accent.

## 3. Route map

Pages are full screens. Sheets are child routes that slide up over their parent; closing a sheet goes back in history, or replaces to the parent when opened by deep link.

| Path | Kind | Screen |
|---|---|---|
| `/sign-in` | page | Sign in and register (`?mode=register`). Keeps `?next=` so a shared link lands where it pointed after sign-in. |
| `/plans` | page, tab | My plans. Home. |
| `/plans/new` | sheet | Name and servings, then straight into the new plan. |
| `/plans/:planId` | page | Plan detail: recipes, servings stepper, actions. |
| `/plans/:planId/add` | sheet, full height | Recipe picker. Search, tap to add, stays open for several adds. |
| `/plans/:planId/edit` | sheet | Rename, duplicate, copy summary, delete. |
| `/plans/:planId/shopping` | page, tab target | Shopping list for the plan. |
| `/plans/:planId/shopping/switch` | sheet | Pick a different plan to shop for. |
| `/shopping` | redirect | To the last-selected plan's shopping list, or an empty state that points to Plans. |
| `/recipes` | page, tab | Library. Search and filters live in the query string (`?q=`, `?meal=`, `?mine=1`). |
| `/recipes/new` | page | Create recipe. |
| `/recipes/import` | sheet | Paste a URL. Blocks with progress until the server answers. |
| `/recipes/:recipeId` | page | Recipe detail. For drafts this is also the review screen. |
| `/recipes/:recipeId/edit` | page | Edit recipe fields and ingredients together. |
| `/recipes/:recipeId/lines/new` | sheet | Add an ingredient line. |
| `/recipes/:recipeId/lines/:lineId` | sheet | Edit or resolve one ingredient line. |
| `/recipes/:recipeId/add-to-plan` | sheet | Choose a plan (current plan first). |
| `/ingredients` | page | Ingredient list, reached from a segmented control at the top of the Recipes tab. |
| `/ingredients/new` | sheet | Rapid multi-add. |
| `/ingredients/:ingredientId` | sheet | Edit or delete. |
| `/account` | page | Name, sign out. Reached from the header avatar. |

Shell: a layout route guards everything except `/sign-in`, renders the page in a scroll area, and pins the bottom tab bar (Recipes, Plans, Shopping) with safe-area padding. The tab bar stays visible under pages and is covered by sheets.

Selected plan: opening `/plans/:planId` or its shopping list records that id in localStorage. The Shopping tab links to it. The id in the URL always wins, so shared links and reloads are exact.

## 4. Screens

### Sign in
Email field, one button. If the server answers "registration required" or "not found", the same screen reveals a name field and submits registration (matched on the error code, not message text). On success go to `?next=` or `/plans`. On app load, the stored user is re-fetched once; if the server does not know the id, sign out.

### Plans home
List of my plans (new endpoint, templates filtered out), newest first: name, servings, last updated. A floating primary button opens `/plans/new`. Swipe-free: row tap opens the plan; the plan's own edit sheet holds rename, duplicate and delete.

### Plan detail
- Header: plan name, edit button, a Shop button to the shopping list.
- Servings stepper (optimistic).
- Recipe rows: name, base servings, meal tag. Tap opens `/recipes/:id`. A trailing remove button removes optimistically with an undo toast.
- Add recipes opens the picker sheet: search field focused, published recipes plus my drafts excluded, recipes already in the plan shown as added. Tap adds optimistically and the sheet stays open.
- Storage: a new plan gets day 1 created immediately after the plan. All additions go to day 1 with meal type `dinner`. Existing multi-day plans are flattened for display, de-duplicated by recipe id; removing a recipe removes every occurrence.
- Duplicate: create plan, create day 1, add each recipe. Not optimistic; shows progress and opens the copy.

### Shopping list
- Header: plan name (tap opens the switch sheet), progress "7 of 23".
- Rows grouped by category. Row: large checkbox target, name, quantity (merged across units as today, "2 cups + 1 tbsp"), and a caption line with the recipes it belongs to ("Chili · Stir fry"). Manual items show a remove button.
- Checked items sink to the bottom of their category, dimmed. `no_longer_needed` rows are shown struck through with a clear action.
- Check-off: optimistic cache update, PATCH per unit entry in the background, rollback plus toast on failure.
- Quick add: a text field docked above the tab bar, always present. Type, press enter: the item appears immediately under Misc, the field clears and keeps focus. Sends `{ description }` only. Failure removes the row and restores the text.
- Overflow menu: reset all purchases (confirm).
- Sync: see section 6.

### Recipes library
Search field, meal filter chips, a Mine toggle. Shows published recipes plus my drafts (draft badge). Header actions: New and Import. Segmented control at the top switches to Ingredients.

### Recipe detail
Name, description, servings, source link, tags, ingredient list. Actions: Add to plan, Edit, Delete (owner only, cosmetic). For a draft, a banner states what blocks publishing (duplicate to resolve, N ingredients to review) and the ingredient list becomes the review list below.

### Ingredient flows — one shared picker

`IngredientPicker`: a combobox over the cached full ingredient list. Typing filters by prefix and substring, ignoring case. Rows show name and category. The last row is always "Create 'typed text'", which expands in place to category and unit (pre-filled from import suggestions when present, otherwise `other` and `pieces`), and one confirm. Creating waits for the server, then selects the new ingredient.

1. **Writing a recipe** (`/recipes/new`, `/recipes/:id/edit`): ingredient lines are a list. The add row is quantity, unit, picker. Choosing an ingredient fills the unit from its default and moves focus to quantity; confirming the line returns focus to a fresh picker, so lines can be entered back to back. New recipes send all lines in the single create request. On an existing recipe each line change uses the per-line endpoints.
2. **Editing lines**: tapping a line opens its sheet: quantity, unit, picker, remove. Saves with the resolve endpoint (`SELECT_EXISTING` plus quantity and unit), which the current app already uses on approved lines.
3. **Review after import**: lines split into Needs review and Matched. Each pending line shows the scraped text and a proposed resolution in one row: accept with one tap, or tap the row for the line sheet with the picker. Accept all resolves everything that has a proposal: new ingredients are de-duplicated by name and created first, then all lines resolve in parallel with per-line progress. Lines with no proposal stay and are highlighted. Publish enables when nothing is pending and no duplicate is flagged.
4. **Managing the list** (`/ingredients`): search, grouped by category, tap to edit in a sheet. The new sheet is a rapid-add form: name, with category and unit that stick between entries; enter adds and keeps the form open. Delete warns that every recipe using the ingredient returns to draft.

### Account
Name and sign out.

## 5. Data layer

- `api/http.ts`: fetch wrapper adding `X-User-Id`, throwing a typed `ApiError { status, code, message }`. Handles 204 and the `null` body from the trip lookup.
- `api/{auth,recipes,ingredients,mealPlans,shopping}.ts`: typed functions and types per domain, only for endpoints this app uses.
- `queries/`: query keys and hooks per domain. Key shapes: `['plans','mine']`, `['plan',id]`, `['shopping',id]`, `['recipes']`, `['recipe',id]`, `['ingredients']`.
- Optimistic mutations (cache edit, rollback on error, invalidate on settle): check-off, quick add and remove manual item, add and remove recipe from plan, rename plan, servings, reset purchases.
- Server-confirmed mutations: sign in, create or import recipe, publish, resolve lines, create or edit or delete ingredient, create or duplicate or delete plan.
- Errors surface in a single toast region; no silent catches.

## 6. Live sync

Backend publishes `{ resource, action }` to `/topic/meal-plans/{mealPlanId}`. One STOMP connection for the app, opened by the shell; `useMealPlanSync(planId)` subscribes while a plan detail or shopping page is open. On a message: invalidate `['plan',id]` and `['shopping',id]`. While a local mutation for that plan is in flight, invalidation is deferred until it settles, so a user's own optimistic rows do not flicker. Reconnect triggers one refetch.

## 7. Backend additions

Both follow the existing action and client patterns. No tests are added (per requirements).

1. **List my plans**
   - `meal-plan-client`: new `GetByCreatedBy` operation and param, interface method, fake implementation. `SELECT ... WHERE created_by = :userId ORDER BY updated_at DESC`.
   - Service: `ListMealPlansByCreatorAction`, wired into `MealPlanService`.
   - Controller: `GET /api/meal-plans` with `params = ["createdBy"]`, returning `List<MealPlanResponse>`. The existing `params = ["planId"]` mapping is untouched.
2. **Sync events**
   - `MealPlanEventPublisher` beside `LadderEventPublisher`, one method: `publishUpdate(mealPlanId, resource, action)`.
   - `MealPlanController` publishes after each successful mutation: update, delete, add or remove day, add recipe, purchase update, manual item add or remove, purchase reset.
   - `DELETE /api/meal-plan-recipes/{id}` does not carry the meal plan id, so the remove action must return the owning meal plan id for the controller to publish to.
3. **Recipe links on shopping items**
   - `usedInRecipes` carries names only today. Add `usedInRecipeRefs: [{ id, name }]` alongside it (additive; the calculator's `ShoppingListRow` already sees each contributing recipe). The shopping row's recipe captions become tappable links to `/recipes/:id`, and same-named recipes no longer collapse.
4. **Hide other people's drafts at the source**
   - `ListRecipesAction` passes the caller's id so the query returns published recipes plus the caller's own drafts. The client's `GetAllParam(status, createdBy)` already exists. The frontend filter is then dropped.

The backend may be changed where it clearly helps (decided 2026-09-19). Candidates deliberately left out for now, to revisit if the frontend workaround proves poor in use:
- A server-side duplicate-plan endpoint (the client-side version is several requests and can stop part way).
- A batch resolve endpoint for Accept all after import.
- Ad hoc shopping items with a quantity and unit.
- Per-recipe servings, and per-recipe amounts on shopping items.
- A first-class flat plan, instead of storing everything under day 1.

## 8. Build order

One long-lived branch, `meal-app-frontend`, merged once at the end. Each step is a reviewable commit series on that branch.

1. **Backend additions** (all four in section 7), then start the dev environment and confirm by hand: create a standalone non-template plan, add day 1, add a recipe, fetch the shopping list and see recipe refs, list by creator, list recipes without another user's drafts, see a sync message arrive.
2. **Scaffold**: clear `webapp/src`, dependencies, theme, router and shell, tab bar, sheet primitive, toast, http layer, query client, sign-in and guard. Approve the accent colour here.
3. **Plans**: home, create, detail, recipe picker, remove, servings, edit sheet, duplicate, copy summary.
4. **Shopping**: list, optimistic check-off, quick add, remove, reset, recipe captions, plan switcher, live sync.
5. **Recipes**: library and filters, detail, create and edit with the ingredient picker, delete, add to plan.
6. **Import and review**: import sheet, review list, accept all, duplicate resolution, publish.
7. **Ingredients**: list, rapid add, edit, delete warning.
8. **Finish**: loading skeletons and empty states, keyboard and screen-reader pass on sheets and the tab bar, phone-width pass at 360px, update `CLAUDE.md` files and the web-manager skill, final `npm run build` and `./gradlew build`.

Agents: kotlin-dev for step 1, web-dev for steps 2 to 8, code-reviewer after each step. Test-engineer and test-reviewer are not used, since no tests are being written.

## 9. Risks

- **Pastel accent contrast**: handled by keeping text and primary actions on the high steps of the scale; checked on the first screen.
- **Slow plan and shopping endpoints** (N+1 on the server): cached data renders instantly and refetches in the background; skeletons on first load.
- **Accept all partial failure**: each line reports its own result; failures stay in Needs review with the error.
- **Flattening old multi-day plans**: removing a recipe removes all its occurrences; nothing else about those plans is changed.
