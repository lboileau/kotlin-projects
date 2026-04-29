# Phase 2 Implementation Plan — clean-up-ui

**Plan-gate decisions (settled, 2026-04-29).**
1. **W1 keeps `RecipesPage.tsx` as a single file** containing a nested `<Routes>` for the five sub-paths. Splitting out per-view files is a much bigger refactor for no functional gain — the file is 1824 lines but the per-view JSX blocks are already cleanly delimited by `view === 'xxx'` guards, which become per-route render functions. Simpler wins.
2. **The W4 deferred in-app "View recipe" button bundles into W1** and **opens the recipe in a new tab** (not close-the-modal-and-navigate). This matches Phase 1's external-link UX and preserves meal-plan modal context, per the explicit recommendation in `plan.md` W4. The TODO marker at `MealPlanModal.tsx:817` is the single replacement site.
3. **`?from=mealplan` query param is NOT needed** in Phase 2. The new tab opens fresh, so there is no "back" button to re-style; the source modal remains in the original tab. Marked as deferred / not-needed.
4. **W14 tests use `MemoryRouter`** from `react-router-dom` to render `RecipesPage` (or the extracted list component) with a chosen initial entry. No existing test uses any router — this PR introduces the convention. Documented in W14 below.
5. **W6 hash sync uses direct `window.history.replaceState` calls** with no react-router involvement. react-router-dom v6 `<Routes>` does not consume hash fragments, so a manual `replaceState` does not fight the router. The `useEffect` for hash sync sits next to the existing `activeView` state at `MealPlanModal.tsx:41`.
6. **Ship order is W1 → W14 → W6.** W14 depends on W1's route split (URL becomes the source of truth for list state) but does NOT need the W4 button. The W4 button bundles into W1 because W1 is the smallest delta that makes the route exist, and shipping the dead-route TODO and the button together is one round-trip of review thought. Not reshuffled.

**Scope.** Decompose Phase 2's three workstreams into PR-sized chunks consistent with the patterns Phase 1 established in `webapp/src/`. The global plan (`plan.md`) is the source of truth for *what*; this file specifies *how* and *in what order*.

**Source of truth.** `camper/docs/clean-up-ui/plan.md` (workstreams W1, W14, W6). Phase 1 carry-overs from `progress.md` (specifically: deferred W4 in-app "View recipe" button → bundled into W1).

**Hard constraints (verified).**
- **Backend untouched.** No edits to `services/`, `clients/`, `databases/`, `libs/`. Re-verified against `webapp/src/api/client.ts` — `RecipeResponse`, `MealPlanRecipeDetailResponse.recipeId`, `MealPlanRecipeDetailResponse.recipeWebLink` are already present; nothing new is needed from the BE.
- **Aesthetic.** Reuse `theme.css` tokens. No new colors, fonts, or shared primitives invented.
- **Build gate.** `cd camper/webapp && npx tsc --noEmit && npm run build && npm run test` must pass on every PR.
- **Live updates preserved.** STOMP `usePlanUpdates` / `useLadderUpdates` hooks remain untouched. W1 changes are router-only inside `RecipesPage` (which has no STOMP subscription); W6 only adds hash-sync to `MealPlanModal` and does not alter `usePlanUpdates`.
- **`MealPlanModal.tsx` is now ~1565 lines** (touched by 4 of 5 Phase 1 PRs). **Do not propose a refactor in Phase 2.** The file already exports `OverviewView` / `ShoppingListView` / `AddItemForm` for testability — that pattern is the dent we'll keep tapping in later phases. W6's hash-sync `useEffect` sits at the top of the `MealPlanModal` component body, immediately after `setActiveView` is declared (line 41).

---

## Phase 2 ship order (3 PRs)

| # | Workstream | Branch | Depends on |
|---|-----------|--------|------------|
| 1 | **W1** Recipes routing (+ deferred W4 in-app "View recipe" button) | `clean-up-ui-w1-recipes-routing` | — (Phase 1 fully merged) |
| 2 | **W14** Persist recipe list search/filter in URL | `clean-up-ui-w14-persist-recipe-search` | W1 (merged to main) |
| 3 | **W6** Meal plan modal tabs in URL hash | `clean-up-ui-w6-mealplan-tab-hash` | — (independent of W1/W14) |

