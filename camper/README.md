# camper

A web app for interactive camping trip planning. Invite people on a trip and let everyone view trip details and contribute to planning — meal plans, equipment, group assignments, and more.

This project is in early development. Features are added incrementally.

## Quick Start

```bash
# Start everything (database, API, webapp) and open browser
./start.sh

# Stop everything
./stop.sh
```

`start.sh` brings up the database, runs migrations, seeds dev data, starts the API and webapp, then opens `http://localhost:3000` in your browser. Press Ctrl+C to stop.

`stop.sh` kills the webapp and service processes and stops the database container.

## Prerequisites

- JDK 21+
- Node.js (for the webapp)
- Docker
- [Flyway CLI](https://flywaydb.org/download)
- PostgreSQL client (`psql`)

## Project Structure

```
camper/
├── libs/common/               # Shared utilities (no I/O)
├── clients/
│   ├── common/                # Result type, error types, ClientContext
│   ├── world-client/          # JDBI data access for worlds
│   ├── user-client/           # JDBI data access for users
│   ├── plan-client/           # JDBI data access for plans & plan members
│   ├── item-client/           # JDBI data access for items
│   ├── itinerary-client/      # JDBI data access for itineraries & events
│   └── assignment-client/     # JDBI data access for assignments & members
├── services/
│   ├── common/                # ApiResponse shared type
│   └── camper-service/        # Spring Boot REST API (port 8080)
├── webapp/                    # React + Vite frontend (port 3000)
└── databases/
    └── camper-db/             # Schema, Flyway migrations, seeds, docker-compose
```

## Manual Setup

If you prefer to run things individually instead of using the scripts:

```bash
# Start PostgreSQL
cd databases/camper-db && docker compose up -d

# Run migrations
flyway -configFiles=databases/camper-db/flyway.conf \
  -locations=filesystem:databases/camper-db/migrations migrate

# Seed development data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db \
  -f databases/camper-db/seed/dev_seed.sql

# Start the API
./gradlew :services:camper-service:bootRun

# Start the webapp (in another terminal)
cd webapp && npm run dev
```

The API runs on `http://localhost:8080`. The webapp runs on `http://localhost:3000` and proxies `/api` requests to the API.

## Tech Stack

- **Backend:** Kotlin 2.1.10, Spring Boot 3.4.3, JDBI 3.x, PostgreSQL 16
- **Frontend:** React 19, TypeScript, Vite 7
- **Build:** Gradle 8.12 (Kotlin DSL)
- **Testing:** JUnit 5, AssertJ, Testcontainers

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/users` | Register user (idempotent) |
| POST | `/api/auth` | Authenticate by email |
| GET | `/api/users/{userId}` | Get user by ID |
| PUT | `/api/users/{userId}` | Update username |
| POST | `/api/plans` | Create a plan |
| GET | `/api/plans` | List plans |
| PUT | `/api/plans/{planId}` | Update plan |
| DELETE | `/api/plans/{planId}` | Delete a plan |
| GET | `/api/plans/{planId}/members` | List plan members |
| POST | `/api/plans/{planId}/members` | Add member by email |
| DELETE | `/api/plans/{planId}/members/{memberId}` | Remove member |
| GET | `/api/items` | List items (query by owner) |
| POST | `/api/items` | Create an item |
| PUT | `/api/items/{itemId}` | Update an item |
| DELETE | `/api/items/{itemId}` | Delete an item |
| GET | `/api/plans/{planId}/itinerary` | Get itinerary with events |
| DELETE | `/api/plans/{planId}/itinerary` | Delete itinerary |
| POST | `/api/plans/{planId}/itinerary/events` | Add itinerary event |
| PUT | `/api/plans/{planId}/itinerary/events/{eventId}` | Update event |
| DELETE | `/api/plans/{planId}/itinerary/events/{eventId}` | Delete event |
| GET | `/api/plans/{planId}/assignments` | List assignments |
| GET | `/api/plans/{planId}/assignments/{assignmentId}` | Get assignment detail |
| POST | `/api/plans/{planId}/assignments` | Create assignment |
| PUT | `/api/plans/{planId}/assignments/{assignmentId}` | Update assignment |
| DELETE | `/api/plans/{planId}/assignments/{assignmentId}` | Delete assignment |
| POST | `/api/plans/{planId}/assignments/{assignmentId}/members` | Add assignment member |
| DELETE | `/api/plans/{planId}/assignments/{assignmentId}/members/{userId}` | Remove assignment member |
| PUT | `/api/plans/{planId}/assignments/{assignmentId}/owner` | Transfer assignment ownership |
| GET | `/api/worlds/{id}` | Get a world by ID |
| GET | `/api/worlds` | List all worlds |
| POST | `/api/worlds` | Create a world |
| PUT | `/api/worlds/{id}` | Update a world |
| DELETE | `/api/worlds/{id}` | Delete a world |

## Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :clients:user-client:test
./gradlew :services:camper-service:test

# Full build (compile + test)
./gradlew clean build
```

## Docker

```bash
# Build image
docker build -t camper .

# Run container
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5433 \
  -e DB_NAME=camper_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  camper
```
