# Phase 1 Implementation Plan — clean-up-ui

**Plan-gate decisions (settled, 2026-04-27).** (1) DOM test infra (jsdom + @testing-library) bundled into W11 — approved. (2) W2 preserves both quantity and unit; clears name/description only — per plan.md as written. (3) AddMemberModal gets a Done button — approved. (4) W4 ships external-link icon only in Phase 1; in-app "View recipe" button defers to a Phase 2 follow-up after W1 — approved.

**Scope.** Decompose the four Phase 1 workstreams into PR-sized chunks consistent with `webapp/src/` patterns. The global plan (`plan.md`) is the source of truth for *what*; this file specifies *how* and *in what order* to ship Phase 1.

**Source of truth.** `camper/docs/clean-up-ui/plan.md` (workstreams W11, W2, W3, W4, W5).

**Hard constraints (verified).**
- **Backend untouched.** No edits to `services/`, `clients/`, `databases/`, `libs/`. All four workstreams are pure FE; data shapes (`MealPlanRecipeDetailResponse.recipeWebLink`, `ShoppingListItemResponse.usedInRecipes`) are confirmed present in `webapp/src/api/client.ts:270-313`.
- **Aesthetic.** Reuse `theme.css` tokens (`--parchment`, `--lavender`, `--sage`, `--ember`, `--charcoal`, etc.); reuse `Button`, `Modal`, `Input`, `Select`, `FormField` from `components/ui/`. No new colors/fonts.
- **Build gate.** `cd camper/webapp && npx tsc --noEmit && npm run build && npm run test` must pass on every PR.
- **Live updates preserved.** STOMP refetch logic in `PlanPage` / `MealPlanModal` / `AssignmentsModal` must continue to function; no changes to `usePlanUpdates`.

---

## Phase 1 ship order (4 PRs)

| # | Workstream | Branch | Depends on |
|---|-----------|--------|------------|
| 1 | **W11** Global toast/snackbar system | `clean-up-ui-w11-toast-system` | — |
| 2 | **W2** Shopping list "Add Item" stays open | `clean-up-ui-w2-shopping-keep-open` | W11 (merged to main) |
| 3 | **W3** Keep-open: GearModal / MealPlanModal recipe-add / AddMemberModal | `clean-up-ui-w3-keep-open-forms` | W11 (merged to main) |
| 4 | **W5** Shopping list info button (recipe usage popover) | `clean-up-ui-w5-shopping-info-popover` | — (independent of W11; uses local state, not toasts) |
| 5 | **W4** Recipe link in meal plan Overview | `clean-up-ui-w4-recipe-link` | — (limited to external `recipeWebLink` icon — see §"W4 vs W1 resolution" below) |

W2 and W3 are sequential to keep diffs reviewable (both touch `MealPlanModal.tsx`). W5 and W4 can each be drafted in parallel with W2/W3 but should land last to avoid merge conflicts in `MealPlanModal.tsx` / `MealPlanModal.css`.

---

## W4 vs W1 resolution (decision)

`plan.md` W4 originally specifies two controls per recipe row:
- **(a) "View recipe" button** → navigates to `/recipes/:recipeId` (route added by W1, Phase 2).
- **(b) External-link icon** → opens `recipeWebLink` in a new tab when present.

W1 is **Phase 2**. The current `App.tsx` only has `/recipes` (list), and `*` catch-all redirects to `/`. Adding (a) now would either break (404 redirect to `/`) or require us to ship a placeholder route inside W4 — both ugly.

**Decision: ship (b) only in Phase 1 W4. Defer (a) to a Phase-2 follow-up PR landed alongside W1.**

