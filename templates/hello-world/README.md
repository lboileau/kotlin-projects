# hello-world

A Kotlin Spring Boot CRUD API for managing worlds.

## What's Included

- **libs/common** — Shared logging utilities
- **clients/common** — `Result<T, E>` type, `ClientContext`, error types (`AppError`, `NotFoundError`, `ValidationError`, `ConflictError`)
- **clients/world-client** — JDBI-based data access client for the `worlds` table (interface + fake for testing)
- **services/common** — `ApiResponse<T>` shared response type
- **services/hello-service** — Spring Boot REST API with world CRUD endpoints
- **databases/hello-world-db** — PostgreSQL schema, Flyway migrations, seed data, docker-compose

## Prerequisites

- JDK 21+
- Docker

## Initial Setup

```bash
# 1. Start PostgreSQL
cd databases/hello-world-db && docker-compose up -d

# 2. Run migrations
flyway -configFiles=databases/hello-world-db/flyway.conf migrate

# 3. Seed dev data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d hello_world_db -f databases/hello-world-db/seed/dev_seed.sql
```

## Quick Start

```bash
./gradlew :services:hello-service:bootRun
```

The service starts on http://localhost:8080.

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/worlds/{id} | Get a world by ID |
| GET | /api/worlds | List all worlds |
| POST | /api/worlds | Create a new world |
| PUT | /api/worlds/{id} | Update a world |
| DELETE | /api/worlds/{id} | Delete a world |

## Example

```bash
# Create a world
curl -X POST http://localhost:8080/api/worlds \
  -H "Content-Type: application/json" \
  -d '{"name": "Pandora", "greeting": "I see you!"}'

# List all worlds
curl http://localhost:8080/api/worlds
```

## Testing

```bash
# Run all tests (requires Docker for Testcontainers)
./gradlew test

# Run specific module tests
./gradlew :clients:world-client:test
./gradlew :services:hello-service:test

# Full build
./gradlew clean build
```

## Docker

```bash
# Build the image
docker build -t hello-world .

# Run the container
docker run -p 8080:8080 \
  -e DB_HOST=host.docker.internal \
  -e DB_PORT=5433 \
  -e DB_NAME=hello_world_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  hello-world
```
