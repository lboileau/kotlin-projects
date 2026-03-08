# camper

A web app for interactive camping trip planning. Invite people on a trip and let everyone view details and contribute to planning (dates, meal plans, equipment, etc.). Early development — features added incrementally.

## Project Structure

```
camper/
├── build.gradle.kts          # Root build config
├── settings.gradle.kts       # Module includes
├── gradle.properties
├── Dockerfile
├── libs/
│   └── common/               # Shared utilities (no I/O)
├── clients/
│   ├── common/               # Result type, error types, ClientContext
│   ├── world-client/         # JDBI data access for worlds table
│   ├── user-client/          # JDBI data access for users table
│   ├── plan-client/          # JDBI data access for plans & plan_members tables
│   ├── item-client/          # JDBI data access for items table
│   ├── itinerary-client/     # JDBI data access for itineraries & itinerary_events tables
│   └── assignment-client/    # JDBI data access for assignments & assignment_members tables
├── services/
│   ├── common/               # ApiResponse shared type
│   └── camper-service/       # Spring Boot REST API
└── databases/
    └── camper-db/            # Schema, migrations, seeds, docker-compose
```

## Module Layout

- **clients/** — Data access & external API clients (I/O). Package: `com.acme.clients.<name>`
- **libs/** — Shared utilities, types, helpers (no I/O). Package: `com.acme.libs.<name>`
- **services/** — Deployable Spring Boot apps. Package: `com.acme.services.<name>`
- **databases/** — SQL schemas, Flyway migrations, seeds, docker-compose

Rule of thumb: "Does it do I/O? → `clients/`. Pure logic/types? → `libs/`."

## Tech Stack

- **Build:** Gradle 8.12 (Kotlin DSL)
- **Language:** Kotlin 2.1.10 / Java 21
- **Framework:** Spring Boot 3.4.3
- **Database:** PostgreSQL 16 via JDBI 3.x
- **Migrations:** Flyway
- **Testing:** JUnit 5, AssertJ, Testcontainers

## Architecture

- **Dependency direction:** services → clients → libs (never reverse)
- **DI:** Spring `@Configuration` beans. No `@Service` annotations on domain classes.
- **Error handling:** `Result<T, E>` sealed class. Never throw for expected failures.
- **Client pattern:** Interface + internal facade + operations + param objects. Factory reads env vars. Fake in testFixtures.
- **Service pattern:** Actions (validate → convert → call client) composed into a Service facade. Validations are 1:1 with actions.
- **Testing:** Unit tests with FakeClient, acceptance tests with Testcontainers + @SpringBootTest.

## Key Conventions

- **Root package:** `com.acme`
- **DB naming:** snake_case, plural tables, UUID PKs, `created_at`/`updated_at` timestamps
- **Kotlin naming:** camelCase properties, PascalCase classes
- **Client interface + fake pattern:** Every client exposes an interface and a fake (testFixtures) for testing

## Quick Start

```bash
# Start database
cd databases/camper-db && docker-compose up -d

# Run migrations
cd databases/camper-db && flyway -configFiles=flyway.conf migrate

# Seed dev data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db -f databases/camper-db/seed/dev_seed.sql

# Run the service
./gradlew :services:camper-service:bootRun

# Run all tests
./gradlew test

# Full build
./gradlew clean build
```

## Production Deployment

Hosted on [Railway](https://railway.com) — project **proactive-quietude**.

- **URL:** https://camper-service-production.up.railway.app
- **Dashboard:** https://railway.com/project/a8deb41f-1bc0-4d2e-a986-a1e07d3c1253
- **Services:** `camper-service` (Spring Boot app) + `Postgres-MuMx` (managed PostgreSQL)

### How it works

- **Dockerfile** builds in three stages: Node (webapp), JDK (Spring Boot jar), JRE (runtime)
- **Webapp** (`webapp/dist`) is served as static files from `/app/static/` by Spring Boot
- **SPA routing:** `WebConfig.kt` forwards non-API, non-static routes to `index.html` for React Router
- **Migrations:** Flyway runs automatically on startup. Migration files live in `databases/camper-db/migrations/` (single source of truth) and are copied to the classpath at build time via Gradle `processResources`
- **Environment variables:** `DB_URL`, `DB_USER`, `DB_PASSWORD` configured on the camper-service in Railway

### Deploying

```bash
# Ensure you're linked to the right service
railway service camper-service

# Deploy from local (uses Dockerfile)
railway up
```

### Adding new migrations

1. Add the migration SQL file to `databases/camper-db/migrations/` (e.g. `V010__create_foo.sql`)
2. That's it — Gradle copies it to the classpath at build time, Flyway applies it on next deploy
