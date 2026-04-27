# Clean Up UI — Frontend-Only Improvements

A focused pass on awkward UI flows across the camper webapp. **Scope: frontend only.** No API or DB changes.

## Goals

- Fix routing so browser back/forward and shareable URLs work.
- Stop forms from closing on every submit when the obvious intent is to add multiple items.
- Surface "what's this for?" context that's already in the API payload but currently hidden.
- Standardize confirms, keyboard shortcuts, and small ergonomic gaps.

## Out of scope

- Any backend, DB, or API contract change.
- Visual redesign of the parchment / campsite aesthetic.
- New features beyond what is listed here.

---

## Frontend-only verification

Every workstream below has been vetted against the existing API surface (`webapp/src/api/client.ts`) and the backend implementation (`services/camper-service/src/main/`). The table summarizes feasibility and any caveats.

| ID | Title | FE-only? | Notes |
|----|-------|----------|-------|
| W1 | Recipes routing | ✅ | Pure react-router refactor. |
| W2 | Shopping list keep-open | ✅ | Local form state only. |
| W3 | Other forms keep-open | ✅ | Same pattern, three call sites. |
| W4 | Recipe link in meal plan | ✅ | `recipeWebLink` already on `MealPlanRecipeDetailResponse`; navigates to the route added in W1. |
| W5 | Shopping list info button | ✅ ⚠️ | `usedInRecipes` is **names only**, not IDs. Popover shows non-clickable text. Linking to recipe detail would need BE change — out of scope. |
| W6 | Meal plan tab hash | ✅ | URL hash, no API touch. |
| W7 | Confirm modal | ✅ | New component, replaces `window.confirm`. |
| W8 | Keyboard support | ✅ | DOM event handlers only. |
| W9 | Blur-save feedback | ✅ | Local UI state, depends on W11. |
| W10 | Empty states | ✅ | Pure UI. |
| W11 | Toast system | ✅ | New context provider. |
| W12 | Multi-day recipe add | ✅ | Loops existing `addRecipeToMeal` calls — no batch endpoint needed. |
| W13 | Quick-create recipe in modal | ✅ | Reuses `createRecipe` + `getIngredients` (already loaded by MealPlanModal). |
| W14 | Persist recipe search | ✅ | URL query param, depends on W1. |
| W15 | Gear pack apply summary + Undo | ✅ | `ApplyGearPackResponse.items` includes IDs; Undo loops `deleteItem` calls. |
| W16 | Assignment progress badges | ✅ | Compute client-side from `getAssignments` + `getPlanMembers` (both already fetched). One extra fetch on PlanPage load. |
| W17 | Member→servings reconcile | ✅ | Compares `mealPlan.servings` (already fetched) to `members.length` (already fetched); calls existing `updateMealPlan`. |
| W18 | Servings context display | ✅ | Data already on payload. |
| W19 | Pending member visuals | ✅ | `PlanMember.invitationStatus` already on payload. |
| W20 | Pending invitations rollup | ✅ ⚠️ | Resend works for `pending`/`failed`/`bounced` statuses (BE auto-resends those). Already-delivered emails **cannot** be force-resent without BE change — workstream scopes around this limit explicitly. |
| W21 | Promote gear packs + import | ✅ | Pure UI. |
| W22 | Ladder rules tooltip | ✅ | Pure UI + localStorage. |
| W23 | Delete + invite consistency | ✅ | Pure UI; depends on W7. |
| W24 | Tabs primitive | ✅ | New shared component. |
| W25 | Modal sizing | ✅ | Pure CSS. |
| W26 | Mobile baseline | ✅ | CSS + minor JS for touch detection. |
| W27 | First-run onboarding | ✅ | localStorage + UI. |
| W28 | Profile setup context | ✅ | Copy change. |
| W29 | Restart ladder copy | ✅ | Copy change; depends on W7. |

**Legend.** ✅ = entirely doable on the frontend. ⚠️ = doable on the frontend, but with a noted limitation that would require a future BE change to fully resolve.

**Two limitations called out above (W5, W20)** are scoped intentionally so that nothing in this plan requires touching `services/`, `clients/`, `databases/`, or `libs/`.

---

## Workstreams

Each workstream is independently shippable. Order is rough priority.

### W1 — Recipes page becomes proper routes

