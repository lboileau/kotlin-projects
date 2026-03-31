---
name: web-manager
description: Scaffold and manage React + TypeScript web applications in the monorepo. Reference skill for frontend patterns and conventions.
user-invocable: true
---

# Web Application Management

You are a web developer building React + TypeScript frontends in a monorepo. Follow these instructions precisely.

## Tech Stack

- **Framework:** React 19 + TypeScript
- **Build:** Vite
- **Routing:** react-router-dom
- **WebSocket:** @stomp/stompjs for STOMP-over-WebSocket live updates
- **Styling:** Plain CSS (no framework) with CSS custom properties
- **No UI frameworks** — all components are custom-built

---

## Project Structure

```
webapp/
├── src/
│   ├── main.tsx            # React entry point
│   ├── App.tsx             # Router + AuthProvider
│   ├── api/
│   │   └── client.ts       # Typed fetch wrapper (all API calls)
│   ├── hooks/              # Custom React hooks
│   ├── context/            # React context providers
│   ├── lib/                # Shared constants and pure helpers (no components)
│   ├── components/
│   │   ├── ui/             # Shared UI primitives (Button, Input, Modal, etc.)
│   │   └── *.tsx           # Feature components (modals, avatars, campsite items)
│   ├── pages/              # Route-level page components
│   └── styles/
│       ├── theme.css       # Design tokens (CSS variables)
│       └── animations.css  # All @keyframes
```

## File Placement Rules

| Type | Location | Example |
|------|----------|---------|
| Shared UI primitives | `components/ui/` | `Button.tsx`, `Input.tsx`, `Modal.tsx` |
| Feature components | `components/` | `ProfileForm.tsx`, `AvatarPreview.tsx` |
| Modal components | `components/` | `GearModal.tsx`, `AssignmentsModal.tsx` |
| Page components | `pages/` | `PlanPage.tsx`, `AccountPage.tsx` |
| Shared constants | `lib/` | `avatarConstants.ts`, `profileConstants.ts` |
| Custom hooks | `hooks/` | `usePlanUpdates.ts` |
| API client | `api/client.ts` | All typed fetch calls |
| Co-located CSS | Same directory as TSX | `Button.css` next to `Button.tsx` |

---

## Shared UI Components

All shared UI primitives live in `components/ui/`. These are the building blocks — use them everywhere instead of ad-hoc styling.

### Button (`components/ui/Button.tsx`)

```tsx
import { Button } from './ui/Button';

<Button>Primary</Button>
<Button variant="secondary">Cancel</Button>
<Button variant="danger">Delete</Button>
<Button variant="ghost" size="sm">Subtle action</Button>
<Button variant="icon">×</Button>
<Button size="lg" loading>Saving...</Button>
```

Props: `variant` (`primary` | `secondary` | `danger` | `ghost` | `icon`), `size` (`sm` | `md` | `lg`), `loading`, `disabled`, `className`, plus standard `ButtonHTMLAttributes`.

CSS classes: `btn`, `btn--{variant}`, `btn--{size}`, `btn--loading`. Styles in `Button.css`.

### Input (`components/ui/Input.tsx`)

```tsx
import { Input } from './ui/Input';

<Input placeholder="Enter text..." />
<Input error="Required field" />
<Input readOnly className="custom-readonly" />
```

Props: `error?: string`, plus standard `InputHTMLAttributes`. Uses `forwardRef` for ref forwarding.

CSS class: `ui-input`. Styles in `ui.css`.

### Select (`components/ui/Select.tsx`)

```tsx
import { Select } from './ui/Select';

<Select
  options={[{ value: 'a', label: 'Option A' }, { value: 'b', label: 'Option B' }]}
  placeholder="Choose one..."
/>
```

Props: `options: { value, label }[]`, `placeholder?: string`, plus standard `SelectHTMLAttributes` (minus `children`).

CSS class: `ui-select`. Styles in `ui.css`.

### FormField (`components/ui/FormField.tsx`)

```tsx
import { FormField } from './ui/FormField';

<FormField label="Email">
  <Input type="email" />
</FormField>
```

Props: `label: string`, `children`, `className?: string`.

CSS classes: `form-field`, `form-field__label`. Styles in `ui.css`.

### CheckboxGroup (`components/ui/CheckboxGroup.tsx`)

```tsx
import { CheckboxGroup } from './ui/CheckboxGroup';

<CheckboxGroup
  options={[{ value: 'a', label: 'Alpha' }, { value: 'b', label: 'Beta' }]}
  selected={selected}
  onChange={setSelected}
/>
```

Props: `options: { value, label }[]`, `selected: string[]`, `onChange: (selected: string[]) => void`.

CSS classes: `ui-checkbox-group`, `ui-checkbox-item`. Styles in `ui.css`.

### Modal (`components/ui/Modal.tsx`)

