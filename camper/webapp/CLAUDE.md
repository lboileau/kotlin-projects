# Meal Planner Webapp

Mobile-first meal planning app: recipes, ingredients, meal plans, shopping lists. Written from scratch on the `meal-app-frontend` branch, replacing the old camping UI. Source of truth for product and design decisions: `../docs/meal-app-frontend/requirements.md` and `../docs/meal-app-frontend/plan.md`.

## Tech Stack

- **Framework:** React 19 + TypeScript, Vite 7
- **Routing:** react-router-dom 7 (`createBrowserRouter`, nested routes)
- **Server state:** TanStack Query 5
- **UI:** Radix Themes 3 (light only, accent set in `src/theme.ts`), Radix `Dialog` primitive for bottom sheets, `@radix-ui/react-icons`
- **Live sync:** `@stomp/stompjs` over `/ws`, topic `/topic/meal-plans/{mealPlanId}`
- **Styling:** Radix props and tokens first; co-located CSS for custom parts. Custom CSS uses the generic `--accent-*` / `--gray-*` tokens, never a named colour scale or hard-coded colours.

## Structure

```
src/
  main.tsx, router.tsx, theme.ts
  api/         http.ts (request + ApiError), queryClient.ts, types.ts, one file per domain
  auth/        AuthProvider, useAuth, RequireAuth, storage
  queries/     TanStack Query hooks and keys: plans, shopping, recipes, ingredients
  sync/        SyncProvider (one STOMP client), useMealPlanSync
  components/  AppShell, TabBar, PageHeader, Sheet, useSheetContainer, SheetSelectContent,
               SheetLink, useSheet, Toast, Stepper, BottomBar, IngredientPicker,
               QueryErrorState, RouteFallback, RecipesIngredientsToggle
  lib/         pure helpers: flatPlan, shoppingRows, parseQuantity, formatQuantity,
               ingredientConstants, normalizeUrl, asyncPool, historyIndex, safeNext, selectedPlan,
               toastStore, usePageTitle, mealPlanSummary
  pages/       sign-in, account, plans, shopping, recipes, ingredients (each area has an
               index.ts barrel that is one lazy chunk)
  styles/      global.css (reset and body only)
```

## Conventions

- **Every screen state has a URL.** Sheets are child routes rendered through the parent page's `<Outlet/>`. Never open a sheet or dialog from local state.
- **Sheets** use `components/Sheet` and close through `useSheet(parentPath, options?)`, which returns `{ sheetProps, close }` — spread `sheetProps` onto `<Sheet>`, call `close()` (goes back when there is in-app history, replaces to the parent otherwise) or `close({ to, replace })` to land somewhere else after a mutation. `canClose`/`onBlockedClose` options let a sheet refuse to close while async work is in flight. Content portalled outside the app tree must be wrapped in a nested `<Theme>` or the Radix tokens do not apply.
- **API calls** go through `api/http.ts`. Callers only ever see `ApiError { status, code, message }` (network failure is `status 0`, code `NETWORK`). Branch on status or code, never on message text.
- **Errors** surface through the global mutation error toast. Optimistic mutations that show their own message set `meta: { suppressErrorToast: true }`.
- **Optimistic updates** for predictable actions (check-off, quick add, add or remove recipe from a plan, rename, servings): edit the cache, roll back on error, invalidate on settle. Creating or importing recipes, creating ingredients and publishing wait for the server.
- **Identity** is the `X-User-Id` header, read from auth storage on each request. All localStorage access is wrapped in try/catch.
- **Mobile first:** design for 360 to 430px, no horizontal scroll, 44px touch targets, safe-area insets. Desktop centres a max-width column.
- **Types:** `verbatimModuleSyntax` is on, so type-only imports use `import type`.
- **Tests:** none are being written for this rewrite. The existing `lib/mealPlanSummary.test.ts` must keep passing.

## Non-obvious decisions

Things a reasonable refactor would undo by accident. Each is deliberate; read the why before changing it.

