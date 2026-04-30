# Phase 4 Implementation Plan — clean-up-ui

**Phase 4 is the largest phase: 9 workstreams.** Polish & consistency. All FE-only. No backend, no DB, no API contract changes.

The shape: we introduce three reusable primitives (`ConfirmModal`, `Tabs`, plus a small banner/list pattern) and consume them across the app. The phase forms two logical sub-stacks that ship sequentially:

- **Sub-stack A — primitives + their consumers:** W7 → W29 → W23 → W24
- **Sub-stack B — surfacing/data UX:** W18 → W19 → W20 → W15 → W9

We ship one serial PR stack (no branch parallelism) — it matches the orchestrator's review queue and gives reviewers a predictable diff order.

---

## Plan-gate decisions (settled, 2026-04-29)

The 14 design questions from the handoff, resolved.

### 1. W7 ConfirmModal API — declarative, async-aware

**Shape:**
```tsx
export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string | ReactNode;          // string for the common case, ReactNode when copy needs <strong>/<em>/<code>
  confirmLabel: string;
  cancelLabel?: string;                 // default "Cancel"
  tone?: 'danger' | 'default';          // default 'danger' (it's a confirm modal — destructive is the common case)
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
  /** Optional flavor copy under the title; matches Modal's existing `flavor` prop. */
  flavor?: string;
  /** Optional icon SVG (ReactNode) rendered above the title. Defaults to a generic warning glyph. */
  icon?: ReactNode;
}
```

**Behavior:**
- **Async-aware.** If `onConfirm` returns a Promise, the confirm button shows a loading state and stays disabled until the promise settles. `onCancel` button is also disabled during the await (otherwise the consumer has to handle a cancelled-mid-request race). Modal Escape and overlay-click are also blocked while pending.
- **Declarative, not imperative.** No `confirm()` Promise-returning helper. Matches the existing `<Modal>` pattern in `components/ui/Modal.tsx` (which is the consumer-of-record for parchment dialogs). Imperative `confirm()` adds a portal/registry layer we don't need for ~6 call sites.
- **Built on top of `<Modal>`.** ConfirmModal renders a `<Modal size="sm" closable={!pending}>` with a fixed action bar (`Cancel`, `Confirm`). We don't re-implement overlay/Escape/portal — we delegate.
- **Tone styling.** `tone='danger'` uses `Button variant='danger'` for the confirm button (existing token). `tone='default'` uses `Button variant='primary'`. No new color tokens.
- **Auto-focus.** On open, focus the cancel button (safer default for destructive actions per Material/HIG conventions).

**Tests this contract.** ConfirmModal renders, calls `onConfirm`/`onCancel`, awaits Promise resolution, disables buttons while pending, returns to ready state on rejection.

### 2. W7 enumeration of `confirm(` call sites

A `grep -rn "confirm(" camper/webapp/src/ --include="*.tsx" --include="*.ts" | grep -v "test\.\|onConfirm\|confirmLabel\|setConfirm\|isConfirm\|confirmation\|//.*confirm\|showConfirm\|confirmModal"` finds **one** literal `window.confirm` survivor:

| # | File | Line | Action | Replacement |
|---|------|------|--------|-------------|
| 1 | `pages/ActivityLadderPage.tsx` | 240 | Restart Ladder | W7 wires it through `<ConfirmModal>` with placeholder copy; W29 finalizes the copy |

Per `plan.md` W7, the original list also called out `HomePage.tsx:18` (delete trip / leave trip), `RecipesPage.tsx:95` (delete recipe), and `GearPacksPanel.tsx:93` (delete pack). Those were **already migrated** to ad-hoc parchment modals during earlier phases (Phase 1 W2/W3 era and the initial pre-`clean-up-ui` UI work). They use `modal-overlay` + `modal-content` divs inline, **not** `<Modal>` and not `<ConfirmModal>`. **W7 migrates these three to use the new `<ConfirmModal>` primitive in addition to the actual `confirm()` migration in ActivityLadderPage.** This is the canonicalisation pass — they're still the "delete" UX W23 standardizes on; they just use a real shared primitive now.

**W7 covered destinations (5 sites in 1 PR):**

| Site | File | Today | After W7 |
|------|------|-------|----------|
| 1. Restart Ladder | `pages/ActivityLadderPage.tsx:240` | `window.confirm()` | `<ConfirmModal>` (W29 copy) |
| 2. Delete Trip | `pages/HomePage.tsx:282-307` | inline parchment overlay | `<ConfirmModal tone='danger'>` |
| 3. Leave Trip | `pages/HomePage.tsx:309-332` | inline parchment overlay | `<ConfirmModal tone='danger'>` |
| 4. Delete Recipe | `pages/RecipesPage.tsx:1170-1192` | inline parchment overlay | `<ConfirmModal tone='danger'>` |
| 5. Delete Gear Pack | `components/GearPacksPanel.tsx:577-603` | inline `gear-pack-card--confirm` card | `<ConfirmModal tone='danger'>` |

**Nudge sites (NOT migrated to ConfirmModal — these are inline confirm-in-place patterns, not modals):**
- `MealPlanModal.tsx:1660-1672` "Reset all purchases" — inline confirm pill with two buttons (`mp-reset-confirm`). This is a **non-modal inline confirm**, kept in place because its UX (right next to the Reset button) is intentional. Document in the plan: not a regression; not in W7 scope.

### 3. W23 delete pattern decision

**Canonical pattern after W23:**
1. **Visual affordance:** hover-revealed icon button (small trash/X SVG) using `Button` shared primitive with `variant='icon'` and `size='sm'`. The button's parent row uses `:hover .delete-btn` to fade it in (existing `--hover-fade` style on `.trip-card__delete-zone`).
2. **Confirm step:** click opens `<ConfirmModal tone='danger' confirmLabel='Delete' />` (or workflow-specific verb like "Remove", "Break Camp", "Leave Camp" — preserve flavor copy from current modals).
3. **Action:** confirm button calls the API; on resolve, the parent state updates (existing handlers stay).

**Existing delete affordances enumerated:**

| Site | Current pattern | After W23 | Notes |
|------|-----------------|-----------|-------|
| HomePage trip cards | `.trip-card__delete-zone` + `.trip-card__delete` SVG button (hover-revealed) | KEEP. This is the canonical pattern. Repaint other sites in this style. | Fixed; W7 already standardizes the modal step. |
| HomePage leave trip | `.trip-card__leave-zone` + `.trip-card__leave` SVG button (hover-revealed) | KEEP. | Same story; verb stays "Leave Camp". |
| GearModal items | `.gear-item-delete` button (always visible, small × icon at row right) | Migrate to hover-reveal pattern. | Currently always-on, awkward at scale. After: hover to reveal trash icon → `<ConfirmModal>` ("Remove this item?"). |
| AssignmentsModal cards | `.assign-action-btn--danger` (always visible at card header, "Delete") | Migrate to hover-reveal pattern + `<ConfirmModal>` ("Break this camp?"). | The card already has hover affordances; consolidate the danger button to hover. |
| AssignmentsModal member kick | `assign-member-remove` (already × icon next to the name) | Wire through `<ConfirmModal>` for plan-owner-removing-someone-else. **Self-leave can keep the immediate action** (no confirm needed — leaving your own assignment is a one-click reversible action; you can re-join). | Distinguish actor-vs-target. |
| RecipesPage cards (list view) | No delete button in list view today. Delete is on the **detail view** ("Remove from Cookbook" button + already-migrated modal). | Add hover-reveal trash icon on list cards as well, going through `<ConfirmModal>`. | Optional polish — gates on whether it complicates the list card layout. **Recommend: detail-view only stays the canonical site; do NOT add to list cards in W23.** Skips a layout-density debate. |
| GearPacksPanel cards | Already-visible "Delete" text button → inline `gear-pack-card--confirm` overlay | Migrate to `<ConfirmModal>` (W7 already does this). W23 polishes the trigger to a small icon + text label, hover-revealed. | |
| Personal pack item delete (GearModal personal section) | Same `.gear-item-delete` X button (always visible). | Same hover-reveal migration as shared gear. | |

**Canonical CSS class:** Define a new shared class `.btn-icon-danger` in `components/ui/Button.css` for the small trash icon usage. Reuse the existing `Button variant='icon'` (already in the design system) and add a danger-color tint via prop (e.g., `<Button variant='icon' size='sm' className='btn-danger-tint'>`). Pattern: each site uses `<Button variant='icon' size='sm' aria-label='Delete trip' onClick={...}>`.

**Not in scope for W23:** Adding new delete affordances where none exist (e.g., recipe list cards). The work is *consistency* — make the affordances that exist look and behave the same. Net effect: zero new "places you can delete from."

### 4. W23 invite verb

**Current verbs enumerated:**

| Site | Current label | After W23 |
|------|---------------|-----------|
| `AddMemberModal` modal title | "Invite Adventurers" | KEEP. (Already "Invite".) |
| `AddMemberModal` submit button | "Send Invitation" / "Send Invitations" | KEEP. (Already "Invite"-flavored.) |
| `PlanPage` campfire ghost button (member-only) | title=`"Invite an adventurer"` | KEEP. |
| `PlanPage` non-member campfire ghost button | text=`"Join Camp"` | KEEP. **This is a genuine "Join"** — the actor is joining themselves, not inviting someone else. Plan.md is explicit about this. |
| `HomePage` trip card non-member action | text=`"Join"` (label `trip-card__join-text`) | KEEP. Same rationale — actor self-joins. |
| `LoginPage` register CTA | `"Join the Expedition"` | KEEP. Self-join in a different sense (account creation). Authentic. |
| `AssignmentsModal` Join button | `"Join"` | KEEP. Self-action — joining a tent/canoe group. |
| `AssignmentsModal` "Add Member" panel button | text=`"Add Member"` | **CHANGE to `"Invite to Group"`** (or `"Invite"`). The actor is the plan owner adding someone else. Per plan.md W23 — "Standardize on Invite verb everywhere except where it's genuinely Join". |
| `AssignmentsModal` cancel toggle | `"Cancel"` (when add panel is open) | KEEP. |
| `CamperAvatar` invite-ghost (around fire) | title=`"Invite an adventurer"`, label=`"Invite someone"` | KEEP. |

**Net change for W23 invite work:** **One label change** in `AssignmentsModal.tsx:316` from `"Add Member"` → `"Invite"`. Document the consistency rationale in commit body. This isn't a workstream — it's a one-line polish bundled with the delete consistency PR. We keep W23 as one PR.

### 5. W24 Tabs primitive API

**Shape:**
```tsx
export interface TabsProps {
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;          // Expected: <Tab value label icon? />[]
  ariaLabel?: string;           // for the tablist's aria-label
  className?: string;           // host can add layout classes (e.g., flexbox row)
}

export interface TabProps {
  value: string;
  label: string;
  icon?: ReactNode;             // optional leading SVG, matches existing assign-tab-icon usage
  disabled?: boolean;
}

export function Tabs({ value, onChange, children, ariaLabel, className }: TabsProps): JSX.Element;
export function Tab(props: TabProps): JSX.Element; // lightweight render-prop child; rendered by Tabs after introspection
```

**Behavior:**
- **Roving tabindex.** Active tab has `tabIndex={0}`, others `tabIndex={-1}` per WAI-ARIA Tabs pattern.
- **Keyboard:** Arrow Left/Right cycle tabs. Home/End jump to first/last. Enter/Space activate (also click).
- **Visual.** Reuses existing tab CSS — extract from `assign-tabs`/`mp-tabs`/`login-card-header` into `Tabs.css` with parametric class names (`ui-tabs`, `ui-tab`, `ui-tab--active`).
- **Hash sync (NOT in this primitive).** The `<Tabs>` primitive is purely value-controlled. `MealPlanModal`'s W6 hash-sync logic stays where it is — it's three `useEffect`s that bridge `window.location.hash` ↔ `activeView` state. The migration replaces ONLY the rendered `<button className="mp-tab">` JSX with `<Tab>` children; the underlying `setActiveView(tab.key)` call stays exactly the same. **This is the critical migration constraint** — read carefully.