W6 could ship in parallel with W14 (no shared files), but stacking it last keeps the orchestrator's review queue serialized and avoids any risk of conflicting test setups (W14 introduces the first `MemoryRouter` test pattern).

---

## What we decided NOT to do (Phase 2)

- **Split `RecipesPage.tsx` into per-view files.** Plan.md W1 says "(or keep one file with `<Routes>` if simpler)"; we pick simpler. The file is large but the view branches are linear `view === 'xxx'` blocks that map 1:1 to `<Route>` elements with no shared mid-tree state. A split is a future cleanup, not Phase 2 scope.
- **Add a `?from=mealplan` query param** to the recipe detail route. Since the W4 in-app button opens in a **new tab**, there is no need for the recipe page to know how it was entered — the meal-plan modal stays open in the source tab. Re-evaluate if a future phase moves the meal-plan modal to a full-page route.
- **Auto-scroll restoration on detail-back.** Plan.md W14 acceptance mentions "scroll position preserved." We will rely on the browser's default `history.scrollRestoration = 'auto'` behavior — react-router-dom v6 with `<BrowserRouter>` preserves scroll on back navigation by default for same-pathname history entries. Do not add a custom scroll handler unless smoke testing reveals it breaks. Flag this as a smoke-check item, not engineered code.
- **Sub-component refactor of `MealPlanModal.tsx`.** Stays as-is for Phase 2. New W6 logic lives in a single `useEffect` next to `setActiveView`.
- **Migrate W14 search persistence to a generic "URL state" hook.** A reusable `useUrlQueryParam('q', '')` hook is tempting, but the only call site is `RecipesPage`. Keep it inline; extract later if W6's hash sync makes the pattern worth lifting.
- **Add `<Tabs>` primitive (W24)**, even though W6 touches the meal-plan tab strip. W24 is Phase 4 — bundling it into W6 would balloon the PR and the global plan deliberately sequences W6 before W24.

---

# PR 1 — W1: Recipes page becomes proper routes (+ in-app View recipe button)

**Branch.** `clean-up-ui-w1-recipes-routing`

**Commit title.** `feat(clean-up-ui): W1 — recipes routing + in-app view recipe button`

**Acceptance (from plan.md W1, plus Phase 1 deferred W4 part-a).**
- Hitting browser back from `/recipes/abc` returns to `/recipes` list with prior search/filter intact (search persistence is technically W14; Phase 2 ships it incrementally — W1 sets up `useNavigate` for back nav, W14 turns on URL-backed search).
- Direct-loading `/recipes/abc/edit` lands on the edit form (or 404s gracefully if recipe missing).
- Sharing a recipe URL with a teammate opens the same view.
- **W4 carry-over:** Each recipe row in MealPlanModal Overview shows a "View recipe" button next to the existing external-link icon. Clicking opens `/recipes/:recipeId` in a new tab. Meal-plan modal context is preserved.

**Dependencies.** Phase 1 merged to main.

## Files to modify

### a) `camper/webapp/src/App.tsx`
- Replace the single `<Route path="/recipes" element={<RecipesPage />} />` with a wildcard delegation:

  ```tsx
  <Route
    path="/recipes/*"
    element={
      <ProtectedRoute>
        <RecipesPage />
      </ProtectedRoute>
    }
  />
  ```

  This lets `RecipesPage` own its sub-routes via a nested `<Routes>` block.

### b) `camper/webapp/src/pages/RecipesPage.tsx`
The big diff. Replace the `view: 'list' | 'detail' | 'create' | 'edit' | 'import'` state machine with a nested `<Routes>` element. Five routes:

| Path | Renders |
|---|---|
| `/recipes` (index) | List view |
| `/recipes/new` | Create form |
| `/recipes/import` | Import form |
| `/recipes/:recipeId` | Detail view (incl. ingredient resolve + publish + add-ingredient panels) |
| `/recipes/:recipeId/edit` | Edit form |

**Implementation approach (recommended — single file, internal `<Routes>`).**