**Problem.** `RecipesPage.tsx` keeps `view` (`list` | `detail` | `create` | `edit` | `import`) in `useState`. Browser back/forward doesn't work. Recipe URLs aren't shareable.

**Change.**
- Replace single-state view with `react-router-dom` routes:
  - `/recipes` — list
  - `/recipes/new` — create form
  - `/recipes/import` — import form
  - `/recipes/:recipeId` — detail (incl. ingredient resolve / publish for drafts)
  - `/recipes/:recipeId/edit` — edit form
- Wire routes in `App.tsx` and split `RecipesPage` into per-view subcomponents (or keep one file with `<Routes>` if simpler).
- Use `useNavigate()` for transitions; preserve `?from=mealplan` style query params if needed for back-button copy.
- Drop the local `view` state entirely.

**Files.** `src/App.tsx`, `src/pages/RecipesPage.tsx` (split), `src/pages/RecipesPage.css`.

**Acceptance.**
- Hitting browser back from `/recipes/abc` returns to `/recipes` list with prior search/filter intact.
- Direct-loading `/recipes/abc/edit` lands on the edit form (or 404s gracefully if recipe missing).
- Sharing a recipe URL with a teammate opens the same view.

---

### W2 — Shopping list "Add Item" stays open

**Problem.** `MealPlanModal.tsx:1306-1382`. After adding a free-form item the form closes — adding 5 items means 5 round trips through "+ Add Item".

**Change.**
- After successful add, **do not** call `setShowAddForm(false)`. Instead:
  - Clear the inputs.
  - Refocus the first field (name).
  - Keep `quantity` / `unit` selectors at last-used value (not reset to 1) so repeating the same kind is fast.
- Close only on explicit "Done" / Cancel button or Escape key.
- Show a small inline "Added <name>" toast/flash for ~1.5s so the user gets feedback that the previous add succeeded.

**Files.** `src/components/MealPlanModal.tsx`, `src/components/MealPlanModal.css`.

**Acceptance.** Open Add Item, add 3 items in a row without ever clicking "+ Add Item" again. Cancel button or Escape closes the form.

---

### W3 — Same keep-open treatment for other repetitive forms

**Problem.** Same friction in three more places.

**Change.**
- `GearModal.tsx:173-179` — gear add form stays open, refocuses name, preserves quantity at last value.
- `MealPlanModal.tsx:829-832` — recipe-add inline search stays open after picking a recipe; only close on explicit Cancel.
- `AddMemberModal` — keep open after add, clear email field, refocus.

Apply the same UX pattern as W2 so behavior is consistent.

**Files.** `src/components/GearModal.tsx`, `src/components/MealPlanModal.tsx`, `src/components/AddMemberModal.tsx`.

**Acceptance.** Adding 5 gear items / 3 recipes to a meal / 4 members in a row never requires re-opening the form.

---

### W4 — Show recipe link in the meal plan

**Problem.** From the meal plan Overview tab, there's no way to view the source recipe or its web URL even though `MealPlanRecipeDetailResponse.recipeWebLink` is on the payload (`api/client.ts:274`).

**Change.**
- For each recipe row in the Overview meal sections, add:
  - A compact "View recipe" link/button that navigates to `/recipes/:recipeId` (opens the recipe detail page from W1; uses `useNavigate` and closes the modal, or opens in a new tab — pick one and apply consistently — recommend new tab so user keeps meal plan context).
  - If `recipeWebLink` is non-null, a small external-link icon next to the row that opens the source URL in a new tab (`target="_blank" rel="noopener noreferrer"`).
- Make sure both controls are reachable by keyboard and have `aria-label` (e.g., "View recipe details", "Open original recipe at example.com").

**Files.** `src/components/MealPlanModal.tsx` (Overview view), `src/components/MealPlanModal.css`.

**Acceptance.** Each recipe under Breakfast/Lunch/Dinner/Snacks shows a link to the recipe detail page and (when present) an external link to the source URL.

---

### W5 — Show which recipes a shopping list item is for

**Problem.** Shopping list rows don't tell you which recipe(s) they belong to. The data is already on the payload: `ShoppingListItemResponse.usedInRecipes: string[]` (`api/client.ts:313`).

