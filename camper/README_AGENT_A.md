<div align="center">

```
                    🌙
            ✦  .    *    .  ✦
        .    *    .    .    *    .
     🌲  .    .  🌲    🌲  .    .  🌲
    /\       /\      /\       /\
   /  \     /  \    /  \     /  \
  /    \   / ⛺ \  /    \   /    \
 /______\ /______\/______\ /______\
          \  🔥  /
     ~~~~~~\____/~~~~~~~~~~~~~~~~~~~~~~~
      ~ ~ ~  ~ ~ ~  ~ ~ ~  ~ ~ ~  ~ ~
```

# 🏕️ Camper

### *Gather 'round the fire and plan your next adventure*

A beautifully crafted web app for **collaborative camping trip planning**.
Invite your friends, gather around a virtual campfire, and plan every detail together — in real time.

<br>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)

</div>

<br>

---

## Table of Contents

- [What is Camper?](#what-is-camper)
- [Features at a Glance](#features-at-a-glance)
- [How It Works](#how-it-works)
- [The Campsite Experience](#the-campsite-experience)
- [Architecture](#architecture)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Real-Time Updates](#real-time-updates)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Deployment](#deployment)
- [Tech Stack](#tech-stack)

---

## What is Camper?

Camper is a full-stack web application that turns camping trip planning into a **shared, interactive experience**. Instead of scattered spreadsheets and group chats, your whole crew plans together in one place — with changes syncing live across all browsers via WebSocket.

The heart of the app is an **animated campsite scene**: your group gathers as illustrated avatars around a glowing campfire, and you click on campsite objects to open collaborative planning tools.

> **Aesthetic:** *"Enchanted Expedition Journal"* — watercolor storybook meets cozy RPG. Parallax wilderness backgrounds, CSS-animated campfire, pure SVG illustrations, and a soft pastel color palette.

---

## Features at a Glance

<table>
<tr>
<td width="50%">

### 🗺️ Trip Management
Create **public** or **private** camping trips. Invite friends by email — they get a beautiful invitation email with a one-click join link.

### 🎒 Gear Checklists
Two layers: **Shared Camp Gear** (communal equipment managed by the trip owner) and **Personal Packs** (each member manages their own packing list). Track progress with visual bars.

### 🍳 Meal Planning
Day-by-day meal planner with categories for breakfast, lunch, dinner, and snacks. Add days as needed — everyone can contribute to the meal plan.

</td>
<td width="50%">

### ⛺ Tent & Canoe Assignments
Organize who sleeps where and who paddles together. Occupancy limits, ownership transfer, and a constraint that each person can only be in one tent and one canoe.

### 📡 Live Collaboration
STOMP-over-WebSocket pushes updates instantly. When someone adds gear or joins a tent group, every connected browser sees it immediately.

### 📧 Email Invitations
Powered by [Resend](https://resend.com). Full delivery lifecycle tracking via webhooks: pending → sent → delivered (or bounced/failed).

</td>
</tr>
</table>

---

## How It Works

```
  ┌──────────┐         ┌──────────┐         ┌──────────┐
  │  CREATE   │────────►│  INVITE  │────────►│   PLAN   │
  │  A TRIP   │         │  FRIENDS │         │ TOGETHER │
  └──────────┘         └──────────┘         └──────────┘
                                                  │
       ┌──────────────────┬───────────────────┬───┘
       ▼                  ▼                   ▼
  ┌─────────┐       ┌──────────┐       ┌──────────┐
  │  GEAR   │       │  MEALS   │       │  TENTS   │
  │Checklists│      │Day-by-day│       │& CANOES  │
  └─────────┘       └──────────┘       └──────────┘
```

1. **Create a trip** — give it a name, set it as public or private
2. **Invite your crew** — add members by email; they receive an invitation email with a join link
3. **Gather at the campfire** — everyone appears as an illustrated avatar seated around the fire
4. **Click campsite objects** to plan collaboratively:
   - **🎒 Equipment pile** → Gear checklists (shared + personal)
   - **🍳 Kitchen** → Meal planner (day-by-day)
   - **⛺ Tent** → Tent & canoe group assignments
   - **🗺️ Map table** → Itinerary *(coming soon)*

---

## The Campsite Experience

The Plan page is the centerpiece — a nighttime campsite scene that brings the group together visually.

```
                        🌙  ✦  *  .  ✦
                    .    *    .    .    *
            🌲                              🌲
           /\    ┌─────────────────────┐    /\
          /  \   │  Members sit around │   /  \
         /    \  │  the campfire as    │  /    \
        /______\ │  illustrated SVG    │ /______\
                 │  avatars in a       │
   🎒            │  semicircle that    │         🗺️
  Equipment      │  scales with group  │      Map Table
   Pile          │  size               │
                 │                     │
                 │       🔥🔥🔥        │
                 │      🔥🪵🔥       │
                 └─────────────────────┘
       ⛺                                    🍳
      Tent                                 Kitchen
```

**Visual details:**
- **Parallax backgrounds** — three variants (night, dusk, campsite) with mouse-tracked layer offsets creating depth
- **Animated campfire** — multi-layered CSS: outer/mid/inner/core flames, floating ember particles, rising smoke, log & stone ring
- **Camper avatars** — SVG-illustrated people with randomized pastel colors, positioned in a semicircle using trigonometry; radius scales dynamically (15% per member beyond 4) to prevent overlap
- **Glowing forest eyes** — randomized pairs of glowing eyes peer out from the dark forest background, reacting to mouse movement
- **Interactable items** — hover glow effect + tooltip; click to open the corresponding planning modal
- **Day/night cycle** — automatically switches based on time of day (6am–7pm = day, 7pm–6am = night)
- **Parchment-textured modals** — themed UI with category-specific flavor text and warm aesthetics

**Design system:** Fonts are *Cinzel Decorative* (display), *Fredericka the Great* (headings), *Lora* (body). Palette defined as CSS custom properties: `--lavender`, `--sage`, `--tan`, `--rose`, `--mint`, `--ember`, `--flame`, `--night-sky`, `--parchment`.

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────┐
│                   Frontend (React 19)                    │
│       Vite 7 · TypeScript · STOMP.js · CSS-only         │
├───────────────────────────┬─────────────────────────────┤
│     REST API (/api/*)     │    WebSocket (/ws)          │
│         HTTP JSON         │    STOMP protocol           │
├───────────────────────────┴─────────────────────────────┤
│                Spring Boot 3.4.3 Service                 │
│                                                          │
│   Controllers ──► Actions ──► Validations                │
│                       │                                  │
│                       ▼                                  │
│   ┌──────────────────────────────────────────────┐      │
│   │              Client Layer (JDBI)              │      │
│   │  Interface + Facade + Operations + Params     │      │
│   └──────────────────────────────────────────────┘      │
├──────────────────────────────────────────────────────────┤
│    PostgreSQL 16  ·  Flyway Migrations  ·  UUID PKs      │
└──────────────────────────────────────────────────────────┘
       ▲                                    ▲
       │                                    │
   Resend SDK                         Resend Webhooks
  (invite emails)                   (delivery tracking)
```

### Module Layout

```
camper/
├── webapp/                    # React 19 + TypeScript + Vite 7
│   ├── src/
│   │   ├── api/               #   Typed fetch wrapper with auto X-User-Id
│   │   ├── hooks/             #   usePlanUpdates (STOMP WebSocket)
│   │   ├── context/           #   AuthContext (localStorage-persisted)
│   │   ├── components/        #   Campfire, Avatars, Modals, Parallax, SVG art
│   │   ├── pages/             #   Login, Home, Plan (campsite), Account
│   │   └── styles/            #   theme.css (design tokens), animations.css
│   └── vite.config.ts         #   Dev server :3000, proxy /api + /ws → :8080
│
├── services/
│   ├── common/                # ApiResponse shared type
│   └── camper-service/        # Spring Boot app
│       └── features/          #   Organized by domain feature:
│           ├── user/          #     controller, service, actions, validations
│           ├── plan/          #     controller, service, actions, mapper
│           ├── item/          #     controller, service, actions, validations
│           ├── itinerary/     #     controller, service, actions
│           ├── assignment/    #     controller, service, actions
│           ├── gearsync/      #     controller, action
│           └── webhook/       #     controller, action (Resend events)
│
├── clients/
│   ├── common/                # Result<T,E> sealed class, error types
│   ├── user-client/           # JDBI — users table
│   ├── plan-client/           # JDBI — plans & plan_members
│   ├── item-client/           # JDBI — items (polymorphic ownership)
│   ├── itinerary-client/      # JDBI — itineraries & itinerary_events
│   ├── assignment-client/     # JDBI — assignments & assignment_members
│   ├── invitation-client/     # JDBI — invitations lifecycle
│   └── email-client/          # Resend SDK + NoOp fallback
│
├── libs/
│   └── common/                # Pure utilities (no I/O)
│
└── databases/
    └── camper-db/             # docker-compose, Flyway migrations, seeds
```

> **Dependency rule:** `services → clients → libs` — never reverse. If it does I/O, it goes in `clients/`. Pure logic goes in `libs/`.

### Key Design Patterns

| Pattern | Implementation |
|---------|---------------|
| **Result\<T, E\>** | Sealed class (`Success` / `Failure`) — no exceptions for expected failures |
| **Client Interface + Fake** | Every client exposes an interface; `testFixtures` provides a `FakeClient` for unit tests |
| **Action Classes** | Each use case: validate → convert → call client. Composed into a Service facade |
| **@Configuration DI** | No `@Service` / `@Component` on domain classes — explicit Spring bean wiring |
| **Event Publishing** | `PlanEventPublisher` broadcasts `{resource, action}` after successful mutations |
| **Polymorphic Items** | Items can be plan-scoped (shared) or user+plan-scoped (personal within a trip) |

---

## Database Schema

12 Flyway migrations · 10 tables · PostgreSQL 16

### Entity Relationship Diagram

```
                          ┌──────────┐
                          │  users   │
                          │──────────│
                          │ id (PK)  │
                          │ email    │ UNIQUE
                          │ username │
                          └────┬─────┘
                               │
              ┌────────────────┼────────────────┐
              │ owns           │ member_of       │ inviter
              ▼                ▼                 ▼
         ┌─────────┐    ┌─────────────┐   ┌────────────┐
         │  plans   │◄───│plan_members │   │invitations │
         │─────────│    │─────────────│   │────────────│
         │ id (PK) │    │ plan_id (PK)│   │ id (PK)    │
         │ name    │    │ user_id (PK)│   │ plan_id    │
         │ owner_id│    └─────────────┘   │ user_id    │
         │visibility│                      │ email      │
         └────┬────┘                      │ status     │
              │                           └────────────┘
    ┌─────────┼──────────┬──────────────┐
    ▼         ▼          ▼              ▼
┌───────┐┌──────────┐┌───────────┐┌────────────────┐
│ items ││itineraries││assignments││  (cascades on  │
│───────││──────────││───────────││  plan delete)  │
│ id    ││ id       ││ id        │└────────────────┘
│plan_id││ plan_id  ││ plan_id   │
│user_id││ (UNIQUE) ││ name      │
│name   │└────┬─────┘│ type      │
│category│     │      │ max_occ.  │
│quantity│     ▼      │ owner_id  │
│packed  │┌─────────┐└─────┬─────┘
└───────┘│itinerary │      │
         │_events   │      ▼
         │─────────│┌─────────────┐
         │ id      ││ assignment  │
         │ title   ││ _members    │
         │ eventAt ││─────────────│
         └─────────┘│assignment_id│
                    │ user_id     │
                    │ plan_id     │
                    └─────────────┘
```

### Table Details

| Table | Purpose | Key Constraints |
|-------|---------|-----------------|
| `users` | User accounts | `email` UNIQUE |
| `plans` | Camping trips | `owner_id` FK → users |
| `plan_members` | Trip membership (M:N) | Composite PK (`plan_id`, `user_id`) |
| `items` | Gear & food checklist items | CHECK: at least one of `plan_id` or `user_id` set |
| `itineraries` | Schedule container (1:1 with plan) | UNIQUE on `plan_id` |
| `itinerary_events` | Scheduled activities | FK → itineraries CASCADE |
| `assignments` | Tent / canoe groups | UNIQUE(`plan_id`, `name`, `type`); `max_occupancy > 0` |
| `assignment_members` | Group membership | UNIQUE(`plan_id`, `user_id`, `assignment_type`) |
| `invitations` | Email invitation tracking | Status lifecycle: pending → sent → delivered |

---

## API Reference

All endpoints require `X-User-Id` header unless noted. Base: `/api`

<details>
<summary><strong>Authentication & Users</strong> — 4 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `POST` | `/api/auth` | — | Sign in by email |
| `POST` | `/api/users` | — | Register (idempotent — returns existing user if email exists) |
| `GET` | `/api/users/{userId}` | `X-User-Id` | Get user profile |
| `PUT` | `/api/users/{userId}` | `X-User-Id` | Update username |

</details>

<details>
<summary><strong>Plans & Members</strong> — 7 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `GET` | `/api/plans` | `X-User-Id` | List your trips (+ public trips) |
| `POST` | `/api/plans` | `X-User-Id` | Create trip (creator auto-added as member) |
| `PUT` | `/api/plans/{planId}` | `X-User-Id` | Update name / toggle visibility (owner only) |
| `DELETE` | `/api/plans/{planId}` | `X-User-Id` | Delete trip (owner only, cascades everything) |
| `GET` | `/api/plans/{planId}/members` | `X-User-Id` | List trip members |
| `POST` | `/api/plans/{planId}/members` | `X-User-Id` | Invite member by email (sends invite email) |
| `DELETE` | `/api/plans/{planId}/members/{id}` | `X-User-Id` | Remove member / leave trip |

</details>

<details>
<summary><strong>Items (Gear & Meals)</strong> — 5 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `GET` | `/api/items?ownerType=...&ownerId=...&planId=...` | `X-User-Id` | List items by owner (plan or user, optionally scoped to plan) |
| `GET` | `/api/items/{id}` | `X-User-Id` | Get single item |
| `POST` | `/api/items` | `X-User-Id` | Create item (plan-level shared or user+plan personal) |
| `PUT` | `/api/items/{id}` | `X-User-Id` | Update item (name, category, quantity, packed) |
| `DELETE` | `/api/items/{id}` | `X-User-Id` | Delete item |

**Item categories:** `camp`, `canoe`, `kitchen`, `personal`, `food_item`, `misc` — or day-prefixed for meals: `day1:breakfast`, `day1:lunch`, etc.

</details>

<details>
<summary><strong>Itinerary & Events</strong> — 5 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `GET` | `/api/plans/{planId}/itinerary` | `X-User-Id` | Get itinerary with all events |
| `DELETE` | `/api/plans/{planId}/itinerary` | `X-User-Id` | Delete entire itinerary |
| `POST` | `/api/plans/{planId}/itinerary/events` | `X-User-Id` | Add event (auto-creates itinerary if none exists) |
| `PUT` | `/api/plans/{planId}/itinerary/events/{id}` | `X-User-Id` | Update event |
| `DELETE` | `/api/plans/{planId}/itinerary/events/{id}` | `X-User-Id` | Delete event |

</details>

<details>
<summary><strong>Assignments (Tents & Canoes)</strong> — 8 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `GET` | `/api/plans/{planId}/assignments` | `X-User-Id` | List all (optional `?type=tent\|canoe`) |
| `GET` | `/api/plans/{planId}/assignments/{id}` | `X-User-Id` | Get assignment with member list |
| `POST` | `/api/plans/{planId}/assignments` | `X-User-Id` | Create group (creator auto-added as member + owner) |
| `PUT` | `/api/plans/{planId}/assignments/{id}` | `X-User-Id` | Update name / max occupancy (owner only) |
| `DELETE` | `/api/plans/{planId}/assignments/{id}` | `X-User-Id` | Delete group (owner only) |
| `POST` | `/api/plans/{planId}/assignments/{id}/members` | `X-User-Id` | Add member to group |
| `DELETE` | `/api/plans/{planId}/assignments/{id}/members/{userId}` | `X-User-Id` | Remove member from group |
| `PUT` | `/api/plans/{planId}/assignments/{id}/owner` | `X-User-Id` | Transfer group ownership |

**Constraint:** Each user can belong to at most **one tent** and **one canoe** per trip.

</details>

<details>
<summary><strong>Gear Sync & Webhooks</strong> — 2 endpoints</summary>

| Method | Endpoint | Auth | Description |
|--------|----------|:----:|-------------|
| `POST` | `/api/plans/{planId}/gear-sync` | `X-User-Id` | Aggregate all personal gear into shared plan gear |
| `POST` | `/api/webhooks/resend` | — | Resend email delivery webhook (status updates) |

</details>

---

## Real-Time Updates

Camper uses **STOMP-over-WebSocket** so all planners see changes instantly:

```
  Browser A (mutator)           Server                  Browser B (observer)
         │                        │                            │
         │── POST /api/items ────►│                            │
         │                        │                            │
         │◄── 201 Created ───────│                            │
         │                        │                            │
         │                        │── STOMP ──────────────────►│
         │                        │  /topic/plans/{planId}     │
         │                        │  {                         │
         │                        │    "resource": "items",    │
         │                        │    "action": "created"     │
         │                        │  }                         │
         │                        │                            │
         │                        │         onUpdate() ───────►│
         │                        │         refetch data ─────►│
         │                        │              ◄── fresh UI ─│
```

### How It Works

1. **Controller** processes a mutation (create/update/delete)
2. On `Result.Success`, calls `PlanEventPublisher.publishUpdate(planId, resource, action)`
3. Server broadcasts a lightweight `{resource, action}` message to `/topic/plans/{planId}`
4. **Frontend** `usePlanUpdates` hook receives the message and routes by resource type:
   - `plan` / `members` → immediate refetch of plan data
   - `assignments` → increment refresh key → AssignmentsModal refetches
   - `itinerary` → increment refresh key → ItineraryModal refetches
   - `items` → modals refetch on next open
5. Auto-reconnects on disconnect with 5-second backoff

---

## Getting Started

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 21+ | Eclipse Temurin recommended |
| Node.js | 22+ | For the React frontend |
| Docker | Latest | For PostgreSQL via docker-compose |
| Gradle | 8.12 | Wrapper included (`./gradlew`) |

### Quick Start

```bash
# 1. Clone and enter the project
cd camper

# 2. Start PostgreSQL
cd databases/camper-db && docker-compose up -d && cd ../..

# 3. Run Flyway migrations
cd databases/camper-db && flyway -configFiles=flyway.conf migrate && cd ../..

# 4. Seed development data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db \
  -f databases/camper-db/seed/dev_seed.sql

# 5. Start the backend (port 8080)
./gradlew :services:camper-service:bootRun &

# 6. Start the frontend (port 3000)
cd webapp && npm install && npm run dev
```

Open **http://localhost:3000** and start planning your trip!

### With Email Invitations

To enable real invite emails, set these environment variables before starting the backend:

```bash
RESEND_API_KEY=re_your_key \
EMAIL_FROM="Camper <noreply@yourdomain.com>" \
APP_BASE_URL="http://localhost:3000" \
./gradlew :services:camper-service:bootRun
```

Without `RESEND_API_KEY`, a NoOp email client is used (invitations are logged but not sent).

---

## Testing

```bash
./gradlew test          # All tests (unit + acceptance)
./gradlew clean build   # Full build with tests
```

### Test Architecture

| Layer | Strategy | Infrastructure |
|-------|----------|----------------|
| **Unit Tests** | FakeClient implementations test service actions in isolation | JUnit 5 + AssertJ |
| **Acceptance Tests** | Full `@SpringBootTest` with real database | Testcontainers (PostgreSQL 16-alpine) |
| **WebSocket Tests** | Channel interceptor captures STOMP broker messages | `ArrayBlockingQueue` + `ChannelInterceptor` |

**Test lifecycle:**
- Testcontainers auto-starts a fresh PostgreSQL instance
- Flyway migrations run against the test database
- `@BeforeEach` truncates all tables with CASCADE for a clean slate
- FakeClients in `testFixtures` source sets enable fast, isolated unit tests

---

## Deployment

### Production Infrastructure

```
┌─────────────────────────────────────┐
│           Railway Platform           │
│                                      │
│  ┌──────────────┐  ┌─────────────┐  │
│  │camper-service │  │  Postgres   │  │
│  │  (Docker)     │◄►│  (managed)  │  │
│  │              │  └─────────────┘  │
│  │  Spring Boot  │                   │
│  │  + React SPA  │◄── Resend        │
│  └──────────────┘    Webhooks       │
│                                      │
└─────────────────────────────────────┘
```

### Three-Stage Docker Build

| Stage | Base Image | Purpose | Output |
|-------|-----------|---------|--------|
| 1 | `node:22-slim` | Build React frontend | `webapp/dist/` |
| 2 | `eclipse-temurin:21-jdk` | Build Spring Boot JAR | `app.jar` |
| 3 | `eclipse-temurin:21-jre` | Runtime (minimal) | Serves app.jar + static files |

Spring Boot serves the React build as static files from `/app/static/`. A `WebConfig` forwards all non-API, non-static routes to `index.html` for SPA routing.

### Deploy

```bash
railway service camper-service
railway up
```

### Environment Variables

| Variable | Required | Default | Purpose |
|----------|:--------:|---------|---------|
| `DB_URL` | **Yes** | — | PostgreSQL JDBC connection URL |
| `DB_USER` | **Yes** | — | Database username |
| `DB_PASSWORD` | **Yes** | — | Database password |
| `RESEND_API_KEY` | No | — | Enables email invitations (NoOp client without) |
| `EMAIL_FROM` | No | `Camper <noreply@example.com>` | Sender address (must be verified Resend domain) |
| `APP_BASE_URL` | No | `http://localhost:5173` | Base URL for links in invitation emails |

### Adding Database Migrations

1. Create a new SQL file in `databases/camper-db/migrations/` (e.g., `V013__add_foo.sql`)
2. Gradle copies it to the classpath at build time; Flyway applies it on next deploy

---

## Tech Stack

<div align="center">

| | Technology | Version |
|:---:|:---|:---:|
| **Frontend** | React + TypeScript + Vite | 19 / 5.9 / 7 |
| **Real-time** | STOMP.js over WebSocket | 7.3 |
| **Backend** | Kotlin + Spring Boot | 2.1.10 / 3.4.3 |
| **Data Access** | JDBI | 3.x |
| **Database** | PostgreSQL + Flyway | 16 |
| **Build** | Gradle (Kotlin DSL) + Docker | 8.12 |
| **Testing** | JUnit 5 + AssertJ + Testcontainers | — |
| **Email** | Resend SDK | — |
| **Hosting** | Railway | — |
| **Runtime** | Java (Eclipse Temurin) | 21 |

</div>

---

<div align="center">

<br>

```
  🔥  Happy camping!  🔥
```

*Built with warmth by campfire light.*

<br>

</div>
