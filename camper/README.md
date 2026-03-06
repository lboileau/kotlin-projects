# camper

A web app for interactive camping trip planning. Invite people on a trip and let everyone view trip details and contribute to planning — dates, meal plans, equipment, and more.

This project is in early development. Features will be added incrementally.

## Project Structure

- **libs/common** — Shared logging utilities
- **clients/common** — `Result<T, E>` type, `ClientContext`, error types
- **clients/world-client** — JDBI-based data access (sample scaffold)
- **services/common** — `ApiResponse<T>` shared response type
- **services/camper-service** — Spring Boot REST API (port 8080)
- **databases/camper-db** — PostgreSQL schema, Flyway migrations, seed data

## Prerequisites

- JDK 21+
- Docker

## Initial Setup

```bash
# Start PostgreSQL
cd databases/camper-db && docker-compose up -d

# Run migrations
cd databases/camper-db && flyway -configFiles=flyway.conf migrate

# Seed development data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d camper_db -f databases/camper-db/seed/dev_seed.sql
```

## Quick Start

```bash
./gradlew :services:camper-service:bootRun
```

The service starts on `http://localhost:8080`.

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/worlds/{id}` | Get a world by ID |
| GET | `/api/worlds` | List all worlds |
| POST | `/api/worlds` | Create a new world |
| PUT | `/api/worlds/{id}` | Update a world |
| DELETE | `/api/worlds/{id}` | Delete a world |

## Example

```bash
# Create a world
curl -s -X POST http://localhost:8080/api/worlds \
  -H "Content-Type: application/json" \
  -d '{"name": "Earth", "greeting": "Hello, World!"}'

# List all worlds
curl -s http://localhost:8080/api/worlds
```

## Testing

```bash
# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :clients:world-client:test
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
