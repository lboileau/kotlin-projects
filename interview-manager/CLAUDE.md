# Interview Manager

Kotlin + Spring Boot API service for managing worlds.

## Project Structure

```
interview-manager/
├── clients/
│   ├── common/              # Result<T,E>, ClientContext, error types
│   └── world-client/        # JDBI-based CRUD client for worlds table
├── libs/
│   └── common/              # Logging utilities
├── services/
│   ├── common/              # ApiResponse wrapper
│   └── interview-service/   # Spring Boot API service (port 8080)
├── databases/
│   └── interview-manager-db/ # Schema, migrations, seeds, docker-compose
├── build.gradle.kts          # Root build (plugins apply false)
├── settings.gradle.kts       # Module includes
├── Dockerfile                # Multi-stage build for interview-service
├── start.sh                  # Start DB + service
└── stop.sh                   # Stop everything
```

## Module Layout

- **clients/** — Data access & external API clients (I/O). Package: `com.acmo.clients.<name>`
- **libs/** — Shared utilities, types, helpers (no I/O). Package: `com.acmo.libs.<name>`
- **services/** — Deployable apps. Package: `com.acmo.services.<name>`
- **databases/** — Schemas, migrations, seeds, docker-compose

Rule of thumb: "Does it do I/O? → `clients/`. Pure logic/types? → `libs/`."

## Tech Stack

- Gradle 8.12, Kotlin 2.1.10, Java 21
- Spring Boot 3.4.3, Spring Dependency Management 1.1.7
- JDBI 3.47.0 (client data access)
- Flyway 10.6.0 (migrations)
- PostgreSQL 16 (database)
- Testcontainers (integration tests)
- JUnit 5.11.4, AssertJ 3.27.3

## Architecture

- **Dependency direction:** services → clients → libs (never the reverse)
- **DI approach:** `@Configuration` bean classes, no `@Service` annotations on domain classes
- **Error handling:** `Result<T, E>` sealed class, never throw for expected failures
- **Testing:** Testcontainers for integration tests, FakeClient for unit tests
- **Client pattern:** Interface + Facade + Operations + Validations + Factory + Fake (in testFixtures)

## Key Conventions

- **Root package:** `com.acmo`
- **Package naming:** `com.acmo.clients.<client>`, `com.acmo.libs.<lib>`, `com.acmo.services.<service>`
- **DB naming:** snake_case, plural tables, UUID PKs, `created_at`/`updated_at` timestamps
- **Kotlin naming:** camelCase properties, PascalCase classes
- **Client interface + fake pattern:** Every client exposes an interface + fake (testFixtures) for testing
- **Validations:** 1:1 with operations/actions, validate before execute

## Quick Start

```bash
# Start everything (DB + service)
./start.sh

# Or manually:
cd databases/interview-manager-db && docker compose up -d
flyway -configFiles=databases/interview-manager-db/flyway.conf migrate
psql -h localhost -p 5433 -U postgres -d interview_manager_db -f databases/interview-manager-db/seed/dev_seed.sql
./gradlew :services:interview-service:bootRun
```
