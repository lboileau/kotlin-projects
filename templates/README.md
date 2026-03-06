# Templates

Project templates and skills for bootstrapping Kotlin Spring Boot applications.

## Structure

```
templates/
├── hello-world/    # Sample project — Kotlin Spring Boot CRUD API
└── skills/         # Claude Code skill definitions for project generation
```

## hello-world

A complete Kotlin Spring Boot CRUD API for managing worlds. Includes:

- **libs/common** — Shared logging utilities
- **clients/common** — `Result<T, E>` type, error types
- **clients/world-client** — JDBI data access client with interface + fake
- **services/common** — `ApiResponse<T>` shared response type
- **services/hello-service** — REST API with 5 CRUD endpoints
- **databases/hello-world-db** — PostgreSQL schema, Flyway migrations, seed data

**Tech:** Gradle 8.12, Kotlin 2.1.10, Java 21, Spring Boot 3.4.3, JDBI, Flyway, Testcontainers

See [hello-world/README.md](hello-world/README.md) for setup and usage.

## Skills

Claude Code skills that automate project scaffolding:

| Skill | Description |
|-------|-------------|
| `create-project` | Bootstraps a full project from scratch |
| `service-manager` | Creates clients, libs, services, and Gradle configs |
| `db-manager` | Creates database schemas, migrations, docker-compose, flyway |
| `create-acceptance-tests` | Creates test infrastructure, fixtures, acceptance tests |
| `build-feature` | Adds a new feature (entity + CRUD) to an existing project |

### Usage

From Claude Code, invoke a skill with:

```
/create-project
/service-manager
/db-manager
/create-acceptance-tests
/build-feature
```