1. Keep the existing top-level `RecipesPage()` function as the route shell. It owns:
   - `recipes` (list), `ingredients`, `loading`, `error`, `search`, `mealTab` — list-level state still needed by the list view.
   - The shared `api.getRecipes()` + `api.getIngredients()` mount-effect.
   - The outer JSX: `<ParallaxBackground>`, `<AppHeader>`, the section nav (Recipes / Ingredients).
   - A nested `<Routes>` block at the bottom of the JSX that switches between five sub-route components.

2. Extract five **internal** sub-components in the same file (no separate files), each receiving the props it needs from the shell:
   - `RecipesListView` — gets `recipes`, `loading`, `error`, `search`, `setSearch`, `mealTab`, `setMealTab`, plus a `navigate` callback that sends the user to detail/create/import.
   - `RecipeDetailView` — reads `:recipeId` via `useParams`, fetches detail via `api.getRecipe(recipeId)` on mount and on `recipeId` change. Owns its own `selectedRecipe`, `pendingEdits`, `resolveModalIngredient`, etc. — these are all currently *only* used in detail view, so move them inside this component.
   - `RecipeCreateView` — owns the create form state (`createName`, `createDesc`, `createLink`, `createServings`, `createMeal`, `createTheme`, `draftIngredients`, picker state, `creating`, `createError`).
   - `RecipeImportView` — owns `importUrl`, `importing`, `importError`. On success calls `navigate(/recipes/${created.id})`.
   - `RecipeEditView` — reads `:recipeId`, owns `editName`, `editDesc`, `editServings`, `editMeal`, `editTheme`, `saving`, `editError`. On save calls `navigate(/recipes/${id})`.

3. The current `setView('xxx')` calls become `navigate('/recipes/...')`. Specifically:
   - `setView('detail')` after `handleViewRecipe(r)` → `navigate(\`/recipes/${r.id}\`)`.
   - `setView('list')` (back button / cancel paths) → `navigate('/recipes')`.
   - `setView('create')` → `navigate('/recipes/new')`.
   - `setView('import')` → `navigate('/recipes/import')`.
   - `setView('edit')` → `navigate(\`/recipes/${selectedRecipe.id}/edit\`)`.

4. **Detail-view fetching contract.** The current code passes `selectedRecipe` via `setSelectedRecipe()` after `api.getRecipe(id)`. The new `RecipeDetailView` instead reads `:recipeId` from `useParams()`, fetches inside `useEffect([recipeId])`, and stores `selectedRecipe` locally. This makes deep-link `/recipes/abc` work (acceptance criterion). Handle three states: loading → render skeleton; not-found → render "Recipe not found" with a back-to-list link; loaded → render the full detail UI.

5. **Shared `recipes` list cache.** When the user creates / edits / publishes / deletes a recipe, the shell's `recipes` array must update (so the list view stays current). Pass an `onRecipesMutated` callback from the shell down into create/edit/detail views. Each callback either re-fetches `api.getRecipes()` (simple, one extra round-trip) or splices the local cache (current behavior). **Pick: re-fetch.** Simpler, eliminates split-brain bugs in detail/list state, and one extra GET is fine.

6. **Drop the local `view` state entirely.** Per plan.md.

7. **Toast usage stays the same** — the `useToast()` hook is called inside the shell and passed down (or each subview calls `useToast()` directly).

### c) `camper/webapp/src/pages/RecipesPage.css`
- No layout changes. The five views render in the same parchment container as today.
- Verify scrolling: confirm that direct-loading `/recipes/abc` does not start scrolled past the AppHeader (scroll-to-top on route change handled by `<BrowserRouter>` default).

### d) `camper/webapp/src/components/MealPlanModal.tsx` — deferred W4 in-app "View recipe" button
At line 817, **replace the TODO comment** with a "View recipe" anchor element. Recommended JSX:

```tsx
<a
  className="mp-recipe-viewlink"
  href={`/recipes/${recipe.recipeId}`}
  target="_blank"
  rel="noopener noreferrer"
  aria-label={`View ${recipe.recipeName} in the recipe chest`}
  title="View recipe details"
>
  <svg aria-hidden="true" width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M2 11V3a1 1 0 0 1 1-1h7l1 1v8a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1z" />
    <line x1="4" y1="5" x2="9" y2="5" />
    <line x1="4" y1="7.5" x2="9" y2="7.5" />
  </svg>
</a>
```

