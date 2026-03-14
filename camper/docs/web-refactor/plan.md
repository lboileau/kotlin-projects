# Web Refactor — Feature Plan

## Feature Summary

Refactor the Camper webapp to eliminate duplicated components, extract a consistent shared UI component library, and unify divergent implementations of the same UI patterns (profile forms, avatar rendering, buttons, inputs, modals). The goal is a clean, efficient frontend where every primitive is defined once and reused everywhere.

**Motivation:** The profile modal on the trip page and the account page profile form are fully duplicated implementations. Avatar color maps are copy-pasted across 5 files. Buttons, inputs, and modals have no shared components — each page/modal re-declares its own styling. The account page renders its own header (with a generic profile image) instead of reusing `AppHeader`.

---

## Audit Findings

### 1. Duplicated Avatar Color Maps (5 copies)

The exact same `SKIN_COLORS` and `HAIR_COLORS` maps are copy-pasted in:
- `components/AvatarHead.tsx`
- `components/CamperAvatar.tsx` (plus SHIRT/PANTS)
- `components/ProfileSetupModal.tsx` (plus SHIRT/PANTS)
- `pages/AccountPage.tsx` (plus SHIRT/PANTS)
- `components/AssignmentsModal.tsx`

### 2. Duplicated Avatar Preview SVG (2 copies)

`ProfileSetupModal.tsx` defines `AvatarPreview` and `AccountPage.tsx` defines `AvatarPreviewSvg` — they are character-for-character identical except the CSS class name.

### 3. Duplicated Profile Form (2 copies)

`ProfileSetupModal.tsx` and `AccountPage.tsx` both implement the same form:
- Trail Name input
- Experience Level select
- Dietary Restrictions checkbox grid
- Avatar preview + Randomize button
- Submit handler calling `api.updateUser()`

They also duplicate `DIETARY_OPTIONS` and `EXPERIENCE_OPTIONS` arrays identically.

### 4. No Shared Button Component

Buttons are styled via ~10 different CSS class families:
- `.modal-btn` / `.modal-btn--secondary` / `.modal-btn--danger` (Modal.css)
- `.login-submit` (LoginPage.css)
- `.account-save`, `.account-back`, `.account-randomize-btn` (AccountPage.css)
- `.setup-randomize-btn` (ProfileSetupModal.css)
- `.gear-qty-btn`, `.gear-item-edit`, `.gear-item-delete` (GearModal.css)
- `.assign-action-btn`, `.assign-join-btn`, `.assign-leave-btn` (AssignmentsModal.css)
- `.app-header__logout`, `.app-header__action-btn`, `.app-header__user-btn` (AppHeader.css)

Many of these re-declare the same sage-green gradient, hover-lift, and disabled styles.

### 5. No Shared Input/Select Component

Inputs are styled via 3 different class families:
- `.modal-input` (Modal.css) — used in modals
- `.login-input` (LoginPage.css) — similar but different border/shadow
- `.account-input` (AccountPage.css) — yet another variant

### 6. No Shared Modal Shell

Every modal manually renders `.modal-overlay` + `.modal-content` + close button SVG inline. The close button X SVG is duplicated in ProfileSetupModal, AccountPage, and likely others.

### 7. AccountPage Renders Its Own Header

`AccountPage.tsx` renders a fully custom header with its own logo, back button, user avatar (generic SVG, not `AvatarHead`!), and logout button — instead of using `AppHeader`. This is why the nav avatar reverts to a generic profile image on the account page.

### 8. No Shared Form Field Component

Every form manually wraps label + input in its own div with its own class (`.setup-field`, `.account-field`, `.login-field`).

---

## Tickets

### Ticket 1: Extract shared avatar constants to `lib/avatarConstants.ts`

**Goal:** Single source of truth for all avatar color maps and option lists.

**What to create:**
- `webapp/src/lib/avatarConstants.ts` — export `SKIN_COLORS`, `HAIR_COLORS`, `SHIRT_COLORS`, `PANTS_COLORS`, `FALLBACK_COLORS`

**What to update:**
- `components/AvatarHead.tsx` — import from lib, delete local maps
- `components/CamperAvatar.tsx` — import from lib, delete local maps
- `components/ProfileSetupModal.tsx` — import from lib, delete local maps
- `pages/AccountPage.tsx` — import from lib, delete local maps
- `components/AssignmentsModal.tsx` — import from lib, delete local maps + `FALLBACK_COLORS`

**Acceptance:** `npm run build` passes. No color map is defined in any component file.

---

### Ticket 2: Extract shared profile constants to `lib/profileConstants.ts`

**Goal:** Single source of truth for dietary and experience options.

**What to create:**
- `webapp/src/lib/profileConstants.ts` — export `DIETARY_OPTIONS`, `EXPERIENCE_OPTIONS`

