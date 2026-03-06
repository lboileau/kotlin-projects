# hello-world-db

PostgreSQL database for the Hello World project.

## Schema

### worlds

| Column     | Type           | Constraints                          |
|------------|----------------|--------------------------------------|
| id         | UUID           | PRIMARY KEY, DEFAULT gen_random_uuid() |
| name       | VARCHAR(100)   | NOT NULL, UNIQUE (uq_worlds_name)    |
| greeting   | VARCHAR(255)   | NOT NULL                             |
| created_at | TIMESTAMPTZ    | NOT NULL, DEFAULT now()              |
| updated_at | TIMESTAMPTZ    | NOT NULL, DEFAULT now()              |

**Indexes:** `idx_worlds_name` on `name`

## Local Setup

### Start the database

```bash
docker-compose up -d
```

This starts PostgreSQL on port **5433** with database `hello_world_db`.

### Connect via psql

```bash
psql -h localhost -p 5433 -U postgres -d hello_world_db
```

### Run migrations (Flyway CLI)

```bash
flyway -configFiles=flyway.conf migrate
```

### Run migrations (programmatic)

Use the `MigrationRunner` utility from your Kotlin code:

```kotlin
MigrationRunner.migrate(
    jdbcUrl = "jdbc:postgresql://localhost:5433/hello_world_db",
    username = "postgres",
    password = "postgres"
)
```

### Seed data

After running migrations, load dev seed data:

```bash
psql -h localhost -p 5433 -U postgres -d hello_world_db -f seed/dev_seed.sql
```

## Migration Workflow

1. Create a new migration file: `migrations/V<NNN>__<description>.sql`
2. Create the corresponding rollback: `migrations/rollback/R<NNN>__<description>.sql`
3. Update the schema file in `schema/tables/` to reflect the new state
4. Run `flyway migrate` to apply