- Position: between `<span className="mp-recipe-name">` and the existing `recipe.recipeWebLink` external-link `<a>`.
- The TODO comment at line 817 disappears.
- Why a regular `<a target="_blank">` and not `useNavigate()`: the explicit decision (above) is "open in a new tab to preserve modal context" — same pattern as the external-link icon next to it. Using `useNavigate` would force a close-the-modal-and-navigate flow, which the user did NOT pick. A plain anchor with `target="_blank"` is also keyboard-accessible by default (Enter to open, middle-click to open in tab — same affordances as the external link).
- `recipe.recipeId` is **already on `MealPlanRecipeDetailResponse`** (`api/client.ts` types confirmed). No payload change needed.

### e) `camper/webapp/src/components/MealPlanModal.css`
Add `.mp-recipe-viewlink` rule, mirroring the existing `.mp-recipe-extlink` style: `display: inline-flex; align-items: center; padding: 2px 4px; color: var(--charcoal); opacity: 0.6;` with `:hover { opacity: 1; color: var(--lavender); }`. Inherit row height — must not grow `.mp-recipe-row`.

## Implementation notes

- **`navigate` inside view subcomponents** uses `useNavigate()` from `react-router-dom`. Already imported in the file.
- **`useParams<{ recipeId: string }>()`** for detail and edit subviews.
- **404 handling for `/recipes/:recipeId`**. If `api.getRecipe(id)` rejects with `404`-like error, render a small "Recipe not found" state with a "Back to recipes" link → `navigate('/recipes')`. Reuse `recipes-empty` styling. **Do not** redirect the user automatically; explicit graceful 404 per acceptance.
- **Catch-all inside RecipesPage**. Add `<Route path="*" element={<Navigate to="/recipes" replace />} />` at the end of the nested `<Routes>` so `/recipes/garbage/extra` goes to the list rather than rendering nothing. (App-level catch-all `<Route path="*" element={<Navigate to="/" replace />} />` in `App.tsx` does NOT match here because `/recipes/*` already matched.)
- **Section-nav active styling.** Currently the "Recipes" section-nav tab is hardcoded `recipes-section-nav__tab--active`. After W1 it stays hardcoded — all five sub-routes are inside `RecipesPage`, so the tab is always active when this component renders. No change needed.
- **`handleBackToList` removed.** Anywhere it's called becomes `navigate('/recipes')`.
- **Mount-effect refetch.** Move `api.getRecipes()` and `api.getIngredients()` into the shell's mount effect (already there). Sub-routes that mutate (create/edit/delete/publish) call back into the shell via `onRecipesMutated()` to trigger a re-fetch. `ingredients` is also re-fetched when a draft creates a new ingredient (existing behavior).
- **Cascade impact.** No external callers construct `View` (the type is local). The only external coupling is `App.tsx`'s route registration. No fakes / fixtures touch this.

## Cascade impact (modified types/components)

- `App.tsx`: `/recipes` route changes to `/recipes/*`. Verify no other `<Link to="/recipes/...">` in the codebase relied on a specific shape.
  - `grep -rn "/recipes" camper/webapp/src/` to confirm. Expected hits: `App.tsx`, `RecipesPage.tsx` (now uses navigate), `IngredientsPage.tsx` (likely cross-link), `AppHeader.tsx` (logo nav), MealPlanModal.tsx (new anchor in W1). All compatible — `BrowserRouter` matches `/recipes` against `/recipes/*` correctly.
