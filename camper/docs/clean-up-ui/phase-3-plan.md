# Phase 3 Implementation Plan — clean-up-ui

**Plan-gate decisions (settled, 2026-04-29).**

1. **W13 form reuse: EXTRACT a shared `RecipeForm.tsx`.** The Phase-2 single-file approach already cleanly isolates `RecipeCreateView` inside `RecipesPage.tsx` (lines 1207–1577) — its state (`createName`, `createDesc`, `createLink`, `createServings`, `createMeal`, `createTheme`, `draftIngredients`, ingredient picker state) is component-local and the only side effects are `api.createRecipe` + `navigate('/recipes')`. We extract that body into `src/components/RecipeForm.tsx` with an `onSuccess(newRecipe: RecipeResponse)` callback prop. `RecipeCreateView` becomes a 6-line wrapper that renders `<RecipeForm onSuccess={() => navigate('/recipes')} />`. The MealPlanModal nested-modal version renders `<RecipeForm onSuccess={(newRecipe) => { closeNestedModal(); selectInRecipeBook(newRecipe); }} />`. Rationale: duplicate-in-place doubles the ingredient-picker state machine (~200 lines) — that's the maintenance trap. A simplified inline form (name + ingredients only) was considered and rejected: the user explicitly wants "the same form" per `plan.md` W13, and stripping fields would create a UX seam between "quick create" and "real create" that surprises users when fields go missing. Extraction is the contract-compliant path with the smallest long-term cost.

2. **W12 grid behavior (all confirmed at the gate).**
   - **Layout:** rows = days (one row per `mealPlan.days[]` entry, labeled `Day {dayNumber}`), columns = the four `MEAL_TYPES` (breakfast / lunch / dinner / snack). One checkbox per cell. Sticky column header on top, sticky row labels on left if vertical scroll is needed. The grid replaces the current popover content (the two `<select>` dropdowns at `MealPlanModal.tsx:1175-1193`).
   - **Submit shape:** local state is `Set<string>` of `${dayId}:${mealType}` keys. On "Add to Meal Plan" click, decompose into `Array<{dayId, mealType}>` and loop `api.addRecipeToMeal(mealPlan.id, dayId, { mealType, recipeId })` **sequentially** (preserves the existing add ordering so STOMP `meals` events arrive in the visual order the user picked).
   - **Pre-check:** the cell at `(currentDay.id, addMealType)` only — matches the single-add path's existing `addDay` + `addMealType` pre-population at `MealPlanModal.tsx:1208-1209`. The user can uncheck and re-pick freely; if they uncheck the only pre-checked cell and submit, the button disables (no-op).
   - **Popover lifecycle:** popover closes after a successful batch (`setAddingToMeal(null)`), matching today's behavior. On any per-cell error, halt the loop, surface the error inline, leave the remaining unchecked cells in the grid for retry.
   - **Toast feedback:** one toast per batch — `success("Added 'Pasta' to 3 meals")`. NOT one per cell; that would spam 5+ toasts and break the 4-visible cap. If only one cell is selected the message reads `success("Added 'Pasta' to Day 2 dinner")` (existing single-add tone preserved).
   - **The "Add" button label** updates dynamically: `Add` (0–1 cells), `Add to N meals` (≥2 cells). Disabled when 0 cells are selected.

3. **W17 sessionStorage key.** `mealPlanReconcileDismissed:${planId}` (one entry per plan). Stored on explicit dismiss; **cleared on successful "Update meal plan to N servings"** click (so the banner returns if the user later removes/adds members again and goes out of sync). Per-tab session means the next browser session re-evaluates from scratch — desired UX (the user gets a fresh prompt next visit if still out of sync).

4. **W16 data flow correction.** `plan.md` W16 says "Compute client-side from `getAssignments` + `getPlanMembers` (both already fetched). One extra fetch on PlanPage load." This is **inaccurate in two ways** and must be corrected:
   - **`getAssignments(planId)` returns `Assignment[]`, NOT `AssignmentDetail[]`** — it has no `.members`. To compute "N members assigned to a tent", we need the per-assignment member lists, which only `getAssignment(planId, assignmentId)` returns. The pattern at `AssignmentsModal.tsx:371-373` (list + parallel `Promise.all` over `getAssignment` per ID) is the existing precedent. We replicate it on PlanPage.
   - **The meal-plan summary is NOT currently fetched on PlanPage.** The kitchen badge needs `getMealPlanForTrip(planId)` for day count / "no meal plan yet" state. **W17 introduces this fetch first** (it needs `mealPlan.servings`); W16 reuses the same state. Concretely: W17's `mealPlan` state on PlanPage becomes the data source for both the reconcile banner (W17) and the kitchen badge (W16).
   - **Net new fetches on PlanPage** (across W17 + W16): one for meal plan (W17), one list + N parallel details for assignments (W16). All four endpoints (`getMealPlanForTrip`, `getAssignments`, `getAssignment`, `getPlanMembers`) already exist on `webapp/src/api/client.ts`. **No BE changes.**

5. **W17 banner placement.** The banner is owned by `PlanPage` (not `MealPlanModal`), as a small `<MealPlanReconcileBanner>` component below the AppHeader, above the campsite scene. This matches `plan.md` W17 ("On PlanPage, after member add/remove…") and avoids modal-coupled visibility (the user should see the prompt without opening the kitchen).

6. **Ship order: W12 → W13 → W17 → W16** (matches handoff order). No reshuffle. Rationale: W12 + W13 both touch the `RecipeBookView` region of `MealPlanModal.tsx` — sequential keeps diffs reviewable. W17 + W16 both add new fetches to `PlanPage.tsx` and W16 reads the meal-plan state W17 introduces — sequential is the cheapest. W17 *could* ship before W12/W13 (no coupling), but stacking it after preserves the global plan's order and keeps the orchestrator's review queue serial.

7. **Live updates / STOMP discipline.** `usePlanUpdates` and `useLadderUpdates` are untouched. W16 adds two new pieces of resource-aware refresh logic to PlanPage's existing `usePlanUpdates` callback: `assignments` events refetch the assignment-detail batch (badge counts update live); `meal-plan` events (note: not currently published — see "Open question A" below) would refetch the meal plan summary. Because no `meal-plan` STOMP topic is published today, W17/W16 do not subscribe; the meal-plan badge updates only on PlanPage mount + on the user's own member add/remove (which already triggers `loadData`).

---

**Scope.** Decompose Phase 3's four workstreams into PR-sized chunks consistent with patterns established in Phases 1 and 2. The global plan (`plan.md`) is the source of truth for *what*; this file specifies *how* and *in what order*.

**Source of truth.** `camper/docs/clean-up-ui/plan.md` (workstreams W12, W13, W17, W16). Phase 1+2 progress notes (sub-component exports, `vi.hoisted()` mock pattern, `MemoryRouter` + `UrlProbe` test scaffolding, hash-state three-effect pattern).