**Change.**
- In each shopping list row (`MealPlanModal.tsx` shopping view, ~lines 1410-1458):
  - Add a small inline info button (ⓘ icon) next to the ingredient name — only shown when `usedInRecipes` is non-empty.
  - On hover, show a tooltip listing the recipe names (e.g., "Pasta Bolognese, Caesar Salad"). On click/focus (mobile + keyboard), show a small popover with the same list.
  - Manual / free-form items (empty `usedInRecipes`) don't render the button — keeps the row clean.
  - Use existing icon button styling from `components/ui/Button.tsx` (variant: `icon`, size: `sm`).
- Add `aria-label="Recipes using this item"` for screen readers.

**FE-only note.** `usedInRecipes` is `List<String>` of recipe **names only**, not IDs (verified in `ShoppingListCalculator.kt:106`). The popover shows names as plain text — not clickable links to the recipe detail page. If clickable links are wanted later, that requires a BE change to expose recipe IDs on the shopping list payload. Out of scope for this pass.

**Files.** `src/components/MealPlanModal.tsx`, `src/components/MealPlanModal.css`.

**Acceptance.** Every recipe-derived shopping list row shows an info button; hover/click reveals the recipe names. Free-form items show no button. Row height does not change.

---

### W6 — Meal plan modal tabs in URL hash

**Problem.** `MealPlanModal.tsx:38` — Overview/Recipes/Shopping/Templates tabs in `useState`. Closing and reopening loses tab. Can't deep-link to "show me the shopping list."

**Change.**
- Sync `activeView` with the URL hash: `#overview`, `#recipes`, `#shopping`, `#templates` (modal stays a modal — no full-page route).
- On modal open, read the hash; on tab change, update the hash via `history.replaceState` (no scroll).
- On modal close, clear the hash.

**Files.** `src/components/MealPlanModal.tsx`.

**Acceptance.** Sharing `/plans/:id#shopping` opens the modal on the shopping list tab. Browser back from `#shopping` returns to `#overview`.

---

### W7 — Replace `window.confirm()` with the parchment modal

**Problem.** Several destructive flows use the native `confirm()` dialog, which clashes with the parchment aesthetic and feels broken on mobile.

**Change.**
- Build a small reusable `<ConfirmModal>` (or extend `Modal` with a `confirm` variant) — title, body, danger button label, cancel button.
- Replace usages:
  - `pages/ActivityLadderPage.tsx` — restart ladder
  - `pages/HomePage.tsx:18` — delete trip / leave trip
  - `pages/RecipesPage.tsx:95` — delete recipe
  - `components/GearPacksPanel.tsx:93` — delete gear pack
  - Any other `window.confirm` / `confirm(` in `src/`.

**Files.** `src/components/ui/ConfirmModal.tsx` (new), call sites above.

**Acceptance.** Searching the codebase for `window.confirm` and bare `confirm(` returns zero results in `src/`. All destructive actions use the styled modal.

---

### W8 — Keyboard support on forms

**Problem.** Most inline forms don't bind Enter (submit) or Escape (cancel/close).

**Change.**
- Audit all forms in `src/components/` and `src/pages/`.
- Add `onKeyDown` handlers: Enter submits, Escape closes/cancels (where it makes sense — not in textarea fields).
- Specifically: `AddMemberModal`, gear add form, shopping list add item, recipe-add inline search, ladder activity add, assignment create.

**Files.** Many — list during implementation.

**Acceptance.** Tab through the app: every form submits with Enter and dismisses with Escape.

---

### W9 — Blur-to-save gets explicit feedback

**Problem.** `MealPlanModal.tsx:640-645` — meal plan name commits silently on blur. No undo or feedback.

