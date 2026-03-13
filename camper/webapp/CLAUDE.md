# Camper Webapp

Interactive camping trip planner frontend. Aesthetic: **"Enchanted Expedition Journal"** — watercolor storybook meets cozy RPG. Parallax wilderness backgrounds, animated campfire campsite scene, SVG illustrated art, soft pastel palette.

## Tech Stack

- **Framework:** React 19 + TypeScript
- **Build:** Vite 7
- **Routing:** react-router-dom
- **WebSocket:** @stomp/stompjs for STOMP-over-WebSocket live updates
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
│   ├── hooks/
│   │   └── usePlanUpdates.ts # STOMP WebSocket hook for live plan updates
│   ├── context/
│   │   └── AuthContext.tsx  # Auth state (localStorage-persisted)
│   ├── components/
│   │   ├── ParallaxBackground.tsx/css  # Layered parallax with mouse tracking
│   │   ├── Campfire.tsx/css            # Animated CSS campfire (flames, embers, smoke, logs, stones)
│   │   ├── CamperAvatar.tsx/css        # SVG illustrated person seated around fire
│   │   ├── CampsiteItems.tsx           # SVG art: TentSVG, EquipmentPileSVG, KitchenSVG, MapTableSVG
│   │   ├── InteractableItem.tsx/css    # Hoverable/clickable campsite object with glow + tooltip
│   │   ├── GearModal.tsx/css            # Equipment & gear management modal (checklist per owner)
│   │   ├── MealPlanModal.tsx/css       # Meal plan modal — overview, recipe book, shopping list
│   │   ├── AssignmentsModal.tsx/css    # Tent & canoe group assignments modal
│   │   ├── ComingSoonModal.tsx         # Themed "not ready" modal with flavor text
│   │   ├── AddMemberModal.tsx          # Form modal to invite member by email
│   │   ├── Modal.css                   # Shared modal styles (parchment aesthetic)
│   │   └── ProtectedRoute.tsx          # Auth guard (redirect to /login)
│   ├── pages/
│   │   ├── LoginPage.tsx/css           # Login/register with night-sky parallax
│   │   ├── HomePage.tsx/css            # Trip list with dusk parallax, flag trail markers
│   │   ├── PlanPage.tsx/css            # THE CENTERPIECE — campsite scene
│   │   └── RecipesPage.tsx/css         # Recipe book — list, detail, create, import, review
│   └── styles/
│       ├── theme.css                   # Design tokens (colors, typography, spacing, shadows)
│       └── animations.css              # All @keyframes (fire, float, twinkle, fade, etc.)
```

## Architecture

### API Layer (`api/client.ts`)
- Typed interfaces: `User`, `Plan`, `PlanMember`, `Item`, `Assignment`, `AssignmentDetail`, `AssignmentMember`, `IngredientResponse`, `RecipeResponse`, `RecipeDetailResponse`, `RecipeIngredientResponse`, `MealPlanResponse`, `MealPlanDetailResponse`, `MealPlanDayResponse`, `MealsByTypeResponse`, `MealPlanRecipeDetailResponse`, `ShoppingListResponse`, `ShoppingListCategoryResponse`, `ShoppingListItemResponse`
- `request<T>()` helper auto-injects `X-User-Id` from localStorage
- All methods return typed promises; throws on non-OK responses

### Live Updates (`hooks/usePlanUpdates.ts`)
- `usePlanUpdates(planId, onUpdate)` — connects to `/ws` via STOMP, subscribes to `/topic/plans/{planId}`
- Calls `onUpdate({ resource, action })` when the server publishes a change notification
- Reconnects automatically on disconnect (5s delay)
- PlanPage routes updates by resource type: `plan`/`members` → refetch plan & members immediately; `assignments` → increment `assignmentsRefreshKey`; `itinerary` → increment `itineraryRefreshKey`
- AssignmentsModal and ItineraryModal accept a `refreshKey` prop — when it increments while the modal is open, they refetch their data
- Items are not live-updated (modals refetch on open)

### Auth (`context/AuthContext.tsx`)
- `AuthProvider` wraps app — stores user in state + localStorage
- `useAuth()` hook: `{ user, login, logout, isAuthenticated }`
- `ProtectedRoute` redirects unauthenticated users to `/login`

### Pages
- **LoginPage** — Night sky parallax. Toggle login/register. Calls `api.login()` or `api.register()`. If sign-in fails because user has no username, auto-switches to Register tab with error message.
- **HomePage** — Dusk parallax. Lists trips as flag trail-marker cards. Create new trip inline. Owners see delete on hover; guest members see leave on hover; non-members of public plans see a "Join" action instead of the arrow (joins then navigates to plan).
- **PlanPage** — Night campsite parallax. Central campfire with members around it. Four interactable background items (tent, equipment, kitchen, map table). Equipment opens GearModal; kitchen opens MealPlanModal; tent opens AssignmentsModal; map table shows ComingSoonModal. Owner sees "Manage Plan" button in header (edit plan name + toggle public/private visibility). Non-members of public plans see a "Join Camp" avatar below the fire; members see the invite "+" ghost. Members can remove themselves; owner can remove others. Pending (invited but not registered) members show their email address. Campfire circle radius scales dynamically with member count to prevent avatar overlap.
  - **GearModal** — Large modal with two sections: "Shared Camp Gear" (plan-level items, editable by plan owner only) and "Personal Packs" (per-member item lists scoped to the current plan, each editable only by the owning user). Supports inline add/edit/delete, category grouping (camp, canoe, kitchen, personal, food, misc), quantity, and packed status with progress bars. Pending adventurers (no username) are filtered from personal pack lists.
  - **MealPlanModal** — Fixed-height (88vh) three-view modal opened from the kitchen campsite item. Three tab views:
    - **Overview:** Editable meal plan name (blur-to-save), servings-per-recipe stepper, save-as-template / load-from-template links. Day tabs (add/remove days). Four meal type sections (Breakfast, Lunch, Dinner, Snacks) each with inline recipe search-and-add and remove buttons. Empty state shows a create form with name input, servings stepper, and optional template loader with preview.
    - **Recipe Book:** Open-book layout (left page: search + filter pills + scrollable recipe list; spine; right page: selected recipe detail with ingredients). "Add to Meal Plan" button pinned at bottom with day + meal type popover (pre-selects active day). Left and right pages scroll independently.
    - **Shopping List:** Progress bar (X of Y purchased). Items grouped by ingredient category. Each row: checkbox toggle, ingredient name, quantity display, status badge (done/more needed/needed/removed). Merges multi-unit entries per ingredient. Reset purchases with confirmation.
    - **Templates:** Save current meal plan as template. Load from template with day-by-day preview (grouped by meal type with icons). Loading a template replaces all days/recipes but preserves the existing meal plan name.
  - **AssignmentsModal** — Fixed-height modal with two tabs (Tents / Canoes). Cards show assignment name, owner, occupancy bar, and member list with mini SVG avatar heads. Features: "Add Tent" / "Add Canoe" buttons (creator auto-added if not already in a group of that type); join (auto-leaves current group of same type); leave (including owner self-leave); owner/plan-owner "Add Member" panel showing available plan members with greyed-out entries for those already in another group of the same type; inline edit name/max occupancy; delete. Pending adventurers are filtered from the add-member list.
- **RecipesPage** — Standalone page at `/recipes` with dusk parallax background. Multi-view single-page flow: `list`, `detail`, `create`, `edit`, `import`.
  - **List view:** Searchable recipe cards (filter by name). Shows published and own draft recipes. "New Recipe" and "Import Recipe" buttons. Each card shows name, status badge (draft/published), base servings, and description snippet. Click to view detail.
  - **Create view:** Form with name, description, optional web link, base servings, and ingredient picker. Ingredient picker is a search-as-you-type dropdown over the global ingredients list; each selected ingredient gets a quantity + unit row. Submits to `POST /api/recipes`.
  - **Import view:** Single URL input. Submits to `POST /api/recipes/import` which scrapes the page and creates a draft recipe via Claude API. Redirects to detail view of the created draft.
  - **Detail view:** Shows full recipe info. For draft recipes, the creator sees a review panel for any `pending_review` ingredients. Each pending ingredient shows `originalText`, suggested match info, and three resolve actions: confirm match, select existing ingredient (search dropdown), or create new ingredient (name + category + unit form). After all ingredients are resolved and no duplicate flag, a "Publish" button appears. Duplicate recipes show a banner with "Not a duplicate" / "Use existing" resolve options.
  - **Edit view:** Form to update name, description, base servings. Submits to `PUT /api/recipes/{id}`.
  - Loads all ingredients on mount alongside recipe list (`GET /api/ingredients`) so the ingredient picker is immediately available.

### Visual Design System
- **Palette:** Defined in `theme.css` as CSS variables (`--lavender`, `--sage`, `--tan`, `--rose`, `--mint`, `--ember`, `--flame`, `--night-sky`, `--parchment`, etc.)
- **Parallax:** Three variants (`night`, `dusk`, `campsite`) with mouse-tracked layer offsets via CSS custom properties
- **Campfire:** Multi-layered CSS (outer/mid/inner/core flames) + ember particles + smoke + log/stone ring
- **Campsite Items:** Pure SVG components for tent, equipment pile, kitchen, map table
- **Avatars:** SVG-illustrated people with randomized pastel colors, positioned in a semicircle around campfire using trigonometry. Circle radius scales dynamically (15% per member beyond 4) to prevent overlap
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
| PUT | `/api/plans/:id` | X-User-Id | PlanPage (update name/visibility) |
| POST | `/api/plans/:id/members` | X-User-Id | PlanPage (invite), HomePage (join) |
| DELETE | `/api/plans/:id/members/:memberId` | X-User-Id | PlanPage (leave/remove), HomePage (leave) |
| GET | `/api/items?ownerType=&ownerId=&planId=` | X-User-Id | GearModal (list items; planId scopes personal items) |
| POST | `/api/items` | X-User-Id | GearModal (create item) |
| PUT | `/api/items/:id` | X-User-Id | GearModal (update item) |
| DELETE | `/api/items/:id` | X-User-Id | GearModal (delete item) |
| GET | `/api/plans/:id/assignments` | X-User-Id | AssignmentsModal (list) |
| GET | `/api/plans/:id/assignments/:assignmentId` | X-User-Id | AssignmentsModal (detail) |
| POST | `/api/plans/:id/assignments` | X-User-Id | AssignmentsModal (create) |
| PUT | `/api/plans/:id/assignments/:assignmentId` | X-User-Id | AssignmentsModal (update) |
| DELETE | `/api/plans/:id/assignments/:assignmentId` | X-User-Id | AssignmentsModal (delete) |
| POST | `/api/plans/:id/assignments/:assignmentId/members` | X-User-Id | AssignmentsModal (add member) |
| DELETE | `/api/plans/:id/assignments/:assignmentId/members/:userId` | X-User-Id | AssignmentsModal (remove member) |
| PUT | `/api/plans/:id/assignments/:assignmentId/owner` | X-User-Id | AssignmentsModal (transfer ownership) |
| GET | `/api/ingredients` | X-User-Id | RecipesPage (ingredient picker) |
| GET | `/api/recipes` | X-User-Id | RecipesPage (list) |
| POST | `/api/recipes` | X-User-Id | RecipesPage (create) |
| POST | `/api/recipes/import` | X-User-Id | RecipesPage (import from URL) |
| GET | `/api/recipes/:id` | X-User-Id | RecipesPage (detail) |
| PUT | `/api/recipes/:id` | X-User-Id | RecipesPage (edit) |
| DELETE | `/api/recipes/:id` | X-User-Id | RecipesPage (delete) |
| PUT | `/api/recipes/:id/ingredients/:ingredientId` | X-User-Id | RecipesPage (resolve pending ingredient) |
| PUT | `/api/recipes/:id/resolve-duplicate` | X-User-Id | RecipesPage (resolve duplicate flag) |
| POST | `/api/recipes/:id/publish` | X-User-Id | RecipesPage (publish draft) |
| GET | `/api/meal-plans?planId=` | X-User-Id | MealPlanModal (get meal plan for trip) |
| GET | `/api/meal-plans/:id` | X-User-Id | MealPlanModal (detail / template preview) |
| POST | `/api/meal-plans` | X-User-Id | MealPlanModal (create) |
| PUT | `/api/meal-plans/:id` | X-User-Id | MealPlanModal (update name/servings) |
| DELETE | `/api/meal-plans/:id` | X-User-Id | MealPlanModal (delete) |
| POST | `/api/meal-plans/:id/days` | X-User-Id | MealPlanModal (add day) |
| DELETE | `/api/meal-plans/:id/days/:dayId` | X-User-Id | MealPlanModal (remove day) |
| POST | `/api/meal-plans/:id/days/:dayId/recipes` | X-User-Id | MealPlanModal (add recipe to meal) |
| DELETE | `/api/meal-plan-recipes/:id` | X-User-Id | MealPlanModal (remove recipe from meal) |
| GET | `/api/meal-plans/:id/shopping-list` | X-User-Id | MealPlanModal (shopping list) |
| PATCH | `/api/meal-plans/:id/shopping-list` | X-User-Id | MealPlanModal (update purchase) |
| DELETE | `/api/meal-plans/:id/shopping-list` | X-User-Id | MealPlanModal (reset purchases) |
| GET | `/api/meal-plans/templates` | X-User-Id | MealPlanModal (list templates) |
| POST | `/api/meal-plans/:id/save-as-template` | X-User-Id | MealPlanModal (save as template) |
| POST | `/api/meal-plans/:id/copy-to-trip` | X-User-Id | MealPlanModal (load template) |

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