**Hard constraints (re-verified).**
- **Backend untouched.** Re-verified against `webapp/src/api/client.ts`:
  - W12 → `addRecipeToMeal(mealPlanId, dayId, { mealType, recipeId })` (line 934). Loop in FE.
  - W13 → `createRecipe(...)` (line 831), `getIngredients()` (line 799), `createIngredient(...)` (line 803). All already used by `RecipeCreateView`.
  - W17 → `updateMealPlan(id, { servings })` (line 908), `getMealPlanForTrip(planId)` (line 893).
  - W16 → `getAssignments(planId)` (line 744), `getAssignment(planId, assignmentId)` (line 749), `getPlanMembers(planId)` (line 637), `getMealPlanForTrip(planId)` (line 893). The first three are used by `AssignmentsModal`; `getMealPlanForTrip` is used by `MealPlanModal`.
  - **No new endpoints, no payload changes.** Code-reviewer must reject any PR that edits `services/`, `clients/`, `databases/`, or `libs/`.
- **Aesthetic.** Reuse `theme.css` tokens; reuse `Button`, `Modal`, `Input`, `Select`, `FormField`, `useToast()` from prior phases.
- **Build gate.** `cd camper/webapp && npx tsc --noEmit && npm run build && npm run test` must pass on every PR.
- **Live updates preserved.** STOMP `usePlanUpdates` is read-only on this phase — no signature changes; the only addition is one or two `if (resource === '...') refetch()` branches inside its callback.
- **`MealPlanModal.tsx` is now ~1623 lines.** Phase 3 grows it further (W12 + W13 add ~150–200 lines in the RecipeBookView region). **No file split this phase** — sub-component exports (`OverviewView`, `RecipeBookView`, `ShoppingListView`) remain the testability pattern; W12 keeps the grid inside `RecipeBookView` rather than spawning a new file.

---

## Phase 3 ship order (4 PRs)

| # | Workstream | Branch | Depends on |
|---|-----------|--------|------------|
| 1 | **W12** Add a recipe to multiple days/meals at once | `clean-up-ui-w12-multi-day-recipe-add` | — (Phase 2 fully merged) |
| 2 | **W13** Quick-create recipe inside MealPlanModal | `clean-up-ui-w13-quick-create-recipe` | — (independent of W12; sequential to keep MealPlanModal diffs reviewable) |
| 3 | **W17** Member→servings reconcile banner | `clean-up-ui-w17-member-servings-reconcile` | — |
| 4 | **W16** Assignment progress badges on PlanPage icons | `clean-up-ui-w16-assignment-progress-badges` | W17 (reuses `mealPlan` state on PlanPage for the kitchen badge) |

W12 ↔ W13 share the RecipeBookView region; sequential. W17 ↔ W16 share PlanPage data fetches; sequential. The two pairs are independent and could in principle ship in parallel, but the orchestrator's serial review queue argues for one stack.

---

## What we decided NOT to do (Phase 3)

- **Build a `RecipeForm` simplified variant** (name + ingredients only) for W13. Plan.md says "the same form RecipesPage uses for create" — extraction wins; a simplified form would create a UX seam.
- **Build a batch endpoint** for W12. Plan.md explicitly says "loops existing endpoint." Sequential FE loop is the contract.
- **Add a `meal-plan` STOMP topic.** No backend changes. The meal-plan badge in W16 refreshes on PlanPage mount and on local member-edit refetch only. If a future phase adds the topic, W16's refresh logic gets one new branch in `usePlanUpdates`.
- **Refactor `MealPlanModal.tsx` into multiple files.** Already 1623 lines; W12 + W13 push it past 1800. The exported sub-components (`OverviewView`, `RecipeBookView`, `ShoppingListView`) remain the testability pattern. A split is a future cleanup, not Phase 3 scope.
- **Add a `<Tabs>` primitive** even though W12's grid uses checkboxes that look tab-like. W24 is Phase 4.
- **Add a global "out-of-sync" indicator** on the kitchen icon (e.g., warning dot when servings mismatch). The W17 banner is enough; piling state onto the icon is W16's badge surface and we keep that surface single-purpose (assignment counts / meal plan day count).

---

# PR 1 — W12: Multi-day recipe add (2D grid)

**Branch.** `clean-up-ui-w12-multi-day-recipe-add`

**Commit title.** `feat(clean-up-ui): W12 — multi-day recipe add grid`

**Acceptance (from plan.md).** A user can add Pasta Bolognese to Day 2 dinner, Day 3 dinner, and Day 5 lunch in one popover interaction. Removing the recipe still works per-cell from the Overview view.

**Dependencies.** Phase 2 fully merged.

## Files to modify

### `camper/webapp/src/components/MealPlanModal.tsx`

**State.** Replace the existing `(addDay, addMealType)` single-cell popover state at lines 95–99 with a Set-based selection. Keep `addingToMeal` as the popover-open flag (it already gates the popover at `MealPlanModal.tsx:1173`).

```tsx
// Old:
//   const [addDay, setAddDay] = useState<string>('');
//   const [addMealType, setAddMealType] = useState<MealType>('breakfast');
// New:
const [selectedCells, setSelectedCells] = useState<Set<string>>(new Set());
// Helper key: `${dayId}:${mealType}`
```

The `addMealType` state can be retained as a "remember last picked meal-type for the next add" UX nicety (default for the pre-checked cell). Keep it.

**Pre-check on popover open.** When the user clicks "Add to Meal Plan" (line 1204), seed `selectedCells` with the single key `${currentDay.id}:${addMealType}`:

```tsx
onClick={() => {
  setAddingToMeal({ recipeId: selectedRecipe.id, recipeName: selectedRecipe.name });
  const currentDay = mealPlan.days[activeDay] ?? mealPlan.days[0];
  if (currentDay) {
    setSelectedCells(new Set([`${currentDay.id}:${addMealType}`]));
  }
}}
```

**Grid markup.** Replace lines 1175–1201 (the two `<select>`s + Add/Cancel actions) with the grid. New JSX inside `mp-add-popover`:

```tsx
<div className="mp-add-popover">
  <div className="mp-add-grid" role="grid" aria-label="Pick days and meal types">
    <div className="mp-add-grid-corner" />
    {MEAL_TYPES.map(mt => (
      <div key={mt.key} className="mp-add-grid-col-header" role="columnheader">
        <span aria-hidden="true">{mt.icon}</span> {mt.label}
      </div>
    ))}
    {mealPlan.days.map(day => (
      <Fragment key={day.id}>
        <div className="mp-add-grid-row-header" role="rowheader">Day {day.dayNumber}</div>
        {MEAL_TYPES.map(mt => {
          const cellKey = `${day.id}:${mt.key}`;
          const checked = selectedCells.has(cellKey);
          return (
            <label
              key={cellKey}
              className={`mp-add-grid-cell ${checked ? 'mp-add-grid-cell--checked' : ''}`}
              role="gridcell"
            >
              <input
                type="checkbox"
                checked={checked}
                onChange={() => toggleCell(cellKey)}
                aria-label={`Day ${day.dayNumber} ${mt.label}`}
              />
            </label>
          );
        })}
      </Fragment>
    ))}
  </div>
  <div className="mp-add-popover-actions">
    <Button
      className="mp-add-confirm"
      onClick={onAddRecipeToMeal}
      disabled={selectedCells.size === 0}
    >
      {selectedCells.size <= 1 ? 'Add' : `Add to ${selectedCells.size} meals`}
    </Button>
    <Button
      variant="secondary"
      className="mp-add-cancel"
      onClick={() => { setAddingToMeal(null); setSelectedCells(new Set()); }}
    >
      Cancel
    </Button>
  </div>
</div>
```

`toggleCell` is a small helper:

```tsx
const toggleCell = (key: string) => {
  setSelectedCells(prev => {
    const next = new Set(prev);
    if (next.has(key)) next.delete(key); else next.add(key);
    return next;
  });
};
```

**Props plumbing.** `RecipeBookProps` (line 1059) currently passes `addDay`, `setAddDay`, `addMealType`, `setAddMealType`, `onAddRecipeToMeal`. Replace `addDay`/`setAddDay` with `selectedCells`/`setSelectedCells`. Keep `addMealType`/`setAddMealType` (used for the pre-check seed). Update both the prop type and the call-site at line 482–504.

**Submit handler.** Replace `handleAddRecipeToMeal` (line 267–278) with a sequential loop:

```tsx
const handleAddRecipeToMeal = async () => {
  if (!mealPlan || !addingToMeal || selectedCells.size === 0) return;
  // Materialize cells as a deterministic array (top-to-bottom, breakfast→snack).
  const orderedCells: { dayId: string; mealType: MealType }[] = [];
  for (const day of mealPlan.days) {
    for (const mt of MEAL_TYPES) {
      if (selectedCells.has(`${day.id}:${mt.key}`)) {
        orderedCells.push({ dayId: day.id, mealType: mt.key });
      }
    }
  }
  try {
    for (const cell of orderedCells) {
      // Sequential — preserves STOMP event ordering. Do NOT Promise.all.
      await api.addRecipeToMeal(mealPlan.id, cell.dayId, {
        mealType: cell.mealType,
        recipeId: addingToMeal.recipeId,
      });
    }
    if (orderedCells.length === 1) {
      const cell = orderedCells[0];
      const day = mealPlan.days.find(d => d.id === cell.dayId);
      const mt = MEAL_TYPES.find(m => m.key === cell.mealType);
      toast.success(`Added "${addingToMeal.recipeName}" to Day ${day?.dayNumber} ${mt?.label}`);
    } else {
      toast.success(`Added "${addingToMeal.recipeName}" to ${orderedCells.length} meals`);
    }
    setAddingToMeal(null);
    setSelectedCells(new Set());
    await loadMealPlan();
  } catch (err) {
    toast.error(err instanceof Error ? err.message : 'Failed to add recipe');
    // Leave the popover open with selectedCells intact so the user can retry.
  }
};
```

**Note on partial-success behavior.** Sequential loop with no rollback: if cell 3 of 5 fails, cells 1-2 are committed and cells 4-5 are unattempted. The error toast surfaces the failure; cells 1-2 are visible in the meal plan after `loadMealPlan()` because they really were added. The user can re-open the popover and retry the unattempted cells. This is the simplest correct behavior — full rollback would require deletes that themselves can fail. Document inline.

### `camper/webapp/src/components/MealPlanModal.css`

Add grid styles:

```css
.mp-add-grid {
  display: grid;
  grid-template-columns: auto repeat(4, minmax(64px, 1fr));
  gap: 4px 6px;
  margin-bottom: var(--space-md);
  max-height: 280px;
  overflow-y: auto;
}
.mp-add-grid-corner { /* empty cell at row=0 col=0 */ }
.mp-add-grid-col-header,
.mp-add-grid-row-header {
  font-size: 0.85rem;
  color: var(--charcoal);
  padding: 4px 6px;
  font-weight: 600;
}
.mp-add-grid-col-header { text-align: center; }
.mp-add-grid-row-header { text-align: right; align-self: center; }
.mp-add-grid-cell {
  display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--tan);
  border-radius: 4px;
  padding: 8px;
  cursor: pointer;
  background: rgba(255,255,255,0.4);
  transition: background-color 120ms, border-color 120ms;
}
.mp-add-grid-cell:hover { background: rgba(255,255,255,0.7); }
.mp-add-grid-cell--checked {
  background: var(--mint);
  border-color: var(--sage);
}
.mp-add-grid-cell input[type="checkbox"] {
  /* visually hidden but kept for a11y */
  position: absolute; opacity: 0; pointer-events: none;
}
/* Use ::before checkmark on the label when checked */
.mp-add-grid-cell--checked::before {
  content: '✓';
  color: var(--charcoal);
  font-weight: 700;
}
```