**Three migration sites:**

| Site | What it currently renders | What changes |
|------|---------------------------|--------------|
| `LoginPage.tsx:73-86` | `.login-card-header` with two `<button className="login-tab login-tab--active">` for Sign In / Register | Becomes `<Tabs value={mode} onChange={setMode}><Tab value="login" label="Sign In" /><Tab value="register" label="Register" /></Tabs>`. The `setMode` calls additional side-effects (`setError('')`); migration must preserve them via a wrapping `onChange` callback. |
| `MealPlanModal.tsx:454-468` | `.mp-tabs` with three buttons mapped from a tab array | Becomes `<Tabs value={activeView} onChange={(v) => setActiveView(v as ViewTab)}>` with three `<Tab>` children. **W6 hash-sync stays exactly as-is** — Effects 1, 2, 3 remain unchanged. The `onChange` passed to Tabs IS still `setActiveView`. |
| `AssignmentsModal.tsx:548-564` | `.assign-tabs` with two buttons (Tents / Canoes), each with a leading `assign-tab-icon` SVG span | Becomes `<Tabs value={activeTab} onChange={(v) => setActiveTab(v as 'tent'\|'canoe')}><Tab value="tent" label="Tents" icon={<TriangleSVG />} /><Tab value="canoe" label="Canoes" icon={<HexSVG />} /></Tabs>`. |

**Compatibility CSS aliases.** Plan.md doesn't require deleting the old class names (`mp-tab`, `login-tab`, `assign-tab`). Recommend: delete old per-site classes after migration to a single shared `ui-tab` class. Each migration site's local CSS file loses ~15 lines. Net reduction.

### 6. W19 avatar pending state

**Already done in part.** `CamperAvatar.tsx` already handles `isPending` (no `name`) and `isFailed` (status `failed/bounced/complained`). Read the file:
- `camper-avatar--pending` modifier class exists.
- Pending avatars currently render a translucent ghost SVG with "Pending..." label.
- Failed renders a "Failed" label and `camper-avatar--failed` class.

**What's missing per plan.md W19:**
1. **Dashed outline** on the avatar circle when pending. (Today: ghost silhouette via low-opacity shapes; no dashed border.)
2. **70% opacity** override (today the ghost shapes are individually low-opacity but the avatar container is 100%).
3. **Envelope icon overlay** — a small ✉ in the top-right corner of the pending avatar.
4. **"Invited:" prefix** in tooltips when the avatar represents a pending invite.

**Where else avatars render:**
- `PlanPage.tsx` — campfire circle uses `<CamperAvatar>`. Already passes `invitationStatus`. ✓
- `AssignmentsModal.tsx` — uses **`AvatarHead`** (compact variant), not `CamperAvatar`. Plan-member lists in the assignment add-panel render `AvatarHead` for each candidate. We need to wire `invitationStatus` through to `AvatarHead` too — and apply equivalent dashed/opacity styling there.
- `manage-plan-members-list` (PlanPage:476-505) — only renders **registered members** (`members.filter(m => m.username)`). **Plan.md W20 expands this to a separate "Pending invitations" section.** W19 doesn't touch the manage-plan-modal directly. The pending visuals here are W20's concern.
- `LadderPeoplePanel` — uses `AvatarHead`. Plan participants are users; `invitationStatus` doesn't apply. No change.

**CSS class names to add (W19):**
- `.camper-avatar--pending` — already exists; ADD: `outline: 2px dashed var(--tan-deep); outline-offset: 4px; opacity: 0.7;`
- `.camper-avatar__envelope-overlay` — new, for the ✉ icon (small parchment chip, top-right corner of avatar figure)
- `.avatar-head--pending` — NEW class on `AvatarHead`, applied when `invitationStatus` indicates pending. Same dashed/opacity treatment, shrunk for the compact size.

**`AvatarHead` prop change (cascade):**

`AvatarHead` today doesn't accept `invitationStatus`. Adding it is a cascade — every call site passes one extra prop. Files that construct `<AvatarHead>`:
- `AssignmentsModal.tsx` — multiple call sites for member lists & add-member candidates.
- `LadderPeoplePanel.tsx` — irrelevant (pass `null`/omit).

The new prop is optional (`invitationStatus?: string | null`); existing call sites compile without modification. Only AssignmentsModal sites get an actual non-null `invitationStatus` from the `PlanMember` they're rendering.

### 7. W20 resend logic

**Already verified:** `services/.../AddPlanMemberAction.kt:32` defines `skipStatuses = setOf("sent", "delayed", "delivered", "complained")`. Statuses that DO trigger a resend email: `pending`, `failed`, `bounced`, plus any null/unknown/empty status.

**W20 client logic:**
```tsx
const RESEND_ENABLED_STATUSES = new Set(['pending', 'failed', 'bounced']);
function canResend(member: PlanMember): boolean {
  return member.invitationStatus != null
    && RESEND_ENABLED_STATUSES.has(member.invitationStatus);
}
```

For statuses NOT in the resendable set (`sent`/`delayed`/`delivered`/`complained`), render a passive label only:
- `delivered` / `sent` → `"Already delivered — ask them to check spam"`
- `delayed` → `"Delivery delayed by recipient mail server"`
- `complained` → `"Recipient flagged as spam — try a different email"`

Resend implementation: `await api.addMember(planId, member.email)`. The BE dedups and re-sends for resendable statuses; no payload change. Existing endpoint at `client.ts:641`.

### 8. W18 shopping list servings

**Already verified:** `ShoppingListResponse.servings: number` exists at `client.ts:293`. `MealPlanResponse.servings: number` (and `MealPlanDetailResponse.servings`) at lines 234 and 247. Both surfaces have access:

- **Shopping list header:** Use `shoppingList.servings` (already on `ShoppingListResponse`). Existing render is `mp-shopping-progress` showing `{purchased} / {total}`. Add a header line **above** the progress bar reading `Shopping list for {servings} servings · {purchased} of {total} purchased`. Copy the singular "1 serving" / "N servings" pluralization from `MealPlanReconcileBanner` (already established in Phase 3).

### 9. W18 recipe book scaling display

**Already verified:** `MealPlanRecipeDetailResponse` has `baseServings: number` and `scaleFactor: number` (`client.ts:275-276`). `mealPlan.servings` is the trip-wide servings; `baseServings` is the recipe's natural servings; `scaledQuantity = quantity * scaleFactor` already on `MealPlanIngredientResponse`. The display: `Base: {baseServings} servings · Scaled to {mealPlan.servings}` directly above the ingredient list in the Recipe Book detail (right page) of MealPlanModal. **No new fields, no new fetches.**

The line reads off the current selected meal plan recipe — the entry the user just clicked in the Recipe Book left page. RecipeBookView already has `selectedRecipe` (a `RecipeDetailResponse`, NOT a `MealPlanRecipeDetailResponse`). `RecipeDetailResponse` has `baseServings` (line 145 of client.ts) — visible. The line's right side (`Scaled to N`) reads `mealPlan.servings`. Both available without a fetch.

### 10. W15 toast undo wiring

**Already verified:** `ToastOptions.action: { label: string; onClick: () => void }` exists. The toast renderer already shows the action button and dismisses-on-click (`Toast.tsx:38-41`).

**Exact call site for W15** (inside `GearPacksPanel.handleApply`):
```tsx
const result = await api.applyGearPack(packId, { planId, groupSize });
toast.success(`Added ${result.appliedCount} items from ${pack.name}`, {
  action: {
    label: 'Undo',
    onClick: async () => {
      // Loop deletes — sequential to keep server load low
      for (const item of result.items) {
        try { await api.deleteItem(item.id); } catch {/* ignore */}
      }
      onItemsChanged();
      toast.info(`Undid ${result.items.length} items from ${pack.name}`);
    },
  },
});
```

**Undo lifecycle:** the toast's auto-dismiss timer (4s default) is the undo window. After the toast dismisses (timeout, manual close, or Undo click), undo is impossible. Per plan.md acceptance: "No undo support after toast dismiss."

**Inline `applySuccess` flash removal.** Today, `GearPacksPanel.tsx:156-160` shows an inline message `"Added N items to your gear list!"` for 3 seconds via `setApplySuccess`. **This becomes redundant** once the toast fires. **Remove the inline flash and its CSS in W15.** Document the simplification.

### 11. W15 returned item IDs

**Already verified:** `ApplyGearPackResponse` has `appliedCount: number` and `items: Item[]` (client.ts:403-406). `Item.id` is on every entry. The Undo handler maps over `result.items` and calls `api.deleteItem(item.id)` for each. Confirmed.

### 12. W9 blur-save feedback

**Today** (`MealPlanModal.tsx:213-221`):
```tsx
const handleUpdateName = async (name: string) => {
  if (!mealPlan) return;
  const trimmed = name.trim();
  if (!trimmed || trimmed === mealPlan.name) return;
  try {
    await api.updateMealPlan(mealPlan.id, { name: trimmed });
    await loadMealPlan();
  } catch { /* */ }
};
```

The catch silently swallows. Two changes for W9:

1. **Inline "Saved" flash** for ~1.5s next to the input on success. Use a local component-state flag (`saveStatus: 'idle' | 'saving' | 'saved' | 'error'`) tracked on the `OverviewView`. On success → flash "Saved" inline (small chip with ✓). Decay to idle after 1.5s. Use a `setTimeout` cleared on unmount.
2. **On failure:** revert the input value (`setEditName(mealPlan.name)`) AND show inline "Failed to save" error AND fire a `useToast().error('Failed to save meal plan name')` — defense in depth (inline + toast).

Plan says "inline flash specifically" for the success case (no toast). For errors, the inline + toast doubles up because the inline flash is brief and the toast has the actionable copy.

**Why inline-only for success?** Toast spam — the user is quickly editing names, and a toast for every blur-save is noisy. Inline is contextual and proportional. Plan.md is explicit: "show a brief inline 'Saved' flash next to the field for ~1.5s after success."

### 13. Order tweak — keep serial (recommended)

Sub-stack A (W7→W29→W23→W24) and sub-stack B (W18→W19→W20→W15→W9) are logically independent except W7 → W29/W23 (shared primitive consumption). Splitting into two parallel branches saves total elapsed time but doubles reviewer context-switching. **Recommend serial.** Total PRs: 9. Same review queue length as Phase 3 doubled — manageable. The orchestrator gets one ordered stack to drive.

**Locked ship order:**
```
1. W7    → ConfirmModal primitive + 5 migrations
2. W29   → Restart Ladder copy
3. W23   → Delete + invite consistency polish
4. W24   → Tabs primitive + 3 migrations
5. W18   → Servings/scaling context (shopping list + recipe book)
6. W19   → Pending member visuals (CamperAvatar + AvatarHead)
7. W20   → Pending invitations rollup (Manage Plan)
8. W15   → Gear pack apply summary + Undo (toast)
9. W9    → Blur-save feedback (inline "Saved" flash)
```

### 14. MealPlanModal.tsx growth

Currently ~1700 lines. Phase 4 adds:
- W18: ~30 lines (servings header on shopping list + base/scaled line on recipe detail)
- W24: ~5 lines net (replace mp-tabs JSX with `<Tabs>`)
- W9: ~15 lines (saveStatus state + flash + error revert wiring)

Net growth: ~50 lines, bringing the file to ~1750. **Don't split this phase.** The sub-component export pattern (`OverviewView`, `ShoppingListView`, `RecipeBookView`, `AddItemForm`) handles testability. A split is a Phase-6+ cleanup.

---

## Hard constraints (re-verified)

- **Backend untouched.** Verified against `webapp/src/api/client.ts`:
  - W7: no API calls (presentation-only).
  - W29: no API calls.
  - W23: same handlers, same endpoints.
  - W24: no API calls.
  - W18: reads existing `ShoppingListResponse.servings`, `MealPlanRecipeDetailResponse.baseServings`/`scaleFactor`. Already on payload.
  - W19: reads existing `PlanMember.invitationStatus`. Already on payload.
  - W20: reads existing `PlanMember` fields (`email`, `username`, `createdAt`, `invitationStatus`, `invitedBy`). Resend uses existing `addMember(planId, email)` endpoint at `client.ts:641`. No payload change. BE dedup logic unchanged.
  - W15: uses existing `applyGearPack` + `deleteItem` endpoints. Loops deletes in FE.
  - W9: uses existing `updateMealPlan` endpoint. No new fields.