**What to update:**
- `components/ProfileSetupModal.tsx` — import from lib, delete local arrays
- `pages/AccountPage.tsx` — import from lib, delete local arrays

**Acceptance:** `npm run build` passes. No option array is defined in any component file.

---

### Ticket 3: Create `AvatarPreview` shared component

**Depends on:** Ticket 1

**Goal:** One avatar body preview component used everywhere.

**What to create:**
- `webapp/src/components/AvatarPreview.tsx` — extract the shared SVG body (full-body seated avatar preview) from `ProfileSetupModal.tsx` `AvatarPreview` function. Props: `avatar: AvatarResponse | null`, `size?: number`.

**What to update:**
- `components/ProfileSetupModal.tsx` — delete local `AvatarPreview` function, import shared component
- `pages/AccountPage.tsx` — delete local `AvatarPreviewSvg` function, import shared component. Delete `account-avatar-preview-svg` CSS class if now unused.

**Acceptance:** `npm run build` passes. Avatar preview renders identically in both modal and account page.

---

### Ticket 4: Create `Button` shared component

**Goal:** One `<Button>` component with variants, replacing all ad-hoc button styling.

**What to create:**
- `webapp/src/components/ui/Button.tsx` — props: `variant` (`primary` | `secondary` | `danger` | `ghost` | `icon`), `size` (`sm` | `md` | `lg`), `loading?: boolean`, `disabled?: boolean`, `className?: string`, `children`, standard button HTML attributes.
- `webapp/src/components/ui/Button.css` — consolidate all button styles from Modal.css and component-specific CSS. Keep the existing sage-green gradient for primary, rose for danger, transparent for secondary, minimal for icon/ghost.

**What to update (incremental — each file at a time):**
- All modals: replace `<button className="modal-btn">` with `<Button variant="primary">`, etc.
- `LoginPage.tsx`: replace `.login-submit` with `<Button>`
- `AccountPage.tsx`: replace `.account-save`, `.account-randomize-btn` with `<Button>`
- `ProfileSetupModal.tsx`: replace `.setup-randomize-btn` with `<Button>`
- `AppHeader.tsx`: replace `.app-header__logout` with `<Button variant="danger" size="sm">`
- Remove now-unused button CSS from Modal.css, LoginPage.css, AccountPage.css, ProfileSetupModal.css, AppHeader.css

**Acceptance:** `npm run build` passes. All buttons render via `<Button>`. Visual appearance unchanged.

---

### Ticket 5: Create `Input`, `Select`, `FormField` shared components

**Goal:** Consistent form primitives.

**What to create:**
- `webapp/src/components/ui/Input.tsx` — styled text/email input. Props: standard input HTML attributes + `error?: string`.
- `webapp/src/components/ui/Select.tsx` — styled select dropdown. Props: `options: {value, label}[]`, `placeholder?: string`, standard select attributes.
- `webapp/src/components/ui/CheckboxGroup.tsx` — grid of checkboxes. Props: `options: {value, label}[]`, `selected: string[]`, `onChange: (selected: string[]) => void`.
- `webapp/src/components/ui/FormField.tsx` — label + child wrapper. Props: `label: string`, `children`.
- `webapp/src/components/ui/ui.css` — consolidated input/select/field styles from Modal.css, LoginPage.css, AccountPage.css.

**What to update:**
- `ProfileSetupModal.tsx` — use `FormField`, `Input`, `Select`, `CheckboxGroup`
- `AccountPage.tsx` — use `FormField`, `Input`, `Select`, `CheckboxGroup`
- `LoginPage.tsx` — use `FormField`, `Input`
- `AddMemberModal.tsx` — use `Input`
- `RecipesPage.tsx` — use `FormField`, `Input`, `Select` where applicable
- Other modals with inline inputs (GearModal, ItineraryModal, MealPlanModal) — use `Input`
- Remove now-unused input/field CSS from Modal.css, LoginPage.css, AccountPage.css, ProfileSetupModal.css

**Acceptance:** `npm run build` passes. All inputs render via shared components. Visual appearance unchanged.

---

### Ticket 6: Create `Modal` shared component (shell)

**Depends on:** Tickets 4, 5

**Goal:** One `<Modal>` component that handles overlay, content card, close button, and escape-to-close.

**What to create:**
- `webapp/src/components/ui/Modal.tsx` — props: `isOpen`, `onClose`, `title?: string`, `flavor?: string`, `size` (`sm` | `md` | `lg`), `closable?: boolean` (default true), `children`. Renders `.modal-overlay` + `.modal-content` + close X + escape handler. When `closable=false`, no X button and no backdrop-dismiss.
- Update `Modal.css` to be the CSS for this component (it already mostly is).