- **Shopping check-off is serialized and coalesced per row, keyed by `${planId}:${row.key}`** (`queries/shopping.ts`, `rowChains` / `rowLatestChecked`, `chainKey`). Two PATCHes for one row can reach the server out of order (check then uncheck quickly). Each row's sends are chained, and a superseded tap sends nothing, so only the latest desired state goes out. The optimistic write in `onMutate` keeps the UI instant; the chain is only about network ordering. The plan id is part of the key because `row.key` alone is `ingredient-{id}` / `manual-{id}`, and ingredients are a global table — the same ingredient can appear in two different plans' shopping lists, so a row-only key let a toggle in one plan coalesce with a toggle of the same ingredient in another.
- **`isMutating(...) === 1`, not `=== 0`, gates the settle-time invalidation** (`invalidateIfLast` in `queries/shopping.ts`, and the check in `sync/useMealPlanSync.ts`). When a mutation's `onSettled` runs, TanStack Query still counts that mutation as in flight, so "I am the last one" is `1`.
- **Rollbacks are targeted and applied to the current cache, never a whole-list snapshot** (`queries/shopping.ts`, `queries/plans.ts`). Restoring a pre-mutation snapshot would also wipe a different concurrent mutation that already succeeded.
- **Adding, removing and duplicating a plan's recipes are server-side, plan-level operations** (`POST /api/meal-plans/{id}/recipes`, `DELETE /api/meal-plans/{id}/recipes/{recipeId}`, `POST /api/meal-plans/{id}/duplicate`). The server picks the day (creating day 1 lazily on first add), is concurrency-safe, and removal is idempotent — the frontend no longer resolves days, retries on 409, or drives per-recipe duplicate progress itself. `addRecipeToPlan`'s `201` vs `200` status (not the body shape) is how the frontend tells "created" apart from "already in the plan" — see `api/http.ts`'s `requestWithStatus`.
- **Optimistic rows with `temp-` ids are not actionable, for two different reasons.** Shopping (`ShoppingRowItem` `isTempRow`): the row has no real id yet to PATCH by. Plan recipes (`PlanDetailPage.tsx` `isPendingOnly`): the remove endpoint targets `recipeId`, which IS known immediately, so this isn't about having a real id — it guards against a DELETE racing ahead of the still-in-flight add POST, landing first as a no-op, only for the POST to then add the recipe back.
- **Plan servings: 400ms debounce, flushed on unmount, override cleared only when nothing newer is queued** (`PlanDetailPage.tsx`). Displayed value is `override ?? server value`, with no effect-based sync.
- **Shopping rows never reorder by checked state** (`lib/shoppingRows.ts` `buildShoppingRows`) — sorted by name only, by design, so nothing shifts position mid-shop just from tapping it. (An earlier version sank checked/no-longer-needed rows to the bottom of their category, with a "settle pin" to stop the row sliding into a just-toggled row's place from absorbing the next tap — removed along with the reordering it existed to smooth over.)
- **`ShoppingPage` splits into an outer component and a `<ShoppingListBody key={planId} .../>`** (`ShoppingPage.tsx`). The route (`/plans/:planId/shopping`) doesn't remount when `SwitchPlanSheet` navigates from one plan's list to another's — same route, just a new `:planId` param — so anything held as component state (the quick-add draft, the reset-confirm dialog, each mutation's own `isPending`) would otherwise carry over from the previous plan. Keying the inner component on `planId` remounts all of that state fresh on every plan switch; the outer component (data fetching, `notFound`/error/loading branches, `<Outlet/>`) stays unkeyed since queries already key themselves off `planId`.
- **Live sync** (`sync/SyncProvider.tsx`, `sync/useMealPlanSync.ts`): one STOMP client, mounted in `AppShell` while signed in. Topic subscriptions are reference counted. On reconnect every subscribed topic gets a synthetic `null` message so handlers refetch. A message arriving while a mutation for that plan is in flight is deferred (300ms recheck), so the user's own echo never flickers their optimistic rows.
- **Sheets close by history index, not `location.key`** (`lib/historyIndex.ts`, `components/useSheet.ts`). Any navigation, including `replace`, mints a fresh key, so a sign-in redirect chain looks like in-app navigation. `window.history.state.idx` survives `replace` and reloads.
- **`useSheet(parentPath, options?)` is the only way to close a sheet.** `close()` goes back or replaces to the parent; `close({ to, replace })` closes then lands elsewhere; `canClose` / `onBlockedClose` refuse closing during async work (`ImportRecipeSheet`). The options are read through a ref synced in a no-deps effect because refs cannot be written during render.
- **`Dialog.Portal` content is wrapped in a nested `<Theme hasBackground={false}>`** (`components/Sheet.tsx`). The portal renders into `document.body`, outside the root theme's DOM subtree, so tokens would not apply otherwise. Radix Themes' own `AlertDialog` and `DropdownMenu` handle this themselves.
- **The root `<Theme>` in `main.tsx` traps ordinary z-index comparisons.** Radix Themes gives `[data-is-root-theme='true']` `position: relative; z-index: 0`, which makes it a stacking context — any normal descendant's z-index, however high, is only ever compared to its siblings *inside* that context, never against something outside it. Anything portalled to `document.body` (a Sheet, an `AlertDialog`, a `Select`/`DropdownMenu`) escapes the root theme entirely and always wins a stacking comparison against ordinary page content, regardless of the page content's own z-index. `components/Toast.tsx`'s `ToastRegion` used to be ordinary page content for exactly this reason: it rendered behind an open Sheet despite `z-index: 1000`, because that 1000 never left the root theme's z-index: 0 context. It's now portalled to `document.body` (nested `<Theme hasBackground={false}>`, same as Sheet) so its z-index is compared in the same un-trapped tier as everything else that's portalled.
- **A `Select`/`DropdownMenu` opened from inside a sheet must portal INTO the sheet, not to `document.body`** (`components/useSheetContainer.ts`, `components/SheetSelectContent.tsx`; `Sheet.tsx` provides the context but keeps its own file free of anything but the `Sheet` component itself, for Fast Refresh). The Dialog's scroll lock (`react-remove-scroll`) only treats wheel/touch-move as "inside the dialog" within the sheet's own content node (its `shards` target) — a `Select` portalled past it to `document.body` reads as scrolling the page behind the sheet, which works for a moment and then gets locked. `SheetSelectContent` is a drop-in replacement for `<Select.Content>` that reads the container from context — but it (or whatever calls `useSheetContainer()`) must be rendered as a genuine JSX descendant of `<Sheet>`, e.g. inside a component a page renders as `<Sheet>`'s child (`IngredientPicker`) or directly inside the page's own `return <Sheet>...</Sheet>`. Calling `useSheetContainer()` from the same component instance that *creates* the `<Sheet>` element returns the context's default (`null`, portals to `document.body` as before) — that component's own position in the tree is above the sheet, not inside it.
- **Full z-index layering order, lowest to highest** (`styles/global.css`): page content < docked bars (`components/BottomBar.tsx`, `z-index: 5`) < sheet overlay (`100`) / sheet content (`101`) < popups and alert dialogs (`200`) < toasts (`1000`, always topmost — an error or an Undo action must survive anything else being open). The `200` tier is one global rule covering the whole family by shared attribute/class rather than enumerating components: `[data-radix-popper-content-wrapper]` (every `@radix-ui/react-popper`-based popup — `Select`, `DropdownMenu`, anything else built on it) plus `.rt-BaseDialogOverlay`/`.rt-BaseDialogContent` (`AlertDialog`, and Radix Themes' own `Dialog` if ever used — both built on the same internal base). It's a fallback for whatever isn't (or can't be) portalled into a sheet via `useSheetContainer` — chiefly `AlertDialog`, which has no container-portalling mechanism of its own. The popper wrapper rule needs `!important`: `@radix-ui/react-popper`'s `Content` mirrors its own computed z-index onto that wrapper as an inline style on every render (reads `.rt-SelectContent` etc., `auto` by default here), and a plain rule can't out-rank an inline style regardless of value — the one place in the app fighting a library-managed inline style rather than another stylesheet rule, which is the legitimate case for `!important`. `AlertDialog`/`Dialog` have no such mechanism, so their rule doesn't need it.
- **Radix CSS is imported modularly** (`main.tsx`): base tokens plus only the colour scales in use (violet, purple, iris, mauve, red, green, amber). A new `color="…"` anywhere needs a matching `tokens/colors/<name>.css` import or it silently renders wrong.
- **Private plans and sharing** (see `../docs/meal-app-frontend/sharing.md` — source of truth for the API contract): a plan detail or shopping-list fetch that comes back `403` is handled exactly like `404` — `usePlan`/`useShoppingList` stop retrying on it, and `PlanDetailPage`/`ShoppingPage` derive a `forbidden` flag the same way they derive `notFound` (checked first, before the "keep showing stale cached data" branch, so a background refetch's `403` — e.g. after the `members` sync event fires because the owner just removed this user — wins over whatever was cached). Both clear the remembered selected-plan id when it matches. The share link (`useShareLink`, `queries/plans.ts`) is deliberately NOT fetched just by opening the edit sheet — `enabled: false`, fetched only via `refetch()` when the Share button is actually tapped, since the first fetch is what creates the link server-side. The `/join/:token` route lives inside the guarded route tree (under `RequireAuth`/`AppShell`), not beside `/sign-in`, so a signed-out visitor is redirected to sign in and comes back to accept automatically — see `JoinPlanPage`'s `attemptedRef` for how it accepts exactly once despite StrictMode's double-invoke. A plan's `role` always comes from the API (`MealPlanResponse.role`/`MealPlanDetailResponse.role`) — never derived by comparing `createdBy`/`userId` on the client, which would be wrong for a member.
- **Code splitting is per area, not per route.** Each `pages/{area}/index.ts` barrel is one lazy chunk, so opening a sheet never waits on the network. `routePrefetch.ts` idle-prefetches the other tab areas once from `AppShell`.
- **Route-change focus relies on effect ordering** (`AppShell.tsx`): it focuses `<main>` only when nothing has claimed focus, and a sheet's autofocused input always claims it first because a descendant's mount focus precedes an ancestor's effect.
- **Two toast live regions are always mounted** (`components/Toast.tsx`), polite for info and assertive for errors, because some screen readers only announce changes to a region that already existed.
- **A new toast replaces any other of the same tone, rather than stacking** (`lib/toastStore.ts` `pushToast`). A rapid flow (e.g. adding several ingredients back to back in `NewIngredientSheet`, now that success is toast-only with no "added this session" list) can fire one info toast every second or two — without this, they'd pile up several deep before any expired. Info and error replace independently, matching the two separate live regions.
- **Quantities go through `lib/parseQuantity.ts`** ("1.5", "1,5", "1/2", "1 1/2", "1½"). Never `parseFloat`: it reads "1/2" as 1. Quantity fields use `inputmode="text"` because the iOS decimal pad has no slash.
- **Scraper suggestions are normalised** (`normalizeCategory` / `normalizeUnit` in `lib/ingredientConstants.ts`) before any ingredient create; the database rejects values outside its fixed lists.
- **`createOrFindIngredient` force-fetches the ingredient list and matches names case-insensitively before creating** (`queries/ingredients.ts`). The database's unique constraint is case-sensitive, so "butter" beside "Butter" raises no 409.
- **`IngredientPicker` reads `value` / `initialQuery` only on mount.** Remount it with a changing `key` to reset it. Options select on `pointerdown` with `preventDefault` so the input's blur cannot swallow the tap. Enter inside the picker never submits the surrounding form.

## Browser verification status

Checked by hand in desktop Chrome at a narrow width (2026-09-19): sign-in redirect with `next`, registration, create plan, create recipe with inline ingredient create and back-to-back line entry, add to plan, Shopping tab redirect, rapid quick add, check-off, two-tab live sync, browser Back closing a sheet, a plan deleted elsewhere switching open tabs to not found, no console errors.

Not yet checked by hand: recipe import (the backend needs `ANTHROPIC_API_KEY`), draft review and Accept all, blocked close and Back during an import, duplicate and delete plan from the edit sheet, STOMP reconnect, reduced-motion sheet close, the timing constants (400ms, 600ms, 180ms, 300ms), and a real phone (iOS keyboard, zoom on focus, safe areas).

## Running

```bash
npm run dev     # http://localhost:3000, proxies /api and /ws to localhost:8080
npm run build   # authoritative type-check gate (tsc -b + vite build)
npm run lint
npm run test
```

Point the dev proxy at a different backend with `VITE_API_TARGET=http://localhost:8081 npm run dev` (`vite.config.ts`); it defaults to `localhost:8080` when unset.
