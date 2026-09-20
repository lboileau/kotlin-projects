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
  components/  AppShell, TabBar, PageHeader, Sheet, SheetLink, useSheet, Toast, Placeholder
  lib/         historyIndex, safeNext, selectedPlan, toastStore, mealPlanSummary
  pages/       sign-in, account, plans, shopping, recipes, ingredients
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

## Running

```bash
npm run dev     # http://localhost:3000, proxies /api and /ws to localhost:8080
npm run build   # authoritative type-check gate (tsc -b + vite build)
npm run lint
npm run test
```
