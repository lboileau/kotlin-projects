# UX review plan — 2026-09-20

**Status: record only.** This is the combined output of a user-testing round on the `meal-app-frontend` branch (PR #315). It is **not an agreed backlog**: the owner strongly disagrees with some of it, and nothing here should be picked up without checking first. The one part that was agreed and taken forward is [Part 3 — navigation and visual distinction](#part-3--navigation-and-visual-distinction-agreed-taken-forward).

## What was done afterwards (2026-09-20)

After reading the reviews the owner chose what to act on. Done: Part 3 (navigation and screen identity, as revised with the owner), all six core-path bugs, the add-to-plan dead end and one-tap add, the "serves N" badge removed and the control relabelled "Servings per recipe", half-ticked "need more" shopping rows, toasts clear of the docked button with a 44px / 7s Undo, the new-recipe draft auto-save, category chips with a required category, the empty plan's primary "Add recipes", and quick-add scroll-and-flash. **Declined: rounding shopping quantities to buyable amounts — the exact computed quantities are intended behaviour.** Not done (not yet discussed): shopping row legibility and pressed states, sheets under the iOS keyboard, "start from last week", adding several lines to an existing recipe in one sheet, sticky category headers, the cooking view, the refetch gate, non-blocking import, the sheet handle, and the smaller bugs list.

## How the round was run

Three independent reviewer agents each walked the whole flow (sign in → ingredients → recipes → plans → sharing → shopping list → account) against the local app, each with a different lens. Their full, unabridged reports are in [`ux-review/`](ux-review/):

| # | Lens | Session | Full report |
|---|---|---|---|
| 1 | First-time user: empty states, copy, discoverability, time to first shopping list | new user `ux-r1@example.com` | [reviewer-1-first-time-user.md](ux-review/reviewer-1-first-time-user.md) |
| 2 | Weekly power user: tap counts, data-entry speed, reuse, undo, lost state, network | `alice@example.com` | [reviewer-2-power-user.md](ux-review/reviewer-2-power-user.md) |
| 3 | Phone in hand (store / kitchen / couch): tap targets, sheets, sync, contrast, feedback | `bob@example.com` + a second session | [reviewer-3-mobile-in-context.md](ux-review/reviewer-3-mobile-in-context.md) |

Caveats that apply to everything below:

- All three drove **desktop Chrome**, not a phone. Anything iOS-specific is inferred from CSS and needs a device check.
- URL import and draft review were run live by reviewer 2 only. The Share button was tapped by reviewer 3 only. Reviewer 1 read both from code.
- Reviewer 1 saw import fail locally ("Couldn't reach that page"); reviewer 2 got the stub's fixed guacamole draft. Not reconciled.
- Hard reloads took 2–4 s and dropped typed input. Probably the Vite dev server under three reviewers; worth checking on a production build.
- The owner's own feedback (Part 2) was added after the reviewers reported. None of the reviewers raised it.

## Part 1 — combined reviewer findings

### Bugs found on core paths (reviewer 2, all live)

1. **Line entry scrolls the app off screen.** Each ingredient-picker focus scrolls `document.body`. After six lines on New recipe the header and Save were off screen and could not be scrolled back. Suggested fix: replace `scrollIntoView` at `IngredientPicker.tsx:217` with a scroll of the real scroller, `html, body { overflow: clip }`, and zero `body.scrollTop` in `viewportGuard`.
2. **A successful import leaves you stuck on the sheet.** POST returns 201, but the sheet's `canClose` guard blocks the close because `isPending` has not re-rendered. Tapping Import again gives a duplicate conflict. Suggested fix: a `force` option on `sheet.close` (`ImportRecipeSheet.tsx:35-39`, `useSheet.ts:79-84`).
3. **Enter in the recipe Name field saves an empty recipe.** Suggested fix: Enter moves to the ingredient picker; Save is the only submit (`NewRecipePage.tsx:79-89`, same in `EditRecipePage`).
4. **A typed but un-added ingredient line is silently dropped on Save.** Suggested fix: auto-append a valid pending line; block Save with a message if it is incomplete (`LinesEditor.tsx:35-40`).
5. **Rapid ingredient add loses entries.** Three names typed back to back created two. Suggested fix: clear and refocus immediately, do not await the mutation (as `ShoppingPage.handleQuickAdd` does).
6. **Search fields drop characters** on Recipes and Ingredients (reviewers 1 and 2). The input is controlled directly by `?q=`. Suggested fix: local state, debounced sync to the URL.

### Where reviewers agree

1. **Shopping quantities nobody can buy** (all three): "0.17 whole lettuce", "¼ whole onion", "0.38 lb", "1 pieces". Suggested fix, in `formatQuantityText` (`lib/shoppingRows.ts`) only: round countable units up, step weights to quarters, pluralise units, no quantity on manual items; optionally "1 (need ¼)".
2. **Bought items silently revert when the plan changes** (1, 2). The server already sends `more_needed`; render an indeterminate checkbox and "Bought 2 · need 1 more" in `ShoppingRowItem.tsx`.
3. **Undo toast sits over the docked button** (2, 3). A late Undo tap fell through to "Shopping list". Raise the toast above `BottomBar`, 7 s for action toasts, 44px Undo and Dismiss.
4. **Add-to-plan dead end, and one-tap add** (all three). With no plans the sheet offers no button; add "Create a plan and add this recipe". With one plan (or a selected plan) the ＋ should add directly with an Undo toast and become a ✓.
5. **Unsaved recipe lost on Back, a tab tap or refresh** (1, 2). Reviewer 1: discard dialog. Reviewer 2: `sessionStorage` draft with "Draft restored · Discard".
6. **Accidental "Other / pieces" ingredients** (1, 2). Enter submits with the defaults. Category chips, required category, unit default from category; same in the inline create panel.
7. **Empty plan's main button is "Shopping list"** (1, 3); should be "+ Add recipes". Reviewer 2: a new plan should open straight into the recipe picker.
8. **Servings are confusing** (all three). "serves 6" badge inside a 2-serving plan. Label "Cooking for 2 people", caption "Every recipe is scaled to this", "scaled from 6" on rows.
9. **Quick-add items land off screen in "Misc"** (1, 3). Scroll to the new row and flash it. There is both an "Other" and a "Misc" group.
10. Smaller, raised by at least two: second tap on an "Added" row should remove it; move recipe Delete away from Edit; Meal/Theme need a "None" option; Select items 44px tall; autofocus the sign-in email.

### Biggest single-reviewer ideas

- **Shopping row legibility and feel (3).** Quantity 13px at 3.82:1 (fails AA); checkbox 16px with a 1.56:1 border; no `:active` states anywhere and tap highlight disabled globally. Exact CSS is in the full report.
- **Sheets under the iOS keyboard (3, inferred).** Sheets are capped at `90dvh`, which ignores the keyboard. `visualViewport` inset on `.sheet-content`. Needs a real phone.
- **Start from last week (2).** "Copy of <last plan>" chip in New plan, "Week of …" default name, remembered servings. About 9 taps and a rename down to 2.
- **Adding lines to an existing recipe (2).** 4 taps and 2 sheet animations per line; keep the sheet open and move focus to quantity.
- **Scroll position (2).** One shared scroller: a recipe opened from a scrolled list renders part-way down, and Back returns to the top of the list.
- **Shopping at the end of a trip (3).** Sticky category headers with "n left", opt-in "Hide checked".
- **Cooking view (3).** 16px lines with bold quantities, lines that are not edit links, tap to mark used, full-width "Open method", scale to plan servings.
- **Refetch storm (2).** Five recipe adds triggered six GETs of the slow plan-detail endpoint; reuse the `invalidateIfLast` gate.
- **Non-blocking import (1, 2).** Close at once with an "Importing…" toast; accept scheme-less URLs; Paste button; "Add it by hand instead" on failure.
- **Sheet handle (3).** Promises a swipe that does not exist. Build drag-to-dismiss or remove it; lighten the overlay to `--black-a6`.

### Smaller bugs

- Visiting a plan URL that does not exist makes the Shopping tab forget the selected plan (`PlanDetailPage.tsx:47-55`).
- A bad join link shows a raw server error toast on top of the proper error page (`useAcceptInvite` lacks `suppressErrorToast`).
- First Share tap right after a page load did nothing (seen once, reviewer 3).
- After duplicating or leaving a plan, browser Back lands on the old edit sheet / the no-access page.
- Validation errors stay on screen until the next submit.
- Saving on Edit ingredient leaves its sheet open; saving on Edit recipe stays on the edit page.
- The "Create 'x'" picker row is clipped at the list's 260px max height.
- For an existing user with no name, the join sign-in screen says "Create account".
- A removed member can rejoin with the same link (the link never rotates).

### What all three said to keep

Keyboard-chained recipe line entry with fraction parsing; every sheet being a URL that survives reload and Back; optimistic check-off and quick add; shopping rows that never reorder; one-tap adds in the plan picker; Undo instead of confirm dialogs; the default plan name; sticky category/unit between ingredient adds; the join flow carrying on through sign-in; accent contrast (6.17:1).

## Part 2 — the owner's feedback

Added by the owner after reading the reviews, and ranked by them above everything in Part 1:

1. **Screens lack distinct looks**, so it is hard to tell at a glance where you are in the app. Each tab has several states (Plans has a list and a detail view, for example), which makes it harder still to keep track of where you are in the navigation stack.
2. **Navigation is not consistent.** Different actions and states take you to different pages depending on where you came from.
3. Follow-up: consider **colour coding and smart icon placement** so each screen has a distinctive feel suited to the job it is doing.

## Part 3 — navigation and visual distinction (agreed, taken forward)

Traced through the routing code, not the running app.

### Causes

1. **Two back mechanisms that disagree.** The header back arrow pushes a fixed destination (`navigate(backTo)`, `PageHeader.tsx:29`); sheets close by going back in history (`useSheet.ts:96`). Open a recipe from a plan, tap the arrow, and you land on the Recipes list in a different tab, while browser Back or a swipe returns to the plan. Because the arrow pushes, a swipe back afterwards returns to the recipe just left. The Account arrow always goes to `/plans`.
2. **Links that change your tab without you tapping one.** The highlighted tab is derived from the URL prefix. A recipe opened from a plan (`PlanDetailPage.tsx:256`) or a shopping row (`ShoppingRowItem.tsx:14`) lights up Recipes. "Add recipes" on an empty shopping list jumps to Plans with a sheet open (`ShoppingPage.tsx:246`).
3. **Each tab behaves differently when tapped.** Plans goes to the last plan, or the list if already on it (`TabBar.tsx:40-46`); Recipes always resets to its list; Shopping opens the last plan. No tab remembers where you were in it.
4. **Ingredients is both a sibling and a child of Recipes**: it has the Recipes/Ingredients toggle and a back arrow to `/recipes`, and the toggle pushes history, so Back ping-pongs.
5. **Save lands somewhere different each time.** New recipe / new plan → the new item; Edit recipe stays on the edit page; Edit ingredient leaves its sheet open; Duplicate plan keeps the old sheet in history.
6. **Every screen uses the same header**: 18px title, white bar, grey hairline, lilac 44px icon buttons, over the same style of row list. A list and its detail differ only by a small arrow and the title text.

### Rules

Navigation:

- **One meaning for Back.** The header arrow goes back in history when there is in-app history, otherwise replaces to the parent. The arrow, a swipe and browser Back always agree.
- **The back button says where it goes** ("‹ Plans", "‹ Week 39").
- **Sheets never change your tab.** "Add recipes" from the shopping list closes back to the shopping list (`/plans/:planId/shopping/add`). A recipe tapped in a plan or on the shopping list opens the real recipe page in the Recipes tab, whose back button returns to where it was opened from. (First built as a read-only copy of the recipe page inside the plan's tab; the owner preferred the one real page.)
- **Tabs behave the same way.** A tab always opens its base screen: the recipe list, the selected plan, its shopping list. (First built to return to where you last were in the tab; the owner preferred the base screen every time.)
- **Ingredients is a sibling only.** No back arrow; the toggle replaces history.
- **One rule after a save.** Create → the new item (replace). Edit → back to where you came from, with a toast. Delete → the parent list.

Visual (as built, after several rounds with the owner):

- **One colour.** Violet throughout. The static header is a violet tint between the list rows and the solid buttons. Three colour-coding attempts were rejected by the owner: tab colours on identity parts throughout the page, the whole accent following the tab (orange then amber buttons looked wrong), and per-tab header colours only (orange / blue / teal clashed with the purple). Which tab you are in is said by the header's icon and title and the active tab.
- **Every screen has the same static header**: the tab's icon, the tab's title, the account button, and no actions. Back/close, Save, Edit and the like sit in a plain row under it; list actions (Recipes' Import and New, Ingredients' Add) sit on the list.
- **Plans and Shopping are each one view of the selected plan** with the same top: the plan's name as a dropdown to switch plan or create one. The plans list is no longer a screen you navigate through, which removed a level from the Plans tab.
- **Detail screens keep a hero** (small "RECIPE" label, full wrapping name, facts), shown as a placeholder while loading so nothing jumps.
- **Directional transitions.** Deeper slides in from the right, back reverses, tabs cross-fade, sheets rise. Off under reduced motion.