- `RecipesPage.tsx`: `view` state removed. No external callers used it (it's component-local).

## Tests to add

| File | Scenarios |
|---|---|
| `src/pages/RecipesPage.routing.test.tsx` (new) | (1) Mount with `MemoryRouter` initialEntries=`['/recipes']` → list view renders with recipe cards; (2) initialEntries=`['/recipes/new']` → create form renders; (3) initialEntries=`['/recipes/import']` → import form renders; (4) initialEntries=`['/recipes/abc']` with mocked `api.getRecipe('abc')` → detail view renders the recipe name; (5) initialEntries=`['/recipes/abc/edit']` → edit form renders pre-populated; (6) initialEntries=`['/recipes/missing']` with `api.getRecipe` rejecting → "Recipe not found" state renders with back link. |
| `src/components/MealPlanModal.recipeViewLink.test.tsx` (new) | (1) Recipe row renders a "View recipe" anchor with `href="/recipes/${recipeId}"`, `target="_blank"`, `rel="noopener noreferrer"`; (2) `aria-label` contains the recipe name; (3) anchor sits before the existing `recipeWebLink` external-link anchor in DOM order. |

**Test setup pattern.** Reuse the Phase-1 `vi.hoisted()` + `vi.mock()` approach for `useToast` and the `api` module:

```tsx
import { MemoryRouter } from 'react-router-dom';
import { render, screen } from '@testing-library/react';
// ... mocks ...

const mockApi = vi.hoisted(() => ({
  getRecipes: vi.fn().mockResolvedValue([{ id: 'abc', name: 'Pasta', /* ... */ }]),
  getIngredients: vi.fn().mockResolvedValue([]),
  getRecipe: vi.fn().mockImplementation((id: string) =>
    id === 'abc' ? Promise.resolve({ id: 'abc', name: 'Pasta', /* full detail */ }) : Promise.reject(new Error('not found'))
  ),
}));
vi.mock('../api/client', () => ({ api: mockApi }));

// Render with MemoryRouter so routing works in tests:
render(
  <MemoryRouter initialEntries={['/recipes/abc']}>
    <Routes>
      <Route path="/recipes/*" element={<RecipesPage />} />
    </Routes>
  </MemoryRouter>
);
```

This is the **first MemoryRouter test in the project**. Document the pattern in the test file's header comment so future tests (W14, possibly future routing tests) can copy-paste.

## Manual smoke checklist

- `/recipes` → list renders, search works.
- Click a recipe → URL changes to `/recipes/<id>`, detail renders.
- Browser back → returns to list (search not yet persisted — that's W14).
- Direct-load `/recipes/<id>` in a fresh tab → detail renders.
- Direct-load `/recipes/<id>/edit` → edit form renders pre-populated.
- Direct-load `/recipes/garbage` → "Recipe not found" with back-to-list.
- Click "Import URL" → URL becomes `/recipes/import`; back → list.
- Click "New Recipe" → URL becomes `/recipes/new`; submit → URL goes to `/recipes` (or `/recipes/<newId>` if we keep the `setView('list')` semantics — pick: `/recipes` to match current behavior).
- From MealPlanModal Overview → click "View recipe" on a recipe row → opens `/recipes/<id>` in a new tab; meal-plan modal still open in original tab.
- W4 external-link icon still works (do not regress).

---

# PR 2 — W14: Persist recipe list search/filter on detail-back

**Branch.** `clean-up-ui-w14-persist-recipe-search`

**Commit title.** `feat(clean-up-ui): W14 — persist recipe search/filter in URL`

**Acceptance (from plan.md).** Search → click recipe → browser back → search term and scroll position preserved.

**Dependencies.** W1 must be merged to main (URL-backed list view is a prerequisite — without `/recipes` as a real route, there's no history entry to push search params onto).

## Files to modify

- `camper/webapp/src/pages/RecipesPage.tsx` — In the `RecipesListView` subcomponent (extracted in W1):
  - Replace `useState('')` for `search` with a URL-backed value via `useSearchParams()` from `react-router-dom`.
  - Replace `useState<string | null>(null)` for `mealTab` with the same pattern.
  - On user input, call `setSearchParams(params, { replace: true })` so back-button history isn't polluted with an entry per keystroke.
  - On detail-back navigation, react-router-dom restores the URL and the list re-reads `q` / `meal` from the URL — search input pre-populates automatically.

  Approach:

  ```tsx
  function RecipesListView(/* ...props */) {
    const [searchParams, setSearchParams] = useSearchParams();
    const search = searchParams.get('q') ?? '';
    const mealTab = searchParams.get('meal'); // null if absent

    const updateSearch = (value: string) => {
      const next = new URLSearchParams(searchParams);
      if (value) next.set('q', value); else next.delete('q');
      setSearchParams(next, { replace: true });
    };
    const updateMealTab = (value: string | null) => {
      const next = new URLSearchParams(searchParams);
      if (value) next.set('meal', value); else next.delete('meal');
      setSearchParams(next, { replace: true });
    };

    // ...
  }
  ```

  - The `search` and `mealTab` reads stay drop-in compatible with existing filter logic (`filteredRecipes`, `availableMealTabs`). No filter logic changes.
  - **Hand off to W1**: After W1, `search` / `setSearch` / `mealTab` / `setMealTab` props (or local state) live on `RecipesListView`. W14 swaps the local `useState` for `useSearchParams` and exposes the same shape.

## Implementation notes

- **`{ replace: true }`** matters: without it, every keystroke pushes a new history entry and `Back` becomes broken (have to click back 8 times to undo "p-a-s-t-a"). With `replace: true`, the URL stays current but never pollutes history. Opening a detail page (via `navigate('/recipes/abc')`) then pushes a real history entry — so back from detail correctly returns to `/recipes?q=pasta`.
- **Filter pills (`mealTab`)**. Currently a button toggles `mealTab` state. After W14 it toggles a search-param. Same UX, different storage.
- **Empty-state "No provisions match your filter."** Already keys off `search || mealTab` — works unchanged.
- **`mealTab === null` semantics.** `URLSearchParams.get('meal')` returns `null` when absent. Drop-in compatible with existing `mealTab === null` checks.
- **`scroll position preserved`** acceptance bullet. Browser default `scrollRestoration='auto'` covers this for back navigation when the URL pathname changes (`/recipes/abc` → `/recipes?q=pasta`). Verify in smoke. If broken, add a single `useEffect` in `RecipesListView` that calls `window.scrollTo(0, sessionStorage.getItem('recipes-list-scroll'))` on mount and stores `window.scrollY` on unmount — but only if smoke shows a regression.
- **No new effects from URL → state direction.** Because `searchParams.get('q')` is read inline, the input's `value={search}` re-renders correctly when react-router updates the URL. No `useEffect([searchParams], ...)` needed.

## Cascade impact

None outside `RecipesPage.tsx`. The URL contract change (`?q=...&meal=...`) is internal to the recipes page; nothing else in the app constructs URLs to `/recipes` with query params.

`grep -rn "to=\"/recipes" camper/webapp/src/` to verify — only `App.tsx` (route registration), `IngredientsPage` (sibling section-nav link, no query param), and `AppHeader.tsx` (logo nav, no query param) reference `/recipes`. No callers pass query params; W14 is additive.

## Tests to add

| File | Scenarios |
|---|---|
| `src/pages/RecipesPage.searchPersistence.test.tsx` (new) | (1) Mount with `MemoryRouter initialEntries={['/recipes?q=pasta']}` → search input shows "pasta", filter applied; (2) typing in search input updates URL via `setSearchParams` (assert via reading `useLocation` in a probe component, or by re-rendering with `MemoryRouter` reading the in-memory history — see "Test setup note"); (3) initialEntries=`['/recipes?q=pasta&meal=dinner']` → both filters applied; (4) clearing the search input removes `q` from the URL. |

**Test setup note.** To assert URL changes inside a `MemoryRouter`, render a small "URL probe" component as a sibling that calls `useLocation()` and surfaces `location.search` to the test:

```tsx
function UrlProbe() {
  const loc = useLocation();
  return <div data-testid="url-search">{loc.search}</div>;
}

render(
  <MemoryRouter initialEntries={['/recipes']}>
    <Routes>
      <Route path="/recipes/*" element={<RecipesPage />} />
    </Routes>
    <UrlProbe />
  </MemoryRouter>
);

// After typing 'pasta':
await user.type(screen.getByPlaceholderText('Search provisions...'), 'pasta');
await waitFor(() => {
  expect(screen.getByTestId('url-search')).toHaveTextContent('?q=pasta');
});
```

This pattern is reusable for W6 (hash assertions) — document it inline.

## Manual smoke checklist

- `/recipes`: type "pasta" → URL becomes `/recipes?q=pasta`; click a recipe → URL becomes `/recipes/<id>`; back button → URL is `/recipes?q=pasta` and search input shows "pasta".
- Apply `meal=dinner` filter → URL gets `&meal=dinner`. Back/forward preserve.
- Clear search → `?q=` disappears from URL.
- Direct-load `/recipes?q=beef` in a fresh tab → list pre-filters to "beef".
- Share a teammate `/recipes?q=pasta&meal=dinner` URL → opens with both filters applied.
- Scroll halfway down list → click recipe → back → scroll position preserved (if browser fails, add the manual scroll-restore patch noted above).

---

# PR 3 — W6: Meal plan modal tabs in URL hash

**Branch.** `clean-up-ui-w6-mealplan-tab-hash`

**Commit title.** `feat(clean-up-ui): W6 — meal plan modal tab hash sync`

**Acceptance (from plan.md).** Sharing `/plans/:id#shopping` opens the modal on the shopping list tab. Browser back from `#shopping` returns to `#overview`.

**Dependencies.** None. Independent of W1/W14.

## Files to modify

- `camper/webapp/src/components/MealPlanModal.tsx`
  - At the top of the component body (line 41 area, where `setActiveView` is declared), add **two `useEffect` blocks**:

    1. **Hash → state (initial + browser back/forward).** When the modal is `isOpen`, read `window.location.hash`, map it to a `ViewTab`, call `setActiveView(...)`. Also subscribe to `hashchange` so back/forward updates the active tab live.

       ```tsx
       useEffect(() => {
         if (!isOpen) return;
         const sync = () => {
           const tab = parseHash(window.location.hash);
           if (tab) setActiveView(tab);
         };
         sync();
         window.addEventListener('hashchange', sync);
         return () => window.removeEventListener('hashchange', sync);
       }, [isOpen]);
       ```

    2. **State → hash.** When `activeView` changes (and modal is open), call `window.history.replaceState(null, '', '#' + activeView)`. Use `replaceState` not `pushState` so the user doesn't accumulate one history entry per tab click. **HOWEVER**, the acceptance criterion says "browser back from `#shopping` returns to `#overview`" — that requires a real history entry. Resolution: use `pushState` for the **first** tab change after opening (so back returns to the prior page, not the prior tab); use `replaceState` for subsequent tab changes within the same modal session. Alternative simpler approach: always `pushState` and accept that "back from `#shopping`" might step through `#recipes` first before exiting. Read the acceptance again: "Browser back from `#shopping` returns to `#overview`" — that explicitly wants tab history. **Decision: always `pushState` on tab change.** Multi-step back through tabs is the documented behavior.

       Actually, re-reading carefully: the simplest correct behavior is `pushState` per tab change, because "back from `#shopping` → `#overview`" requires that `#overview` was previously pushed. Implement as `pushState`.

       ```tsx
       useEffect(() => {
         if (!isOpen) return;
         const target = '#' + activeView;
         if (window.location.hash !== target) {
           window.history.pushState(null, '', target);
         }
       }, [isOpen, activeView]);
       ```

       Combined with the `hashchange` listener above, browser back/forward triggers `hashchange` → `setActiveView(parseHash(...))` → no infinite loop because the next effect compares `window.location.hash !== target` and skips if equal.

  - **On modal close**, clear the hash:

    ```tsx
    useEffect(() => {
      if (isOpen) return;
      if (window.location.hash) {
        // Strip the hash; replaceState (no new history entry) so closing modal doesn't muddle history.
        window.history.replaceState(null, '', window.location.pathname + window.location.search);
      }
    }, [isOpen]);
    ```

  - Add a small helper at module scope:

    ```tsx
    function parseHash(hash: string): ViewTab | null {
      const stripped = hash.replace(/^#/, '');
      if (stripped === 'overview' || stripped === 'recipes' || stripped === 'shopping') {
        return stripped;
      }
      return null;
    }
    ```

  - **Do not** modify the existing tab-button click handlers. They still call `setActiveView(tab.key)` directly; the state→hash effect handles URL sync.
  - The tab strip is at line 378–392. Leave it untouched.
  - **Note on existing `ViewTab` type.** Currently `'overview' | 'recipes' | 'shopping'`. The plan mentions a `templates` tab — that's NOT a top-level tab in the current implementation; it's a sub-flow inside Overview. Don't add a `templates` hash value. Confirm by re-reading lines 378–392 (only three tabs render). If the user later promotes templates to a tab, the hash type extends.

## Implementation notes

- **Why `pushState` not react-router `useNavigate`.** react-router-dom v6 `<Routes>` does not match on hash; `useNavigate({ hash: '#shopping' })` works for navigating *to* a hash, but a modal is rendered above an existing route (`/plans/:id`) and we don't want to push a new pathname. Manual `history.pushState` keeps the pathname intact and only changes the hash, leaving react-router's view unchanged. Confirmed: react-router-dom's history listener does NOT react to hash-only changes — no fight.
- **`hashchange` event** fires on browser back/forward when the hash changes (whether triggered by `pushState`/`replaceState` or by user navigation). It does NOT fire when the same code calls `pushState` programmatically — so the `state → hash` and `hash → state` effects are decoupled cleanly.
- **Edge: opening modal on `/plans/abc#shopping`.** Initial `isOpen` flips to `true`, the `hash → state` effect runs, `parseHash('#shopping')` → `'shopping'`, `setActiveView('shopping')` runs, then the `state → hash` effect runs but sees `window.location.hash === '#shopping'` and skips. Correct.
- **Edge: opening modal with no hash.** `parseHash('')` returns `null`, `activeView` stays at default `'overview'`, the `state → hash` effect runs and pushes `#overview`. The first tab change pushes another history entry. Acceptable.
- **Closing modal clears the hash via `replaceState`.** This avoids a history entry for "modal closed". Re-opening the modal (without a hash) defaults to overview again.
- **No race condition with `usePlanUpdates`.** That hook fires STOMP refetches. It does not read `window.location.hash` and is not affected by `pushState`. Verified by grepping — `usePlanUpdates.ts` only touches `/topic/plans/{planId}` STOMP destination.

## Cascade impact

None. The hash mechanism is internal to `MealPlanModal`. No other component reads or writes the URL hash. Only `App.tsx`'s `<BrowserRouter>` is involved, and `<BrowserRouter>` is hash-agnostic.

`grep -rn "window.location.hash\|history.pushState\|history.replaceState" camper/webapp/src/` to confirm no existing callers.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.hashSync.test.tsx` (new) | (1) Render modal with `isOpen={true}` and starting URL hash `#shopping` (set via `window.history.replaceState` before render) → shopping tab is active (assert via `mp-tab--active` class on the Shopping tab button); (2) clicking the "Recipe Book" tab updates `window.location.hash` to `#recipes`; (3) firing a `hashchange` event with `#overview` (simulating browser back) updates the active tab to overview; (4) closing the modal (re-render with `isOpen={false}`) clears the hash. |

**Test setup pattern.** No `MemoryRouter` needed — `window.location.hash` and `window.history.pushState` are jsdom built-ins. Reset hash in `beforeEach` via `window.history.replaceState(null, '', '/')`.

```tsx
beforeEach(() => {
  vi.clearAllMocks();
  window.history.replaceState(null, '', '/');
});
```

Reuse the `vi.hoisted()` + `vi.mock('../api/client', ...)` pattern from Phase 1 to stub `api.getMealPlanForTrip`, `api.getRecipes`, `api.getTemplates`, `api.getIngredients`. Provide minimal payloads to bypass the loading state and render the tab strip.

## Manual smoke checklist

- Open MealPlanModal → URL hash becomes `#overview`.
- Click "Recipe Book" → URL is `/plans/abc#recipes`. Click "Shopping List" → `#shopping`.
- Browser back → tab returns to "Recipe Book". Back again → "Overview". Back again → modal closes (history pre-modal).
- Forward → re-traverses tabs.
- Direct-load `/plans/abc#shopping` (close modal first, then paste URL into address bar) → modal opens on Shopping List.
- Close modal via X button → hash clears (no `#` in URL bar).
- Re-open modal → defaults to Overview, hash becomes `#overview`.
- Verify nothing in `usePlanUpdates` regresses: while STOMP is connected and the modal is open, push a `meals` event → modal still refetches as before.

---

# Build / test commands (run after every PR)

```bash
cd camper/webapp
npx tsc --noEmit   # quick type-check
npm run build      # canonical type-check + bundle
npm run test       # vitest run
```

Code-reviewer must reject any PR where any of the three fails. Code-reviewer must also reject any PR that modifies files outside `camper/webapp/src/`.

# Open questions / flags

All Phase 2 plan-gate questions resolved (see header). No open items. If smoke testing reveals scroll-position regression on W14 detail-back, add a single sessionStorage-backed scroll-restore patch in the W14 PR (not a separate PR) — flagged in W14 implementation notes.