```tsx
import { Modal } from './ui/Modal';

<Modal isOpen={isOpen} onClose={onClose}>
  <p>Simple content</p>
</Modal>

<Modal isOpen={isOpen} onClose={onClose} title="Heading" flavor="Subtitle text" size="lg">
  <p>With header</p>
</Modal>

<Modal isOpen={isOpen} onClose={onClose} size="xl" className="custom-modal">
  <div className="custom-modal-body">Complex layout</div>
</Modal>

<Modal isOpen={isOpen} onClose={() => {}} closable={false}>
  <p>Cannot be dismissed</p>
</Modal>
```

Props: `isOpen`, `onClose`, `title?: string`, `flavor?: string`, `size` (`sm` | `md` | `lg` | `xl`), `closable` (default `true`), `className?: string`, `children`.

Size guide:
- `sm` (340px) — small dialogs
- `md` (420px) — default, simple modals (AddMember, ProfileSetup)
- `lg` (600px) — medium modals (Assignments, LogBook, Itinerary)
- `xl` (860px) — large modals (MealPlan, Gear)

Features: escape-to-close, backdrop click dismiss, close X button (all disabled when `closable=false`).

CSS classes: `modal-overlay`, `modal-content`, `modal-content--{size}`, `modal-close-btn`. Styles in `Modal.css`.

---

## Conventions

### Component Patterns

1. **Function components only.** No class components.
2. **Named exports.** `export function MyComponent()` — no default exports.
3. **Co-located CSS.** Each component with custom styles has a `.css` file next to it (e.g., `GearModal.tsx` + `GearModal.css`).
4. **CSS imports at the top.** Import co-located CSS in the component file: `import './GearModal.css';`.
5. **Use shared UI components.** Never create ad-hoc buttons, inputs, selects, or modals. Always use `Button`, `Input`, `Select`, `FormField`, `CheckboxGroup`, `Modal` from `components/ui/`.
6. **SVG inline.** All illustrations are inline SVG — no external image files.
7. **No animation libraries.** CSS-only animations via keyframes in `animations.css`.

### CSS Patterns

1. **CSS custom properties** for all design tokens — colors, spacing, fonts, radii. Defined in `theme.css`.
2. **BEM-like naming** for component CSS: `.component-name`, `.component-name__element`, `.component-name--modifier`.
3. **No CSS frameworks.** Pure CSS only.
4. **Shared UI styles** in `components/ui/ui.css` (inputs, selects, checkboxes, form fields) and `components/ui/Button.css`.
5. **Modal base styles** in `components/Modal.css`. Custom modal layout overrides go in the modal's own CSS file.
6. **Custom modal CSS** should only contain layout/overflow/height — never re-declare background, border, box-shadow, border-radius, or animation (those come from `.modal-content`). Use `padding: 0` only when the modal has internal sections (header/body/footer) that manage their own padding with section borders.

### Shared Constants

Constants shared across components go in `lib/`:
- `lib/avatarConstants.ts` — color maps (`SKIN_COLORS`, `HAIR_COLORS`, `SHIRT_COLORS`, `PANTS_COLORS`, `FALLBACK_COLORS`)
- `lib/profileConstants.ts` — option arrays (`DIETARY_OPTIONS`, `EXPERIENCE_OPTIONS`)

Never duplicate constants in component files. Import from `lib/`.

### State & Data Flow

1. **Auth context** (`context/AuthContext.tsx`) — stores user in state + localStorage. `useAuth()` hook: `{ user, login, logout, isAuthenticated }`.
2. **API client** (`api/client.ts`) — typed `request<T>()` helper, auto-injects `X-User-Id`. All methods return typed promises.
3. **Live updates** (`hooks/usePlanUpdates.ts`) — STOMP WebSocket for real-time plan changes. Components use `refreshKey` pattern to trigger refetches.
4. **Local form state** — forms use `useState` for controlled inputs. Dirty checking compares current state against initial prop values.

### Modal Pattern

When creating a new modal:

```tsx
import { useState } from 'react';
import { Modal } from './ui/Modal';
import { Button } from './ui/Button';
import { Input } from './ui/Input';
import { FormField } from './ui/FormField';

interface MyModalProps {
  isOpen: boolean;
  onClose: () => void;
  // ... feature-specific props
}

export function MyModal({ isOpen, onClose, ...props }: MyModalProps) {
  // state, handlers...

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="md">
      {/* Use FormField, Input, Select, Button for forms */}
      <FormField label="Name">
        <Input value={name} onChange={e => setName(e.target.value)} />
      </FormField>
      <Button onClick={handleSubmit}>Save</Button>
    </Modal>
  );
}
```

### Page Pattern

Pages use `AppHeader` for navigation, `ParallaxBackground` for visuals:

```tsx
import { AppHeader } from '../components/AppHeader';
import { ParallaxBackground } from '../components/ParallaxBackground';

export function MyPage() {
  return (
    <div className="my-page">
      <ParallaxBackground variant="dusk" />
      <div className="my-page-content">
        <AppHeader pageTitle="My Page" />
        {/* page content */}
      </div>
    </div>
  );
}
```

---

## Build & Verify

```bash
# Dev server (requires API on :8080)
cd webapp && npm run dev

# Type check
cd webapp && npx tsc --noEmit

# Production build
cd webapp && npm run build
```

Always run `npx tsc --noEmit` after changes to verify types. Run `npm run build` for final verification.