**Change.**
- Keep the blur-to-save behavior (it's nice for non-critical fields), but show a brief inline "Saved" flash next to the field for ~1.5s after success.
- If the request fails, revert to the previous value and show an inline error.

**Files.** `src/components/MealPlanModal.tsx`.

**Acceptance.** Editing the meal plan name shows a "Saved" indicator. Network failure reverts the edit and shows an error.

---

### W10 — Empty states with CTAs

**Problem.** No-data states don't guide users.

**Change.**
- `RecipesPage` list view: when zero recipes, show parchment-styled card with "Create your first recipe" + "Import from URL" buttons.
- `MealPlanModal` Overview when meal plan is empty but plan exists: short tagline explaining what templates are.
- `LadderListPage` already has a CTA — verify it's prominent.

**Files.** `src/pages/RecipesPage.tsx`, `src/components/MealPlanModal.tsx`.

**Acceptance.** A new user with no data sees clear next steps on every list page.

---

### W11 — Global toast / snackbar system

**Problem.** Most successful actions are silent: invites sent, recipes published, templates loaded, gear packs applied — no confirmation. Errors live next to inputs sometimes; sometimes nowhere. This is also a prerequisite for W14, W15, W17, W18, W21, W23.

**Change.**
- Add a `<ToastProvider>` to `App.tsx` exposing `useToast()` with `success(msg, opts?)`, `error(msg, opts?)`, `info(msg, opts?)`. Variants support an optional `action` (label + handler) for "Undo".
- Render toasts in a fixed corner stack (top-right desktop, bottom-center mobile). Auto-dismiss after 4s; pause on hover; up to 4 visible at once.
- Style with parchment aesthetic (existing tokens — no new variables).
- Migrate the obvious places in this same workstream: invite sent, recipe published, template loaded, profile saved.

**Files.** `src/components/ui/Toast.tsx` + `Toast.css` (new), `src/context/ToastContext.tsx` (new), `src/App.tsx`, plus call-site edits.

**Acceptance.** Successful actions across the app produce a toast. Toasts dismiss correctly, can be stacked, work on mobile.

---

### W12 — Add a recipe to multiple days/meals at once

**Problem.** Recipe Book → Add to Meal lets you pick **one** day + meal type. Adding the same recipe to days 2/3/5 means three full round-trips through the recipe book.

**Change.**
- Replace the day + meal-type single-select popover with a 2D grid: rows = days, columns = breakfast/lunch/dinner/snack. Checkboxes toggle membership.
- "Add to Meal Plan" button submits all selected cells in one batch (loop calls to existing `addRecipeToMeal` endpoint — no API change).
- Pre-check the currently active day so the "single add" path stays one click.

**Files.** `src/components/MealPlanModal.tsx`, `src/components/MealPlanModal.css`.

**Acceptance.** A user can add Pasta Bolognese to Day 2 dinner, Day 3 dinner, and Day 5 lunch in one popover interaction. Removing the recipe still works per-cell from the Overview view.

---

### W13 — Quick-create recipe inside MealPlanModal

**Problem.** From Recipe Book tab, there's no way to create a new recipe without closing the modal and navigating to `/recipes/new`. Users lose meal plan context.

**Change.**
- Add a "+ New Recipe" button at the top of the Recipe Book left page.
- Opens a nested `Modal` (size `md`) with the same form `RecipesPage` uses for create. On success, the new recipe appears selected in the Recipe Book detail view, ready to "Add to Meal Plan."
- Optionally a "More options" link that closes the meal plan modal and navigates to `/recipes/new` for users who want the full page.

**Files.** `src/components/MealPlanModal.tsx`, possibly extract the create form from `RecipesPage` into `src/components/RecipeForm.tsx` for reuse.

**Acceptance.** From Recipe Book tab, click "+ New Recipe" → fill name/servings/ingredients → save → recipe is selected and ready to add. Meal plan modal context is preserved throughout.

---

### W14 — Persist recipe list search/filter on detail-back

**Problem.** In `RecipesPage`, searching for "beef", clicking a result, then returning to the list resets the search.

**Change.** Move `searchQuery` (and any active filters) to URL query params (`?q=beef`). With W1 in place, the list URL already exists, so back navigation restores the filter naturally.

**Files.** `src/pages/RecipesPage.tsx` (the list-view subcomponent after W1's split).

**Acceptance.** Search → click recipe → browser back → search term and scroll position preserved.

---

### W15 — Gear pack apply summary + Undo

**Problem.** Applying a gear pack adds 20 items silently. No diff, no undo path short of manual delete.

**Change.**
- After successful apply, show a toast (W11): "Added 20 items from Cooking Pack" with an "Undo" action.
- Undo deletes the items just added (track returned IDs from the apply response and call `DELETE /api/items/:id` for each).
- If user dismisses the toast or 4s pass, undo expires.

**Files.** `src/components/GearPacksPanel.tsx`.

**Acceptance.** Apply pack → toast appears with item count → clicking Undo removes those items. No undo support after toast dismiss.

---

### W16 — Assignment progress badges on PlanPage icons

**Problem.** Tent/canoe icons on the campsite scene give no signal whether assignments exist or are complete.

**Change.**
- Compute on-load: total members vs. members in at least one tent / canoe assignment.
- Render a small parchment badge on each icon: e.g., "3/5" or a checkmark if complete. On hover, show a tooltip "3 of 5 adventurers assigned to a tent."
- Same for the kitchen icon (e.g., "Meal plan: 5 days" or "No meal plan yet").

**Files.** `src/components/InteractableItem.tsx`, `src/pages/PlanPage.tsx`.

**Acceptance.** A glance at PlanPage tells the user what's planned vs. unplanned.

---

### W17 — Member count → servings reconciliation

**Problem.** Member changes don't propagate to the meal plan. A 9-person trip can have a 4-serving meal plan and nobody notices.

**Change.**
- On PlanPage, after member add/remove, if a meal plan exists and `mealPlan.servings !== memberCount`, show a non-blocking inline banner (or toast via W11) with one-click "Update meal plan to N servings."
- Dismissable; remembers dismissal for the session (sessionStorage).

**Files.** `src/pages/PlanPage.tsx`, possibly a small `<MealPlanReconcileBanner>` component.

**Acceptance.** Adding a member when the meal plan is out of sync surfaces the offer. Clicking it updates servings (via existing `PUT /api/meal-plans/:id`). Dismiss hides it.

---

### W18 — Surface servings/scaling context in shopping list & recipe book

**Problem.** Shopping list shows quantities with no context: "for how many people?" Recipe Book detail shows base + scaled but isn't visually clear.

**Change.**
- Shopping list header: "Shopping list for 4 servings · X of Y purchased" instead of just the progress bar.
- Recipe Book detail: a clear "Base: 2 servings · Scaled to 4" line above the ingredient list.

**Files.** `src/components/MealPlanModal.tsx`, `src/components/MealPlanModal.css`.

**Acceptance.** Both views show servings context unambiguously.

---

### W19 — Distinguish pending vs. registered members

**Problem.** Pending invitees in the campfire circle look like real members.

**Change.**
- Render pending avatars with a dashed outline and ~70% opacity.
- Add a small "pending" badge or envelope icon overlay.
- In tooltips/labels, prefix with "Invited:" before the email.

**Files.** `src/components/CamperAvatar.tsx`, `src/pages/PlanPage.tsx`, possibly `AvatarHead`.

**Acceptance.** Pending vs. active members are visually distinct in the circle and in the AssignmentsModal member lists.

---

### W20 — Pending invitations rollup in Manage Plan

**Problem.** No central view of pending invites. No way to retrigger a failed/bounced invite without removing the member.

**Change.**
- In Manage Plan modal, add a "Pending invitations (N)" section listing each pending member with email, invite date (`createdAt` on `PlanMember`), and the current `invitationStatus` (e.g., "Pending", "Sent", "Bounced", "Failed").
- For invitations whose status is `pending`, `failed`, or `bounced`: show a "Resend invite" button that calls `POST /api/plans/:id/members` with the same email. The existing dedup logic in `AddPlanMemberAction` will send a fresh email for these statuses.
- For invitations whose status is `sent`/`delivered`/`delayed`/`complained`: do **not** show a resend button (backend skips email send for those). Show a passive label: "Already delivered — ask them to check spam." This avoids a misleading button that does nothing.
- Provide a "Remove" button on every pending row so the owner can cancel an outstanding invite.

**Files.** Manage Plan modal (embedded in `PlanPage.tsx` or its own component).

**Acceptance.** Owner sees every pending invite with status, can resend the ones BE supports resending, and can remove any pending invite. No misleading affordances for already-delivered invites.

**FE-only note.** The full "force resend already-delivered email" feature is **not possible without a BE change** (`AddPlanMemberAction:32` skips re-send for sent/delivered/delayed/complained statuses). This workstream consciously scopes around that limit — no BE change needed.

---

### W21 — Promote Gear Packs and Recipe Import

**Problem.** Both features are buried — Gear Packs in a collapsible inside Equipment; Import Recipe in a same-weight button next to "New Recipe."

**Change.**
- **Gear Packs:** Add a header CTA inside the Shared Camp Gear section: "✨ Start with a template". On empty state (no shared items yet), show a card-style suggestion with 1-2 popular packs and "Browse all".
- **Recipe Import:** On `/recipes` empty state (post-W1), make Import the visually primary action ("Try importing from a URL") with a screenshot/illustration. In the populated state, keep both buttons but give Import an icon to differentiate.

**Files.** `src/components/GearModal.tsx`, `src/components/GearPacksPanel.tsx`, `src/pages/RecipesPage.tsx`.

**Acceptance.** A new user can find both features without help. Empty states promote them.

---

### W22 — Activity ladder rules tooltip / hint

**Problem.** Voters land on an active ladder with no idea how rounds work or what state means.

**Change.**
- Add a "How voting works" link in `LadderPage` ACTIVE state header, opening a small popover: "Each round, vote for one of two activities. The round ends when everyone votes. Lowest scoring activity is eliminated."
- One-time hint banner above the matchup on a user's first visit (track in localStorage, e.g., `seenLadderRules`).

**Files.** `src/pages/LadderPage.tsx`, possibly a small `<HintBanner>` component.

**Acceptance.** First-time voters see an explanation. Returning users don't get nagged.

---

### W23 — Standardize delete pattern + invite verbiage

**Problem.** Three delete UX patterns (hover icon, inline icon, edit-panel button). Three invite verbs ("Join", "Join Camp", "Add Member").

**Change.**
- **Delete:** Pick hover-revealed icon button → confirm via `<ConfirmModal>` (W7). Apply uniformly: HomePage trip cards, GearModal items, AssignmentsModal cards, RecipesPage cards, gear pack items.
- **Invite:** Standardize on "Invite" verb everywhere except where the public-plan join action genuinely is "join" (non-member of public plan). Reuse `AddMemberModal` for all owner-driven adds. Update labels accordingly.

**Files.** `src/pages/HomePage.tsx`, `src/components/GearModal.tsx`, `src/components/AssignmentsModal.tsx`, `src/pages/RecipesPage.tsx`, `src/components/GearPacksPanel.tsx`, `src/components/AddMemberModal.tsx`, `src/pages/PlanPage.tsx`.

**Acceptance.** A user encountering a "delete" or "invite" action anywhere in the app sees the same UI pattern.

---

### W24 — Reusable `<Tabs>` primitive

**Problem.** LoginPage, MealPlanModal, AssignmentsModal each implement tabs differently.

**Change.**
- Add `src/components/ui/Tabs.tsx` exposing `<Tabs value onChange><Tab value label /></Tabs>` with consistent styling and keyboard support (arrow keys to switch).
- Migrate LoginPage, MealPlanModal, AssignmentsModal.

**Files.** `src/components/ui/Tabs.tsx` + `Tabs.css` (new), three migration sites.

**Acceptance.** All tab UIs look and feel identical. Arrow-key navigation works.

---

### W25 — MealPlanModal sizing & GearModal layout polish

**Problem.** MealPlanModal is fixed 88vh — cramped on small laptops. GearModal stretches vertically and wastes horizontal space on wide screens.

**Change.**
- MealPlanModal: change height to `min(88vh, 900px)` and scroll internal content. On screens <900px tall, use 95vh.
- GearModal: on screens >1200px wide, use a 2-column layout (Shared Camp Gear | Personal Packs); collapse to single-column below.

**Files.** `src/components/MealPlanModal.css`, `src/components/GearModal.css`.

**Acceptance.** Both modals feel comfortable on 13" laptops and don't waste space on wide displays.

---

### W26 — Mobile responsiveness baseline

**Problem.** App is desktop-only. Campsite scene overlaps on mobile, modals are fixed widths, AppHeader compresses unreadably.

**Change.** Time-boxed pass — not a full mobile redesign:
- **Modals:** All sizes use `min(<size>, 95vw)` and let internal content scroll. `Modal.css`.
- **AppHeader:** Below 640px, hide logo, truncate page title with ellipsis, move Account/Logout into a hamburger menu.
- **Campsite scene (PlanPage):** Below 768px, render a vertical action list under the campfire scene with the four interactable items as buttons (Tent, Equipment, Kitchen, Map Table). Keep the campfire + avatars as decorative scene above. Hide parallax mouse-tracking on touch devices (no mousemove anyway, but skip the JS listener).
- **MealPlanModal day tabs:** Already scroll horizontally; ensure swipe works.

**Files.** `src/components/ui/Modal.css`, `src/components/AppHeader.tsx/css`, `src/pages/PlanPage.tsx/css`.

**Acceptance.** Every primary flow works on a 375×812 viewport (iPhone SE). Not promising pixel-perfect — promising "usable."

---

### W27 — First-run onboarding guide

**Problem.** New users land on HomePage with no roadmap.

**Change.**
- Detect first-run via localStorage flag (`onboarded:v1`).
- On HomePage with zero trips, show a 3-step illustrated card: (1) Create your trip, (2) Invite your group, (3) Plan meals & gear together.
- After the first trip is created, show a similar inline tip on PlanPage on first visit: "Click the tent to assign sleeping arrangements, the kitchen to plan meals, the equipment pile to track gear."
- Dismiss button on each. Set localStorage flag on dismiss or after the user completes the implied action.

**Files.** `src/pages/HomePage.tsx`, `src/pages/PlanPage.tsx`, possibly `src/components/OnboardingTip.tsx`.

**Acceptance.** A logged-out → register → first-load journey shows guidance. Returning users see nothing extra.

---

### W28 — ProfileSetupModal context line

**Problem.** Modal pops up uninvited; users don't know why.

**Change.** Add a one-line subhead under the title: "Pick an avatar and trail name so others recognize you around the campfire."

**Files.** Wherever `ProfileSetupModal` is defined.

**Acceptance.** Modal explains itself.

---

### W29 — Restart Ladder confirm copy

**Problem.** "Restart Ladder" button gives no hint about what restart means.

**Change.** With W7's ConfirmModal in place, use it here with copy: "Restart this ladder? All votes will be reset and a new tournament will begin with the current activities. This cannot be undone."

**Files.** `src/pages/LadderPage.tsx`.

**Acceptance.** Restart action shows a clear, themed confirm with descriptive copy.

---

## Suggested PR stack

Each workstream is its own PR. Recommended ship order:

**Phase 1 — quick wins & user-flagged issues:**
1. W2 + W3 (keep-open forms).
2. W5 (recipe info button on shopping list).
3. W4 (recipe link in meal plan).
4. W11 (toast system) — unblocks many later workstreams.

**Phase 2 — routing & structure:**
5. W1 (recipes routing) — unblocks W4 link target, W14.
6. W14 (persist search) — trivial after W1.
7. W6 (meal plan tab hash).

**Phase 3 — high-value workflow improvements:**
8. W12 (multi-day recipe add).
9. W13 (quick-create recipe in modal).
10. W17 (member→servings reconcile).
11. W16 (assignment progress badges).

**Phase 4 — polish & consistency:**
12. W7 (confirm modal) + W29 (restart copy).
13. W23 (delete + invite consistency) — depends on W7.
14. W24 (tabs primitive).
15. W18 (servings context).
16. W19 (pending member visuals) + W20 (invitations rollup).
17. W15 (gear pack apply summary) — depends on W11.
18. W9 (blur-save feedback) — depends on W11.

**Phase 5 — discoverability & guidance:**
19. W21 (promote gear packs + import).
20. W22 (ladder rules hint).
21. W27 (first-run onboarding) + W28 (profile context).
22. W10 (empty states).

**Phase 6 — sizing & mobile:**
23. W25 (modal sizing polish).
24. W26 (mobile baseline).
25. W8 (keyboard) — can ship anywhere; placed late since mechanical.

## Non-goals / explicit "not now"

- **Avatar batch endpoint** for `LadderPeoplePanel` — requires BE work, deferred.
- **Drag-drop reordering** of meal plan days, ladder activities, assignments — needs API support for sort order, deferred.
- **Bulk select / mark-all on shopping list** — pure FE, queue for follow-up.
- **MealPlanModal full-page mode** — keep as modal; W6 hash routing closes deep-link gap.
- **Recipe form unsaved-changes guard** — nice to have, deferred.
- **Activity-to-meal-plan linkage** (e.g., "kayak day → calorie-dense meals") — too speculative without designer input.
- **Gear-pack → assignment scoping** (apply pack to a specific tent group) — useful but needs data-model thought; deferred.
- **Cost rollup / budget tracking** — could be FE-computed from itinerary + activity ladder, but scope creep for this pass.
- **Activity ladder thumbnail carousel on list page** — visual upgrade only, deferred.
- **LoginPage / parallax mobile redesign** — W26 covers baseline only.
- **Full icon library standardization** — touch as we encounter; not a dedicated workstream.

## Acceptance for the whole feature

- `npm run build` passes (canonical type-check gate).
- `npm run test` passes.
- Manual smoke: every workstream's acceptance bullet checked by hand in the browser.
- No backend file (`services/`, `clients/`, `databases/`, `libs/`) is modified by this work.