- Phase 1 W4 deliverable: external-link icon (only when `recipeWebLink` is non-null), opens in new tab with `target="_blank" rel="noopener noreferrer"`, `aria-label` set.
- Phase 2 follow-up: add the in-app "View recipe" button immediately after W1 ships (track in `progress.md`; it's a small one-file change).

This keeps Phase 1 strictly correct against the current route table and avoids dead UI.

---

## Pre-W11 infra decision: enable DOM tests

`webapp/vitest.config.ts` currently sets `environment: 'node'`, and `package.json` has no `@testing-library/react` or `jsdom`. The single existing test (`mealPlanSummary.test.ts`) is a pure-function test.

The toast system, popover, and keep-open behavior tests benefit greatly from DOM rendering. **Decision: add `jsdom`, `@testing-library/react`, `@testing-library/user-event`, and `@testing-library/jest-dom` as devDependencies in W11**, switch `vitest.config.ts` to `environment: 'jsdom'`, and add a `src/test/setup.ts` for `@testing-library/jest-dom` matchers.

This is a one-time infra add bundled with W11 (since W11 is the most test-worthy of the four). The pure-function test in `src/lib/mealPlanSummary.test.ts` continues to pass under jsdom (it uses no DOM APIs). If the orchestrator prefers to keep node-only tests, override here — fall back plan: extract the toast reducer to a pure function and test it in node env; skip component tests for W2–W5 and document "manual smoke required."

---

# PR 1 — W11: Global toast/snackbar system

**Branch.** `clean-up-ui-w11-toast-system`

**Commit title.** `feat(clean-up-ui): W11 — global toast/snackbar system`

**Acceptance (from plan.md).** Successful actions across the app produce a toast. Toasts dismiss correctly, can be stacked, work on mobile.

**Dependencies.** None. Ship FIRST.

## Files to create

- `camper/webapp/src/context/ToastContext.tsx` — Context provider exposing `useToast()` hook with `success`, `error`, `info` methods. Manages internal toast queue (stable IDs via `crypto.randomUUID()`), auto-dismiss timers, hover-pause, max-visible cap.
- `camper/webapp/src/components/ui/Toast.tsx` — Toast list renderer (consumes context state) + individual `<ToastItem>` component. Position-fixed container, parchment styling. No business logic.
- `camper/webapp/src/components/ui/Toast.css` — Parchment-styled toast item, slide-in animation, success/error/info variant accents (using existing `--mint`, `--ember`, `--lavender` tokens). Position-fixed container at top-right (desktop) and bottom-center (mobile, ≤640px) via media query.
- `camper/webapp/src/context/toastReducer.ts` — Pure reducer (`ToastAction = ADD | DISMISS | PAUSE | RESUME`) extracted from the context for unit testing. State: `{ toasts: ToastItem[] }`.
- `camper/webapp/src/context/ToastContext.test.tsx` — DOM tests for `useToast()` and `<ToastProvider>`.
- `camper/webapp/src/context/toastReducer.test.ts` — Pure unit tests for the reducer.
- `camper/webapp/src/test/setup.ts` — Imports `@testing-library/jest-dom/vitest`.

## Files to modify

- `camper/webapp/src/App.tsx` — Wrap `<Routes>` in `<ToastProvider>` (just inside `<AuthProvider>`), render `<Toast />` (the list renderer) once at the top of the tree (sibling to `<Routes>`).
- `camper/webapp/vitest.config.ts` — Switch `environment` to `'jsdom'`, add `setupFiles: ['src/test/setup.ts']`.
- `camper/webapp/package.json` — Add devDeps: `jsdom`, `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`.
- `camper/webapp/src/components/AddMemberModal.tsx` — Replace silent success path with `useToast().success('Invitation sent to ' + email)` per email; keep error rendering inline for failures.
- `camper/webapp/src/pages/RecipesPage.tsx` — On publish success, fire `success('Recipe published')`; on draft creation success, fire `success('Recipe saved as draft')`. Replace any `alert()` with `error()`.
- `camper/webapp/src/components/ProfileForm.tsx` — On save success, fire `success('Profile saved')`.
- `camper/webapp/src/components/MealPlanModal.tsx` — When loading a template completes successfully, fire `success('Template loaded')` (one call site, in the existing `onLoadTemplate` handler).

> **Important — scope discipline.** Do NOT migrate everything to toasts in this PR. Only the four "obvious wins" listed above (per plan.md W11 "Migrate the obvious places in this same workstream"). W9 / W15 / W17 toast usage is Phase 4.

## Implementation notes

**Public API of `useToast()`:**
```ts
type ToastVariant = 'success' | 'error' | 'info';
interface ToastAction { label: string; onClick: () => void; }
interface ToastOptions { durationMs?: number; action?: ToastAction; id?: string; }
interface ToastApi {
  success: (msg: string, opts?: ToastOptions) => string; // returns toast id
  error:   (msg: string, opts?: ToastOptions) => string;
  info:    (msg: string, opts?: ToastOptions) => string;
  dismiss: (id: string) => void;
}
```

- **Default duration:** 4000ms (per plan).
- **Max visible:** 4. On overflow, the oldest non-hovered toast is evicted.
- **Pause on hover:** track `hoveredId` in state; `setTimeout` is reset (cleared + rescheduled with remaining time) on enter/leave.
- **Action button:** when `opts.action` is provided, render a "Undo"-style ghost button inside the toast. Clicking calls `action.onClick()` then dismisses the toast.
- **z-index.** Toast container uses `z-index: 1200` — must sit above `Modal` (`z-index: 100`, see `Modal.css:4`) and `SideNav` (`z-index: 1100`, see `SideNav.css:6`). Document this constant inline as the canonical "above everything" layer.
- **Position.** Desktop: `position: fixed; top: 16px; right: 16px;`. Mobile (≤640px): `bottom: 16px; left: 16px; right: 16px;` (full-width stack, items still fixed-height).
- **Reduced motion.** If `prefers-reduced-motion: reduce` matches, drop the slide-in transform; keep opacity fade only.
- **Stacking.** Flexbox column, `gap: 8px`. Newest toast appended to the end (visually top on desktop because the container is anchored top-right and we append downward; this matches user expectation that "most recent is most prominent" — see common toast patterns. If the team prefers prepend-at-top, swap the array order in the renderer).
- **Cleanup.** `useEffect` on context unmount must clear all pending timers to avoid memory leaks.
- **No new theme tokens.** Use `--parchment`, `--charcoal`, `--mint` (success accent), `--ember` (error accent), `--lavender` (info accent). Border / shadow per existing modal patterns.

## Tests to add

| File | Scenarios |
|---|---|
| `src/context/toastReducer.test.ts` | (1) `ADD` appends a toast and assigns ID; (2) `DISMISS` removes by ID; (3) `ADD` past max-visible evicts oldest; (4) `PAUSE` and `RESUME` flip the per-item `paused` flag without losing other toasts. |
| `src/context/ToastContext.test.tsx` | (1) `useToast().success('hi')` renders one toast with the message; (2) auto-dismiss after `durationMs` (use `vi.useFakeTimers()`); (3) calling `success` four times stacks four; the fifth eviction-removes the first; (4) toast with `action` renders the action button and clicking dismisses + invokes the handler; (5) hovering pauses dismissal; leaving resumes; (6) `dismiss(id)` clears the matching toast. |

## Manual smoke checklist (for code-reviewer)

- Invite a member from PlanPage → toast "Invitation sent to …".
- Save profile from `/account` → "Profile saved" toast.
- Publish a draft recipe → "Recipe published" toast.
- Open MealPlanModal → load from template → "Template loaded" toast.
- Trigger an error path (invalid email) → red `error` toast still readable.
- Stack 5+ rapid toasts → only 4 visible, oldest evicted.
- Hover a toast → it doesn't dismiss until cursor leaves.
- Resize to 375px → toasts dock to bottom of viewport.

---

# PR 2 — W2: Shopping list "Add Item" stays open

**Branch.** `clean-up-ui-w2-shopping-keep-open`

**Commit title.** `feat(clean-up-ui): W2 — shopping list add-item stays open`

**Acceptance (from plan.md).** Open Add Item, add 3 items in a row without ever clicking "+ Add Item" again. Cancel button or Escape closes the form.

**Dependencies.** W11 must be merged to main (uses `useToast()` for the inline "Added" feedback).

## Files to create

None.

## Files to modify

- `camper/webapp/src/components/MealPlanModal.tsx` — Refactor the `ShoppingListView` add-item flow (lines ~1245–1380):
  - Remove `setShowAddForm(false)` from the success branch of `handleSubmit`.
  - On success:
    - **Ingredient mode:** clear `selectedIngredient` (so the next item is a fresh search); **preserve `addQuantity` and `addUnit`** at last-used values. Refocus the IngredientSearch input via a forwarded ref — see "Cascade impact" below.
    - **Free-form mode:** clear `addDescription` only; **preserve `addQuantity` and `addUnit`** at last-used values (even though free-form mode currently doesn't render qty/unit fields, they live in the same component-level state, so leaving them set keeps the user's last value if they switch back to ingredient mode within the same form session). Refocus the description input via existing `useRef`.
  - On success, call `useToast().success(\`Added "${displayName}"\`, { durationMs: 1500 })`.
  - Add `onKeyDown` handler at the form root: Escape closes (`setShowAddForm(false); resetAddForm();`).
  - Cancel button stays as today; behavior unchanged.

- `camper/webapp/src/components/ui/IngredientSearch.tsx` — Forward a ref to the inner search input. Use `forwardRef<HTMLInputElement, Props>(...)`. **Cascade check:** existing call sites (`grep -r "IngredientSearch(" src/`) — only used inside MealPlanModal's shopping add and recipe creation in `RecipesPage.tsx`. Forwarded ref is optional, so existing call sites keep compiling.

- `camper/webapp/src/components/MealPlanModal.css` — Minor padding tweak only if the form gains a focus ring or "Added" inline flash. No layout changes.

## Implementation notes

- **What's preserved.** Per plan.md W2 as written, both `addQuantity` and `addUnit` are preserved at their last-used values; only `addDescription` (free-form) and `selectedIngredient` (ingredient mode) are cleared. This makes the "1 onion, 1 carrot, 1 leek" rhythm one-tap-per-item with the same qty/unit auto-applied.
- **Toast vs inline flash.** plan.md W2 says "small inline 'Added <name>' toast/flash". With W11 in place, the global toast satisfies this — no need for a separate inline element. Use `durationMs: 1500` to honor the "~1.5s" requirement.
- **Escape key.** Bind `onKeyDown` on the form `<div className="mp-shopping-add-form">` (not on individual inputs) so Escape works regardless of which field has focus. Inside `<input>` elements, Escape may already trigger native behavior on some browsers — explicitly call `e.preventDefault()` then close.
- **Ref propagation.** If forwardRef-ifying `IngredientSearch` is too invasive, alternate path: expose an imperative `focus()` method via `useImperativeHandle`. Either is acceptable — the developer picks the cleaner shape during implementation.

## Cascade impact (modified types/components)

`IngredientSearch.tsx` — adding a `forwardRef`. Affected call sites:
- `src/components/MealPlanModal.tsx` (shopping add)
- `src/pages/RecipesPage.tsx` (ingredient picker in create/edit recipe forms)

Both callers don't need changes (`forwardRef` is non-breaking when the ref is optional). Confirm with `npx tsc --noEmit`.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.shoppingAdd.test.tsx` (new) | (1) Submit free-form item → form stays open, description input cleared and refocused, quantity + unit preserved; (2) submit ingredient item → form stays open, ingredient cleared (fresh search), quantity + unit preserved at last-used values, IngredientSearch input refocused; (3) Cancel button closes form; (4) Escape key closes form; (5) Submit calls `useToast().success` with the added name (mock the toast context). |

> **Test setup note.** This test renders `<ShoppingListView>` in isolation. Extract `ShoppingListView` props into a clean shape if needed, or render the full `MealPlanModal` with mocked `api` calls. Prefer the former for test simplicity. If the existing `ShoppingListView` is tightly coupled to internal state, the developer should pass through props rather than refactor — flag the awkwardness rather than expanding scope.

## Manual smoke checklist

- Open MealPlanModal shopping tab → click "+ Add Item" → add 3 free-form items in a row without clicking "+ Add Item" again. Each shows a brief "Added …" toast.
- Switch to Ingredient mode → add 3 different ingredients with the same quantity + unit selected once (qty/unit persist across submits; only the ingredient selection clears).
- Press Escape → form closes.

---

# PR 3 — W3: Keep-open treatment for GearModal, recipe-add, AddMemberModal

**Branch.** `clean-up-ui-w3-keep-open-forms`

**Commit title.** `feat(clean-up-ui): W3 — keep-open across gear / recipe-add / invite forms`

**Acceptance (from plan.md).** Adding 5 gear items / 3 recipes to a meal / 4 members in a row never requires re-opening the form.

**Dependencies.** W11 must be merged to main (toast feedback per add).

## Files to create

None.

## Files to modify

### a) `camper/webapp/src/components/GearModal.tsx`

`AddItemForm` (lines ~160–220) already partially keeps open: it clears `name`, resets quantity to 1, and refocuses. Two changes:
- **Preserve `quantity` at last value** (per plan.md W3): remove `setQuantity(1)` after success. Quantity stays at whatever the user just used.
- **Toast on success:** call `useToast().success(\`Added "${name}"\`, { durationMs: 1500 })`.
- **No close-on-success ever.** The form is already always-visible inside the gear list — no `setShowForm(false)` call exists. Confirmed.

> **Discovery while reading.** GearModal's `AddItemForm` is *already* keep-open by design. The only behavioral changes for W3 are (i) preserve quantity, (ii) add toast feedback. Document this in the commit body so reviewer knows the small diff is correct and not under-shipping.

### b) `camper/webapp/src/components/MealPlanModal.tsx` — recipe-add inline

Lines ~814–856. Currently, picking a recipe from the inline search calls `onAddRecipeInline(...)` then immediately `setAddingMealType(null); setInlineSearch('');`, which closes the form.

Changes:
- After successful add, clear `inlineSearch` only. Keep `addingMealType` set so the form stays open.
- Refocus the search input. Wrap the existing `<input className="mp-inline-add-search">` with a `useRef<HTMLInputElement>(null)` and call `inputRef.current?.focus()` after the add.
- Toast: `useToast().success(\`Added "${r.name}" to ${mealTypeLabel(key)}\`)`.
- Cancel button (line 842) and Escape close as today.
- Add Escape key handler on the form root.
- Add an explicit "Done" affordance? Plan says "Cancel" closes — keep that. No Done button.

### c) `camper/webapp/src/components/AddMemberModal.tsx`

Currently the modal accepts a list of emails (multi-row form), and on submit calls `onAdd(email)` per row. On full success it calls `onClose()` (line 92). On partial failure it filters to failed rows and stays open.

Changes (per plan.md W3 "AddMemberModal — keep open after add, clear email field, refocus"):
- After full success, **do not** call `onClose()`. Reset `emails` to `['']` (single empty row), call `inputRefs.current[0]?.focus()`.
- Add a "Done" button next to "Cancel" that explicitly closes the modal.
- The existing W11 toast call (added in PR 1) already fires per email — leave as-is.
- Keep the existing partial-failure behavior unchanged.

> **UX note.** With keep-open, the existing "+ Add another email" multi-row affordance becomes redundant for the common case. Keep both — multi-row is still useful for paste-friendly bulk invite; keep-open is for "one at a time, hands-on-keyboard" rhythm. Don't remove the multi-row code.

## Implementation notes

- **Single PR, three call sites.** Three independent diffs; review them in the order: GearModal (smallest, ~5 lines), recipe-add (medium, ~15 lines), AddMemberModal (largest, ~20 lines + new Done button).
- **Keep behavior consistent with W2.** Pattern is: clear primary inputs → preserve secondary state where reasonable → refocus primary input → toast → close only on explicit Cancel/Done/Escape.
- **mealTypeLabel helper.** A `MEAL_TYPES` array already exists at `MealPlanModal.tsx:25`. Reuse `MEAL_TYPES.find(m => m.key === key)?.label ?? key` rather than introducing a new helper.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/GearModal.addItem.test.tsx` (new) | (1) Submit gear item → name cleared, quantity preserved, name input refocused; (2) toast fired with the added name. |
| `src/components/MealPlanModal.recipeAdd.test.tsx` (new) | (1) Pick a recipe from inline search → search cleared but `addingMealType` stays set; (2) Cancel closes the inline form; (3) Escape closes. |
| `src/components/AddMemberModal.test.tsx` (new) | (1) Single-row submit success → emails reset to `['']`, modal stays open, first input refocused; (2) "Done" button calls `onClose`; (3) partial-failure path still filters to failed rows (existing behavior). |

> Mock `useToast()` and `api` calls in all three.

## Manual smoke checklist

- GearModal: add 5 gear items in a row, last quantity persisting.
- MealPlanModal Overview: add 3 recipes to dinner of Day 1 in a row.
- AddMemberModal: invite 4 emails one at a time with Done to close.

---

# PR 4 — W5: Shopping list info button (recipe usage popover)

**Branch.** `clean-up-ui-w5-shopping-info-popover`

**Commit title.** `feat(clean-up-ui): W5 — shopping list recipe usage info button`

**Acceptance (from plan.md).** Every recipe-derived shopping list row shows an info button; hover/click reveals the recipe names. Free-form items show no button. Row height does not change.

**Dependencies.** None. Independent of W11 (uses local popover state, not toasts).

## Files to create

- `camper/webapp/src/components/ui/InfoPopover.tsx` — Reusable info icon button + popover. Props: `items: string[]`, `ariaLabel: string`, `triggerSize?: 'sm' | 'md'`. Internal state: `isOpen`, anchored to the icon. Closes on outside click + Escape.
- `camper/webapp/src/components/ui/InfoPopover.css` — Parchment popover styling, small triangle pointer, `position: absolute` with overflow handling. Reuses existing tokens.
- `camper/webapp/src/components/ui/InfoPopover.test.tsx` — DOM tests.

## Files to modify

- `camper/webapp/src/components/MealPlanModal.tsx` — In `ShoppingListView` rendering (lines ~1410–1457), inside `mp-shopping-item`, after `mp-shopping-name`, render `<InfoPopover>` only when `item.usedInRecipes.length > 0`. Pass `items={item.usedInRecipes}` and `ariaLabel="Recipes using this item"`.
  - Mergeable item already aggregates `usedInRecipes` (`MealPlanModal.tsx:1202`). Confirm dedup: `Array.from(new Set(item.usedInRecipes))` to avoid duplicates when the same ingredient appears in multiple meal-types.
- `camper/webapp/src/components/MealPlanModal.css` — Adjust `.mp-shopping-item` flex layout to accommodate the icon button without changing row height. The icon is `16×16` with `4px` left margin; existing rows have `display: flex; align-items: center` so this should not reflow.

## Implementation notes

- **Names only, not links.** `usedInRecipes` is `string[]` of recipe **names** (not IDs) — confirmed in `webapp/src/api/client.ts:313` and called out in plan.md W5 FE-only note. Render as plain text. Do **not** attempt to derive IDs by name match — incorrect matches are worse than no link.
- **Trigger UX.**
  - Hover (desktop): show tooltip-style popover.
  - Click/focus (mobile + keyboard): toggle popover with sticky open until outside click / Escape.
  - One pattern that handles both: `onMouseEnter`/`onMouseLeave` + `onClick`/`onFocus`/`onBlur`. Click "pins" the open state; mouse leave only closes if not pinned.
- **Icon.** Inline SVG circle-with-i, 16×16, stroke `currentColor`. Use `Button` shared primitive's `variant="icon" size="sm"` styling — the popover trigger IS a `<Button variant="icon" size="sm" type="button">` wrapping the SVG, ensuring consistent hit area + focus ring.
- **Aria.** `aria-haspopup="dialog"`, `aria-expanded={isOpen}`, `aria-label` per prop. Popover container has `role="dialog"` and `aria-label` referencing the button.
- **Outside-click handling.** Use `useEffect` to attach a `mousedown` listener to `document` while open; close on event whose target is not inside the popover. Detach on close. Standard pattern.
- **Z-index.** Popover sits inside the modal, so `z-index: 1` relative to its positioning context is fine. Do NOT exceed modal z-index — toasts (1200) should still occlude.
- **Reusability.** `InfoPopover` lives in `components/ui/` because plan.md W7 (Phase 4) and W22 (Phase 5) will likely reuse it (W22 explicitly mentions a popover). Keep the API minimal: items + label.
- **Row height.** The icon is 16×16 inside a 20×20 button; existing row content (checkbox 16, name text) is taller. Visually verify in dev that row height doesn't grow.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/ui/InfoPopover.test.tsx` | (1) Hovering the trigger opens the popover; (2) clicking pins it open through mouse-leave; (3) outside click closes; (4) Escape closes; (5) all items render in the list; (6) `aria-expanded` toggles correctly. |

The shopping-list integration is best covered by manual smoke + the existing W2 test file (which renders `ShoppingListView`); add one assertion there if convenient: rows with `usedInRecipes` non-empty render an info button; rows with empty `usedInRecipes` (manual / freeform items) do not.

## Manual smoke checklist

- Open shopping list with recipe-derived items → each row shows an info button after the name.
- Hover → popover lists recipes.
- Click → popover stays open; click outside → closes.
- Manual / freeform items show no info button.
- Tab through with keyboard → focus moves to the icon, Enter/Space opens, Escape closes.
- Verify row height unchanged by toggling on/off.

---

# PR 5 — W4: Recipe link in meal plan Overview

**Branch.** `clean-up-ui-w4-recipe-link`

**Commit title.** `feat(clean-up-ui): W4 — external recipe link in meal plan overview`

**Acceptance (from plan.md, scoped per "W4 vs W1 resolution" above).** Each recipe under Breakfast/Lunch/Dinner/Snacks shows an external link to the source URL (when `recipeWebLink` is non-null). The in-app "View recipe" button is **deferred** to a Phase 2 follow-up landing immediately after W1.

**Dependencies.** None.

## Files to create

None.

## Files to modify

- `camper/webapp/src/components/MealPlanModal.tsx` — In the recipe row rendering (lines ~802–812 inside `OverviewView`'s meal-type loop), append an external-link `<a>` after `mp-recipe-name`:
  - Render only when `recipe.recipeWebLink` is non-null.
  - `<a href={recipe.recipeWebLink} target="_blank" rel="noopener noreferrer" className="mp-recipe-extlink" aria-label={\`Open original recipe at ${hostname(recipe.recipeWebLink)}\`}>` containing a small external-link SVG icon (16×16).
  - The existing `mp-recipe-remove` button stays at the end of the row.
- `camper/webapp/src/components/MealPlanModal.css` — Add `.mp-recipe-extlink` rule: `display: inline-flex; align-items: center; padding: 2px 4px; color: var(--charcoal); opacity: 0.6;` with `:hover { opacity: 1; color: var(--lavender); }`. Inherit row height; do not add margin that grows the row.

## Implementation notes

- **`hostname(url)` helper.** Inline as `try { return new URL(url).hostname; } catch { return 'recipe source'; }`. No new lib.
- **Icon.** Inline SVG of the standard "external-link" arrow-out-of-box. 12×12 inside a 16×16 button-like container. Use `currentColor` stroke.
- **Keyboard accessibility.** Native `<a target="_blank">` is already keyboard accessible; ensure tab order between recipe name → external link → remove button.
- **No changes to remove logic.** The existing `mp-recipe-remove` button stays after the link.
- **Where exactly.** Line numbers will drift after W2/W3 PRs; reference the JSX block:

  ```tsx
  {dayRecipes.map(recipe => (
    <div key={recipe.id} className="mp-recipe-row">
      <span className="mp-recipe-name">{recipe.recipeName}</span>
      {/* NEW: external link if web link present */}
      {recipe.recipeWebLink && (
        <a className="mp-recipe-extlink" href={recipe.recipeWebLink} target="_blank" rel="noopener noreferrer" aria-label={…}>
          <svg …/>
        </a>
      )}
      <button className="mp-recipe-remove" …>…</button>
    </div>
  ))}
  ```

- **Phase 2 follow-up reminder.** Add a TODO comment next to the new link: `// TODO(W1): add in-app "View recipe" button → /recipes/${recipe.recipeId} once W1 routes ship` — so the follow-up PR is easy to find via grep.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.recipeLink.test.tsx` (new) | (1) Recipe row with non-null `recipeWebLink` renders an `<a target="_blank">` with the URL and `rel="noopener noreferrer"`; (2) recipe row with null `recipeWebLink` does not render the anchor; (3) `aria-label` includes the hostname. |

These are pure render assertions; render `OverviewView` (or a small wrapper) with two test recipes — one with a web link, one without.

## Manual smoke checklist

- Open MealPlanModal Overview with recipes that have `recipeWebLink` set → external icon appears, opens source URL in a new tab.
- Recipes without `recipeWebLink` → no icon (and no broken layout).
- Row height unchanged.
- Tab order: name → external link (if present) → remove → next recipe.

---

# What we decided NOT to do (Phase 1)

- **Migrate every silent action to a toast.** Only the four W11 "obvious wins" listed above; W9, W15, W17, W20 toast usage is Phase 4.
- **In-app "View recipe" button (W4 part a).** Deferred to a Phase 2 follow-up after W1, because the route doesn't exist yet (see W4 vs W1 resolution).
- **Make `usedInRecipes` clickable.** Names only — IDs aren't on the payload. plan.md W5 explicitly scopes this out.
- **Refactor `MealPlanModal.tsx`.** It's 1487 lines and ripe for splitting, but a refactor is out of Phase 1 scope. Phase 1 PRs only touch the regions they need.
- **Keyboard support across forms (W8).** Plan W8 is Phase 6. Don't bundle Enter/Escape audits into W2/W3 — only the specific Escape behavior plan.md calls for.
- **Add a Done button to all keep-open forms.** Only `AddMemberModal` gets a Done button (because its existing model has multi-row submit semantics). Other forms close via Cancel/Escape per the plan.

# Open questions / flags

All Phase 1 plan-gate questions resolved (see header). No open items.

---

# Build / test commands (run after every PR)

```bash
cd camper/webapp
npx tsc --noEmit   # quick type-check
npm run build      # canonical type-check + bundle (tsc -b is stricter than --noEmit)
npm run test       # vitest run
```

Code-reviewer must reject any PR where any of the three fails. Code-reviewer must also reject any PR that modifies files outside `camper/webapp/src/`, `camper/webapp/package.json`, `camper/webapp/vitest.config.ts`, or `camper/webapp/src/test/setup.ts`.
