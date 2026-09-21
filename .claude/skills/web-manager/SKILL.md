---
name: web-manager
description: Scaffold and manage React + TypeScript web applications in the monorepo. Reference skill for frontend patterns and conventions.
user-invocable: true
---

# Web Application Management

You are a web developer building React + TypeScript frontends in a monorepo. Follow these instructions precisely.

## Tech Stack

- **Framework:** React 19 + TypeScript
- **Build:** Vite 7
- **Routing:** React Router 7 (`createBrowserRouter`, nested routes)
- **Server state:** TanStack Query 5 (caching, optimistic updates, refetch invalidation)
- **UI:** Radix Themes 3 (light only, accent color set in `src/theme.ts`), Radix `Dialog` primitive for sheets, `@radix-ui/react-icons`
- **WebSocket:** @stomp/stompjs for STOMP-over-WebSocket live sync
- **Styling:** Radix props and tokens for semantic components; co-located CSS (CSS Modules / scoped) for custom layout only. **No utility frameworks, no global CSS except reset.**

---

## Project Structure

```
webapp/
├── src/
│   ├── main.tsx, router.tsx, theme.ts
│   ├── api/         http.ts (fetch + ApiError), queryClient.ts, one file per domain (auth, recipes, etc.)
│   ├── auth/        AuthProvider, useAuth, RequireAuth
│   ├── queries/     TanStack Query hooks and keys per domain
│   ├── sync/        SyncProvider (STOMP), useMealPlanSync
│   ├── components/  AppShell, TabBar, PageHeader, Sheet, SheetLink, useSheet, Toast, IngredientPicker, etc.
│   ├── lib/         pure helpers: parseQuantity, flatPlan, ingredientConstants, toastStore, etc.
│   ├── pages/       area folders (sign-in, plans, shopping, recipes, ingredients); each has index.ts barrel (one lazy chunk)
│   └── styles/      global.css (reset and body only)
```

## Key Files

| File | Purpose |
|------|---------|
| `api/http.ts` | Typed fetch wrapper, ApiError definition, X-User-Id injection |
| `api/{domain}.ts` | Domain-specific endpoints (auth, recipes, mealPlans, shopping, etc.) |
| `queries/{domain}.ts` | TanStack Query hooks and query keys for each domain |
| `sync/useMealPlanSync.ts` | STOMP subscription and invalidation logic |
| `auth/AuthProvider.tsx` | User state + localStorage, useAuth hook |
| `theme.ts` | Radix Themes config (accent color, appearance, etc.) |
| `router.tsx` | createBrowserRouter with nested routes; sheets are child routes |
| `components/Sheet.tsx` | Bottom-sheet wrapper around Radix Dialog |
| `components/useSheet.ts` | Hook for closing sheets via history or navigation |
| `lib/historyIndex.ts` | History-index tracking for sheet navigation |

## Conventions

### Every screen state has a URL

- **Pages** render at route-level (full screen)
- **Sheets** are child routes rendered through parent `<Outlet/>`
- Never open a sheet from local state; always navigate to its route
- Sheets close through `useSheet(parentPath)`, which goes back in history or replaces to the parent

### API & Error Handling

1. **All API calls** go through typed functions in `api/{domain}.ts`
2. **ApiError** has `{ status, code, message }` — branch on status or code, never on message text
3. **Network error** is `status: 0, code: 'NETWORK'`
4. **Errors surface** in a global toast (top-right, assertive region) unless `meta: { suppressErrorToast: true }`

### Server State with TanStack Query

1. **Query keys** live in `queries/{domain}.ts` alongside their hooks (e.g., `['plans', 'mine']`, `['recipe', id]`)
2. **Optimistic mutations** edit the cache in `onMutate`, rollback on error, invalidate on settle
3. **Mutations that wait for server** do not optimistically edit
4. **Refetch on focus** is enabled globally (staleTime varies per query)
5. **Deferred invalidation** — if a mutation is in flight when a sync message arrives, defer the invalidation until the mutation settles (300ms recheck)

### Component Patterns

1. **Function components only**
2. **Named exports** — `export function MyComponent()`
3. **Radix Themes for semantic UI** — use `Button`, `TextField`, `Select`, etc. with Radix props instead of custom classes
4. **Custom CSS only for layout** — tab bar, sheet sizing, shopping row layout. Use CSS Modules or co-located scoped CSS.
5. **Tokens, never hard-coded colors** — `var(--accent-9)`, `var(--gray-5)`, etc., matching Radix's generated token names
6. **SVG inline** — all icons and illustrations
7. **Lazy per-area** — each `pages/{area}/` is one chunk; opening a sheet never waits for network

### Sheets

```tsx
import { useSheet } from '../components/useSheet';
import { Sheet } from '../components/Sheet';

export function MySheet() {
  const { sheetProps, close } = useSheet('/parent-path');
  
  const handleSave = async () => {
    // Do work...
    close(); // go back or replace to parent
    // or: close({ to: '/some/path', replace: true })
  };

  return (
    <Sheet {...sheetProps} title="Sheet Title">
      {/* content */}
      <button onClick={close}>Cancel</button>
      <button onClick={handleSave}>Save</button>
    </Sheet>
  );
}
```

Use `canClose` / `onBlockedClose` to refuse closing during async work (e.g., import in progress).

### Radix Themes Setup

- Imported once in `main.tsx` with modular color token CSS (`base.css`, `colors/violet.css`, etc.)
- Accent color set in `src/theme.ts` and configured in `<Theme accentColor="..."/>`
- Nested `<Theme hasBackground={false}>` wraps portal content (`Sheet`, dialogs) so tokens apply outside the root DOM tree
- Custom CSS uses generic tokens: `--accent-1` through `--accent-12`, `--gray-1` through `--gray-12`, red, green, amber (as needed)

### Mobile First

- Design for 360–430px first, then desktop
- No horizontal scroll at any viewport
- 44px+ touch targets
- Safe-area insets in layout (top, bottom)
- Desktop centers content in a max-width column

### Testing

- No tests required for new features (per requirements). Existing tests (e.g., `lib/mealPlanSummary.test.ts`) must keep passing.
- Test environment: `node` for pure function tests, `jsdom` if testing React components
- Type error in a test file fails the build (types are coupled to production)

### Build & Verify

```bash
cd webapp

npm run dev          # http://localhost:3000, proxies /api and /ws to localhost:8080
npm run build        # authoritative type-check gate (tsc -b + vite build)
npm run lint

# Point the dev proxy at a different backend
VITE_API_TARGET=http://localhost:8081 npm run dev
```

**Important:** `npm run build` is the canonical type-check gate, not `tsc --noEmit`. It runs `tsc -b` (enforces `verbatimModuleSyntax`) plus `vite build`.

## Reference

For product and design decisions not covered here, see `../docs/meal-app-frontend/requirements.md`, `../docs/meal-app-frontend/plan.md`, and `webapp/CLAUDE.md` in the codebase. That file also documents non-obvious decisions (e.g., coalesced check-off, history-index sheet closing, portal theming).
