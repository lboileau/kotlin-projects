# Interview Manager

Kotlin + Spring Boot API service for managing worlds.

## Quick Start

```bash
./start.sh    # Start DB, run migrations, seed data, start service (opens browser)
./stop.sh     # Stop service and DB
```

## Prerequisites

- JDK 21+
- Docker
- Flyway CLI
- psql (PostgreSQL client)

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
```

## Manual Setup

```bash
# 1. Start database
cd databases/interview-manager-db && docker compose up -d && cd ../..

# 2. Run migrations
flyway -configFiles=databases/interview-manager-db/flyway.conf migrate

# 3. Seed dev data
PGPASSWORD=postgres psql -h localhost -p 5433 -U postgres -d interview_manager_db -f databases/interview-manager-db/seed/dev_seed.sql

# 4. Start service
./gradlew :services:interview-service:bootRun
```

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/worlds/{id}` | Get a world by ID |
| GET | `/api/worlds` | List all worlds |
| POST | `/api/worlds` | Create a new world |
| PUT | `/api/worlds/{id}` | Update a world |
| DELETE | `/api/worlds/{id}` | Delete a world |

## Testing

```bash
# Run all tests
./gradlew clean build

# Run specific module tests
./gradlew :clients:world-client:test
./gradlew :services:interview-service:test
```

## Docker

```bash
# Build image
docker build -t interview-manager .

# Run (requires database to be running)
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/interview_manager_db \
  -e DB_USER=postgres \
  -e DB_PASSWORD=postgres \
  interview-manager
```
