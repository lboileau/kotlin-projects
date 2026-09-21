# UX review 3 — mobile, in context (store / kitchen / couch)

Date: 2026-09-20 · Branch: meal-app-frontend (PR #315) · Record only, not an agreed backlog

Method: I drove the live app at http://r3.localhost:3000 as bob@example.com in a 500x755 CSS-px window, with a second session on r3b.localhost. Every measurement is from getBoundingClientRect, computed styles, or a canvas-resolved contrast calculation. iOS-specific items are marked INFERRED and need a device check. I did not touch the repo.

Two deviations from the brief:
- charlie@example.com is seeded with a NULL name, so the app showed "Create account". My rules forbid creating accounts, so I used alice@example.com as the second session. Alice was also being used by reviewer 2; I did not touch their data.
- Import-from-URL and draft review were not exercised live. I reviewed those from code only.

Data I left behind: ingredients "[R3] rice noodles" and "[R3] tamarind paste", recipe "[R3] Pad thai", plan "[R3] Week dinners" (share token created; Alice joined, was removed, rejoined, then left).

## 1. Verdict

The structure is good. Every sheet has a URL, Back works, deep links work, optimistic updates are instant, and two-session sync works. Recipe line entry (pick, quantity, Enter, next picker) is fast. What it lacks is feedback: the custom CSS has no pressed states and no micro-animation at all. The only `transition` outside Sheet.css is the tab pill, and tap highlight is turned off globally, so taps on rows, check-offs and toasts give no visual response.

The shopping list is the most-used screen and the weakest at arm's length. Quantities are 13px at 3.82:1 contrast. The checkbox is a 16px box with a 1.56:1 border. Quantities read "¼ whole onion" and "0.38 lb". A quick-added item lands off-screen with no feedback.

The single change that would raise the love factor most is making the shopping row tactile and legible: bigger round check, animated tick and strike, 15px quantity in gray-11, shop-friendly rounding, and a per-category "n left" count.

The biggest risk is the bottom sheets under the iOS keyboard (INFERRED).

## 2. Flow walkthrough

**Sign in.** The card is clean and the join context line is a nice touch.
- The email input is not autofocused.
- The input is 38px tall and the Continue button is 40px.
- On the join path, the lazy-chunk spinner swallowed my first keystrokes because nothing was focusable yet.

**Ingredients.** Rapid add works: Enter adds, the field clears and refocuses, category and unit stick, and the toast moves to the top while a sheet is open.
- Radix Select options are 32px tall, under the 44px target.
- "Done" duplicates the X.
- Rows are 44px, which is fine.

**New recipe.** Back-to-back line entry is excellent, and "1 1/2" parses correctly.
- Save is top-right only, which is hard to reach one-handed.
- The picker's inline results are cut off by the tab bar: the first result shows and the second is clipped. The picker's scrollIntoView cannot help because the page has no room left to scroll.
- The per-line quantity input is 70x30 and the unit select is 88x32. The trash button is 44px but sits directly next to them.

**Recipe detail (kitchen).**
- Ingredient lines are 14px with the quantity unemphasised ("200 pieces [R3] rice noodles"). Units are not pluralised ("3 clove garlic").
- For owners, every line is a link into the edit sheet, so messy-hand taps will open editors.
- "Source" is the only route to the method (there are no steps in the data model), and it is a 64x21px, 14px text link.
- There is no scaling to the plan's servings. The plan is set to 2, the chili serves 8, and the shopping list is scaled, but the recipe view is not.
- Delete (red) sits 8px from Edit in the header.

**Plans.**
- The New plan sheet autofocuses, and Enter creates and lands in the plan. Good.
- On an empty plan the primary docked CTA is "Shopping list", which is useless at that point. "Add recipes" is a 126x32 soft button at the top right.
- The Add recipes sheet is fast and "Added" badges appear instantly. Tapping an "Added" row does nothing (no toggle-off). Rows show no servings or tags. Autofocused search would pop the keyboard over a 7-item list.
- The remove-recipe button is 31x31 and sits flush against the 425px-wide row link.
- The undo toast lasts 4 seconds, its Undo button is 52x29 and its Dismiss is 23x23.
- The undo toast sits exactly over the docked "Shopping list" button. My slightly late Undo tap fell through and navigated me to Shopping, and the recipe stayed removed. A timely Undo works.

**Shopping (store).**
- Header: the plan-name switcher (44px tall), a 12px "2 of 17" count, and a thin bar.
- Rows: the label is a 408px-wide hit area, about 60px tall. That is good. Name is 16px/500. Quantity is 13px gray-10, 3.82:1, which fails AA. Checkbox is 16x16 with border contrast 1.56:1. Checked text is gray-9 with a strike, 3.30:1.
- There is no animation on check.
- Category headers are 14px medium in the same colour as items, not sticky, with no counts.
- The 60x60 info button opens 44px recipe pills, which is nice.
- Quick add works and refocuses. Both of my items went into "Misc" at the bottom of a 1646px list and only the counter changed (15 to 17). There was no toast, scroll or highlight, so I couldn't tell where they went.
- Quantities such as "¼ whole bell pepper", "¼ whole onion" and "0.38 lb ground beef" are not things you can buy.

**Share and join (couch).**
- My first tap on Share after a fresh load did nothing visible. The second tap showed "Link copied". See Bugs.
- Signed out, a join link goes to sign-in with "Sign in to open the plan that was shared with you", then auto-joins, lands on the plan, and shows a "You joined ..." toast. Smooth.
- Opening the link again shows "You already have this plan".
- Alice's check-off appeared in Bob's open list within about 2 seconds. Nothing marks who did it and nothing flashes.
- When the owner removed Alice, her open shopping list flipped live to "You don't have access to this plan". That is correct but abrupt.
- A removed member can rejoin with the same link, because the link never rotates.
- The member's options sheet is titled "Edit plan" with a pencil icon, although a member can only Duplicate, Copy, Share or Leave.
- After leaving, the toast and redirect to /plans work. Browser Back then lands on the no-access page.

**Deep links and refresh.**
- /plans/new, /plans/:id/edit and /recipes/:id/lines/new all restore the page with the sheet over it.
- Closing from a history index of 0 replaces to the parent correctly.
- A cold load of /recipes/new showed a blank white screen with only the tab bar for about 1 second.

## 3. Suggestions, ranked by impact

**1. Make the shopping row legible and tactile.** High. Effort M.
- Where: `pages/shopping/ShoppingRowItem.tsx` and its CSS.
- Today: 16px checkbox with a 1.56:1 border, 13px gray-10 quantity at 3.82:1, no motion.
- Change:
  - Pass `size="3"` to the Checkbox and override it to 24px with `border-radius: 999px` and a border of `var(--gray-8)`.
  - Set `.shopping-row__quantity` to `font-size: 0.9375rem; color: var(--gray-11)` (5.9:1).
  - Set `.shopping-row__name` to `font-size: 1.0625rem`.
  - On check, animate a 120ms tick scale from 0.6 to 1 with `cubic-bezier(.2,.8,.2,1)`.
  - Draw the strike-through with a `background-size` transition from 0 to 100% over 180ms, and transition the colour over 180ms.
  - Add `.shopping-row__hit-area:active { background: var(--accent-3); }` with `transition: background 80ms` and `border-radius: var(--radius-3)`.
  - Disable all of this under `prefers-reduced-motion`.
- Why: quantity is the information you need at arm's length, and each tap needs a visible response.

**2. Make bottom sheets keyboard-aware (INFERRED, verify on device).** High; potentially a blocker on iPhone. Effort M.
- Where: `components/Sheet.css`, `components/Sheet.tsx`, `lib/viewportGuard.ts`.
- Today: the sheet is `position: fixed; bottom: 0; max-height: 90dvh`, and `dvh` ignores the keyboard.
  - Add-ingredient sheet: 280px tall with an autofocused picker. The results list measured at y=341 to 601 of 755, which a roughly 336px keyboard would cover.
  - New plan sheet: the "Create plan" button sits under the keyboard.
  - Add recipes sheet: autofocus hides half the list and the Done button.
- Change:
  - In `viewportGuard` (or a new `useKeyboardInset`), on `visualViewport` `resize`/`scroll` set `--kb: max(0px, innerHeight - vv.height - vv.offsetTop)` on `documentElement`.
  - On `.sheet-content`, set `bottom: var(--kb, 0px); max-height: calc(90dvh - var(--kb, 0px)); transition: bottom 200ms`.
  - Add `interactive-widget=resizes-content` to the viewport meta in `index.html` for Android Chrome.
- Why: every create flow on iPhone starts with a keyboard covering the sheet it opened.

**3. Stop the toast covering the docked CTA, and make Undo a real target.** High. Effort S.
- Where: `components/Toast.css`, `lib/toastStore.ts`, `components/BottomBar.tsx`.
- Today: the toast sits at `bottom: 76px`, on top of the BottomBar button. It lasts 4 seconds. Undo is 52x29. A late tap falls through to "Shopping list".
- Change:
  - BottomBar sets `--bottom-bar-h` on the body while mounted (a ResizeObserver works; the bar is about 65px).
  - Toast uses `bottom: calc(76px + var(--bottom-bar-h, 0px) + 8px + env(safe-area-inset-bottom))`.
  - Toasts that carry an action last 7000ms.
  - `.toast-region__action` gets `min-height: 44px; min-width: 64px; padding: 0 12px`. The dismiss button becomes 44x44.
  - Entrance animation: 160ms, `translateY(8px)` plus opacity.
- Why: I hit this in the first ten minutes. It undid nothing and navigated me away.

**4. Show quantities you can actually buy.** High. Effort S.
- Where: `lib/shoppingRows.ts` `formatQuantityText`. Leave `formatQuantity` alone for recipes.
- Today: "¼ whole onion", "0.38 lb".
- Change, for shopping only:
  - Count units (whole, pieces, clove, bunch, can) round up with `Math.ceil`. If the value changed, show the exact figure muted: "1 (need ¼)".
  - Weights and volumes round to sensible steps: lb and oz to 0.25 (so "½ lb"), g and ml to the nearest 5.
- Why: nobody buys a quarter of a pepper.

**5. Add pressed states everywhere.** High. Effort S.
- Where: `PlansPage.css` `.plans-page__row`, `RecipesPage.css` `.recipes-page__row-open`, `PlanDetailPage.css` `.plan-detail-page__recipe-link`, `IngredientsPage.css` row, the `AddRecipeToPlanSheet`, `SwitchPlanSheet` and `AddToPlanSheet` rows, `TabBar.css`, and `.header-icon-button`.
- Today: `-webkit-tap-highlight-color: transparent` is set and there are zero `:active` rules.
- Change:
  - Rows: `:active { background: var(--accent-4); transform: scale(0.985); }` with `transition: transform 80ms, background 80ms`.
  - Tab bar: `.tab-bar__item:active .tab-bar__pill { background: var(--accent-4); transform: scale(0.92); }`.
- Why: instant touch response is what makes the app feel native.

**6. Give quick add visible feedback.** High. Effort S.
- Where: `ShoppingPage.tsx` `handleQuickAdd`.
- Today: the item lands in "Misc" at the bottom with no visible change.
- Change:
  - After the mutate call, scroll the new row into view with `scrollIntoView({ block: 'center', behavior: 'smooth' })`, keyed by a data attribute on the temp id.
  - Flash the row's background from `var(--accent-4)` to transparent over 900ms.
  - Keep focus in the input.
  - Under reduced motion, show `toast.info('Added to Misc')` instead.
- Why: without feedback people add the same item twice.

**7. Add per-category progress and a "Hide checked" toggle.** Medium. Effort M.
- Where: `ShoppingPage.tsx` and its CSS.
- Today: headers are 14px and non-sticky, and finding the last three unchecked items means rescanning the whole list.
- Change:
  - `.shopping-page__category-header` becomes `position: sticky; top: <header height>; background: var(--color-background); z-index: 1`. The title becomes 12px uppercase with `letter-spacing: .04em` in gray-11 (the Ingredients page already does this). Add a right-aligned "2 left", or a green check when the category is done.
  - Add a "Hide checked" item to the overflow menu, stored per plan in localStorage. It applies only when toggled, so items checked while it is on stay visible and dimmed until it is toggled again. That keeps the deliberate "rows never move when tapped" rule.
  - Raise "x of y" from 12px to 14px.
- Why: the end of a shop is where the current list costs the most time.

**8. Make the recipe view usable while cooking.** Medium. Effort M.
- Where: `RecipeDetailPage.tsx` and its CSS.
- Today: lines are 14px, owner taps open editors, Source is a 64x21 link, and nothing scales.
- Change:
  - Line text goes to `size="3"` (16px) with the quantity and unit in a `font-weight: 600` span, `line-height: 1.5`.
  - Ingredient lines become non-links on the detail page. Editing already lives on `/edit`, and the "Add ingredient" link stays.
  - Tapping a line toggles a local "used" state (strike, gray-9) so the cook keeps their place.
  - Source becomes a full-width `variant="soft" size="3"` button reading "Open method · {hostname}".
  - When the selected plan contains the recipe, add a segmented control "Recipe (8) | Plan (2)" that multiplies quantities by planServings / baseServings.
  - Optional: call `navigator.wakeLock.request('screen')` while the page is mounted, inside try/catch.
- Why: this removes accidental edits and makes quantities readable at counter distance.

**9. Either implement swipe-to-dismiss or remove the handle.** Medium. Effort M to implement, S to remove.
- Where: `Sheet.tsx` and `Sheet.css`.
- Today: there is a drag handle but no drag behaviour. Also, `--black-a8` (0.6) is a heavy overlay for a pastel app.
- Change:
  - Add pointer handlers on the handle and header that follow `translateY`.
  - On release past 96px or faster than 0.5px/ms, call `onClose`. Otherwise spring back over 200ms.
  - Set the overlay to `var(--black-a6)`.
  - If the drag is not built, delete the handle.
- Why: the handle promises a gesture that doesn't exist.

**10. Bring undersized targets up to 44px.** Medium. Effort S.
- Remove-recipe (31x31) in `PlanDetailPage`: use `size="4"` or min 44x44, and add a 12px gap from the row link.
- Remove-member (31x31) and the confirm buttons (32px) in `EditPlanSheet`: use size 3.
- "Add recipes" (126x32): use size 3.
- `LinesEditor` quantity (70x30) and unit (88x32): give them `min-height: 44px`.
- The "Mine" switch (35x20) in `RecipesPage`: wrap the label and switch in a 44px-tall label element.
- Radix Select items (32px): add the global rule `.rt-SelectItem { min-height: 44px; }`.
- Sign-in input (38px) and buttons (40px): `min-height: 44px`.
- Recipe Source link (64x21).

**11. Fix the empty-plan CTA.** Medium. Effort S.
- Where: `PlanDetailPage.tsx`.
- Today: an empty plan's primary docked CTA is "Shopping list".
- Change:
  - When there are 0 recipes, the docked primary button becomes "+ Add recipes".
  - The empty copy becomes "No recipes yet. Add a few and the shopping list builds itself."
  - When the plan has recipes, the bar shows both: a soft "Add recipes" and a solid "Shopping list".

**12. Give the picker room on full pages.** Medium. Effort S.
- Where: `LinesEditor.css` or `IngredientPicker.tsx`.
- Today: the results list is clipped by the tab bar because the page can't scroll further.
- Change:
  - Add `padding-bottom: 320px` to the editor while the picker is open.
  - Make the list's `max-height: min(260px, 40dvh)`, and end it on a whole row so the "Create" row isn't cut in half. It is clipped today at 260px.

**13. Fix sharing copy and affordances.** Medium. Effort S.
- Where: `EditPlanSheet.tsx`, `PlanDetailPage.tsx`.
- Change:
  - Title the sheet "Plan options" for members.
  - Give "Copy summary" a clipboard icon. It currently uses Share2Icon, which collides with the header Share button.
  - Remove-member confirm copy: "Remove Alice? They can rejoin with the link."
  - Leave confirm copy: "Leave "{name}"? You'll lose access until someone shares the link again."
  - Show member initials or "Shared with Alice" under the plan title on the detail page.
  - Offer "Open shopping list" as a toast action after joining.

**14. Show presence on synced changes.** Medium. Effort S to M.
- Where: `sync/useMealPlanSync.ts` plus the row CSS.
- Change:
  - When a refetch triggered by sync changes a row's status, flash that row's background from `var(--accent-3)` over 1.2 seconds.
  - The API has no purchasedBy field, so attribution needs a small backend addition. It is optional.

**15. Add thumb-reach Save and Add buttons.** Medium. Effort S.
- Where: `NewRecipePage` and `EditRecipePage`; the Recipes list New and Import buttons.
- Today: these actions exist only in the top-right of the header.
- Change: add a BottomBar "Save recipe" (the header button can stay). On the Recipes list, add a BottomBar with "New recipe" and an Import icon, which also matches the Plans page.

**16. Move Delete out of the recipe header.** Medium. Effort S.
- Where: `RecipeDetailPage`.
- Today: Delete sits 8px from Edit.
- Change: move Delete to the bottom of the edit page or into an overflow menu. The confirm dialog already exists.

**17. Smaller polish items.** Effort S each.
- Autofocus the sign-in email field.
- The route fallback shows a blank screen for about 1 second on a cold deep link. Show a header skeleton instead.
- In Add recipes, let a tap on an "Added" row remove the recipe, or show the row as disabled.
- The Stepper is round on Plans and square on the recipe form. Use one style.
- Pluralise units ("3 cloves").
- The "serves 8" badge inside a plan set to 2 servings is confusing. Show "scaled from 8" or drop it.
- The Add-to-plan sheet has no "New plan" row.
- INFERRED: BottomBar and TabBar both add `env(safe-area-inset-bottom)`. That would leave a double gap when the inset is non-zero, so remove it from `.bottom-bar`.

## 4. Quick wins (under an hour each)

Suggestions 3, 5, 6, 10, 11, 12, 13 and 16. From 1, the CSS-only part (quantity size and colour, checkbox size and border). The rounding in 4. From 9, the overlay change to `--black-a6`. From 17: sign-in autofocus, the double safe-area, and unit plurals.

## 5. What's already great (don't lose it)

- URL-addressable sheets, Back closing sheets, and deep links restoring the page with the sheet over it.
- The line-entry focus choreography when writing a recipe.
- Rapid-add ingredients with sticky category and unit.
- Instant optimistic add and remove with Undo.
- Rows never reordering on check, and the whole row label acting as the toggle.
- The info-button recipe pills at 44px.
- Inline confirms for destructive actions, instead of stacked dialogs.
- The join flow surviving sign-in and auto-accepting, and "You already have this plan".
- Live 403 handling when a member is removed.
- The toast moving to the top while a sheet is open.
- Accent contrast is fine: accent-11 on white is 6.17:1, white on accent-9 is 5.39:1, and gray-11 is 5.9:1.
- The console stayed clean throughout.

## 6. Bugs

1. **Undo toast expiry lets the tap fall through to the CTA.** Open Plan detail, remove a recipe, wait about 4 seconds, then tap where Undo was. The tap lands on "Shopping list" and navigates there, and the recipe stays removed. Suggestion 3 fixes this.
2. **First Share tap gave no feedback (seen once, not reproduced).** Do a full page load of `/plans/:id` and tap the header Share button within about 1 second. There was no toast, no clipboard write, and nothing in the console. The second tap showed "Link copied". My guess is that `share()` in `useSharePlan.ts` returns silently on `!planName` while the plan is still loading. Disable the button until the plan has loaded, or show a spinner and queue the share.
3. **The "Create 'x'" picker row is clipped at the list's 260px max-height** when there are five or more matches. Seen in the Add ingredient sheet after typing "on".
4. **Keystrokes typed during the lazy-chunk spinner on /sign-in are lost**, because no input exists yet and nothing is autofocused afterwards. Autofocus fixes it.
5. **After leaving a plan, browser Back lands on "You don't have access"** for the plan just left. `close({ to: '/plans', replace: true })` only replaces the `/edit` history entry. Minor.
6. **Copy mismatch on the join sign-in path for a nameless existing user.** The screen stacks "Tell us your name to finish signing up." over "Sign in to open the plan..." and the button reads "Create account", for an account that already exists. "Finish signing up" fits better.