**What to update:**
- `ProfileSetupModal.tsx` — use `<Modal>`, remove manual overlay/close/escape
- `AddMemberModal.tsx` — use `<Modal>`
- `GearModal.tsx` — use `<Modal size="lg">`
- `AssignmentsModal.tsx` — use `<Modal>`
- `ItineraryModal.tsx` — use `<Modal>`
- `MealPlanModal.tsx` — use `<Modal size="lg">`
- `LogBookModal.tsx` — use `<Modal>`
- `ComingSoonModal.tsx` — use `<Modal>`

**Acceptance:** `npm run build` passes. All modals use `<Modal>`. Escape-to-close works consistently across all modals.

---

### Ticket 7: Unify profile editing — `AccountPage` uses shared `ProfileForm`

**Depends on:** Tickets 3, 6

**Goal:** Eliminate the duplicated profile form. AccountPage should reuse the same form as ProfileSetupModal.

**What to do:**
- Extract the profile form body (trail name, experience, dietary, avatar preview, randomize, submit) from `ProfileSetupModal.tsx` into a `ProfileForm.tsx` component. Props: `user`, `onSave`, `avatar`, `onRandomize`, `submitLabel`, etc.
- `ProfileSetupModal.tsx` — renders `<Modal>` wrapping `<ProfileForm>` (first-time setup or edit)
- `AccountPage.tsx` — renders `<ProfileForm>` directly in the page layout (no modal). Delete the 100+ lines of duplicated form code.
- `AccountPage.tsx` — use `<AppHeader>` instead of its custom header. This fixes the generic profile image bug — `AppHeader` already uses `<AvatarHead>` which renders the real avatar.

**Acceptance:** `npm run build` passes. Profile editing looks and works the same. The account page header uses `AppHeader` with the real avatar. PlanPage profile modal and AccountPage share the exact same form component.

---

### Ticket 8: Clean up dead CSS

**Depends on:** Ticket 7

**Goal:** Remove all CSS classes that are no longer referenced after the refactoring.

**What to do:**
- Audit every `.css` file for classes no longer used in any `.tsx` file
- Remove dead classes from: `LoginPage.css`, `AccountPage.css`, `ProfileSetupModal.css`, `Modal.css`, `AppHeader.css`, and all modal CSS files
- Ensure no component-specific CSS re-declares styles that are now in `ui/Button.css`, `ui/ui.css`, or `Modal.css`

**Acceptance:** `npm run build` passes. No unused CSS classes remain. `npx tsc --noEmit` passes.

---

## Dependency Graph & Parallelism

```
Ticket 1 (avatar constants)  ──┐
Ticket 2 (profile constants) ──┼──→ Ticket 3 (AvatarPreview) ──┐
                                │                                ├──→ Ticket 7 (unify profile) ──→ Ticket 8 (dead CSS)
Ticket 4 (Button)     ─────────┤                                │
Ticket 5 (Input/Form) ─────────┼──→ Ticket 6 (Modal shell) ────┘
```

- **Phase 1 (parallel):** Tickets 1, 2, 4, 5
- **Phase 2 (parallel):** Tickets 3, 6
- **Phase 3:** Ticket 7
- **Phase 4:** Ticket 8

---

## New File Structure After Refactor

```
webapp/src/
├── lib/
│   ├── avatarConstants.ts      # NEW — shared color maps
│   └── profileConstants.ts     # NEW — dietary/experience options
├── components/
│   ├── ui/
│   │   ├── Button.tsx          # NEW — shared button
│   │   ├── Button.css          # NEW
│   │   ├── Input.tsx           # NEW — shared input
│   │   ├── Select.tsx          # NEW — shared select
│   │   ├── CheckboxGroup.tsx   # NEW — shared checkbox grid
│   │   ├── FormField.tsx       # NEW — shared form field wrapper
│   │   ├── Modal.tsx           # NEW — shared modal shell
│   │   └── ui.css              # NEW — consolidated form styles
│   ├── AvatarPreview.tsx       # NEW — shared avatar body preview
│   ├── ProfileForm.tsx         # NEW — shared profile form
│   ├── AvatarHead.tsx          # UPDATED — imports from lib
│   ├── CamperAvatar.tsx        # UPDATED — imports from lib
│   ├── ProfileSetupModal.tsx   # UPDATED — uses Modal, ProfileForm
│   ├── AssignmentsModal.tsx    # UPDATED — imports from lib, uses Modal
│   ├── ...other modals         # UPDATED — use Modal shell
│   └── Modal.css               # UPDATED — reduced to Modal shell styles only
├── pages/
│   ├── AccountPage.tsx         # UPDATED — uses AppHeader, ProfileForm
│   ├── LoginPage.tsx           # UPDATED — uses Button, Input, FormField
│   └── ...
```
