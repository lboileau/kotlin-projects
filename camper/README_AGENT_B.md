<div align="center">

```
                        ✦  .    ·  ✦
               ✦  ·         .        ·  ✦
          .        ·    ★    .    ·        .
       ·      ✦                      ✦      ·
     .    ·      🌲    ⛺    🌲      ·    .
    ·   .      🌲  🌲 /  \ 🌲  🌲      .   ·
   .  ·      🌲🌲🌲  /    \ 🌲🌲🌲      ·  .
  · .      🌲🌲🌲🌲 /______\ 🌲🌲🌲🌲      . ·
  .       ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~       .
  ·           🔥  👤  👤  👤  👤  🔥           ·
  .          ~~~~~~~~🪵🪵🪵~~~~~~~~          .
  ·      ·    .    ·    .    ·    .    ·      ·
```

# 🏕️ Camper

### Plan your adventure — together, in real time

*An interactive camping trip planner where your group gathers around a virtual campfire.*
*Click on campsite objects to plan gear, meals, tent assignments, and more.*

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-7-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vite.dev/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=flat-square&logo=railway&logoColor=white)](https://railway.com/)

</div>

---

## Table of Contents

- [What is Camper?](#what-is-camper)
- [How It Works](#how-it-works)
- [The Campsite Experience](#the-campsite-experience)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Real-Time Updates](#real-time-updates)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Deployment](#deployment)
- [Design Patterns](#design-patterns)
- [Tech Stack](#tech-stack)

---

## What is Camper?

**Camper** turns the chaos of group trip planning into an enchanting campsite experience. Create a trip, invite your friends by email, and collaborate in real time — everyone sees changes as they happen.

### Features

<table>
<tr>
<td width="50%">

**🔥 Interactive Campsite**
Hand-drawn SVG scene with animated campfire, smoke, embers, and mouse-tracked parallax backgrounds.

**🎒 Gear Checklists**
Two layers: **Shared Camp Gear** (communal, managed by owner) and **Personal Packs** (per-member, scoped to trip). Track quantities and packed/unpacked status with progress bars.

**🍳 Meal Planning**
Day-by-day meal planner with categories: breakfast, lunch, dinner, and snacks. Add days as needed — everyone contributes.

**📧 Email Invitations**
Invite by email via [Resend](https://resend.com). Full delivery lifecycle: pending → sent → delivered (or bounced/failed/complained).

**🌙 Day/Night Toggle**
Switch the campsite between day and night — avatars change expressions, the sky shifts, and stars appear.

</td>
<td width="50%">

**👥 Live Collaboration**
STOMP WebSocket pushes every change instantly. When someone adds gear or joins a tent, all browsers update in real time.

**⛺ Tent Assignments**
Create named tent groups with capacity limits. One tent per person per trip. Transfer ownership, manage members.

**🚣 Canoe Pairings**
Same system for canoes — assign paddling partners with occupancy controls and type-uniqueness constraints.

**🗓️ Itinerary**
Timeline of events with dates, descriptions, and details. Auto-creates on first event.

**🔒 Public & Private Trips**
Public plans let anyone join from the home page. Private plans are invitation-only. Owner controls visibility.

</td>
</tr>
</table>

---

## How It Works

```
  ┌──────────────┐       ┌──────────────┐       ┌──────────────┐
  │   CREATE A   │──────►│    INVITE    │──────►│    PLAN      │
  │     TRIP     │       │   FRIENDS    │       │  TOGETHER    │
  └──────────────┘       └──────────────┘       └──────┬───────┘
                                                       │
              ┌────────────────┬───────────────┬───────┘
              ▼                ▼               ▼
        ┌──────────┐    ┌──────────┐    ┌──────────┐
        │🎒 GEAR   │    │🍳 MEALS  │    │⛺ TENTS  │
        │Checklists│    │Day-by-day│    │& CANOES  │
        └──────────┘    └──────────┘    └──────────┘
```

1. **Create a trip** — name it, choose public or private visibility
2. **Invite your crew** — add members by email; they receive a branded invitation with a join link
3. **Gather at the campfire** — everyone appears as an illustrated SVG avatar seated around the fire
4. **Click campsite objects** to open collaborative planners:
   - **🎒 Equipment pile** → Gear checklists (shared camp gear + personal packs)
   - **🍳 Kitchen** → Meal planner (day-by-day, all categories)
   - **⛺ Tent** → Tent & canoe group assignments with capacity management
   - **🗺️ Map table** → Itinerary timeline
5. **See changes live** — WebSocket pushes updates to all connected browsers instantly

---

## The Campsite Experience

The heart of Camper is the **plan page** — an illustrated campsite where your group comes to life:

```
                        🌙  ✦  ·  ★  ·  ✦
                    ·  ✦  ·  .  ·  .  ·  ✦  ·
        ┌──────────────────────────────────────────┐
        │                                          │
        │     🌲🌲       ⛺         🌲🌲          │
        │    🌲🌲🌲    ┌────┐     🌲🌲🌲         │
        │              │TENT│     🍳               │
        │              └────┘   ┌─────┐  🎒       │
        │   ┌─────┐             │MEALS│ ┌────┐    │
        │   │ MAP │    👤 👤    └─────┘ │GEAR│    │
        │   │TABLE│   👤 🔥 👤          └────┘    │
        │   └─────┘    👤 👤                       │
        │          ~~~~~~~~~~~                     │
        └──────────────────────────────────────────┘
              Click any object to open its planner
```

**What you see:**
- **Animated campfire** — multi-layered CSS flames with rising embers, drifting smoke, and a glowing stone ring
- **Member avatars** — SVG-illustrated characters arranged in a dynamic semicircle (arc radius scales with group size)
- **Four clickable objects** — tent (assignments), kitchen (meals), equipment pile (gear), map table (itinerary)
- **Parallax forest** — three depth layers that respond to mouse movement, complete with hidden glowing eyes in the dark
- **Day/night toggle** — click the sun/moon to switch atmospheres

**Design language:**
- Fonts: *Cinzel Decorative* (display) · *Fredericka the Great* (headings) · *Lora* (body)
- Palette: warm pastels (`lavender`, `sage`, `tan`, `rose`, `mint`) against `charcoal` and `night-sky`
- Zero external UI frameworks — 100% custom CSS with CSS custom properties

---

## Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                        WEBAPP (React 19)                         │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌───────────┐  ┌──────────┐       │
│  │  Login   │  │   Home   │  │ Plan Page │  │ Account  │       │
│  │  Page    │  │  (Trips) │  │(Campsite) │  │  Page    │       │
│  └──────────┘  └──────────┘  └─────┬─────┘  └──────────┘       │
│                                     │                            │
│       ┌─────────────────────────────┼─────────────────────┐     │
│       │  Gear Modal │ Meal Modal │ Assignments │ Itinerary│     │
│       └─────────────────────────────┼─────────────────────┘     │
│                                     │                            │
│            REST (fetch)             │       STOMP/WebSocket      │
└─────────────────────────────────────┼────────────────────────────┘
                                      │
         ┌────────── /api/* ──────────┼──── /ws ──────────┐
         │                            │                    │
┌────────┼────────────────────────────┼────────────────────┼───────┐
│        ▼            CAMPER SERVICE  ▼  (Spring Boot)     ▼       │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      Controllers                           │  │
│  │  UserCtrl · PlanCtrl · ItemCtrl · ItineraryCtrl            │  │
│  │  AssignmentCtrl · GearSyncCtrl · WebhookCtrl               │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                              │                                    │
│  ┌──────────────────────────┼─────────────────────────────────┐  │
│  │              Service Layer (Action classes)                 │  │
│  │                                                             │  │
│  │    Validate ──→ Convert ──→ Execute ──→ Publish Event       │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                              │                                    │
│  ┌──────────────────────────┼──────────────────┬──────────────┐  │
│  │          JDBI Clients    │                  │ Email Client  │  │
│  │                          │                  │  (Resend SDK) │  │
│  │  UserClient · PlanClient │ ItemClient       │  or NoOp      │  │
│  │  ItineraryClient · AssignmentClient         │               │  │
│  │  InvitationClient                           │               │  │
│  └──────────────────────────┬──────────────────┴──────────────┘  │
│                              │                                    │
└──────────────────────────────┼────────────────────────────────────┘
                               │
                    ┌──────────┴──────────┐
                    │    PostgreSQL 16     │
                    │  Flyway migrations  │
                    │    UUID PKs         │
                    └─────────────────────┘
```

### Dependency Rule

```
services ──→ clients ──→ libs       (never reversed)
   │             │           │
   │  Spring     │  JDBI     │  Pure Kotlin
   │  Boot DI    │  + I/O    │  No I/O
```

---

## Project Structure

```
camper/
│
├── webapp/                           # ── FRONTEND ──────────────────
│   ├── src/
│   │   ├── pages/                    # LoginPage, HomePage, PlanPage, AccountPage
│   │   ├── components/               # Campfire, CamperAvatar, ParallaxBackground,
│   │   │                             # GearModal, MealModal, AssignmentsModal,
│   │   │                             # ItineraryModal, AddMemberModal, CampsiteItems
│   │   ├── api/client.ts             # Typed HTTP client with ~40 API methods
│   │   ├── context/AuthContext.tsx    # Auth state (localStorage + React context)
│   │   ├── hooks/usePlanUpdates.ts   # STOMP WebSocket subscription hook
│   │   └── styles/                   # theme.css (tokens) + animations.css (keyframes)
│   ├── vite.config.ts                # Dev server on :3000, proxy to :8080
│   └── package.json                  # React 19, STOMP.js, Vite 7
│
├── services/                         # ── BACKEND ───────────────────
│   ├── common/                       # Shared ApiResponse type
│   └── camper-service/               # Spring Boot 3.4.3 REST API
│       └── src/main/kotlin/.../features/
│           ├── user/                  # controller/ · actions/ · service/
│           ├── plan/                  # controller/ · actions/ · service/
│           ├── item/                  # controller/ · actions/ · service/
│           ├── itinerary/             # controller/ · actions/ · service/
│           ├── assignment/            # controller/ · actions/ · service/
│           ├── gearsync/              # External gear sync endpoint
│           └── webhook/               # Resend delivery callback
│
├── clients/                          # ── DATA ACCESS ───────────────
│   ├── common/                       # Result<T,E>, error types, ClientContext
│   ├── user-client/                  # JDBI → users table
│   ├── plan-client/                  # JDBI → plans + plan_members
│   ├── item-client/                  # JDBI → items (polymorphic ownership)
│   ├── itinerary-client/             # JDBI → itineraries + events
│   ├── assignment-client/            # JDBI → assignments + members
│   ├── invitation-client/            # JDBI → invitations lifecycle
│   └── email-client/                 # Resend SDK (+ NoOp for local dev)
│
├── libs/common/                      # ── SHARED LOGIC ──────────────
│                                     # Pure utilities, no I/O
│
├── databases/camper-db/              # ── DATABASE ──────────────────
│   ├── migrations/                   # V001–V012 Flyway SQL migrations
│   ├── seed/dev_seed.sql             # Development seed data
│   └── docker-compose.yml            # PostgreSQL 16 on port 5433
│
└── Dockerfile                        # 3-stage: Node → JDK → JRE
```

---

## Database Schema

12 Flyway migrations produce the following schema:

```
                    ┌─────────────┐
                    │    users    │
                    │─────────────│
                    │ id (PK)     │
                    │ email (UQ)  │
                    │ username    │
                    └──────┬──────┘
                           │
            ┌──────────────┼──────────────────────┐
            │              │                      │
            ▼              ▼                      ▼
     ┌─────────────┐ ┌──────────┐         ┌────────────┐
     │plan_members │ │  plans   │         │invitations │
     │ (M:N join)  │ │──────────│         │────────────│
     │─────────────│ │ id (PK)  │────────>│ plan_id    │
     │ plan_id (PK)│ │ name     │         │ user_id    │
     │ user_id (PK)│ │visibility│         │ inviter_id │
     └─────────────┘ │ owner_id │         │ email      │
                      └────┬─────┘         │ status     │
                           │               └────────────┘
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
   ┌─────────────┐ ┌─────────────┐  ┌──────────────┐
   │    items    │ │ itineraries │  │ assignments  │
   │─────────────│ │ (1:1/plan)  │  │──────────────│
   │ id (PK)     │ │─────────────│  │ id (PK)      │
   │ plan_id     │ │ id (PK)     │  │ plan_id      │
   │ user_id     │ │ plan_id(UQ) │  │ name         │
   │ name        │ └──────┬──────┘  │ type         │
   │ category    │        │         │ max_occupancy│
   │ quantity    │        ▼         │ owner_id     │
   │ packed      │ ┌─────────────┐  └──────┬───────┘
   └─────────────┘ │ itinerary_  │         │
                   │   events    │         ▼
                   │─────────────│  ┌──────────────┐
                   │ title       │  │ assignment_  │
                   │ description │  │   members    │
                   │ details     │  │──────────────│
                   │ event_at    │  │assignment_id │
                   └─────────────┘  │ user_id      │
                                    │ plan_id      │
                                    │ type         │
                                    └──────────────┘
```

### Key Constraints

| Constraint | Enforcement |
|-----------|-------------|
| One tent + one canoe per user per plan | `UNIQUE(plan_id, user_id, assignment_type)` |
| Assignment names unique per plan+type | `UNIQUE(plan_id, name, type)` |
| Max occupancy enforced | `CHECK(max_occupancy > 0)` + app-level validation |
| One itinerary per plan | `UNIQUE(plan_id)` on itineraries |
| Items must belong to a plan | `CHECK(plan_id IS NOT NULL)` |
| Ownership transfer on user deletion | PostgreSQL trigger → assignments transfer to plan owner |
| Cascade deletes | Deleting a plan removes all children (members, items, assignments, invitations) |
| Normalized emails | Migration V010: lowercase + strip dots from local part |

---

## API Reference

All endpoints require the `X-User-Id` header unless noted. Responses use standard HTTP status codes.

<details>
<summary><strong>🔐 Authentication & Users</strong> — 4 endpoints</summary>

| Method | Endpoint | Auth | Description | Success |
|--------|----------|------|-------------|---------|
| `POST` | `/api/auth` | — | Sign in by email | `200` User |
| `POST` | `/api/users` | — | Register (idempotent by email) | `201` User |
| `GET` | `/api/users/{userId}` | `X-User-Id` | Get user profile | `200` User |
| `PUT` | `/api/users/{userId}` | `X-User-Id` | Update username | `200` User |

> **Auth flow:** Sign in returns the user object. If username is null, returns `403 REGISTRATION_REQUIRED` — frontend auto-switches to the register form.

</details>

<details>
<summary><strong>🗺️ Plans & Members</strong> — 7 endpoints</summary>

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `POST` | `/api/plans` | Create trip (creator auto-added as member) | `201` |
| `GET` | `/api/plans` | List trips (owned + member + public) | `200` |
| `PUT` | `/api/plans/{planId}` | Update name or visibility (owner only) | `200` |
| `DELETE` | `/api/plans/{planId}` | Delete trip (owner only) | `204` |
| `GET` | `/api/plans/{planId}/members` | List members with invitation status | `200` |
| `POST` | `/api/plans/{planId}/members` | Add member by email (sends invitation) | `201` |
| `DELETE` | `/api/plans/{planId}/members/{id}` | Remove member | `204` |

> **Visibility:** Public plans appear in everyone's list (non-members see `isMember: false` and can join). Private plans are invitation-only.

</details>

<details>
<summary><strong>🎒 Items (Gear & Supplies)</strong> — 5 endpoints</summary>

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `POST` | `/api/items` | Create item (plan-shared or personal) | `201` |
| `GET` | `/api/items?ownerType=...&ownerId=...&planId=...` | List items by owner | `200` |
| `GET` | `/api/items/{id}` | Get item details | `200` |
| `PUT` | `/api/items/{id}` | Update item (name, category, qty, packed) | `200` |
| `DELETE` | `/api/items/{id}` | Delete item | `204` |

> **Ownership model:** Items with only `plan_id` are shared camp gear. Items with both `plan_id` + `user_id` are personal packs scoped to that trip.
>
> **Categories:** `camp`, `canoe`, `kitchen`, `personal`, `food`, `misc`

</details>

<details>
<summary><strong>🗓️ Itinerary & Events</strong> — 5 endpoints</summary>

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `GET` | `/api/plans/{planId}/itinerary` | Get itinerary with events (auto-creates) | `200` |
| `POST` | `/api/plans/{planId}/itinerary/events` | Add event (auto-creates itinerary) | `201` |
| `PUT` | `/api/plans/{planId}/itinerary/events/{eventId}` | Update event | `200` |
| `DELETE` | `/api/plans/{planId}/itinerary/events/{eventId}` | Delete event | `204` |
| `DELETE` | `/api/plans/{planId}/itinerary` | Delete entire itinerary | `204` |

</details>

<details>
<summary><strong>⛺ Assignments (Tents & Canoes)</strong> — 8 endpoints</summary>

| Method | Endpoint | Description | Success |
|--------|----------|-------------|---------|
| `POST` | `/api/plans/{planId}/assignments` | Create group (creator auto-added) | `201` |
| `GET` | `/api/plans/{planId}/assignments` | List groups (optional `?type=tent\|canoe`) | `200` |
| `GET` | `/api/plans/{planId}/assignments/{id}` | Get group with member list | `200` |
| `PUT` | `/api/plans/{planId}/assignments/{id}` | Update name or max occupancy (owner) | `200` |
| `DELETE` | `/api/plans/{planId}/assignments/{id}` | Delete group (owner only) | `204` |
| `POST` | `/api/plans/{planId}/assignments/{id}/members` | Add member to group | `201` |
| `DELETE` | `/api/plans/{planId}/assignments/{id}/members/{userId}` | Remove member | `204` |
| `PUT` | `/api/plans/{planId}/assignments/{id}/owner` | Transfer ownership | `200` |

> **Rules:** Each member can be in at most one tent and one canoe per trip. Capacity is enforced. Owner cannot be removed (must transfer first).

</details>

<details>
<summary><strong>🔗 Gear Sync & Webhooks</strong> — 2 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/api/plans/{planId}/gear-sync` | `X-User-Id` | Sync gear from external source |
| `POST` | `/api/webhooks/resend` | — | Resend email delivery webhook |

</details>

---

## Real-Time Updates

Camper uses **STOMP over WebSocket** so every member sees changes live:

```
  Alice (Browser A)              Server              Bob (Browser B)
        │                          │                       │
        │                          │    subscribe to       │
        │                          │◄─ /topic/plans/{id} ──│
        │                          │                       │
        │── POST /api/items ──────►│                       │
        │                          │                       │
        │◄── 201 Created ─────────│                       │
        │                          │── STOMP message ─────►│
        │                          │   { resource: "items" │
        │                          │     action: "created"} │
        │                          │                       │
        │                          │          refetch() ──►│
        │                          │◄── GET /api/items ────│
        │                          │── 200 [items] ───────►│
        │                          │                       │
```

### How It Works

1. **Subscribe** — Frontend connects to `/ws` via STOMP and subscribes to `/topic/plans/{planId}`
2. **Mutate** — Any member makes a change (create, update, delete) via REST API
3. **Publish** — Controller calls `PlanEventPublisher.publishUpdate(planId, resource, action)`
4. **Broadcast** — STOMP message sent to all subscribers of that plan's topic
5. **Refetch** — Frontend routes the update by `resource` type:
   - `plan` or `members` → full page data refetch
   - `assignments` or `itinerary` → increment refresh key (modal refetches on next open)

**Connection config:** 5-second reconnect delay, 10-second heartbeat interval (both directions).

---

## Getting Started

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Eclipse Temurin recommended |
| Node.js | 22+ | With npm |
| Docker | Latest | For PostgreSQL via docker-compose |
| Gradle | 8.12 | Wrapper included (`./gradlew`) |

### Quick Start

```bash
# 1. Start PostgreSQL
cd databases/camper-db && docker-compose up -d

# 2. Run Flyway migrations
cd databases/camper-db && flyway -configFiles=flyway.conf migrate

# 3. Seed development data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db \
  -f databases/camper-db/seed/dev_seed.sql

# 4. Start the backend (port 8080)
./gradlew :services:camper-service:bootRun

# 5. Start the frontend (port 3000)
cd webapp && npm install && npm run dev
```

Open **http://localhost:3000** — the Vite dev server proxies `/api` and `/ws` to the backend.

### With Email Invitations

```bash
RESEND_API_KEY=re_xxx \
EMAIL_FROM="onboarding@resend.dev" \
APP_BASE_URL="http://localhost:3000" \
./gradlew :services:camper-service:bootRun
```

Without `RESEND_API_KEY`, the app uses a NoOp email client — invitations work but no emails are sent.

---

## Testing

```bash
# Run all tests (unit + acceptance)
./gradlew test

# Full build (compile + test + package)
./gradlew clean build
```

### Test Strategy

| Layer | Approach | Speed | What it verifies |
|-------|----------|-------|-----------------|
| **Unit** | FakeClient (in-memory) | ⚡ Fast | Service actions, validations, error paths |
| **Acceptance** | Testcontainers + `@SpringBootTest` | 🐳 Slower | Full HTTP round-trips against real PostgreSQL |
| **WebSocket** | Broker channel interceptor | ⚡ Fast | STOMP messages published on mutations |

**Key testing patterns:**

- Every client module ships a **FakeClient** in `testFixtures/` — in-memory implementations for isolated unit tests
- Acceptance tests use **Testcontainers** to spin up a real PostgreSQL 16 container
- Tables are **truncated with CASCADE** in `@BeforeEach` — each test starts clean
- Test fixtures insert data directly via `JdbcTemplate` for deterministic setup
- `@Primary` bean overrides swap real clients for test-specific configurations

---

## Deployment

### Production Infrastructure

| Component | Platform | Details |
|-----------|----------|---------|
| **Application** | [Railway](https://railway.com) | Spring Boot serves React as static files |
| **Database** | Railway PostgreSQL | Managed instance, auto-connected |
| **Email** | [Resend](https://resend.com) | Invite emails + delivery status webhooks |

### Docker Build (3 Stages)

```
┌─────────────────────────────────────────────┐
│  Stage 1: node:22-slim                      │
│  npm ci → npm run build → webapp/dist/      │
├─────────────────────────────────────────────┤
│  Stage 2: eclipse-temurin:21-jdk            │
│  ./gradlew bootJar → app.jar                │
├─────────────────────────────────────────────┤
│  Stage 3: eclipse-temurin:21-jre            │
│  COPY app.jar + webapp/dist → /app/static/  │
│  Spring Boot serves SPA + API on :8080      │
└─────────────────────────────────────────────┘
```

SPA routing: `WebConfig.kt` forwards all non-API, non-static routes to `index.html` for React Router.

### Deploy Commands

```bash
railway service camper-service
railway up
```

### Environment Variables

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `DB_URL` | **Yes** | `jdbc:postgresql://localhost:5433/camper_db` | PostgreSQL JDBC URL |
| `DB_USER` | **Yes** | `postgres` | Database username |
| `DB_PASSWORD` | **Yes** | `postgres` | Database password |
| `RESEND_API_KEY` | No | — | Enables real email sending (NoOp without it) |
| `EMAIL_FROM` | No | `Camper <noreply@example.com>` | Sender address (must be verified Resend domain) |
| `APP_BASE_URL` | No | `http://localhost:5173` | Base URL for links in invitation emails |

### Adding Migrations

1. Create `databases/camper-db/migrations/V0XX__description.sql`
2. Gradle copies migrations to classpath at build time
3. Flyway auto-applies on next startup — no manual step needed

---

## Design Patterns

### Client Interface + Fake

Every data access client follows the same internal structure:

```
clients/foo-client/
├── api/FooClient.kt                  # Public interface
├── internal/
│   ├── JdbiFooClient.kt              # JDBI facade (implements interface)
│   ├── operations/
│   │   ├── CreateFoo.kt              # One class per DB operation
│   │   ├── GetFoo.kt
│   │   └── ListFoo.kt
│   └── validations/
│       └── ValidateCreateFoo.kt      # 1:1 with operations
├── FooClientFactory.kt               # Factory: reads env vars, returns real client
└── testFixtures/
    └── FakeFooClient.kt              # In-memory fake for unit tests
```

### Action-Based Services

```
     ┌───────────┐     ┌───────────┐     ┌───────────┐     ┌────────────┐
     │ Validate  │────►│  Convert  │────►│  Execute  │────►│  Return    │
     │  input    │     │ to domain │     │  (client) │     │ Result<T,E>│
     └───────────┘     └───────────┘     └───────────┘     └────────────┘
```

- Services compose **Actions** into a facade — no `@Service` annotations, only `@Configuration` beans
- Errors use a sealed `Result<T, E>` class — **exceptions are never thrown** for expected failures
- Each action has a 1:1 validation class

### Event-Driven Live Updates

```
REST mutation succeeds
        │
        ▼
PlanEventPublisher.publishUpdate(planId, resource, action)
        │
        ▼
STOMP broadcast to /topic/plans/{planId}
        │
        ▼
Frontend usePlanUpdates hook → selective refetch by resource type
```

---

## Tech Stack

<div align="center">

| | Layer | Technologies |
|---|:---:|---|
| 🖥️ | **Frontend** | React 19 · TypeScript 5.9 · Vite 7 · @stomp/stompjs · Custom CSS |
| ⚙️ | **Backend** | Kotlin 2.1.10 · Spring Boot 3.4.3 · Java 21 · JDBI 3.x |
| 🗄️ | **Database** | PostgreSQL 16 · Flyway migrations · UUID primary keys |
| 📧 | **Email** | Resend SDK · Delivery webhooks · NoOp fallback |
| 🔌 | **Real-time** | STOMP over WebSocket · Spring messaging · Simple broker |
| 🔨 | **Build** | Gradle 8.12 (Kotlin DSL) · npm · Docker multi-stage |
| 🧪 | **Testing** | JUnit 5 · AssertJ · Testcontainers · FakeClient pattern |
| 🚀 | **Deploy** | Railway · 3-stage Dockerfile · Flyway auto-migrate |

</div>

---

<div align="center">
<br/>

```
    🔥🔥🔥
   🔥🪵🔥
  🔥🔥🔥🔥
 ~~~~~~~~~~~~
```

**Happy camping!** 🏕️

*Gather your friends. Plan your adventure. See you around the fire.*

</div>