- **No new theme tokens.** Reuse `--lavender`, `--sage`, `--rose-deep`, `--ember`, `--mint`, `--tan-deep`, `--charcoal`, etc.
- **STOMP `usePlanUpdates` untouched.** No new resource branches (W20's pending-list is read at modal open time and post-resend; live updates aren't required for the rollup).
- **Build gate.** `cd camper/webapp && npx tsc --noEmit && npm run build && npm run test` must pass on every PR.
- **Code-reviewer rejects** any PR that modifies files outside `camper/webapp/src/` (or `package.json`/`vitest.config.ts`/`src/test/setup.ts`).

---

## What we decided NOT to do (Phase 4)

- **Imperative `confirm()` Promise helper.** Nice ergonomically; not worth the portal/registry plumbing for ~6 sites. Declarative `<ConfirmModal>` matches `<Modal>` and is what reviewers expect.
- **Splitting MealPlanModal.tsx.** ~1750 lines is uncomfortable but not blocking. Phase-6+ cleanup.
- **Adding delete affordances where none exist** (e.g., RecipesPage list cards). W23 is consistency, not feature expansion.
- **Backend resend force flag for sent/delivered statuses.** Plan.md W20 explicitly accepts the FE-only limitation. Document the passive label.
- **STOMP topic for meal-plan changes.** No new topic. W9's blur-save flash is local-only.
- **W18 going further than the header line.** No bulk re-scaling UI in shopping list. The reconcile banner from W17 is the existing escape hatch.
- **Renaming `addMember` → `inviteMember` in `api/client.ts`.** Breaking name; not worth the cascade. The verb consistency in W23 is UI labels only.
- **Keyboard tab navigation in ConfirmModal beyond what `<Modal>` already does.** Modal handles Escape; ConfirmModal handles confirm-button focus and keep-focus-trapped. W8 (Phase 6) is the comprehensive keyboard pass.

---

## Phase 4 ship order (9 PRs)

