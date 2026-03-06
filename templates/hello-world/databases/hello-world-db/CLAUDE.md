# hello-world-db

This is a Gradle module that provides PostgreSQL database schema, migrations, and a `MigrationRunner` utility class for running Flyway migrations programmatically.

## Schema

### worlds
- `id` UUID PRIMARY KEY DEFAULT gen_random_uuid()
- `name` VARCHAR(100) NOT NULL — UNIQUE constraint `uq_worlds_name`, index `idx_worlds_name`
- `greeting` VARCHAR(255) NOT NULL
- `created_at` TIMESTAMPTZ NOT NULL DEFAULT now()
- `updated_at` TIMESTAMPTZ NOT NULL DEFAULT now()

## Naming Conventions
- All identifiers: snake_case
- Table names: plural
- Primary key: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- Foreign keys: `<singular_table>_id`
- Indexes: `idx_<table>_<columns>`
- Unique constraints: `uq_<table>_<columns>`

## Adding/Modifying Tables
1. Create or update the schema file in `schema/tables/<NNN>_<table>.sql` (current-state DDL)
2. Create a migration in `migrations/V<NNN>__<description>.sql` (incremental change)
3. Create a rollback in `migrations/rollback/R<NNN>__<description>.sql`
4. Update seed data in `seed/dev_seed.sql` if needed
5. Keep schema files and migrations in sync

## Migration Numbering
- Format: `V<NNN>__<description>.sql` (Flyway convention, double underscore)
- Rollbacks: `R<NNN>__<description>.sql`
- Current highest migration: V001

## MigrationRunner
The `MigrationRunner` object in `src/main/kotlin/com/example/databases/helloworlddb/MigrationRunner.kt` provides:
- `migrate(jdbcUrl, username, password)` — run pending migrations
- `cleanAndMigrate(jdbcUrl, username, password)` — drop all objects and re-migrate (for tests)

## Connection
- Port: 5433
- Database: hello_world_db
- User: postgres / postgres