(Final CSS is the developer's call — token reuse is what matters; no new color variables.)

## Implementation notes

- **No props for `addDay`/`setAddDay` after this PR.** Search MealPlanModal for stale references; they're only used inside `RecipeBookView` and the popover open handler.
- **`Fragment` import.** Add to the existing `react` import line if not already present (`import { Fragment, useEffect, … } from 'react'`).
- **Keyboard a11y.** The native checkbox + label is keyboard accessible by default (Space toggles, Tab moves). The grid container gets `role="grid"`, headers `role="columnheader"`/`role="rowheader"`, cells `role="gridcell"`. Don't roll a custom keyboard-arrow handler in this PR — defer to W8 (Phase 6) if the standard table-grid arrow nav is needed.
- **No popover-outside-click handler change.** The existing popoverRef at line 1085 stays — clicking outside the popover does NOT close it (current behavior). The user clicks Cancel or completes Add. Don't bundle outside-click behavior into this PR.
- **`useToast()` is already imported** in MealPlanModal (Phase 1 W11). Reuse.

## Cascade impact

- `RecipeBookProps` interface (line 1059) — `addDay: string`, `setAddDay: (v: string) => void` removed; `selectedCells: Set<string>`, `setSelectedCells: Dispatch<SetStateAction<Set<string>>>` added. Caller at line 482–504 updates accordingly.
- No callers outside `MealPlanModal.tsx`. The component is local. Confirm with `grep -rn "RecipeBookView" camper/webapp/src/` — single match (definition site only).

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.multiDayRecipeAdd.test.tsx` (new) | (1) Open Recipe Book, select a recipe, click "Add to Meal Plan" → grid renders with rows = `mealPlan.days` and 4 mealtype columns; the cell at (`activeDay`, `addMealType`) is pre-checked. (2) Click 3 different cells → button label updates to "Add to 4 meals" (3 + 1 pre-checked); button is enabled. (3) Submit → mock `api.addRecipeToMeal` is called 4 times in order (top-left → bottom-right by day order then meal-type order); after resolution, popover closes (`addingToMeal` resets), `selectedCells` clears, and `loadMealPlan` is called. (4) Toast fires once with message containing "to 4 meals". (5) Single-cell submit → toast fires with single-meal message ("Day X dinner" form). (6) Uncheck the pre-checked cell so 0 cells remain → button disabled. (7) Cancel button closes the popover and clears selection. |

**Test setup.** Reuse Phase-1+2 `vi.hoisted()` + `vi.mock('../api/client', ...)` pattern. Pass a minimal `mealPlan` fixture with 3 days. Use `vi.spyOn` on the toast hook OR mock `useToast()` to assert calls.

```tsx
const mockToast = vi.hoisted(() => ({
  success: vi.fn(),
  error: vi.fn(),
  info: vi.fn(),
  dismiss: vi.fn(),
}));
vi.mock('../context/ToastContext', () => ({
  useToast: () => mockToast,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));
```

## Manual smoke checklist

- Open MealPlanModal → Recipe Book → select Pasta → Add to Meal Plan → grid renders.
- Pre-checked cell matches active day + last meal-type. Click 2 more cells. Button reads "Add to 3 meals".
- Submit. Toast: "Added 'Pasta' to 3 meals". Popover closes. Switch to Overview tab → Pasta appears under all 3 selected (day, meal-type) slots.
- Verify removal from Overview still works per-cell (existing behavior unchanged).
- Try with a single pre-checked cell → toast reads "Day X breakfast" form.
- Try a deliberately failing case (e.g., disconnect network) — error toast fires, popover stays open, selection retained.

---

# PR 2 — W13: Quick-create recipe inside MealPlanModal

**Branch.** `clean-up-ui-w13-quick-create-recipe`

**Commit title.** `feat(clean-up-ui): W13 — quick-create recipe inside MealPlanModal`

**Acceptance (from plan.md).** From Recipe Book tab, click "+ New Recipe" → fill name/servings/ingredients → save → recipe is selected and ready to add. Meal plan modal context is preserved throughout.

**Dependencies.** None coupled to W12, but ships sequentially because both touch the RecipeBookView region of `MealPlanModal.tsx`.

## Files to create

### `camper/webapp/src/components/RecipeForm.tsx`

The extracted form, derived from `RecipeCreateView` (RecipesPage.tsx:1207-1577). Public API:

```tsx
import type { Dispatch, SetStateAction } from 'react';
import type { IngredientResponse, RecipeResponse } from '../api/client';

export interface RecipeFormProps {
  ingredients: IngredientResponse[];
  /** When the form creates a new ingredient via the picker, parent updates the shared ingredient list. */
  setIngredients: Dispatch<SetStateAction<IngredientResponse[]>>;
  /** Called after `api.createRecipe` resolves successfully. The new RecipeResponse is passed back. */
  onSuccess: (newRecipe: RecipeResponse) => void;
  /** Called when the user clicks Cancel. */
  onCancel: () => void;
  /** Optional: hide the outer cancel/back chrome (used when nested inside a Modal that already provides one). Default false. */
  hideHeader?: boolean;
  /** Optional: override the submit button label. Default 'Add to Cookbook'. */
  submitLabel?: string;
}

export function RecipeForm(props: RecipeFormProps) { /* ... */ }
```

The body is a near-verbatim copy of `RecipeCreateView`'s body (lines 1218–1576) with three diffs:
1. Replace `navigate('/recipes')` after success with `props.onSuccess(newRecipe)`. To pass back the `RecipeResponse`, capture the result of `api.createRecipe(...)` (it already returns `RecipeResponse`).
2. Replace `navigate('/recipes')` on Cancel button click (lines 1337, 1562) with `props.onCancel()`.
3. Remove the `useToast()` call. Toast is the parent's responsibility — the parent decides what message fires (e.g., RecipesPage fires "Recipe saved as draft", MealPlanModal fires "Recipe added to cookbook"). Pass nothing about toasts through props.

**Constants** (`MEALS`, `THEMES`, `CATEGORIES`, `UNITS`, `DraftIngredient` type) are currently defined at the top of `RecipesPage.tsx`. Move them to `src/lib/recipeConstants.ts` so both `RecipesPage.tsx` and `RecipeForm.tsx` import from one place. Verify all current call sites for these constants.

### `camper/webapp/src/lib/recipeConstants.ts` (new)

```ts
export const MEALS = ['breakfast', 'lunch', 'dinner', 'snack'] as const;
export const THEMES = [/* ... existing values from RecipesPage.tsx top */];
export const CATEGORIES = [/* ... */];
export const UNITS = [/* ... */];

export interface DraftIngredient {
  ingredientId: string;
  ingredientName: string;
  quantity: number;
  unit: string;
}
```

Inspect `RecipesPage.tsx` lines ~1–60 for the actual values; copy verbatim.

### `camper/webapp/src/components/RecipeForm.css` (new)

Move (do not copy) the form-specific rules from `RecipesPage.css` whose class names are used by the form: `.recipes-form`, `.recipes-form-section`, `.recipes-field`, `.recipes-row`, `.recipes-input`, `.recipes-select`, `.recipes-textarea`, `.recipes-form-error`, `.recipes-form-actions`, `.recipes-cancel-btn`, `.recipes-submit-btn`, `.recipes-draft-ingredients`, `.recipes-draft-ingredient*`, `.recipes-form-container`, `.recipes-form-title`, `.recipes-form-subtitle`, `.ingredient-picker*`, `.ingredient-pill*`, `.recipe-detail__add-create-*`. Verify with `grep -rn "recipes-form\|ingredient-picker" camper/webapp/src/` — these classes are used only by the form. The remaining `recipes-*` classes (page chrome) stay in `RecipesPage.css`. Import `RecipeForm.css` from `RecipeForm.tsx`.

**If the move is too risky to vet in one PR**, alternate approach: keep CSS in `RecipesPage.css` and import it from both `RecipesPage.tsx` and `RecipeForm.tsx`. This is uglier but safer; pick whichever the developer is comfortable defending in code review. Document the choice in the commit body.

## Files to modify

### `camper/webapp/src/pages/RecipesPage.tsx`

`RecipeCreateView` (lines 1207–1577) shrinks to:

```tsx
function RecipeCreateView({
  ingredients,
  setIngredients,
  onRecipesMutated,
}: {
  ingredients: IngredientResponse[];
  setIngredients: Dispatch<SetStateAction<IngredientResponse[]>>;
  onRecipesMutated: () => void;
}) {
  const navigate = useNavigate();
  const toast = useToast();

  return (
    <div className="recipes-create-view">
      <div className="recipes-detail-header">
        <button className="recipes-back-btn" onClick={() => navigate('/recipes')}>
          {/* existing back arrow SVG */}
          Cancel
        </button>
      </div>
      <RecipeForm
        ingredients={ingredients}
        setIngredients={setIngredients}
        onSuccess={() => {
          toast.success('Recipe saved as draft');
          onRecipesMutated();
          navigate('/recipes');
        }}
        onCancel={() => navigate('/recipes')}
      />
    </div>
  );
}
```

Remove the now-extracted form state from `RecipesPage.tsx`. Imports of `MEALS` / `THEMES` / `CATEGORIES` / `UNITS` / `DraftIngredient` switch to `from '../lib/recipeConstants'`.

### `camper/webapp/src/components/MealPlanModal.tsx`

Add a "+ New Recipe" button at the top of the Recipe Book left page (above the search input at line 1090). Wire it to a nested `<Modal>` containing `<RecipeForm>`.

**State.** Inside `MealPlanModal` (top-level component body, around line 95 where existing modal state lives):

```tsx
const [showQuickCreate, setShowQuickCreate] = useState(false);
```

`ingredients` is already loaded by MealPlanModal (line 105 area; check). If not, add the same `getIngredients` mount-effect that RecipesPage uses, OR load lazily when the user opens the quick-create modal. **Recommend lazy load** — first click triggers `api.getIngredients()` if `ingredients` is empty.

Actually, re-reading MealPlanModal: `ingredients` is already loaded at the modal level for the shopping-list manual-add ingredient picker (Phase 1 W2 work touched this region). Verify with `grep -n "getIngredients\|ingredients\[\]" src/components/MealPlanModal.tsx`. If present, reuse. If not, add a one-shot lazy load on quick-create modal open.

**Top-of-RecipeBookView left page** (insert before line 1090):

```tsx
<button
  type="button"
  className="mp-book-quick-create"
  onClick={() => onOpenQuickCreate()}
>
  + New Recipe
</button>
```

`onOpenQuickCreate` is a new prop on `RecipeBookProps`, called from the parent component. Or, since RecipeBookView is exported but lives in the same file, we can pass `setShowQuickCreate` directly.

**Nested modal.** Render below the existing Modal in `MealPlanModal`'s JSX (or alongside it — since `Modal` from `components/ui/Modal` already supports stacking via z-index):

```tsx
{showQuickCreate && (
  <Modal
    isOpen
    onClose={() => setShowQuickCreate(false)}
    title="New Recipe"
    size="md"
    flavor="Add a recipe to the camp cookbook"
  >
    <RecipeForm
      ingredients={ingredients}
      setIngredients={setIngredients}
      onSuccess={(newRecipe) => {
        setShowQuickCreate(false);
        toast.success('Recipe added to cookbook');
        // Reload the recipes list so the new recipe shows up in Recipe Book.
        loadRecipes();
        // Auto-select the new recipe in the Recipe Book detail view.
        api.getRecipe(newRecipe.id).then(detail => setSelectedRecipe(detail)).catch(() => {});
      }}
      onCancel={() => setShowQuickCreate(false)}
    />
    <div className="mp-quick-create-more">
      <a
        href="/recipes/new"
        target="_blank"
        rel="noopener noreferrer"
        className="mp-quick-create-more-link"
      >
        More options →
      </a>
    </div>
  </Modal>
)}
```

The "More options" link opens the full RecipesPage create form in a new tab (preserves modal context, matches Phase-2 W4-carryover pattern).

**Recipes list refetch.** `loadRecipes` already exists inside MealPlanModal (or `loadAllData`). Verify and reuse. The new recipe must appear in the Recipe Book left-page list after creation; this happens automatically via the existing recipe list state once `loadRecipes()` re-runs.

**`setIngredients` propagation.** `MealPlanModal` already manages an `ingredients` array (verify); pass through to `<RecipeForm>` so the picker can add new ingredients (the form's "Create new ingredient" path needs `setIngredients` to keep the in-memory list in sync). If the modal does NOT currently own this state, add it as a one-shot mount fetch.

### `camper/webapp/src/components/MealPlanModal.css`

Add styles:

```css
.mp-book-quick-create {
  align-self: stretch;
  margin-bottom: 8px;
  padding: 6px 10px;
  border: 1px dashed var(--tan-deep);
  border-radius: 6px;
  background: rgba(255,255,255,0.4);
  color: var(--charcoal);
  font-weight: 600;
  cursor: pointer;
  transition: background-color 120ms;
}
.mp-book-quick-create:hover { background: rgba(255,255,255,0.7); }
.mp-quick-create-more {
  margin-top: 12px;
  text-align: center;
}
.mp-quick-create-more-link {
  font-size: 0.85rem;
  color: var(--lavender);
  opacity: 0.8;
  text-decoration: underline;
  text-underline-offset: 3px;
}
```

## Implementation notes

- **Don't bundle CSS-class renames into this PR.** The form keeps its existing `.recipes-*` class names. Renaming to `.recipe-form-*` is a follow-up and would explode the diff.
- **Don't migrate `RecipeImportView` or `RecipeEditView` to use `RecipeForm`.** The contract is clear: only the create flow extracts. Edit form has different fields (no ingredients picker), import form is a single URL input. Different shapes; don't unify.
- **Modal stacking.** The existing `Modal` component uses `z-index: 100` (per `Modal.css`). Two stacked modals work today (the toast container at z-index 1200 still occludes both). Test by opening the quick-create modal and triggering a toast; toast must appear on top.
- **No "More options" if PR scope creeps.** If review is tight, the "More options" link can defer to a follow-up. Plan.md calls it "Optionally" — minimum-viable: nested modal only, link omitted.
- **Auto-select after create.** After `setSelectedRecipe(detail)`, the user is one click away from "Add to Meal Plan" (W12 grid). This is the core UX win of W13 — preserves meal-plan context end-to-end.

## Cascade impact

Files that construct or reference `MEALS`, `THEMES`, `CATEGORIES`, `UNITS`, or `DraftIngredient`:
- `camper/webapp/src/pages/RecipesPage.tsx` — switches imports to `lib/recipeConstants`. Verify with `grep -n "MEALS\|THEMES\|CATEGORIES\|UNITS\|DraftIngredient" src/pages/RecipesPage.tsx`. Expected matches at top of file (constants defined) + ~10 use sites in `RecipeCreateView` + a handful in `RecipeDetailView` (existing ingredient resolve flow uses `CATEGORIES` and `UNITS`). All call sites swap to imports.
- `camper/webapp/src/components/MealPlanModal.tsx` — new caller; imports from `lib/recipeConstants` for any meal-type/category references inside RecipeForm (transitive).
- `camper/webapp/src/components/RecipeForm.tsx` — new caller.

Run `grep -rn "from '\.\./pages/RecipesPage'" camper/webapp/src/` to confirm no other file imports these constants from RecipesPage today. If found, switch.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/RecipeForm.test.tsx` (new) | (1) Renders all form fields (name, description, link, servings, meal, theme, ingredients picker). (2) Submitting with empty name → inline error "Name is required"; `api.createRecipe` not called. (3) Submitting with valid payload → calls `api.createRecipe` with the typed body; on resolution, calls `props.onSuccess` with the returned `RecipeResponse`. (4) Cancel button calls `props.onCancel`. (5) Adding a draft ingredient via the picker → ingredient appears in the draft list; submit includes it in the payload. (6) "Create new ingredient" path → calls `api.createIngredient`, calls `setIngredients` to push the new ingredient into shared state. |
| `src/components/MealPlanModal.quickCreateRecipe.test.tsx` (new) | (1) "+ New Recipe" button renders at the top of Recipe Book left page. (2) Clicking opens the nested Modal with the form. (3) Filling and submitting the form → mocked `api.createRecipe` resolves; nested modal closes; mocked `api.getRecipe(newId)` is called and `selectedRecipe` updates (assert via the right-page detail's recipe name). (4) Toast fires "Recipe added to cookbook". (5) "More options" link has `target="_blank"` and `href="/recipes/new"`. (6) Cancel button closes the nested modal without calling `api.createRecipe`. |
| `src/pages/RecipesPage.recipeCreateView.test.tsx` (new, regression) | (1) `/recipes/new` route still renders the form (now wrapped in `<RecipeCreateView>` → `<RecipeForm>`). (2) Submitting still navigates to `/recipes` and fires the existing "Recipe saved as draft" toast. Reuses the `MemoryRouter` + `vi.hoisted()` pattern from Phase 2. |

## Manual smoke checklist

- Open MealPlanModal → Recipe Book → "+ New Recipe" button visible at top of left page.
- Click → nested modal opens with the same form layout as `/recipes/new`.
- Fill name + 1 ingredient → Save → modal closes → toast "Recipe added to cookbook" → recipe appears in Recipe Book left list and is auto-selected on the right.
- Click "Add to Meal Plan" (W12 grid) on the new recipe → add succeeds end-to-end.
- "More options" link opens `/recipes/new` in a new tab; meal-plan modal stays open.
- `/recipes/new` regression: full create flow still works (smoke the same name/ingredient/save flow on the standalone page).

---

# PR 3 — W17: Member→servings reconcile banner

**Branch.** `clean-up-ui-w17-member-servings-reconcile`

**Commit title.** `feat(clean-up-ui): W17 — member→servings reconcile banner`

**Acceptance (from plan.md).** Adding a member when the meal plan is out of sync surfaces the offer. Clicking it updates servings (via existing `PUT /api/meal-plans/:id`). Dismiss hides it.

**Dependencies.** None.

## Files to create

### `camper/webapp/src/components/MealPlanReconcileBanner.tsx`

Small parchment-styled banner. Public API:

```tsx
export interface MealPlanReconcileBannerProps {
  mealPlanId: string;
  currentServings: number;
  desiredServings: number;
  /** Called after the PUT succeeds. Parent should refetch meal plan. */
  onUpdated: () => void;
  /** Called when user clicks Dismiss. */
  onDismiss: () => void;
}

export function MealPlanReconcileBanner(props: MealPlanReconcileBannerProps) {
  const [updating, setUpdating] = useState(false);
  const toast = useToast();

  const handleUpdate = async () => {
    setUpdating(true);
    try {
      await api.updateMealPlan(props.mealPlanId, { servings: props.desiredServings });
      toast.success(`Meal plan updated to ${props.desiredServings} servings`);
      props.onUpdated();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to update servings');
    } finally {
      setUpdating(false);
    }
  };

  return (
    <div className="meal-plan-reconcile-banner" role="status" aria-live="polite">
      <span className="meal-plan-reconcile-banner__icon" aria-hidden="true">⚠</span>
      <span className="meal-plan-reconcile-banner__text">
        Your meal plan is set for {props.currentServings} {props.currentServings === 1 ? 'serving' : 'servings'},
        but you have {props.desiredServings} adventurers in camp.
      </span>
      <Button
        size="sm"
        onClick={handleUpdate}
        disabled={updating}
        className="meal-plan-reconcile-banner__update"
      >
        {updating ? 'Updating…' : `Update to ${props.desiredServings}`}
      </Button>
      <Button
        size="sm"
        variant="ghost"
        onClick={props.onDismiss}
        className="meal-plan-reconcile-banner__dismiss"
        aria-label="Dismiss reminder"
      >
        ×
      </Button>
    </div>
  );
}
```

### `camper/webapp/src/components/MealPlanReconcileBanner.css`

Parchment-themed inline alert. Reuse `--ember`, `--lavender`, `--tan` tokens. No new color variables.

```css
.meal-plan-reconcile-banner {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 14px;
  background: rgba(232, 179, 99, 0.18);
  border: 1px solid var(--ember);
  border-radius: 8px;
  margin: 12px auto;
  max-width: 720px;
  font-size: 0.95rem;
  color: var(--charcoal);
}
.meal-plan-reconcile-banner__icon { font-size: 1.2rem; color: var(--ember); }
.meal-plan-reconcile-banner__text { flex: 1; }
.meal-plan-reconcile-banner__update { /* existing Button styles */ }
.meal-plan-reconcile-banner__dismiss { color: var(--charcoal); opacity: 0.5; }
.meal-plan-reconcile-banner__dismiss:hover { opacity: 1; }
```

## Files to modify

### `camper/webapp/src/pages/PlanPage.tsx`

**State.** Add meal plan + dismissed state below the existing `members` state (line 28-area):

```tsx
const [mealPlan, setMealPlan] = useState<MealPlanDetailResponse | null>(null);
const [reconcileDismissed, setReconcileDismissed] = useState<boolean>(() => {
  if (!planId) return false;
  return sessionStorage.getItem(`mealPlanReconcileDismissed:${planId}`) === '1';
});
```

**Fetch.** Inside `loadData` (line 39 area), add a parallel fetch:

```tsx
const [plans, memberData, mealPlanData] = await Promise.all([
  api.getPlans(),
  api.getPlanMembers(planId),
  api.getMealPlanForTrip(planId),  // returns MealPlanDetailResponse | null
]);
setPlan(plans.find(p => p.id === planId) || null);
setMembers(memberData);
setMealPlan(mealPlanData);
```

`getMealPlanForTrip` already exists at `client.ts:893` and returns `null` when no meal plan exists for the trip. The banner condition (below) gates on `mealPlan != null`.

**Banner trigger condition.**

```tsx
const memberCountForServings = members.filter(m => m.username).length; // exclude pending invites
const showReconcileBanner =
  mealPlan != null &&
  !reconcileDismissed &&
  mealPlan.servings !== memberCountForServings &&
  memberCountForServings > 0;
```

**Decision: count only registered members** (`m.username` truthy). Pending invites haven't joined the trip; counting them would inflate the desired servings before they accept. Document this inline.

**Render.** Below the AppHeader, above the campsite scene (line ~206):

```tsx
{showReconcileBanner && mealPlan && (
  <MealPlanReconcileBanner
    mealPlanId={mealPlan.id}
    currentServings={mealPlan.servings}
    desiredServings={memberCountForServings}
    onUpdated={() => {
      sessionStorage.removeItem(`mealPlanReconcileDismissed:${planId}`);
      setReconcileDismissed(false);
      loadData();
    }}
    onDismiss={() => {
      if (planId) sessionStorage.setItem(`mealPlanReconcileDismissed:${planId}`, '1');
      setReconcileDismissed(true);
    }}
  />
)}
```

**STOMP integration.** No changes to `usePlanUpdates` resource branches in this PR. The existing `members`/`plan` branches already trigger `loadData()`, which now also refetches the meal plan. Net effect: when another user adds/removes a member, the banner re-evaluates automatically.

## Implementation notes

- **Why `MealPlanDetailResponse` not `MealPlanResponse`?** `getMealPlanForTrip` returns the detail response (with days). We don't need days here, but using the same shape avoids a separate summary fetch and keeps W16 reuse trivial.
- **`memberCount` vs `memberCountForServings`.** PlanPage already uses `members.length` (line 153) for the campfire circle layout. That count includes pending. The banner uses the registered-only count. Don't conflate; keep both variables explicit.
- **What if `mealPlan` is null?** No banner — the user has no meal plan yet, so there's nothing to reconcile. The empty-state nudge is a separate Phase-5 concern (W10).
- **What if `memberCountForServings === 0`?** Edge case after the owner removes everyone (impossible — they can't remove themselves while owner). Guard anyway.
- **Why `sessionStorage` not `localStorage`?** Per plan.md W17: "Dismissable; remembers dismissal for the session (sessionStorage)." Per-tab. The next browser session re-evaluates from scratch — desired UX.
- **Banner toast on update success.** The banner fires its own toast via `useToast()`. After success, `onUpdated()` clears `reconcileDismissed` (in case servings later drift again) and calls `loadData()` to refresh `mealPlan.servings`.
- **Race with usePlanUpdates.** If a STOMP `members` event arrives while the user is mid-click on "Update to N", the `await` chain still completes; the subsequent `loadData()` is idempotent.
- **No new translation/copy files.** Plain string literals match the rest of the app.

## Cascade impact

- `PlanPage.tsx` — add `mealPlan` state, banner. No exported types changed.
- `MealPlanDetailResponse` import added.
- No callers of MealPlanReconcileBanner outside PlanPage. New component.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanReconcileBanner.test.tsx` (new) | (1) Renders text matching "set for {currentServings}" and "have {desiredServings} adventurers". (2) Singular form when `currentServings === 1`. (3) Clicking Update → calls `api.updateMealPlan` with `{ servings: desiredServings }`; on resolution, calls `onUpdated`; toast.success fires. (4) Clicking Dismiss → calls `onDismiss`. (5) Update failure → calls `toast.error`; `onUpdated` not called. (6) Update button disables during the request. |
| `src/pages/PlanPage.reconcileBanner.test.tsx` (new) | (1) When `getMealPlanForTrip` returns `mealPlan.servings = 4` and members has 5 registered users → banner renders. (2) When servings === count → no banner. (3) When `getMealPlanForTrip` returns `null` → no banner. (4) When `sessionStorage[mealPlanReconcileDismissed:<planId>]==='1'` → no banner. (5) Clicking Dismiss writes the sessionStorage key. (6) After successful Update, sessionStorage key is cleared. |

**Test setup.** Reset sessionStorage in `beforeEach`:

```tsx
beforeEach(() => {
  sessionStorage.clear();
  vi.clearAllMocks();
});
```

Mock `api.getMealPlanForTrip`, `api.getPlans`, `api.getPlanMembers`, `api.updateMealPlan` via the established `vi.hoisted()` pattern. Render `PlanPage` inside a `MemoryRouter initialEntries={['/plans/abc']}` (matches Phase-2 routing test pattern).

## Manual smoke checklist

- Open a plan with a meal plan and 4 servings; add a member → banner appears reading "set for 4 servings, but you have 5 adventurers".
- Click "Update to 5" → toast "Meal plan updated to 5 servings"; banner disappears (servings now match).
- Add another member → banner returns ("4 → 6").
- Click Dismiss → banner disappears; refresh page (same tab) → still dismissed.
- Open a new tab → banner returns (sessionStorage scope).
- Open a plan with no meal plan → no banner.
- Plan with only the owner (1 registered member) and 1-serving meal plan → no banner.

---

# PR 4 — W16: Assignment progress badges on PlanPage icons

**Branch.** `clean-up-ui-w16-assignment-progress-badges`

**Commit title.** `feat(clean-up-ui): W16 — assignment progress badges on PlanPage icons`

**Acceptance (from plan.md).** A glance at PlanPage tells the user what's planned vs. unplanned.

**Dependencies.** W17 must be merged to main (W16 reads `mealPlan` state W17 introduces on PlanPage).

## Files to modify

### `camper/webapp/src/components/InteractableItem.tsx`

Add a `badge` prop. Optional. Renders a small parchment chip overlaying the top-right of the icon's content area.

```tsx
interface BadgeContent {
  label: string;          // e.g., "3/5", "✓", "5 days", "—"
  tone: 'progress' | 'complete' | 'empty';
}

interface Props {
  id: string;
  label: string;
  x: number;
  y: number;
  children: React.ReactNode;
  onClick: () => void;
  badge?: BadgeContent;     // NEW
  badgeTooltip?: string;    // NEW — hover text for the badge specifically (e.g., "3 of 5 adventurers assigned")
}

export function InteractableItem({ id, label, x, y, children, onClick, badge, badgeTooltip }: Props) {
  const [hovered, setHovered] = useState(false);
  return (
    <button
      className={`interactable-item ${hovered ? 'interactable-item--hovered' : ''}`}
      style={{ left: `${x}%`, top: `${y}%` }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={onClick}
      data-item={id}
    >
      <div className="interactable-item__glow" />
      <div className="interactable-item__content">
        {children}
        {badge && (
          <span
            className={`interactable-item__badge interactable-item__badge--${badge.tone}`}
            title={badgeTooltip}
            aria-label={badgeTooltip || `${label}: ${badge.label}`}
          >
            {badge.label}
          </span>
        )}
      </div>
      <div className={`interactable-item__tooltip ${hovered ? 'interactable-item__tooltip--visible' : ''}`}>
        <span className="tooltip-icon">&#9733;</span>
        {label}
      </div>
    </button>
  );
}
```

### `camper/webapp/src/components/InteractableItem.css`

Add badge styling:

```css
.interactable-item__badge {
  position: absolute;
  top: -6px;
  right: -10px;
  min-width: 26px;
  height: 22px;
  padding: 0 6px;
  background: var(--parchment);
  border: 1.5px solid var(--tan-deep);
  border-radius: 11px;
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--charcoal);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
  box-shadow: 0 2px 4px rgba(0,0,0,0.15);
  transition: transform 200ms;
}
.interactable-item--hovered .interactable-item__badge { transform: scale(1.08); }
.interactable-item__badge--progress { color: var(--charcoal); }
.interactable-item__badge--complete { background: var(--mint); border-color: var(--sage); }
.interactable-item__badge--empty {
  background: rgba(255,255,255,0.6);
  border-style: dashed;
  color: var(--charcoal);
  opacity: 0.6;
}
```

### `camper/webapp/src/pages/PlanPage.tsx`

**State.** Below the W17 `mealPlan` state, add assignments state:

```tsx
const [assignmentDetails, setAssignmentDetails] = useState<AssignmentDetail[]>([]);
```

**Fetch.** In `loadData`, add the assignments list + parallel detail fetch (mirrors `AssignmentsModal.tsx:371-373` pattern):

```tsx
const [plans, memberData, mealPlanData, assignmentsList] = await Promise.all([
  api.getPlans(),
  api.getPlanMembers(planId),
  api.getMealPlanForTrip(planId),
  api.getAssignments(planId),
]);
setPlan(plans.find(p => p.id === planId) || null);
setMembers(memberData);
setMealPlan(mealPlanData);
const details = await Promise.all(
  assignmentsList.map(a => api.getAssignment(planId, a.id))
);
setAssignmentDetails(details);
```

This adds 1 + N requests. With typical N≤8 assignments per plan, this is acceptable on PlanPage mount. If smoke shows latency issues, **fallback option:** lazy-fetch assignments only when the user has fully loaded (not gating campfire render); render badges as a skeleton chip until details arrive.

**Live updates.** Extend the existing `usePlanUpdates` callback (line 71) to refetch on `assignments` events. PlanPage already increments `assignmentsRefreshKey` for the modal; we add a parallel branch to update PlanPage's own `assignmentDetails`:

```tsx
usePlanUpdates(planId, useCallback((message) => {
  const { resource } = message;
  if (resource === 'plan' || resource === 'members') {
    loadData();  // also refreshes assignmentDetails + mealPlan via the unified loader
  }
  if (resource === 'assignments') {
    setAssignmentsRefreshKey(k => k + 1);
    loadData();  // NEW: also refresh PlanPage's badge data
  }
  // … existing branches
}, [loadData]));
```

This makes badge counts live-update as members join/leave assignments from the AssignmentsModal — a UX win.

**Compute badges.**

```tsx
const memberCountForBadges = members.filter(m => m.username).length;

function computeAssignmentBadge(type: 'tent' | 'canoe'): BadgeContent {
  const filtered = assignmentDetails.filter(a => a.type === type);
  const assignedUserIds = new Set<string>();
  for (const a of filtered) {
    for (const m of a.members) assignedUserIds.add(m.userId);
  }
  const assigned = assignedUserIds.size;
  const total = memberCountForBadges;
  if (total === 0) return { label: '—', tone: 'empty' };
  if (assigned === 0) return { label: `0/${total}`, tone: 'empty' };
  if (assigned === total) return { label: '✓', tone: 'complete' };
  return { label: `${assigned}/${total}`, tone: 'progress' };
}

const tentBadge = computeAssignmentBadge('tent');
const canoeBadge = computeAssignmentBadge('canoe');
const kitchenBadge: BadgeContent =
  mealPlan == null
    ? { label: '—', tone: 'empty' }
    : { label: `${mealPlan.days.length}d`, tone: 'progress' };

const tentBadgeTooltip = tentBadge.tone === 'empty' && tentBadge.label === '—'
  ? 'No adventurers in camp yet'
  : `${tentBadge.label === '✓' ? memberCountForBadges : tentBadge.label.split('/')[0]} of ${memberCountForBadges} adventurers assigned to a tent`;
// (similar for canoe and kitchen)
```

**Render.** Pass `badge` and `badgeTooltip` to the existing InteractableItem instances at lines 216–234:

```tsx
<InteractableItem id="tent" label="Tents & Canoe Pairings" x={10} y={58}
  onClick={() => setActiveModal('assignments')}
  badge={tentBadge}
  badgeTooltip={tentBadgeTooltip}
>
  <TentSVG />
</InteractableItem>

<InteractableItem id="kitchen" label="Camp Kitchen & Meals" x={88} y={78}
  onClick={() => setActiveModal('kitchen')}
  badge={kitchenBadge}
  badgeTooltip={kitchenBadgeTooltip}
>
  <KitchenSVG />
</InteractableItem>
```

The "tent" item's badge represents both tent and canoe progress merged into one — but `plan.md` W16 says badges go on "tent/canoe icons" separately. Currently the tent and canoe share one icon (`InteractableItem id="tent" label="Tents & Canoe Pairings"`). **Decision: render a single combined badge on the tent icon** showing the worst-of (i.e., the most behind dimension), with tooltip listing both. Rationale: there's only one icon to attach to. Document the design choice; if the user wants visually separated badges later, splitting the icon into two is a Phase-6 mobile-pass decision (W26 already restructures this region).

```tsx
function combineTentAndCanoeBadge(): { badge: BadgeContent; tooltip: string } {
  const tent = computeAssignmentBadge('tent');
  const canoe = computeAssignmentBadge('canoe');
  // Worst-of presentation; tooltip shows both
  const worst = (tent.tone === 'empty' || canoe.tone === 'empty') ? 'empty'
              : (tent.tone === 'complete' && canoe.tone === 'complete') ? 'complete'
              : 'progress';
  const label = worst === 'complete' ? '✓'
              : worst === 'empty'    ? '—'
              : `${Math.min(...[tent, canoe].filter(b => b.tone === 'progress').map(b => parseInt(b.label.split('/')[0], 10)))}/${memberCountForBadges}`;
  const tooltip = `Tents: ${tent.label} · Canoes: ${canoe.label}`;
  return { badge: { label, tone: worst }, tooltip };
}
```

Equipment / itinerary / log book icons get **no badge** in this PR — Phase 4+ may add them, plan.md W16 doesn't specify equipment/itinerary.

## Implementation notes

- **`Assignment.type`** is already `'tent' | 'canoe'` per `client.ts:101`. Use directly.
- **Deduplication.** A user can be in multiple tents/canoes (different overlapping nights) — the spec doesn't preclude it. The Set-of-userIds dedup is correct: "members in **at least one** tent assignment" per plan.md.
- **STOMP refetch order.** When `assignments` events arrive, both `assignmentsRefreshKey++` (refreshes the open modal) and `loadData()` (refreshes the PlanPage data) run. Both are independent and idempotent.
- **Performance.** N+1 fetches on mount. Typical N is 2–6. Acceptable. If a future plan has 50+ assignments and this becomes slow, the BE can add a `?expand=members` flag on `getAssignments` — but that's a future BE change, out of scope.
- **No badge on equipment / itinerary / log book.** plan.md W16 only mentions tent/canoe and kitchen explicitly. Don't expand scope.
- **No animation on badge count change.** Reduced motion concerns aside, animating the count is polish-time work (Phase 4+).

## Cascade impact

- `InteractableItem.tsx` — props grow by 2 optional fields. All 5 existing call sites in `PlanPage.tsx` continue to compile (props optional). Verify with `grep -rn "<InteractableItem" camper/webapp/src/` — 5 matches, all in PlanPage.
- `PlanPage.tsx` — adds `assignmentDetails` state, +1 STOMP branch, +1 fetch in `loadData`. No exported types changed.
- No tests outside PlanPage / InteractableItem reference these.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/InteractableItem.test.tsx` (new) | (1) Renders without badge when `badge` prop is absent. (2) With `badge={ label: '3/5', tone: 'progress' }`, badge text "3/5" renders. (3) With `tone: 'complete'`, the badge has the `--complete` modifier class. (4) `badgeTooltip` prop populates `title` and `aria-label`. |
| `src/pages/PlanPage.badges.test.tsx` (new) | (1) With 5 registered members, 1 tent containing 3 members → tent/canoe combined badge label is "3/5" (worst-of since canoe is 0). (2) With all members assigned to tents AND canoes → badge label "✓", tone "complete". (3) With 0 members assigned → badge label "0/5", tone "empty". (4) With `getMealPlanForTrip` returning a plan with 5 days → kitchen badge "5d". (5) With null meal plan → kitchen badge "—" empty tone. (6) `getAssignments` returning empty → tent badge "0/5". |

**Test setup.** Mock `api.getAssignments` to return assignments with various `type` values, and `api.getAssignment` to return `AssignmentDetail` objects with `members` arrays. Use the same `vi.hoisted()` pattern.

## Manual smoke checklist

- Open a plan with 5 registered members.
- 0 tent assignments → badge reads "0/5" (empty tone, dashed border).
- Create 1 tent with 3 members → badge reads "3/5".
- Add 2 more to tents → "✓" with mint background.
- Verify hover tooltip on the badge reads `Tents: 5/5 · Canoes: …`.
- Kitchen icon: 0 meal plan → "—"; create meal plan with 3 days → "3d"; add 2 days → "5d".
- Open AssignmentsModal, add a member to a tent — close the modal — PlanPage badge updates live (STOMP-driven refetch).
- Refresh PlanPage with assignments + meal plan in place; badges render after the loading state, not before (don't render stale "—" briefly).

---

# Build / test commands (run after every PR)

```bash
cd camper/webapp
npx tsc --noEmit   # quick type-check
npm run build      # canonical type-check + bundle (tsc -b is stricter)
npm run test       # vitest run
```

Code-reviewer must reject any PR where any of the three fails. Code-reviewer must also reject any PR that modifies files outside `camper/webapp/src/` (or `package.json`/`vitest.config.ts`/`src/test/setup.ts`, but Phase 3 doesn't touch infra).

# Open questions / flags

- **A. (Documented, not blocking.)** No `meal-plan` STOMP topic exists today. The kitchen badge in W16 refreshes on PlanPage mount + on local member refetch only. This is a known limitation, NOT a bug. Adding the topic is a future BE change; out of scope.
- **B. (Documented, not blocking.)** W16 renders one combined tent+canoe badge because the icon is shared. If splitting them is desired, that's an icon/layout change that should bundle with W26 (Phase 6 mobile work) which already restructures this region.

All other Phase 3 plan-gate questions resolved (see header).
