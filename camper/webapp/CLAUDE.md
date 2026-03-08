# Camper Webapp

Interactive camping trip planner frontend. Aesthetic: **"Enchanted Expedition Journal"** — watercolor storybook meets cozy RPG. Parallax wilderness backgrounds, animated campfire campsite scene, SVG illustrated art, soft pastel palette.

## Tech Stack

- **Framework:** React 19 + TypeScript
- **Build:** Vite 7
- **Routing:** react-router-dom
- **Styling:** Plain CSS (no framework) with CSS custom properties
- **Fonts:** Cinzel Decorative (display), Fredericka the Great (headings), Lora (body) — via Google Fonts

## Project Structure

```
webapp/
├── CLAUDE.md
├── index.html              # Entry HTML with Google Fonts
├── vite.config.ts          # Dev server (port 3000), API proxy to :8080
├── src/
│   ├── main.tsx            # React entry point
│   ├── App.tsx             # Router + AuthProvider
│   ├── api/
│   │   └── client.ts       # API client (typed fetch wrapper)
│   ├── context/
│   │   └── AuthContext.tsx  # Auth state (localStorage-persisted)
│   ├── components/
│   │   ├── ParallaxBackground.tsx/css  # Layered parallax with mouse tracking
│   │   ├── Campfire.tsx/css            # Animated CSS campfire (flames, embers, smoke, logs, stones)
│   │   ├── CamperAvatar.tsx/css        # SVG illustrated person seated around fire
│   │   ├── CampsiteItems.tsx           # SVG art: TentSVG, EquipmentPileSVG, KitchenSVG, MapTableSVG
│   │   ├── InteractableItem.tsx/css    # Hoverable/clickable campsite object with glow + tooltip
│   │   ├── GearModal.tsx/css            # Equipment & gear management modal (checklist per owner)
│   │   ├── ComingSoonModal.tsx         # Themed "not ready" modal with flavor text
│   │   ├── AddMemberModal.tsx          # Form modal to invite member by email
│   │   ├── Modal.css                   # Shared modal styles (parchment aesthetic)
│   │   └── ProtectedRoute.tsx          # Auth guard (redirect to /login)
│   ├── pages/
│   │   ├── LoginPage.tsx/css           # Login/register with night-sky parallax
│   │   ├── HomePage.tsx/css            # Trip list with dusk parallax, flag trail markers
│   │   └── PlanPage.tsx/css            # THE CENTERPIECE — campsite scene
│   └── styles/
│       ├── theme.css                   # Design tokens (colors, typography, spacing, shadows)
│       └── animations.css              # All @keyframes (fire, float, twinkle, fade, etc.)
```

## Architecture

### API Layer (`api/client.ts`)
- Typed interfaces: `User`, `Plan`, `PlanMember`, `Item`
- `request<T>()` helper auto-injects `X-User-Id` from localStorage
- All methods return typed promises; throws on non-OK responses

### Auth (`context/AuthContext.tsx`)
- `AuthProvider` wraps app — stores user in state + localStorage
- `useAuth()` hook: `{ user, login, logout, isAuthenticated }`
- `ProtectedRoute` redirects unauthenticated users to `/login`

### Pages
- **LoginPage** — Night sky parallax. Toggle login/register. Calls `api.login()` or `api.register()`.
- **HomePage** — Dusk parallax. Lists trips as flag trail-marker cards. Create new trip inline. Owners see delete on hover; guest members see leave on hover; non-members of public plans see a "Join" action instead of the arrow (joins then navigates to plan).
- **PlanPage** — Night campsite parallax. Central campfire with members around it. Four interactable background items (tent, equipment, kitchen, map table). Equipment opens GearModal; tent/kitchen/itinerary show ComingSoonModal. Owner sees "Manage Plan" button in header (toggle public/private visibility). Non-members of public plans see a "Join Camp" avatar below the fire; members see the invite "+" ghost. Members can remove themselves; owner can remove others.
  - **GearModal** — Large modal with two sections: "Shared Camp Gear" (plan-level items, editable by plan owner only) and "Personal Packs" (per-member item lists, each editable only by the owning user). Supports inline add/edit/delete, category grouping (camp, canoe, kitchen, personal, food, misc), quantity, and packed status with progress bars.

### Visual Design System
- **Palette:** Defined in `theme.css` as CSS variables (`--lavender`, `--sage`, `--tan`, `--rose`, `--mint`, `--ember`, `--flame`, `--night-sky`, `--parchment`, etc.)
- **Parallax:** Three variants (`night`, `dusk`, `campsite`) with mouse-tracked layer offsets via CSS custom properties
- **Campfire:** Multi-layered CSS (outer/mid/inner/core flames) + ember particles + smoke + log/stone ring
- **Campsite Items:** Pure SVG components for tent, equipment pile, kitchen, map table
- **Avatars:** SVG-illustrated people with randomized pastel colors, positioned in a circle around campfire using trigonometry
- **Modals:** Parchment-textured with category-specific flavor text

## API Endpoints Used

All calls go through Vite proxy (`/api` → `localhost:8080`).

| Method | Endpoint | Auth | Used By |
|--------|----------|------|---------|
| POST | `/api/auth` | No | LoginPage (sign in) |
| POST | `/api/users` | No | LoginPage (register) |
| GET | `/api/plans` | X-User-Id | HomePage |
| POST | `/api/plans` | X-User-Id | HomePage (create trip) |
| GET | `/api/plans/:id/members` | X-User-Id | PlanPage |
| PUT | `/api/plans/:id` | X-User-Id | PlanPage (update visibility) |
| POST | `/api/plans/:id/members` | X-User-Id | PlanPage (invite), HomePage (join) |
| DELETE | `/api/plans/:id/members/:memberId` | X-User-Id | PlanPage (leave/remove), HomePage (leave) |
| GET | `/api/items?ownerType=&ownerId=` | X-User-Id | GearModal (list items) |
| POST | `/api/items` | X-User-Id | GearModal (create item) |
| PUT | `/api/items/:id` | X-User-Id | GearModal (update item) |
| DELETE | `/api/items/:id` | X-User-Id | GearModal (delete item) |

## Running

```bash
# Dev (requires API running on :8080)
npm run dev        # → http://localhost:3000

# Build
npm run build      # → dist/

# Type check
npx tsc --noEmit
```

## Conventions

- **No UI framework** — all styling is custom CSS with CSS variables
- **SVG art** — all illustrations are inline SVG, no external image files
- **Animations** — CSS-only (keyframes in animations.css), no JS animation libraries
- **Component pattern:** Each visual component has co-located `.tsx` + `.css` files
- **Modals** share `Modal.css` for base parchment styling
- **Parallax** uses CSS `calc()` with `--mouse-x`/`--mouse-y` custom properties set via JS mousemove listener