| # | Workstream | Branch | Depends on |
|---|-----------|--------|------------|
| 1 | **W7** ConfirmModal primitive + 5 migrations | `clean-up-ui-w7-confirm-modal` | — (Phase 3 fully merged) |
| 2 | **W29** Restart Ladder confirm copy | `clean-up-ui-w29-restart-ladder-copy` | W7 |
| 3 | **W23** Delete + invite consistency | `clean-up-ui-w23-delete-invite-consistency` | W7 |
| 4 | **W24** Tabs primitive + 3 migrations | `clean-up-ui-w24-tabs-primitive` | — (independent of W7/W23 but ships sequentially to keep review queue serial) |
| 5 | **W18** Servings/scaling context | `clean-up-ui-w18-servings-context` | — |
| 6 | **W19** Pending member visuals | `clean-up-ui-w19-pending-member-visuals` | — |
| 7 | **W20** Pending invitations rollup | `clean-up-ui-w20-pending-invitations-rollup` | W19 (reuses pending styling on the rollup avatar list) |
| 8 | **W15** Gear pack apply summary + Undo | `clean-up-ui-w15-gear-pack-apply-undo` | — (uses Phase 1's `useToast`) |
| 9 | **W9** Blur-save feedback | `clean-up-ui-w9-blur-save-feedback` | — (uses Phase 1's `useToast`) |

---

# PR 1 — W7: ConfirmModal primitive + 5 migrations

**Branch.** `clean-up-ui-w7-confirm-modal`

**Commit title.** `feat(clean-up-ui): W7 — ConfirmModal primitive + migrate window.confirm + ad-hoc modals`

**Acceptance (from plan.md).** Searching the codebase for `window.confirm` and bare `confirm(` returns zero results in `src/`. All destructive actions use the styled modal.

**Dependencies.** Phase 3 fully merged.

## Files to create

### `camper/webapp/src/components/ui/ConfirmModal.tsx` (new)

Public API per plan-gate decision #1:
```tsx
import { useState, useEffect, type ReactNode } from 'react';
import { Modal } from './Modal';
import { Button } from './Button';

export interface ConfirmModalProps {
  isOpen: boolean;
  title: string;
  message: string | ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: 'danger' | 'default';
  flavor?: string;
  icon?: ReactNode;
  onConfirm: () => void | Promise<void>;
  onCancel: () => void;
}

export function ConfirmModal({
  isOpen, title, message, confirmLabel, cancelLabel = 'Cancel',
  tone = 'danger', flavor, icon,
  onConfirm, onCancel,
}: ConfirmModalProps) {
  const [pending, setPending] = useState(false);

  // Reset pending state if the parent closes the modal externally
  useEffect(() => { if (!isOpen) setPending(false); }, [isOpen]);

  const handleConfirm = async () => {
    setPending(true);
    try {
      await Promise.resolve(onConfirm());
    } finally {
      setPending(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={pending ? () => {/* blocked while pending */} : onCancel}
      size="sm"
      closable={!pending}
    >
      {icon && <div className="modal-icon-large">{icon}</div>}
      <h2 className="modal-title">{title}</h2>
      {flavor && <p className="modal-flavor">{flavor}</p>}
      <div className="confirm-modal-message">{message}</div>
      <div className="modal-actions">
        <Button variant="secondary" onClick={onCancel} disabled={pending}>
          {cancelLabel}
        </Button>
        <Button
          variant={tone === 'danger' ? 'danger' : 'primary'}
          onClick={handleConfirm}
          disabled={pending}
          loading={pending}
          autoFocus={false}  // intentionally NOT autofocused — destructive default
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
```

**Note:** `<Button>` already supports `loading` per `components/ui/Button.tsx` (per webapp/CLAUDE.md). Verify the prop exists; if not, the loading state can be inlined here as `{pending ? '...' : confirmLabel}`.

**Auto-focus the cancel button** (per plan-gate #1). Achieve via a `useRef` + `useEffect` on isOpen rising edge:
```tsx
const cancelRef = useRef<HTMLButtonElement>(null);
useEffect(() => {
  if (isOpen) cancelRef.current?.focus();
}, [isOpen]);
// Pass ref to Cancel button: <Button ref={cancelRef} ...>
```

If `<Button>` doesn't currently `forwardRef`, this is a one-line change in `Button.tsx` (`forwardRef<HTMLButtonElement, ButtonProps>`). Verify and patch as needed.

### `camper/webapp/src/components/ui/ConfirmModal.css` (new)

Just one rule for the message container; everything else inherits from `Modal.css`:
```css
.confirm-modal-message {
  font-size: 1rem;
  color: var(--charcoal);
  text-align: center;
  margin: var(--space-md) 0 var(--space-lg);
  line-height: 1.5;
}
```

(Reuse `--space-md`, `--space-lg`. No new tokens.)

## Files to modify

### `camper/webapp/src/pages/ActivityLadderPage.tsx`

**Lines 238-260 area.** Replace the `if (!confirm(...))` guard with a state-driven `<ConfirmModal>`:

```tsx
const [restartConfirmOpen, setRestartConfirmOpen] = useState(false);

const performRestart = async () => {
  if (!ladderId) return;
  setRestartLoading(true);
  setActionError('');
  try {
    await api.ladders.restart(ladderId);
    await loadLadder();
    // ...existing reset side effects
  } catch (err) {
    setActionError(err instanceof Error ? err.message : 'Failed to restart.');
  } finally {
    setRestartLoading(false);
    setRestartConfirmOpen(false);
  }
};

// Existing button:
<Button onClick={() => setRestartConfirmOpen(true)} disabled={restartLoading}>
  Restart Ladder
</Button>

// At the bottom of the page JSX:
<ConfirmModal
  isOpen={restartConfirmOpen}
  title="Restart Ladder?"
  message="This will reset all votes and start a new tournament with the current activities."
  confirmLabel="Restart"
  cancelLabel="Keep Current"
  tone="danger"
  onConfirm={performRestart}
  onCancel={() => setRestartConfirmOpen(false)}
/>
```

**W7 ships placeholder copy** (the exact strings above). **W29 finalizes the copy** (next PR). Don't bundle copy polish into W7.

### `camper/webapp/src/pages/HomePage.tsx`

**Lines 282-332.** Two ad-hoc modals → two `<ConfirmModal>` instances:

```tsx
<ConfirmModal
  isOpen={!!deletingPlan}
  title="Abandon Expedition?"
  message={`"${deletingPlan?.name ?? ''}" will be lost to the wilderness forever.`}
  confirmLabel="Break Camp"
  cancelLabel="Keep Camp"
  tone="danger"
  icon={/* preserve existing trash SVG (lines 287-294) */}
  onConfirm={handleDeleteConfirm}
  onCancel={() => setDeletingPlan(null)}
/>

<ConfirmModal
  isOpen={!!leavingPlan}
  title="Leave Expedition?"
  message={`You'll pack up your gear and leave "${leavingPlan?.name ?? ''}" behind.`}
  confirmLabel="Leave Camp"
  cancelLabel="Stay at Camp"
  tone="danger"
  icon={/* preserve existing exit-arrow SVG (lines 313-318) */}
  onConfirm={handleLeaveConfirm}
  onCancel={() => setLeavingPlan(null)}
/>
```

**Preserve the existing icons.** Each ad-hoc modal currently renders its own SVG inside `.modal-icon-large`. Pass the same SVG as the new `icon` prop. Visual parity is the goal — the new component just routes through the shared primitive.

The `.modal-overlay` + `.modal-content` JSX is gone after migration.

**`handleDeleteConfirm` and `handleLeaveConfirm` already exist** (lines 36, 53 area). Make them async (they already are). The pending state is internal to ConfirmModal — they don't need their own loading flag anymore. Remove if present (verify `deletingPlan` clears in those handlers; it does at line 41).

### `camper/webapp/src/pages/RecipesPage.tsx`

**Lines 1170-1192.** One ad-hoc modal → `<ConfirmModal>`:

```tsx
<ConfirmModal
  isOpen={!!deletingRecipe}
  title="Remove from Cookbook?"
  message={`"${deletingRecipe?.name ?? ''}" will be lost from the recipe chest.`}
  confirmLabel="Remove"
  cancelLabel="Keep It"
  tone="danger"
  icon={/* preserve existing trash SVG */}
  onConfirm={handleDeleteConfirm}
  onCancel={() => setDeletingRecipe(null)}
/>
```

### `camper/webapp/src/components/GearPacksPanel.tsx`

**Lines 577-603.** The inline `gear-pack-card--confirm` overlay was a *card-level* confirm, not a modal. Migration to a modal is the goal — it's more accessible and matches the rest of the app:

```tsx
const packBeingDeleted = deleteConfirmPackId
  ? packs.find(p => p.id === deleteConfirmPackId) ?? null
  : null;

<ConfirmModal
  isOpen={!!packBeingDeleted}
  title="Delete Gear Pack?"
  message={
    <>This will ungroup all items from plans that use <strong>{packBeingDeleted?.name}</strong>. Are you sure?</>
  }
  confirmLabel={deleting ? '...' : 'Delete'}
  cancelLabel="Cancel"
  tone="danger"
  onConfirm={() => packBeingDeleted && handleDeleteConfirm(packBeingDeleted.id)}
  onCancel={() => setDeleteConfirmPackId(null)}
/>
```

**Remove the inline confirm card branch (lines 577-603).** The `isDeleteConfirm` check at line 423 now is unused — clean it up. The list always renders the normal pack card; the modal floats above.

**Note:** `handleDeleteConfirm` already takes a `packId: string`. Pass `packBeingDeleted.id` through. ConfirmModal handles its own pending state, so the local `deleting` flag becomes redundant. Recommend: remove `setDeleting(true)`/`setDeleting(false)` calls from `handleDeleteConfirm`. The button-level loading is handled by ConfirmModal's `pending` state.

## Implementation notes

- **`<Button loading>` prop.** If it doesn't exist on `Button`, add it (variant of `disabled` plus a small spinner SVG). Or use `disabled={pending}` and let the label change to `'...'`. **Recommend the latter** for minimum-diff; document.
- **`<Button>` autoFocus.** If the existing component doesn't expose `autoFocus`, add the attribute pass-through. Trivial.
- **`<Button>` `forwardRef`.** If the existing component doesn't expose `ref`, wrap with `forwardRef`. Trivial — Button is already a generic styled wrapper.
- **`onConfirm` async.** When the consumer's handler returns a real Promise (`HomePage.handleDeleteConfirm` calls `await api.deletePlan(...)`), ConfirmModal's `setPending(true)` blocks the buttons until the API call resolves. **The consumer doesn't need its own loading state** — but it can still keep one if it surfaces elsewhere (e.g., for screen-reader announcements). Verify no double-disable.
- **Modal stacking with toast.** Confirm during a destructive action that toasts (e.g., a delete that triggers a "Plan deleted" success toast). Toast z-index is 1200; modal overlay is ~100. Toast over modal is correct.
- **Backdrop dismiss while pending.** `<Modal closable={!pending}>` blocks overlay-click and Escape. ConfirmModal also passes a no-op `onClose` while pending. Belt + suspenders.
- **`closeable` typo.** `Modal.tsx` uses `closable`. Match.
- **No new translation files.** Strings are inline.

## Cascade impact

- New `ConfirmModal.tsx`/`ConfirmModal.css` — no callers outside the 5 migration sites.
- `Button.tsx` may need: `loading?: boolean`, `autoFocus?: boolean`, or `forwardRef` (verify; patch only if missing).
- `Modal.tsx` — used by ConfirmModal. No prop changes. Already supports `closable` and `size='sm'`.
- HomePage / RecipesPage / GearPacksPanel / ActivityLadderPage — replace ad-hoc overlay JSX. All in same PR.
- `Modal.css` — possibly add `.modal-actions` rule check (already exists per inline pattern). No new rules required.

**Files that construct deleting/leaving state aren't impacted** — those state hooks (`deletingPlan`, `leavingPlan`, `deletingRecipe`, `deleteConfirmPackId`, `restartConfirmOpen`) stay. Only the modal *render* changes.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/ui/ConfirmModal.test.tsx` (new) | (1) Renders title + message + confirm/cancel buttons when `isOpen=true`. (2) Renders nothing when `isOpen=false`. (3) Click cancel → calls `onCancel`. (4) Click confirm → calls `onConfirm`. (5) Async `onConfirm`: button disabled during await; re-enabled on resolution. (6) Async `onConfirm` rejecting: button re-enabled on rejection. (7) Pending state blocks Escape (Modal closable=false). (8) `tone='danger'` renders danger-variant button; `tone='default'` renders primary. (9) `cancelLabel` defaults to "Cancel". (10) `flavor` and `icon` props render correctly. (11) Cancel button receives focus on open. |
| `src/pages/ActivityLadderPage.restart.test.tsx` (new) | (1) Click "Restart" → ConfirmModal opens. (2) Click "Keep Current" → modal closes, `api.ladders.restart` NOT called. (3) Click "Restart" confirm → `api.ladders.restart(ladderId)` called once; `loadLadder` follows. (4) Failure → modal closes (or stays open with error inline — pick implementation; document choice). |
| `src/pages/HomePage.delete.test.tsx` (new) | (1) Click trash on owned trip → ConfirmModal opens with the trip name in the message. (2) Confirm → `api.deletePlan` called; trip removed from list. (3) Cancel → no API call. (4) Same for leave (non-owned member trip). |
| `src/pages/RecipesPage.delete.test.tsx` (new) | (1) Click "Remove from Cookbook" on detail → ConfirmModal opens. (2) Confirm → `api.deleteRecipe` called; navigates to `/recipes`. (3) Cancel → no API call. |
| `src/components/GearPacksPanel.delete.test.tsx` (new) | (1) Click "Delete" on owned pack → ConfirmModal opens with pack name in `<strong>`. (2) Confirm → `api.deleteGearPack` called; pack removed. (3) Cancel → modal closes; pack stays. |

**Test setup.** Reuse `vi.hoisted()` + `vi.mock('../api/client', ...)` pattern. ConfirmModal tests don't need a router (it's a leaf primitive).

## Manual smoke checklist

- Restart Ladder, Delete Trip, Leave Trip, Delete Recipe, Delete Gear Pack — all show the new parchment confirm modal. `window.confirm` no longer appears for any flow.
- Confirm button disables and shows pending state while the API call is in flight (test by network-throttling).
- Escape key cancels the confirm (when not pending). Overlay click cancels (when not pending).
- Cancel button has focus when the modal opens.
- `grep -rn "window.confirm\|confirm(" camper/webapp/src/` returns zero matches outside test files.

---

# PR 2 — W29: Restart Ladder confirm copy

**Branch.** `clean-up-ui-w29-restart-ladder-copy`

**Commit title.** `feat(clean-up-ui): W29 — restart ladder confirm copy`

**Acceptance (from plan.md).** Restart action shows a clear, themed confirm with descriptive copy.

**Dependencies.** W7.

## Files to modify

### `camper/webapp/src/pages/ActivityLadderPage.tsx`

Update the `<ConfirmModal>` props to use the final copy from plan.md W29:
```tsx
<ConfirmModal
  isOpen={restartConfirmOpen}
  title="Restart this ladder?"
  message="All votes will be reset and a new tournament will begin with the current activities. This cannot be undone."
  confirmLabel="Restart Ladder"
  cancelLabel="Keep Current Round"
  tone="danger"
  onConfirm={performRestart}
  onCancel={() => setRestartConfirmOpen(false)}
/>
```

That's it. One PR, copy-only diff.

## Implementation notes

- Keep the icon optional (don't add one unless plan.md asks). The W7 placeholder didn't include an icon; W29 doesn't either. The modal renders without `icon` cleanly.
- No CSS changes. No state changes. No new imports.

## Tests to add

| File | Scenarios |
|---|---|
| `src/pages/ActivityLadderPage.restart.test.tsx` (modify existing from W7) | Update assertions: title text now reads "Restart this ladder?", message contains "All votes will be reset", confirm label is "Restart Ladder", cancel label is "Keep Current Round". |

## Manual smoke checklist

- Restart action shows the final copy.
- No regressions on W7 behavior (async confirm, etc.).

---

# PR 3 — W23: Delete + invite consistency

**Branch.** `clean-up-ui-w23-delete-invite-consistency`

**Commit title.** `feat(clean-up-ui): W23 — delete + invite UX consistency`

**Acceptance (from plan.md).** A user encountering a "delete" or "invite" action anywhere in the app sees the same UI pattern.

**Dependencies.** W7 (`<ConfirmModal>` available; HomePage / RecipesPage / GearPacksPanel already migrated).

## Scope summary

Two themes:
1. **Delete pattern consistency** — make hover-revealed delete buttons the canonical visible affordance. Migrate GearModal items, AssignmentsModal cards, AssignmentsModal kick-member, GearPacksPanel cards (already opens `<ConfirmModal>` from W7 — this PR adds the hover-reveal trigger styling).
2. **Invite verb consistency** — one label change (`"Add Member"` → `"Invite"` in AssignmentsModal).

## Files to modify

### `camper/webapp/src/components/GearModal.tsx`

**Find the gear item delete button** (lines ~143). Change from always-visible to hover-revealed:

```tsx
{/* Was: <button className="gear-item-delete" ...> */}
<button
  className="gear-item-delete gear-item-delete--hover"
  onClick={() => onDeleteRequest(item)}
  title="Remove item"
  aria-label={`Remove ${item.name}`}
>
  {/* SVG trash icon */}
</button>
```

`onDeleteRequest` is a NEW handler that opens a `<ConfirmModal>` for the item:
```tsx
const [deletingItem, setDeletingItem] = useState<Item | null>(null);

const onDeleteRequest = (item: Item) => setDeletingItem(item);
const onDeleteConfirm = async () => {
  if (!deletingItem) return;
  await api.deleteItem(deletingItem.id);
  await loadItems();  // existing reload helper
  setDeletingItem(null);
};

// At the modal end:
<ConfirmModal
  isOpen={!!deletingItem}
  title="Remove this item?"
  message={`"${deletingItem?.name ?? ''}" will be cleared from your gear list.`}
  confirmLabel="Remove"
  tone="danger"
  onConfirm={onDeleteConfirm}
  onCancel={() => setDeletingItem(null)}
/>
```

The existing `onDelete` handler (immediate delete) is replaced. **All call sites** of the old direct-delete pattern need to call `onDeleteRequest` instead. Verify with `grep -n "onDelete\b" camper/webapp/src/components/GearModal.tsx`.

### `camper/webapp/src/components/GearModal.css`

Add hover-reveal styling. The pattern should match `trip-card__delete` (HomePage) — fade in on row hover:

```css
.gear-item-delete--hover {
  opacity: 0;
  transition: opacity 150ms ease;
}
.gear-item:hover .gear-item-delete--hover,
.gear-item:focus-within .gear-item-delete--hover {
  opacity: 1;
}
```

Adjust selector to whatever the row's parent class is (likely `.gear-list-row` or `.gear-item`). Don't add new color tokens; reuse the existing rose-deep / charcoal palette already in `gear-item-delete`.

### `camper/webapp/src/components/AssignmentsModal.tsx`

**Two changes:**

**1. Delete assignment confirm modal** — line 213 currently `<button onClick={() => onDelete(assignment.id)}>`. Wrap in ConfirmModal:

```tsx
const [deletingAssignment, setDeletingAssignment] = useState<AssignmentDetail | null>(null);

// Click handler:
onClick={() => setDeletingAssignment(assignment)}

// Modal:
<ConfirmModal
  isOpen={!!deletingAssignment}
  title={`Break this ${deletingAssignment?.type === 'tent' ? 'tent' : 'canoe'}?`}
  message={`Members will be unassigned. This cannot be undone.`}
  confirmLabel="Break Camp"
  tone="danger"
  onConfirm={async () => {
    if (deletingAssignment) {
      await onDelete(deletingAssignment.id);
      setDeletingAssignment(null);
    }
  }}
  onCancel={() => setDeletingAssignment(null)}
/>
```

`onDelete` is a prop already passed from the parent (verify line 213 area). The ConfirmModal swaps the immediate behavior for a two-step flow.

**2. Invite verb change** — line 316 currently `{showAddMember ? 'Cancel' : 'Add Member'}`. Change `'Add Member'` to `'Invite'`:

```tsx
{showAddMember ? 'Cancel' : 'Invite'}
```

That's the entire invite-verb pass for this PR.

**3. Member-kick confirm (plan-owner action)** — line 284 area. The kick is currently immediate. Plan-owner removing another member is a destructive cross-actor action; wrap in ConfirmModal:

```tsx
// Distinguish self-leave (immediate) vs other-remove (confirm)
const [removingMember, setRemovingMember] = useState<{
  assignmentId: string;
  userId: string;
  displayName: string;
} | null>(null);

const handleRemoveMemberClick = (assignmentId: string, member: AssignmentMember) => {
  if (member.userId === currentUserId) {
    // Self-leave: immediate
    onRemoveMember(assignmentId, member.userId);
  } else {
    setRemovingMember({ assignmentId, userId: member.userId, displayName: member.displayName });
  }
};

// Modal:
<ConfirmModal
  isOpen={!!removingMember}
  title="Remove from group?"
  message={`${removingMember?.displayName ?? 'They'} will be unassigned from this ${activeTab === 'tent' ? 'tent' : 'canoe'}.`}
  confirmLabel="Remove"
  tone="danger"
  onConfirm={async () => {
    if (removingMember) {
      await onRemoveMember(removingMember.assignmentId, removingMember.userId);
      setRemovingMember(null);
    }
  }}
  onCancel={() => setRemovingMember(null)}
/>
```

The button click route changes to `handleRemoveMemberClick`. Existing `onRemoveMember` prop unchanged.

### `camper/webapp/src/components/AssignmentsModal.css`

Match GearModal's hover-reveal pattern on the danger button (line 213):
```css
.assign-action-btn--danger {
  opacity: 0;
  transition: opacity 150ms ease;
}
.assign-card:hover .assign-action-btn--danger,
.assign-card:focus-within .assign-action-btn--danger {
  opacity: 1;
}
```

(Adjust `.assign-card` to whatever the card-row class actually is.)

### `camper/webapp/src/components/GearPacksPanel.tsx`

**Lines ~624.** The "Delete" text button is always visible. Make it hover-reveal:

```tsx
<button
  className="gear-pack-delete-btn gear-pack-delete-btn--hover"
  onClick={() => setDeleteConfirmPackId(pack.id)}
  title={`Delete ${pack.name}`}
  aria-label={`Delete ${pack.name}`}
>
  Delete
</button>
```

CSS in `GearPacksPanel.css`:
```css
.gear-pack-delete-btn--hover {
  opacity: 0;
  transition: opacity 150ms ease;
}
.gear-pack-card:hover .gear-pack-delete-btn--hover,
.gear-pack-card:focus-within .gear-pack-delete-btn--hover {
  opacity: 1;
}
```

The `<ConfirmModal>` flow (from W7) is unchanged. This PR purely tweaks the trigger's visibility.

## Implementation notes

- **No changes to PlanPage member-removal flows** (line 354-363 area). Those go through the avatar's onRemove and have their own UX (the avatar X button is already always-visible per the campfire-circle aesthetic and is intentional). Don't migrate those — the campfire is its own design language. Document.
- **No changes to RecipesPage list cards.** Plan-gate decision: not in scope.
- **The W23 "delete pattern" canonicalization** is purely visual + behavioral: same trash icon (or "Delete" text), same hover-reveal mechanism, same `<ConfirmModal>` step. We don't introduce a new shared component for the delete button itself — the pattern is `<Button variant='icon' size='sm' className='whatever-hover'>` with site-specific layout class. **A future PR could extract `<HoverRevealDeleteButton>` once we have 5+ identical sites.**
- **No new icons.** Reuse the existing trash SVGs already used in HomePage/RecipesPage. If a site uses a custom icon (GearModal's gear-item-delete), keep its visual character.

## Cascade impact

- `GearModal.tsx` — adds `deletingItem` state + ConfirmModal at the modal-end.
- `AssignmentsModal.tsx` — adds two confirm states (`deletingAssignment`, `removingMember`); existing `onDelete` and `onRemoveMember` props still flow through.
- `GearPacksPanel.tsx` — CSS class change only on the delete button.
- New imports of `ConfirmModal` in three files.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/GearModal.delete.test.tsx` (new) | (1) Hover gear item → trash icon visible. (2) Click → ConfirmModal opens with item name. (3) Confirm → `api.deleteItem` called; row disappears. (4) Cancel → no API call. |
| `src/components/AssignmentsModal.delete.test.tsx` (new) | (1) Hover assignment card → danger button appears. (2) Click delete → ConfirmModal opens with type-specific copy. (3) Confirm → `onDelete(assignment.id)` called. (4) Self-leave (member kicks themselves) → immediate `onRemoveMember` call, NO ConfirmModal. (5) Plan-owner removes another member → ConfirmModal opens. |
| `src/components/AssignmentsModal.invite.test.tsx` (new, small) | (1) "Add Member" panel toggle button label reads "Invite" (not "Add Member"). |
| `src/components/GearPacksPanel.delete.test.tsx` (modify W7's existing test) | Update CSS-class assertions: delete button has `--hover` modifier. |

## Manual smoke checklist

- Hover a gear item → trash appears → click → ConfirmModal → confirm → item removed.
- Hover an assignment card → "Delete" button appears at the card header → click → confirm → assignment broken.
- AssignmentsModal "Add Member" button now reads "Invite".
- Plan-owner removing another member from a tent shows ConfirmModal; member self-leaving does not.
- Hover a gear pack card → "Delete" appears → confirm → pack deleted.

---

# PR 4 — W24: Tabs primitive + 3 migrations

**Branch.** `clean-up-ui-w24-tabs-primitive`

**Commit title.** `feat(clean-up-ui): W24 — Tabs primitive + LoginPage / MealPlanModal / AssignmentsModal migrations`

**Acceptance (from plan.md).** All tab UIs look and feel identical. Arrow-key navigation works.

**Dependencies.** None.

## Files to create

### `camper/webapp/src/components/ui/Tabs.tsx` (new)

Per plan-gate decision #5:

```tsx
import {
  Children, isValidElement, cloneElement, useRef, useEffect,
  type ReactNode, type KeyboardEvent,
} from 'react';
import './Tabs.css';

export interface TabsProps {
  value: string;
  onChange: (value: string) => void;
  children: ReactNode;
  ariaLabel?: string;
  className?: string;
}

export interface TabProps {
  value: string;
  label: string;
  icon?: ReactNode;
  disabled?: boolean;
}

// Tab is just a marker component — Tabs introspects children.
export function Tab(_props: TabProps): null { return null; }

export function Tabs({ value, onChange, children, ariaLabel, className }: TabsProps) {
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);

  // Collect Tab children
  const tabs: TabProps[] = Children.toArray(children)
    .filter(isValidElement)
    .map(child => (child as React.ReactElement<TabProps>).props);

  const activeIndex = tabs.findIndex(t => t.value === value);

  const handleKeyDown = (e: KeyboardEvent<HTMLButtonElement>, idx: number) => {
    let next = -1;
    if (e.key === 'ArrowRight') next = (idx + 1) % tabs.length;
    else if (e.key === 'ArrowLeft') next = (idx - 1 + tabs.length) % tabs.length;
    else if (e.key === 'Home') next = 0;
    else if (e.key === 'End') next = tabs.length - 1;
    if (next !== -1) {
      e.preventDefault();
      // Skip disabled tabs
      while (tabs[next].disabled && next !== idx) {
        next = (next + (e.key === 'ArrowLeft' || e.key === 'End' ? -1 : 1) + tabs.length) % tabs.length;
      }
      if (!tabs[next].disabled) {
        onChange(tabs[next].value);
        tabRefs.current[next]?.focus();
      }
    }
  };

  return (
    <div className={`ui-tabs ${className ?? ''}`} role="tablist" aria-label={ariaLabel}>
      {tabs.map((tab, idx) => {
        const isActive = tab.value === value;
        return (
          <button
            key={tab.value}
            ref={el => { tabRefs.current[idx] = el; }}
            type="button"
            role="tab"
            aria-selected={isActive}
            aria-disabled={tab.disabled || undefined}
            tabIndex={isActive ? 0 : -1}
            disabled={tab.disabled}
            className={`ui-tab ${isActive ? 'ui-tab--active' : ''}`}
            onClick={() => !tab.disabled && onChange(tab.value)}
            onKeyDown={(e) => handleKeyDown(e, idx)}
          >
            {tab.icon && <span className="ui-tab__icon" aria-hidden="true">{tab.icon}</span>}
            <span className="ui-tab__label">{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
}
```

### `camper/webapp/src/components/ui/Tabs.css` (new)

Reuse existing visual language. Pull common rules from `assign-tabs`/`mp-tabs`/`login-card-header`:

```css
.ui-tabs {
  display: flex;
  gap: 4px;
  padding: 6px;
  border-bottom: 1.5px solid var(--tan-deep);
  background: rgba(255, 248, 231, 0.4);
  border-radius: 8px 8px 0 0;
}
.ui-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: transparent;
  border: none;
  font-family: var(--font-display);
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--charcoal-light);
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 150ms, color 150ms;
}
.ui-tab:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.4);
  color: var(--charcoal);
}
.ui-tab:focus-visible {
  outline: 2px solid var(--lavender);
  outline-offset: 2px;
}
.ui-tab--active {
  background: var(--parchment);
  color: var(--charcoal);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}
.ui-tab:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.ui-tab__icon { display: inline-flex; line-height: 0; }
```

(Tweak as needed during implementation; the spirit is "match existing parchment tabs.")

## Files to modify

### `camper/webapp/src/pages/LoginPage.tsx`

**Lines 73-86.** Replace the inline tab buttons:

```tsx
<Tabs
  value={mode}
  onChange={(v) => { setMode(v as 'login' | 'register'); setError(''); }}
  ariaLabel="Authentication mode"
  className="login-tabs"
>
  <Tab value="login" label="Sign In" />
  <Tab value="register" label="Register" />
</Tabs>
```

Imports: `import { Tabs, Tab } from '../components/ui/Tabs';`. Remove `.login-tab` / `.login-tab--active` rules from `LoginPage.css` (or keep them as no-ops if used elsewhere — verify with `grep -rn "login-tab" camper/webapp/src/`). The replacement uses `.login-tabs` as the host class only for layout positioning if needed.

### `camper/webapp/src/components/MealPlanModal.tsx`

**Lines 454-468.** Replace the inline tab buttons:

```tsx
<Tabs
  value={activeView}
  onChange={(v) => setActiveView(v as ViewTab)}
  ariaLabel="Meal plan sections"
  className="mp-tabs-container"
>
  <Tab value="overview" label="Overview" />
  <Tab value="recipes" label="Recipe Book" />
  <Tab value="shopping" label="Shopping List" />
</Tabs>
```

**Critical: W6 hash sync stays exactly as-is.** The three `useEffect`s at lines 58-85 are unchanged. The migration only swaps the rendered JSX. `setActiveView` continues to be called by `onChange`; the hash-state reflects-the-state effect (Effect 2, lines 71-77) fires on every `setActiveView` call regardless of source. Tested manually: clicking a tab → onChange → setActiveView → Effect 2 → pushState. Works.

Remove `.mp-tab` / `.mp-tab--active` from `MealPlanModal.css` (verify no other consumer with grep).

### `camper/webapp/src/components/AssignmentsModal.tsx`

**Lines 548-564.** Replace the inline tab buttons:

```tsx
<Tabs
  value={activeTab}
  onChange={(v) => setActiveTab(v as 'tent' | 'canoe')}
  ariaLabel="Assignment type"
  className="assign-tabs-container"
>
  <Tab
    value="tent"
    label="Tents"
    icon={<span aria-hidden="true">{'△'}</span>}
  />
  <Tab
    value="canoe"
    label="Canoes"
    icon={<span aria-hidden="true">{'◠'}</span>}
  />
</Tabs>
```

(The icons today are inline `△` and `◠` glyphs in `<span className="assign-tab-icon">`. The new shape passes them as `icon` prop.)

Remove `.assign-tab` / `.assign-tab--active` / `.assign-tab-icon` from `AssignmentsModal.css`.

## Implementation notes

- **`Children.toArray` over `Children.map`.** We need the props extracted; `Children.toArray` filters fragments cleanly.
- **`<Tab>` returns null on render.** It's a marker. Tabs is the actual renderer. This is the standard React pattern (e.g., react-router's `<Route>` was once like this; some Material UI components still use it).
- **Hash sync invariant.** The W6 hash sync logic in MealPlanModal stays exactly the same. Re-verify by manual smoke after migration: click a tab → URL hash updates → click browser back → previous tab activates.
- **AssignmentsModal kept activeTab values.** `'tent' | 'canoe'`. Type narrowing on `onChange` is the consumer's responsibility (`v as 'tent' | 'canoe'` cast).
- **No new color tokens.** Reuses parchment/lavender/charcoal palette.
- **Old CSS class removal.** Be careful: `assign-tab-icon` may have specificity in another file. Grep before deleting.

## Cascade impact

- `LoginPage.tsx` / `LoginPage.css` — replace tabs JSX; remove old classes.
- `MealPlanModal.tsx` / `MealPlanModal.css` — replace tabs JSX; remove old classes; **W6 hash sync untouched**.
- `AssignmentsModal.tsx` / `AssignmentsModal.css` — replace tabs JSX; remove old classes.
- New file `Tabs.tsx` / `Tabs.css` — only consumers are the three migration sites in this PR.

**Test files that reference old tab class names** need updates:
- Search: `grep -rn "mp-tab\|assign-tab\|login-tab" camper/webapp/src/ --include="*.test.tsx"`
- Likely matches in W6 tab hash-sync tests. Update selectors to `[role="tab"]` and `[aria-selected="true"]`.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/ui/Tabs.test.tsx` (new) | (1) Renders all `<Tab>` children with correct labels. (2) Active tab has `aria-selected="true"` and `tabindex="0"`. (3) Click on inactive tab calls `onChange` with that value. (4) Arrow Right cycles to next tab; wraps to first. (5) Arrow Left cycles to previous tab; wraps to last. (6) Home jumps to first; End to last. (7) Disabled tab is skipped during arrow navigation. (8) `icon` prop renders alongside label. (9) `tabIndex` is roving (only active tab has 0; others -1). |
| `src/pages/LoginPage.tabs.test.tsx` (new) | (1) Sign In / Register tabs render. (2) Click Register → mode changes to 'register'; error cleared. (3) Arrow Right works. |
| `src/components/MealPlanModal.tabs.test.tsx` (modify W6's existing test if it asserts `mp-tab` classes) | (1) All three tabs render with role="tab". (2) Click "Recipe Book" → `activeView` state updates → URL hash changes to `#recipes` (W6 invariant). (3) Arrow Right cycles overview → recipes → shopping → wraps to overview. |
| `src/components/AssignmentsModal.tabs.test.tsx` (new) | (1) Tents / Canoes tabs render with icons. (2) Click "Canoes" → `activeTab` becomes 'canoe'. (3) Arrow Right cycles. |

## Manual smoke checklist

- LoginPage: tabs work; arrow keys cycle.
- MealPlanModal: tabs work; URL hash still updates per W6; browser back walks through tabs.
- AssignmentsModal: tabs work; tent/canoe icons render.
- Visual parity: tab styles match across all three sites.
- Keyboard-only navigation: Tab into tablist → arrow keys cycle without leaving the tablist.

---

# PR 5 — W18: Servings/scaling context

**Branch.** `clean-up-ui-w18-servings-context`

**Commit title.** `feat(clean-up-ui): W18 — surface servings/scaling context in shopping list & recipe book`

**Acceptance (from plan.md).** Both views show servings context unambiguously.

**Dependencies.** None.

## Files to modify

### `camper/webapp/src/components/MealPlanModal.tsx`

**Two text additions:**

**1. Shopping list header.** In `ShoppingListView` (around the `mp-shopping-progress` element — find with `grep -n "mp-shopping-progress" camper/webapp/src/components/MealPlanModal.tsx`). Add a header line ABOVE the progress bar:

```tsx
<div className="mp-shopping-context">
  Shopping list for {shoppingList.servings} {shoppingList.servings === 1 ? 'serving' : 'servings'}
  {' · '}
  {shoppingList.fullyPurchasedCount} of {shoppingList.totalItems} purchased
</div>
{/* existing progress bar below */}
```

If a `mp-shopping-progress` line already shows "X / Y purchased", consolidate or reposition. The single header line is the goal.

**2. Recipe Book detail scaling line.** In `RecipeBookView` right page (where ingredients render). Find the ingredient list block — usually under a heading like "Ingredients". Add ABOVE it:

```tsx
{selectedRecipe && mealPlan && (
  <div className="mp-recipe-scaling">
    Base: {selectedRecipe.baseServings} {selectedRecipe.baseServings === 1 ? 'serving' : 'servings'}
    {' · '}
    Scaled to {mealPlan.servings} {mealPlan.servings === 1 ? 'serving' : 'servings'}
  </div>
)}
```

`selectedRecipe` is the `RecipeDetailResponse` selected in the recipe book; `selectedRecipe.baseServings` is on the type. `mealPlan.servings` is from the meal plan.

**Edge cases:**
- If no meal plan exists when the recipe book opens, do NOT render the scaling line (the recipe is being viewed independently). Today, RecipeBookView is only reachable from inside a meal plan, so `mealPlan` is non-null. Guard anyway.
- Pluralization: "1 serving" vs "N servings". Already established in W17/MealPlanReconcileBanner — copy the helper or inline it.

### `camper/webapp/src/components/MealPlanModal.css`

```css
.mp-shopping-context {
  font-size: 0.95rem;
  color: var(--charcoal);
  margin-bottom: var(--space-sm);
  font-weight: 500;
}
.mp-recipe-scaling {
  font-size: 0.9rem;
  color: var(--charcoal-light);
  font-style: italic;
  margin-bottom: var(--space-sm);
  padding: 4px 0;
  border-bottom: 1px dotted var(--tan-deep);
}
```

Reuse `--space-sm`, `--charcoal`, `--charcoal-light`, `--tan-deep`. No new tokens.

## Implementation notes

- **No new fetches.** Both fields exist on already-loaded responses.
- **The `Shopping list for N servings` line replaces nothing** — it's additive context. The existing progress display can stay as-is or absorb into the new header. Prefer the latter (consolidate) for cleaner UI.
- **Keep the line above the progress bar**, not in the `.mp-shopping-progress` block. They serve different purposes (context vs progress).
- **Recipe Book scaling line** sits between the recipe name/description and the ingredient list. Don't put it above the recipe name — it's metadata about the *list* below.

## Cascade impact

- `MealPlanModal.tsx` — two new render blocks (~30 lines net).
- `MealPlanModal.css` — two new rules.
- No new types, no new props, no API changes.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.servingsContext.test.tsx` (new) | (1) Open shopping list with `servings=4`, `totalItems=10`, `fullyPurchasedCount=3` → header reads `"Shopping list for 4 servings · 3 of 10 purchased"`. (2) Singular: `servings=1` → "1 serving". (3) Recipe Book: select a recipe with `baseServings=2`, meal plan `servings=4` → scaling line reads `"Base: 2 servings · Scaled to 4 servings"`. (4) Singular: `baseServings=1` → "1 serving". (5) When no recipe selected, scaling line does NOT render. |

Reuse Phase 1+2+3 setup: `vi.hoisted` mocks for `api.getMealPlanForTrip` / `api.getShoppingList` / `api.getRecipe`.

## Manual smoke checklist

- Open MealPlanModal → Shopping List → header reads "Shopping list for {N} servings · X of Y purchased".
- Recipe Book → select recipe → "Base: 2 servings · Scaled to 4" line appears above the ingredient list.
- Change meal plan servings via the stepper → scaling line updates after reload.

---

# PR 6 — W19: Pending member visuals

**Branch.** `clean-up-ui-w19-pending-member-visuals`

**Commit title.** `feat(clean-up-ui): W19 — pending member visual treatment (dashed, opacity, envelope)`

**Acceptance (from plan.md).** Pending vs. active members are visually distinct in the circle and in the AssignmentsModal member lists.

**Dependencies.** None.

## Files to modify

### `camper/webapp/src/components/CamperAvatar.tsx`

Add an envelope-icon overlay when `isPending`:

```tsx
{isPending && (
  <span className="camper-avatar__envelope" aria-hidden="true">
    <svg width="14" height="14" viewBox="0 0 14 14">
      <rect x="1" y="3" width="12" height="8" rx="1.5" fill="var(--parchment)" stroke="var(--tan-deep)" strokeWidth="1.2" />
      <path d="M1.5,3.5 L7,8 L12.5,3.5" fill="none" stroke="var(--tan-deep)" strokeWidth="1.2" />
    </svg>
  </span>
)}
```

Tooltip prefix: extend the existing `title` logic to prepend `Invited:` when pending. Today `title` is set only on failure (line 100); add for pending:

```tsx
title={
  isFailed && email ? `Failed to invite ${email}` :
  isPending && email ? `Invited: ${email}` :
  undefined
}
```

### `camper/webapp/src/components/CamperAvatar.css`

Add the dashed-outline + opacity + envelope-position rules:

```css
.camper-avatar--pending {
  /* preserve existing rules, ADD: */
  outline: 2px dashed var(--tan-deep);
  outline-offset: 6px;
  opacity: 0.7;
  border-radius: 50%;
}
.camper-avatar--pending .camper-avatar__envelope {
  position: absolute;
  top: 0;
  right: -4px;
  background: var(--parchment);
  border-radius: 50%;
  padding: 2px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}
```

(Tweak positioning to match the visual language; verify it doesn't clip behind sibling avatars in the circle.)

### `camper/webapp/src/components/AvatarHead.tsx`

Add an optional `invitationStatus` prop and apply pending styling:

```tsx
interface AvatarHeadProps {
  // ...existing...
  invitationStatus?: string | null;
}

export function AvatarHead({ /* existing */, invitationStatus }: AvatarHeadProps) {
  const isPending = invitationStatus === 'pending'; // mirror CamperAvatar's logic;
  // verify whether pending status semantics match (CamperAvatar uses `!name` as primary, status only for failed)
  // ...
  return (
    <div className={`avatar-head ${isPending ? 'avatar-head--pending' : ''}`}>
      {/* existing svg */}
    </div>
  );
}
```

### `camper/webapp/src/components/AvatarHead.css` (or wherever AvatarHead's CSS lives)

```css
.avatar-head--pending {
  outline: 1.5px dashed var(--tan-deep);
  outline-offset: 2px;
  opacity: 0.7;
  border-radius: 50%;
}
```

(Compact — no envelope overlay on the small variant; the dashed outline conveys the same info.)

### `camper/webapp/src/components/AssignmentsModal.tsx`

Where `<AvatarHead>` is rendered for plan members, pass `invitationStatus`:

```tsx
<AvatarHead
  /* existing props */
  invitationStatus={member.invitationStatus}
/>
```

Find call sites: `grep -n "AvatarHead" camper/webapp/src/components/AssignmentsModal.tsx`. Pass through for every member-list rendering.

## Implementation notes

- **`isPending` semantics in CamperAvatar today.** Currently `isPending = !name`. That includes any member without a username (registered users post-account-creation get a username immediately). The dashed/opacity/envelope styling should activate on this same condition — they ARE pending. **Don't change the condition.** Just add the new visual treatment.
- **Envelope icon placement.** The CamperAvatar in the campfire circle is positioned via `transform: translate()`. The envelope overlay needs to be a child of the avatar div with `position: absolute` and a small offset. Test layering with adjacent avatars (z-index may matter).
- **AvatarHead's pending semantics.** Today AvatarHead has no concept of pending. Adding `invitationStatus` is the cascade. The condition for "pending" treatment: `invitationStatus === 'pending'` OR a synthetic flag passed by the caller (e.g., AssignmentsModal might prefer to treat any non-registered member as pending). For consistency with CamperAvatar, default to `invitationStatus === 'pending'` and let callers also pass a `forcePending?: boolean` if more flexibility is needed later. **Recommend not adding `forcePending` until a real consumer needs it.**
- **No backend change.** `invitationStatus` is already on `PlanMember`. AvatarHead consumers are AssignmentsModal (PlanMember source) and LadderPeoplePanel (LadderParticipant source — no `invitationStatus`; the prop stays optional). LadderPeoplePanel call sites omit the prop; default behavior is unchanged.

## Cascade impact

**Files that construct `<CamperAvatar>`:**
- `PlanPage.tsx` — already passes `invitationStatus`. ✓
- `MealPlanModal.tsx` — does it? Verify with grep. (It doesn't directly; uses `useToast` etc.) No change.

**Files that construct `<AvatarHead>`:**
- `AssignmentsModal.tsx` — multiple call sites. UPDATE all to pass `invitationStatus={member.invitationStatus}`.
- `LadderPeoplePanel.tsx` — multiple call sites. NO change (prop is optional; LadderParticipant has no `invitationStatus`).

Search: `grep -rn "<AvatarHead" camper/webapp/src/` to enumerate. **Listing here would be premature** — the plan says enumerate at implementation start, fix every call site.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/CamperAvatar.pending.test.tsx` (new) | (1) `name=null` → renders `camper-avatar--pending` class. (2) Pending avatar shows envelope SVG overlay. (3) `email='x@y.com'` + pending → tooltip reads `"Invited: x@y.com"`. (4) Registered avatar (`name='Alice'`) → no envelope, no `--pending` class. |
| `src/components/AvatarHead.pending.test.tsx` (new) | (1) `invitationStatus='pending'` → renders `avatar-head--pending` class. (2) `invitationStatus='delivered'` or null → no `--pending` class. (3) Existing call sites (LadderPeoplePanel) without the prop render normally. |
| `src/pages/PlanPage.pendingAvatar.test.tsx` (new, integration) | (1) Render PlanPage with a member who has `invitationStatus='pending'` and `username=null` → that avatar is rendered with pending visual class. (2) Member with `username='Alice'` → no pending class. |

## Manual smoke checklist

- PlanPage with a pending invite → that avatar has dashed outline, ~70% opacity, envelope ✉ overlay.
- Hover the pending avatar → tooltip "Invited: alice@example.com".
- Open AssignmentsModal → in the add-member panel, pending plan members show with dashed outline (compact treatment).
- Registered members are visually unchanged.

---

# PR 7 — W20: Pending invitations rollup

**Branch.** `clean-up-ui-w20-pending-invitations-rollup`

**Commit title.** `feat(clean-up-ui): W20 — pending invitations rollup in Manage Plan`

**Acceptance (from plan.md).** Owner sees every pending invite with status, can resend the ones BE supports resending, and can remove any pending invite. No misleading affordances for already-delivered invites.

**Dependencies.** W19 (pending styling reused).

## Files to create

### `camper/webapp/src/components/PendingInvitationsList.tsx` (new)

A new sub-component used inside the Manage Plan modal in PlanPage. Public API:

```tsx
import { useState } from 'react';
import type { PlanMember } from '../api/client';
import { Button } from './ui/Button';
import { useToast } from '../context/ToastContext';
import { ConfirmModal } from './ui/ConfirmModal';
import { AvatarHead } from './AvatarHead';

const RESEND_ENABLED_STATUSES = new Set(['pending', 'failed', 'bounced']);

function statusLabel(status: string | null): string {
  switch (status) {
    case 'pending': return 'Pending';
    case 'sent': return 'Sent';
    case 'delivered': return 'Delivered';
    case 'delayed': return 'Delayed';
    case 'failed': return 'Failed';
    case 'bounced': return 'Bounced';
    case 'complained': return 'Marked as spam';
    default: return 'Unknown';
  }
}

function passiveLabel(status: string | null): string | null {
  switch (status) {
    case 'sent':
    case 'delivered':
      return 'Already delivered — ask them to check spam';
    case 'delayed':
      return 'Delivery delayed by recipient mail server';
    case 'complained':
      return 'Recipient flagged as spam — try a different email';
    default:
      return null;
  }
}

export interface PendingInvitationsListProps {
  members: PlanMember[];
  planId: string;
  onResend: (email: string) => Promise<void>;     // wraps api.addMember(planId, email)
  onRemove: (memberId: string) => Promise<void>;  // wraps api.removeMember(planId, memberId)
}

export function PendingInvitationsList({ members, planId, onResend, onRemove }: PendingInvitationsListProps) {
  const toast = useToast();
  const pending = members.filter(m => !m.username && m.email);
  const [resending, setResending] = useState<string | null>(null);  // userId currently being resent
  const [removingMember, setRemovingMember] = useState<PlanMember | null>(null);

  if (pending.length === 0) return null;

  const handleResend = async (member: PlanMember) => {
    if (!member.email) return;
    setResending(member.userId);
    try {
      await onResend(member.email);
      toast.success(`Invitation resent to ${member.email}`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to resend invitation');
    } finally {
      setResending(null);
    }
  };

  return (
    <div className="pending-invitations">
      <span className="manage-plan-setting-label">
        Pending invitations ({pending.length})
      </span>
      <div className="pending-invitations__list">
        {pending.map(member => {
          const canResend = RESEND_ENABLED_STATUSES.has(member.invitationStatus ?? '');
          const passive = passiveLabel(member.invitationStatus);
          const sent = new Date(member.createdAt).toLocaleDateString('en-US', {
            month: 'short', day: 'numeric', year: 'numeric'
          });
          return (
            <div key={member.userId} className="pending-invitation-row">
              <AvatarHead
                /* existing required props */
                invitationStatus={member.invitationStatus}
              />
              <div className="pending-invitation-row__info">
                <span className="pending-invitation-row__email">{member.email}</span>
                <span className="pending-invitation-row__meta">
                  Invited {sent} · {statusLabel(member.invitationStatus)}
                </span>
                {passive && (
                  <span className="pending-invitation-row__passive">{passive}</span>
                )}
              </div>
              <div className="pending-invitation-row__actions">
                {canResend && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => handleResend(member)}
                    disabled={resending === member.userId}
                  >
                    {resending === member.userId ? 'Sending…' : 'Resend invite'}
                  </Button>
                )}
                <Button
                  size="sm"
                  variant="ghost"
                  onClick={() => setRemovingMember(member)}
                  aria-label={`Remove invitation for ${member.email}`}
                >
                  Remove
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      <ConfirmModal
        isOpen={!!removingMember}
        title="Cancel this invitation?"
        message={`The invitation to ${removingMember?.email ?? ''} will be canceled. You can re-send later.`}
        confirmLabel="Cancel Invite"
        cancelLabel="Keep It"
        tone="danger"
        onConfirm={async () => {
          if (removingMember) {
            await onRemove(removingMember.userId);
            setRemovingMember(null);
          }
        }}
        onCancel={() => setRemovingMember(null)}
      />
    </div>
  );
}
```

### `camper/webapp/src/components/PendingInvitationsList.css` (new)

```css
.pending-invitations {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  margin-top: var(--space-md);
}
.pending-invitations__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}
.pending-invitation-row {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: 8px 12px;
  background: rgba(255, 248, 231, 0.4);
  border: 1px solid var(--tan);
  border-radius: 6px;
}
.pending-invitation-row__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pending-invitation-row__email {
  font-weight: 600;
  color: var(--charcoal);
  overflow: hidden;
  text-overflow: ellipsis;
}
.pending-invitation-row__meta {
  font-size: 0.85rem;
  color: var(--charcoal-light);
}
.pending-invitation-row__passive {
  font-size: 0.85rem;
  color: var(--charcoal-light);
  font-style: italic;
  margin-top: 2px;
}
.pending-invitation-row__actions {
  display: flex;
  gap: 6px;
}
```

## Files to modify

### `camper/webapp/src/pages/PlanPage.tsx`

Inside the `managePlan` modal (line 420+), insert the rollup AFTER the existing Members list (line 506), gated to `isOwner`:

```tsx
{isOwner && (
  <PendingInvitationsList
    members={members}
    planId={planId!}
    onResend={async (email) => {
      await api.addMember(planId!, email);
      await loadData();
    }}
    onRemove={async (memberId) => {
      await api.removeMember(planId!, memberId);
      await loadData();
    }}
  />
)}
```

Import: `import { PendingInvitationsList } from '../components/PendingInvitationsList';`.

## Implementation notes

- **Owner-only visibility.** Plan.md says "Owner sees every pending invite". Non-owner members shouldn't see the resend/remove affordances. Gate via `isOwner`.
- **Why owner not also manager?** Manager-level permissions for member management aren't established yet (current pattern is `isOwner` for plan-level admin). Keep parity with existing `Manage Plan` semantics. If managers gain plan-admin powers later, that's a separate workstream.
- **Avoid silent BE skip.** Per plan.md and verified: `delivered`/`sent`/`delayed`/`complained` statuses get a passive label and NO Resend button. The button only appears for `pending`/`failed`/`bounced` (and any unknown status). This prevents the "I clicked Resend and nothing happened" UX failure.
- **Toast on resend success.** "Invitation resent to {email}". On failure, error toast.
- **Removing a pending invite uses the same `removeMember` endpoint.** No new endpoint. The user has a userId in the system but no username (registered=false). Removing them deletes the `plan_members` row and the invitation.
- **STOMP live-update.** `usePlanUpdates` already calls `loadData` on `members` events, which refreshes `members` state — the rollup re-renders with the new pending count automatically. No new STOMP wiring.
- **Filter logic.** `pending = members.filter(m => !m.username && m.email)`. Pending members have null username (registration not completed) but DO have an email (their invited email). Edge case: a member with `null` email is malformed; defensively filter.
- **AvatarHead `invitationStatus` propagation** depends on W19. This PR is sequenced after W19; the prop already exists.
- **No backend change.** Re-verified.

## Cascade impact

- New file `PendingInvitationsList.tsx` + CSS.
- `PlanPage.tsx` — adds the rollup block inside Manage Plan modal.
- `AvatarHead` already accepts `invitationStatus` (W19).
- No type changes to `PlanMember` (already has needed fields).

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/PendingInvitationsList.test.tsx` (new) | (1) Renders nothing when no pending members. (2) Renders one row per pending member with email, invite date, status label. (3) `pending` status → Resend button visible. (4) `failed` status → Resend button visible. (5) `bounced` status → Resend button visible. (6) `delivered` status → NO Resend, passive label shows "Already delivered". (7) `complained` status → NO Resend, passive label shows "Recipient flagged as spam". (8) Click Resend → calls `onResend(email)`; toast success fires. (9) Resend failure → toast error. (10) Click Remove → ConfirmModal opens. (11) Confirm Remove → `onRemove(userId)` called. (12) Cancel Remove → `onRemove` NOT called. |
| `src/pages/PlanPage.pendingInvitations.test.tsx` (new, integration) | (1) Owner viewing Manage Plan with 2 pending + 3 registered members → rollup shows "Pending invitations (2)" with two rows. (2) Non-owner does not see the rollup. (3) Resend → calls `api.addMember(planId, email)` and `loadData`. (4) Remove → calls `api.removeMember(planId, memberId)` and `loadData`. |

## Manual smoke checklist

- As owner of a plan with 2 pending invites (1 `pending`, 1 `delivered`, 1 `failed`):
  - Open Manage Plan → "Pending invitations (3)" section visible.
  - Pending row: Resend button visible.
  - Delivered row: Resend hidden, passive label "Already delivered — ask them to check spam".
  - Failed row: Resend visible.
- Click Resend → toast "Invitation resent to ...".
- Click Remove → ConfirmModal → confirm → invitation row disappears.
- As non-owner: Manage Plan modal does not show the rollup.

---

# PR 8 — W15: Gear pack apply summary + Undo

**Branch.** `clean-up-ui-w15-gear-pack-apply-undo`

**Commit title.** `feat(clean-up-ui): W15 — gear pack apply toast summary + Undo action`

**Acceptance (from plan.md).** Apply pack → toast appears with item count → clicking Undo removes those items. No undo support after toast dismiss.

**Dependencies.** None (W11 already shipped in Phase 1; toast supports `action`).

## Files to modify

### `camper/webapp/src/components/GearPacksPanel.tsx`

**Lines 150-166.** Replace `handleApply` with toast-based feedback:

```tsx
import { useToast } from '../context/ToastContext';
// ... at the top

// Inside the component:
const toast = useToast();

const handleApply = async (packId: string) => {
  setApplying(true);
  setApplyError(null);
  // setApplySuccess removed — replaced by toast
  try {
    const result = await api.applyGearPack(packId, { planId, groupSize });
    const packName = packs.find(p => p.id === packId)?.name ?? 'pack';
    toast.success(`Added ${result.appliedCount} items from ${packName}`, {
      action: {
        label: 'Undo',
        onClick: async () => {
          // Sequential deletes — small N (≤30 items per pack typically)
          for (const item of result.items) {
            try { await api.deleteItem(item.id); } catch {/* ignore individual failures */}
          }
          onItemsChanged();
          toast.info(`Undid ${result.items.length} items from ${packName}`);
        },
      },
    });
    setPreviewPackId(null);
    setPackDetail(null);
    onItemsChanged();
  } catch (e) {
    setApplyError(e instanceof Error ? e.message : 'Failed to apply gear pack');
  } finally {
    setApplying(false);
  }
};
```

**Remove inline `applySuccess` state and rendering.** Per plan-gate decision #10, the inline flash at line 415-418 (the `gear-packs-success` div) is redundant with the toast and should be deleted:

```tsx
// REMOVE:
const [applySuccess, setApplySuccess] = useState<string | null>(null);
// ... and ...
{applySuccess && (<div className="gear-packs-success">{applySuccess}</div>)}
```

### `camper/webapp/src/components/GearPacksPanel.css`

Remove the `.gear-packs-success` rule (now unused). Verify with grep: `grep -rn "gear-packs-success" camper/webapp/src/` — single match in the CSS, no other consumers.

## Implementation notes

- **Sequential deletes during Undo.** Plan.md explicitly says "loops `deleteItem`". `Promise.all` would be faster but riskier (one failure halts the others; sequential allows partial rollback). For ≤30 items, sequential is acceptable.
- **Per-item failure during Undo.** Swallowed silently. The user already saw the success toast; surfacing per-item delete failures here would be alarming. The aggregate "Undid N items" toast is best-effort.
- **No undo-of-undo.** Once Undo runs, the items are gone for good (or rather, the user clicks "Apply" again to redo). Don't add a re-redo escalation; out of scope.
- **Toast lifetime.** Default 4000ms (`DEFAULT_DURATION` in `toastReducer.ts`). Hovering pauses the timer (W11 feature). The Undo window is "until the toast dismisses" — hover to extend.
- **`packs` list at click time.** The pack name lookup uses `packs.find(p => p.id === packId)`. If the pack was deleted between apply and the toast firing (impossible — `applying` blocks), this returns undefined; fall back to the literal `"pack"`.
- **`onItemsChanged()` after Undo.** The parent `GearModal` reloads its item list. Without this, the items stay rendered until the next refresh. Verify the prop is plumbed.

## Cascade impact

- `GearPacksPanel.tsx` — toast added, `applySuccess` state removed, inline render block removed.
- `GearPacksPanel.css` — one rule removed.
- No callers outside.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/GearPacksPanel.applyUndo.test.tsx` (new) | (1) Apply pack → mock `api.applyGearPack` resolves with `{appliedCount: 5, items: [{id:'1'},{id:'2'},...]}`. Toast.success called with message containing "5 items" and an `action` object with `label: 'Undo'`. (2) Click Undo → mock `api.deleteItem` called 5 times in order with each ID. (3) After Undo, `toast.info` called with message containing "Undid 5 items". (4) `onItemsChanged` called twice (once after apply, once after undo). (5) Apply failure → no toast.success; `applyError` set inline. |

**Test setup.** Reuse `vi.hoisted()` for toast and api mocks. Specific helper: assert `toast.success` was called with `expect.objectContaining({ action: expect.objectContaining({ label: 'Undo' }) })`.

## Manual smoke checklist

- Open GearModal → expand Gear Packs → preview a pack → Apply.
- Toast appears: "Added 5 items from Cooking Pack" with "Undo" button.
- Don't click Undo → wait 4s → toast dismisses → items remain.
- Apply again → toast → click Undo → items deleted; second toast "Undid 5 items".
- Hover the toast → timer pauses → can leisurely click Undo within hover.
- Apply with throttled network → applying state shows; on success, toast fires.
- Old inline "Added N items..." green flash is gone.

---

# PR 9 — W9: Blur-save feedback (meal plan name)

**Branch.** `clean-up-ui-w9-blur-save-feedback`

**Commit title.** `feat(clean-up-ui): W9 — meal plan name blur-save inline feedback`

**Acceptance (from plan.md).** Editing the meal plan name shows a "Saved" indicator. Network failure reverts the edit and shows an error.

**Dependencies.** None (W11 already shipped).

## Files to modify

### `camper/webapp/src/components/MealPlanModal.tsx`

**Refactor `handleUpdateName` to track save status:**

```tsx
const [nameSaveStatus, setNameSaveStatus] = useState<'idle' | 'saving' | 'saved' | 'error'>('idle');
const savedFlashTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

useEffect(() => {
  return () => {
    if (savedFlashTimerRef.current) clearTimeout(savedFlashTimerRef.current);
  };
}, []);

const handleUpdateName = async (name: string) => {
  if (!mealPlan) return;
  const trimmed = name.trim();
  if (!trimmed || trimmed === mealPlan.name) return;
  setNameSaveStatus('saving');
  try {
    await api.updateMealPlan(mealPlan.id, { name: trimmed });
    await loadMealPlan();
    setNameSaveStatus('saved');
    if (savedFlashTimerRef.current) clearTimeout(savedFlashTimerRef.current);
    savedFlashTimerRef.current = setTimeout(() => setNameSaveStatus('idle'), 1500);
  } catch (err) {
    // Revert input + inline error + toast
    // The OverviewView's editName state is local; we expose a "revert" by reloading
    // the meal plan (server state is authoritative — name didn't change).
    setNameSaveStatus('error');
    toast.error(err instanceof Error ? err.message : 'Failed to save meal plan name');
    // After 3s, drop back to idle
    if (savedFlashTimerRef.current) clearTimeout(savedFlashTimerRef.current);
    savedFlashTimerRef.current = setTimeout(() => setNameSaveStatus('idle'), 3000);
  }
};
```

**Pass `nameSaveStatus` to `OverviewView`:**

```tsx
<OverviewView
  /* ...existing props... */
  nameSaveStatus={nameSaveStatus}
/>
```

Update `OverviewProps` to include `nameSaveStatus: 'idle' | 'saving' | 'saved' | 'error'`.

**Inside `OverviewView`** (around the meal plan name input at line 729):

```tsx
<div className="mp-plan-name-row">
  <input
    className="mp-plan-name-input"
    value={editName}
    onChange={e => setEditName(e.target.value)}
    onBlur={() => onUpdateName(editName)}
    onKeyDown={e => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); }}
  />
  {nameSaveStatus === 'saving' && (
    <span className="mp-name-save-flash mp-name-save-flash--saving" aria-live="polite">
      Saving…
    </span>
  )}
  {nameSaveStatus === 'saved' && (
    <span className="mp-name-save-flash mp-name-save-flash--saved" aria-live="polite">
      ✓ Saved
    </span>
  )}
  {nameSaveStatus === 'error' && (
    <span className="mp-name-save-flash mp-name-save-flash--error" aria-live="polite" role="alert">
      Couldn't save — try again
    </span>
  )}
</div>
```

**Revert behavior.** On error, `loadMealPlan()` is NOT called (the server still has the old name; the local state's `editName` may show the user's typed value). To make the revert visible, **re-sync `editName` to `mealPlan.name` on error**:

```tsx
// Inside OverviewView (or via a useEffect):
useEffect(() => {
  if (nameSaveStatus === 'error' && mealPlan) {
    setEditName(mealPlan.name);
  }
}, [nameSaveStatus, mealPlan?.name]);
```

This snaps the input back to the persisted value. The error flash sits next to it for ~3s.

### `camper/webapp/src/components/MealPlanModal.css`

```css
.mp-plan-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mp-name-save-flash {
  font-size: 0.85rem;
  font-style: italic;
  animation: fadeIn 200ms ease;
}
.mp-name-save-flash--saving { color: var(--charcoal-light); }
.mp-name-save-flash--saved { color: var(--sage-deep); font-weight: 600; }
.mp-name-save-flash--error { color: var(--rose-deep); font-weight: 600; }
@keyframes fadeIn {
  from { opacity: 0; transform: translateX(-4px); }
  to { opacity: 1; transform: translateX(0); }
}
```

(`fadeIn` may already be in `animations.css`; reuse if so. Otherwise inline.)

## Implementation notes

- **Why both inline AND toast on error?** The inline flash is brief and contextual; the toast persists longer and gives the user time to read. Plan.md says "show an inline error" and the toast adds redundancy for users who blur and look elsewhere.
- **Don't toast on success.** Plan.md is explicit: inline only for the "Saved" success case. Avoids toast spam during quick edits.
- **`saveStatus` reset on idle.** Timer clears the flash after 1.5s success / 3s error. Component unmount cleans the timer.
- **Aria-live.** Inline status updates are announced once via `aria-live="polite"`; the error variant also uses `role="alert"` for assistive tech.
- **What about servings stepper, day add/remove?** Those are also blur-save-ish (the +/- stepper commits immediately). Plan.md W9 specifies only the meal plan name. Don't expand scope.
- **Cascade with W18.** The new line below the name input (servings context) doesn't conflict — they live in different rows.

## Cascade impact

- `MealPlanModal.tsx` — `handleUpdateName` rewritten; `nameSaveStatus` state added; `OverviewView` props extended by one field.
- `OverviewView` — new prop `nameSaveStatus`; new render block for the flash.
- `MealPlanModal.css` — three new classes + one keyframe (or reuse).
- No type changes outside MealPlanModal.

## Tests to add

| File | Scenarios |
|---|---|
| `src/components/MealPlanModal.blurSave.test.tsx` (new) | (1) Edit the name → blur → status='saving' visible briefly. (2) On API resolve, status='saved' for ~1.5s with "✓ Saved" text. (3) After 1.5s, status returns to 'idle' (no flash). (4) On API reject, status='error', `toast.error` fires, input reverts to original value. (5) Editing without changing the name doesn't trigger save. (6) Pressing Enter blurs the input and triggers save (existing behavior preserved). |

**Test setup.** Use vitest fake timers (`vi.useFakeTimers`) to verify the 1.5s/3s timeouts without waiting in real time. Mock `useToast`.

## Manual smoke checklist

- Open MealPlanModal → edit meal plan name → blur (or Enter) → "Saving…" briefly → "✓ Saved" for ~1.5s → fades away.
- Disconnect network → edit name → blur → "Couldn't save — try again" inline + toast error → input reverts to original.
- Edit name to same value → blur → no save (no API call, no flash).
- Multiple rapid edits → only the latest blur fires a save (debounce isn't required; the component already only saves on blur).

---

# Build / test commands (run after every PR)

```bash
cd camper/webapp
npx tsc --noEmit
npm run build
npm run test
```

Code-reviewer must reject any PR where any of the three fails. Code-reviewer must also reject any PR that modifies files outside `camper/webapp/src/` (or the small set of allowed test/build infra files).

---

# Open questions / flags

- **A. (Documented, not blocking.)** ConfirmModal autofocus mechanism depends on whether `<Button>` already supports `forwardRef`. Patch in W7 if needed; trivial. The plan-gate decision is "cancel button auto-focused on open" — implementation tactical, design firm.
- **B. (Documented, not blocking.)** Tabs primitive's `<Tab>` is a marker that returns `null`. Some teams prefer `<Tabs items={[{value, label}]}>`. The marker pattern is more declarative and what plan.md asks for. If review pushes back, the items-array form is a 5-minute reshape.
- **C. (Documented, not blocking.)** W19's `AvatarHead` invitationStatus prop semantics: do we treat ALL non-`delivered`/`sent` statuses as "pending visual" or only the literal `'pending'` string? Recommend the latter for simplicity; document at implementation. The CamperAvatar's `isPending` (no name) and `isFailed` (failed/bounced/complained) coexist — pending visual is for `isPending`, not `isFailed`.
- **D. (Documented, not blocking.)** W20's PendingInvitationsList sequence: AvatarHead is rendered for context, but compact AvatarHead may not have `name` info to render an initial. Verify what AvatarHead expects; if it needs a `name` it's null for pending invites — degrade gracefully (show a generic ghost or just an envelope icon as the avatar slot). **Recommend: skip the avatar in the rollup row when no avatarSeed exists; show only the email + status.** Simplifies the visual.
- **E. (Documented, not blocking.)** W7's `<Button loading>` prop is referenced. If the prop doesn't exist on `Button` today, the plan-fallback is `disabled={pending}` + label change to `'...'`. Implementation choice; not a blocker.

All other Phase 4 plan-gate questions are resolved (see top of this file).
