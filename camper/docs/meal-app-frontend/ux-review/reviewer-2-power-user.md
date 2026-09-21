# UX review 2 — weekly power user

Date: 2026-09-20 · Branch: meal-app-frontend (PR #315) · Record only, not an agreed backlog

UX review 2 (power user): the shopping list and plan recipe picker are already fast. Recipe entry and import have two blockers and four high-severity bugs. Full report below.

Everything was driven live at http://r2.localhost:3000 as alice, in a 500px-wide window. Each finding is tagged "(live)" if observed in the browser or "(code)" if inferred from source. Hard reloads took 2–4s to become interactive and dropped input during that time. I did not count that as a finding (Vite dev server, three reviewers sharing it), but it is worth checking on a production build.

## 1. Verdict

The end of the pipeline (plan, shopping list) is fast today. Adding recipes in the plan picker is one tap each and optimistic. Check-off and quick add are instant, and ten rapid mutations produced only two refetches. The start of the pipeline (ingredients, recipes, import) is where a weekly user loses time, mostly to six defects: two blockers and four high-severity bugs. Two of the defects are blockers on core paths. Line entry on New recipe scrolls the whole app frame off screen until Save is unreachable. A successful import leaves the user stuck on the import sheet.

Once those are fixed, the biggest weekly time-saver is starting this week's plan from last week's. That means a "Copy of <last plan>" option in New plan that lands directly in the recipe picker. Today that takes about 9 taps plus a rename, and it could take 2.

## 2. Task timing (touch taps, typing excluded)

| Task | Today | Achievable | How |
|---|---|---|---|
| Reach "Add ingredients" | 3 (Recipes → Ingredients → Add) | 3 | Fine. |
| Add an ingredient in the rapid sheet | 0 taps + Enter when category and unit are unchanged. 4 more taps to change both (2 selects × 2). Entries typed fast are lost (#8). | 0–2 | Category chips, unit default derived from category, clear the name immediately. |
| New recipe with 6 existing ingredients | About 8 (New, 6 result taps, Save), using Enter to move between fields. This is excellent when bugs #1, #3 and #4 do not hit. | 8 | Fix the bugs and keep the flow. |
| Add 6 lines to an existing recipe | 24 (4 per line, plus 12 sheet animations) | 6–7 | #7: keep the sheet open and move focus to quantity. |
| Inline-create an ingredient mid-recipe | 5–6 (Create row, 2 selects × 2, Create), with the form below the fold | 2–3 | #20. |
| Import a recipe | 2 taps + paste, then blocked for up to 60s, then stuck (#2) | 2, then keep working | #2, #19. |
| Create a plan | 2, plus 1 tap per extra serving, plus 1 more to open the picker | 2 | #11. |
| Reuse last week's plan | 3 (plan → edit → Duplicate), plus about 5 to rename "… copy" | 2 | #10. |
| Add a recipe to a plan from the picker | 1 each | 1 | Keep as is. |
| Add a recipe to a plan from the library or a recipe page | 2, with no undo and no "already in plan" state | 1 | #13. |
| Share a plan | 1 | 1 | Keep as is. |
| Get to the shopping list | 1 (tab) | 1 | Keep as is. |
| Check off an item | 1, instant | 1 | Keep as is. |
| Switch plan | 2 | 2 | Fine. |

## 3. Suggestions, ranked

**1. Ingredient picker focus scrolls the whole app frame off screen — Blocker (live). Effort S.**
- Where: `webapp/src/components/IngredientPicker.tsx:217-225` (`scrollIntoView` in `onFocus`), `styles/global.css`, `lib/viewportGuard.ts`.
- Today:
  - Each picker focus scrolls `document.body` as well as the scroll container. `overflow: hidden` elements can still be scrolled programmatically.
  - After 6 lines on /recipes/new I measured `body.scrollTop = 217` with `scrollHeight` 973 against `clientHeight` 755.
  - The header and Save button were off screen and the tab bar floated mid-screen over a blank band.
  - The shift persists after blur. `viewportGuard` resets only `document.scrollingElement` (html) and skips while an input is focused. The user cannot scroll it back.
- Change:
  - Replace `scrollIntoView` with a manual scroll of the nearest real scroller: `const s = el.closest('.app-shell__scroll, <sheet body selector>'); s.scrollTo({ top: s.scrollTop + el.getBoundingClientRect().top - s.getBoundingClientRect().top - 8, behavior: 'smooth' })`.
  - Set `html, body { overflow: clip }`, because `clip` cannot be scrolled programmatically.
  - In `viewportGuard.reset()`, also zero `document.body.scrollTop`.
- Why: this is the core data-entry screen, and the more lines you add the worse it gets.

**2. A successful import leaves the user stuck on the import sheet — Blocker (live). Effort S.**
- Where: `pages/recipes/ImportRecipeSheet.tsx:35-39,73-77` and `components/useSheet.ts:79-84`.
- Today:
  - POST /api/recipes/import returned 201.
  - `sheet.close({to})` then runs before React re-renders `isPending=false`, so the sheet's own `canClose` guard blocks the close.
  - The sheet stays open, shows the toast "Still reading the recipe — hang tight", and never navigates.
  - Tapping Import again returns a duplicate CONFLICT.
- Change:
  - Add `force?: boolean` to `CloseTarget` and skip the `canClose` check when it is set.
  - Call `sheet.close({ to, replace: true, force: true })` here.
  - Alternatively, track the in-flight state in a ref that is set to false before `close`.

**3. Enter, or the phone keyboard's "next" key, in the Name field saves the recipe — High (live). Effort S.**
- Where: `NewRecipePage.tsx:79-89` and `EditRecipePage.tsx:143-153`.
- Today: typing "[R2] Chili" and pressing Enter created an empty recipe and navigated away. `enterKeyHint="next"` only relabels the key, and implicit form submission still fires.
- Change: on the Name input, `onKeyDown` Enter should `preventDefault()` and focus the next field. For a new recipe, jump straight to the ingredient picker, because Description, Source, Meal and Theme are optional. Better still, remove implicit submit altogether and make Save the only way to submit.

**4. A fully typed but un-added line is silently dropped on Save — High (live). Effort S–M.**
- Where: `LinesEditor.tsx:35-40` (pending state is private) and `NewRecipePage.tsx:42`.
- Today: "garlic / 3 / clove" was sitting in the add row. I tapped Save and the recipe was saved without it.
- Change:
  - Expose the pending state with `onPendingChange({ingredient, quantity, unit} | null)`.
  - In `handleSubmit`, if the pending line is valid, append it automatically.
  - If an ingredient is selected but the quantity is empty or invalid, block the save with `Finish the "garlic" line or clear it.` and focus the quantity field.

**5. Scroll position leaks into the next page and is lost on Back — High (live). Effort M.**
- Where: `components/AppShell.tsx`. There is one shared `.app-shell__scroll` and no scroll management anywhere (grep found none).
- Today:
  - I opened a recipe from a scrolled list and the detail page rendered at `scrollTop=201`, with the draft badge and description cut off.
  - Ingredients scrolled to 1500 → another tab → Back returned to 0.
  - With 60 recipes, every "open a recipe, go back" restarts at the top of the list.
- Change, in AppShell:
  - Keep a `Map<location.key, scrollTop>`, saved on scroll.
  - On a location change whose page-level match id changed (use `useMatches()`, and ignore sheet child routes), restore the saved value inside `requestAnimationFrame` when `useNavigationType()==='POP'`, otherwise set 0.

**6. The New recipe form loses everything on refresh, Back or a tab-bar tap — High (code: all state is `useState`; not tested live). Effort M.**
- Change:
  - Persist `{name, description, servings, webLink, meal, theme, lines}` to `sessionStorage['meal-planner.new-recipe-draft']` on change.
  - Restore it on mount with a toast "Draft restored" and a `Discard` action.
  - Clear it after create succeeds.
  - Do not add a confirm dialog.

**7. Adding lines to an existing recipe takes 4 taps and 2 animations per line — High (live). Effort S, or M for the inline version.**
- Where: `pages/recipes/AddLineSheet.tsx:27-47`.
- Today: after picking an ingredient, focus stays in the picker. `LinesEditor` moves focus to quantity; this sheet does not. After Add, the sheet closes.
- Change:
  - Add a quantity ref and focus it in `handleSelect`.
  - On success, do not close. Reset ingredient, quantity and unit, bump a picker `key` with `autoFocus`, and show a toast such as "Added 1 lb ground beef".
  - Add a soft "Done" button. `NewIngredientSheet` already works this way.
  - A better version on `EditRecipePage` is to render the same inline add row as `LinesEditor`, wired to `useAddRecipeIngredient`. That page already says "Changes save immediately".

**8. Rapid ingredient add drops fast entries — High (live). Effort S.**
- Where: `pages/ingredients/NewIngredientSheet.tsx:34-60,133`.
- Today:
  - `setName('')` runs after the `await`, so text typed during the request is wiped.
  - The submit button is `loading` (disabled), so Enter is swallowed.
  - I typed three names back to back and only two were created, with no error.
- Change: copy `ShoppingPage.handleQuickAdd` (`ShoppingPage.tsx:163-175`).
  - Clear the name and refocus immediately.
  - Call `mutate` without awaiting it.
  - Never disable the button.
  - On error, restore the text only if the field is still empty, and show the error.
  - The duplicate check is already synchronous.

**9. Bought items revert to looking never-bought when the plan changes — High (live). Effort S–M.**
- Where: `pages/shopping/ShoppingRowItem.tsx:42-43` and `lib/shoppingRows.ts:66-67,126`.
- Today:
  - I checked 5 rows, then went back, removed a recipe and changed servings from 4 to 6.
  - The list went from "5 of 26" to "1 of 23". Bell pepper, garlic and lettuce were unchecked with no hint that I already had some.
  - The server sends `more_needed` and `deriveOverallStatus` computes it, but the row renders only done or not done.
- Change:
  - For `overallStatus === 'more_needed'`, show `checked="indeterminate"` and a quantity line "¼ whole more · have ½". Compute required − purchased per entry in a new `formatRemainingText(row)`.
  - Tapping the row still marks it done.
  - Also add an explanatory caption, "No longer in the plan", to the struck-through row that has the Clear button.

**10. One-step "start from last week" — Medium (live). Effort S–M, no backend change.**
- Where: `pages/plans/NewPlanSheet.tsx` and `EditPlanSheet.tsx:82-90`.
- Today: open the plan → edit → Duplicate, which lands on "[R2] Week 39 copy", then about 5 taps to rename. Browser Back from the copy reopens the original's edit sheet, because `close({to})` has no `replace`.
- Change:
  - In New plan, add a "Start from" row of chips: `Empty` and `Copy of <most recent plan name>`.
  - With copy selected, submit calls `useDuplicatePlan` with `{planId, name}`. `DuplicateMealPlanRequest.name` already exists on the backend.
  - Default the name to "Week of Sep 21" (next Monday) in both modes.
  - Default servings to the source plan's value.
  - In EditPlanSheet, add `replace: true` and pass a date-based name instead of "copy".

**11. A new plan should land in the recipe picker and remember servings — Medium (live). Effort S.**
- Where: `NewPlanSheet.tsx:19,32`.
- Change:
  - Close to `/plans/${plan.id}/add`.
  - Initialise servings from `localStorage['meal-planner.last-servings']`, written on create and on stepper change. A household of 4 currently taps + twice every week.
  - In `Stepper.tsx`, let a tap on the number open a numeric input.

**12. Recipe picker at scale — Medium (live and code). Effort S each.**
- Where: `pages/plans/AddRecipeToPlanSheet.tsx`.
- With 60 recipes:
  - The query persists after an add and there is no clear button, so it has to be deleted by hand before the next search. After `handleAdd`, call `input.select()`, and add the same clear button that RecipesPage has.
  - Added rows are `disabled`, so a mis-tap cannot be undone in place. Make them toggles that call `useRemoveRecipeFromPlan`.
  - There are no meal chips or row meta. Reuse the RecipesPage chips and show "Serves N · Dinner".
- Optional backend change (M): add `lastPlannedAt` to `RecipeResponse`, to support a "Recent" group at the top of the picker.

**13. One-tap add from the library — Medium (live). Effort M.**
- Where: `RecipesPage.tsx:245-254` and `AddToPlanSheet.tsx`.
- Today: tap ＋, tap the plan, get a toast with no Undo. The row never shows that the recipe is in the plan.
- Change:
  - When a selected plan exists (`getSelectedPlanId`), ＋ adds directly to it. The toast reads "Added to Week 39" with an `Undo` action.
  - The button becomes a filled ✓ when `usePlan(selectedPlanId)` contains the recipe, and tapping the ✓ removes it.
  - Choosing a different plan stays on the recipe detail page.
  - The AddToPlanSheet empty state is currently a dead end ("create one from the Plans tab first"). Add a button `New plan with this recipe`.

**14. Refetch storm when adding recipes — Medium (live). Effort S.**
- Where: `queries/plans.ts:307-312` and `:416-421`.
- Today: 5 adds produced 5 POSTs and 6 GETs of the plan detail, which is the slow N+1 endpoint, plus shopping invalidations.
- Change: use the same `isMutating({mutationKey: planKey(planId)}) === 1` gate as `invalidateIfLast` in `queries/shopping.ts:46`.

**15. The toast covers the primary call to action for 4 seconds — Medium (live). Effort S.**
- Where: `components/Toast.css:5`.
- Today: after removing a recipe, my tap on "Shopping list" landed on the Undo toast, which sits exactly over the BottomBar. The same happens over quick-add and "Add to plan".
- Change: `body:has(.bottom-bar) .toast-region { bottom: calc(76px + 68px + env(safe-area-inset-bottom)); }`.

**16. Search inputs — Medium (live). Effort S.**
- Where: `IngredientsPage.tsx:72-87` and `RecipesPage.tsx:94-116`.
- Dropped characters:
  - The inputs are controlled directly from `?q=`. React Router 7 runs navigations inside `startTransition`, so fast input drops characters.
  - Typing "[R2]" instantly left "]" in the field. Slow per-key typing was fine.
  - Change: hold the value in local state and sync it to the URL in a debounced (150ms) `replace`.
- Sticky controls: the search field and chips scroll away with the list. Make the controls block `position: sticky; top: <header height>`.
- The Ingredients search has no clear button.
- Recipe search matches name only. Also match `theme` and `meal`, and add theme chips.

**17. Meal and Theme cannot be cleared once set — Medium (live). Effort S.**
- Where: `EditRecipePage.tsx:216-239` and `NewRecipePage.tsx:152-175`.
- Today: there is no "None" item, although `fieldPatch` and the backend support clearing.
- Change: add `<Select.Item value="none">None</Select.Item>` and map it to ''.

**18. Saving on Edit recipe stays on the edit page — Medium (live). Effort S.**
- Change: after success, `navigate(backTo, {replace: true})` and keep the toast.

**19. Make import non-blocking — Medium (code; the local stub answers in about 3s). Effort S–M.**
- Change:
  - On submit, close the sheet immediately and show a toast "Importing…".
  - Show a pending skeleton row at the top of the Recipes list.
  - When the import finishes, reuse the existing unmounted path (`ImportRecipeSheet.tsx:79-82`, the "Recipe imported · Open" toast).
  - Add a "Paste" button that uses `navigator.clipboard.readText()`.
- Observed locally: the stub imports a fixed "Classic Guacamole" draft. Duplicate detection and Accept all (2 PUTs in parallel) worked well. The stub proposes "avocado → NEW INGREDIENT" although avocado exists; the frontend create-or-find matched it correctly.

**20. Inline ingredient create is heavy — Polish (live). Effort M.**
- Where: `IngredientPicker.tsx:283-338`.
- Today: the name is shown twice (a disabled input plus a Name field). Category, Unit and Create sit below the fold. Select items are about 28px tall.
- Change:
  - Drop the Name field and edit the name in the picker input.
  - Replace the category select with a wrapped row of 10 chips.
  - Default the unit by category: spice → tsp, produce → whole, meat/seafood → lb, pantry → cup, dairy → cup, bakery → pieces.
  - Add `@media (pointer: coarse) { .rt-SelectItem { min-height: 44px } }`.
  - Use the same chips in NewIngredientSheet.

**21. Polish items. Effort S each.**
- Plan rows show "serves 2" under a 6-serving plan. Show "×3" in `PlanDetailPage.tsx:258`.
- The shopping list shows "½ whole bell pepper". Round countable units (whole, pieces, can, bunch) up for display, as "1 (½ needed)".
- The sign-in email field is not autofocused; `activeElement` was BODY.
- Duplicating a plan drops manual shopping items. Consider carrying them over unchecked.
- After a Select choice, focus stays on the select trigger, so Enter reopens the select instead of submitting.

## 4. Quick wins (under an hour each)

#2, #3, #8, #11, #14, #15, #17 and #18. Also the `replace: true` plus date-based name part of #10, the clear button and `select()` parts of #12, the `overflow: clip` plus guard part of #1, the quantity-focus part of #7, and the sticky controls and clear button parts of #16.

## 5. Already great — do not lose

- `LinesEditor` keyboard chaining: ingredient → Enter → quantity → Enter → a fresh, focused picker. Six lines with no pointer use. "1 1/2" is parsed as 1½, and the unit is filled from the ingredient's default.
- The shopping list:
  - Optimistic check-off with per-row coalescing.
  - Quick add that clears and refocuses instantly; three items typed at full speed all landed.
  - Rows that never reorder while you shop.
  - 10 mutations produced only 2 GETs.
- A plan picker that stays open, with one-tap optimistic adds.
- Remove with Undo instead of a confirm dialog.
- The Plans tab returning to the current plan, and the Shopping tab opening the last plan.
- One-tap Share from the header, with token reuse.
- Every sheet being a URL.
- New plan's date placeholder as the default name.
- Category and unit that stick between entries in rapid add, plus the "Already in the list" hint.
- The Accept all flow.

## 6. Bugs

1. Frame shift (#1).
   - Repro: /recipes/new → focus "Add ingredient" → add 3–6 lines.
   - `document.body.scrollTop` grows through 49, 100 and 217, and it persists after blur.
2. Import stuck after a 201 (#2).
   - Repro: /recipes/import → paste any URL → Import.
   - The sheet stays open with a "hang tight" toast, and the draft has been created.
3. Enter in the recipe Name field submits the form (#3).
4. A pending line is dropped on Save (#4).
5. Scroll carry-over and loss (#5).
6. Rapid ingredient add loses entries typed while the previous one is pending (#8).
7. `more_needed` rows render as never bought (#9).
8. Back after Duplicate reopens the original's edit sheet (`EditPlanSheet.tsx:88`).
9. Meal and Theme cannot be cleared (#17).
10. The URL-controlled search drops characters on fast input (#16).
11. The toast overlays BottomBar actions (#15).

No console errors were seen at any point.

Test data left in the database, all prefixed [R2]:
- 5 ingredients.
- Recipes: [R2] Chili, [R2] Chicken tacos, and [R2] Classic Guacamole, which is the imported draft renamed.
- Plans: [R2] Week 39 and [R2] Week 39 copy.
- 3 manual shopping items.

No repo files were touched.
