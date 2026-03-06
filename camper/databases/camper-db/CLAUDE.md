# camper-db

PostgreSQL database module for the Camper project. This is a Gradle module that exposes a `MigrationRunner` utility class (`com.acme.databases.camperdb.MigrationRunner`) for running migrations programmatically.

## Schema

### worlds

```sql
CREATE TABLE worlds (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    greeting   VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_worlds_name UNIQUE (name)
);
CREATE INDEX idx_worlds_name ON worlds (name);
```

## Naming Conventions

- All identifiers: `snake_case`
- Table names: plural (e.g., `worlds`)
- Primary key: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- Foreign keys: `<singular_table>_id`
- Indexes: `idx_<table>_<columns>`
- Unique constraints: `uq_<table>_<columns>`
- Check constraints: `ck_<table>_<description>`

## Standard Columns

Every table automatically includes:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`
- `updated_at TIMESTAMPTZ NOT NULL DEFAULT now()`

## Adding a New Table

1. Create `schema/tables/<NNN>_<table>.sql` with current-state DDL.
2. Create `migrations/V<NNN>__create_<table>.sql` with idempotent DDL.
3. Create `migrations/rollback/R<NNN>__drop_<table>.sql`.
4. Add seed data to `seed/dev_seed.sql`.
5. Update this file with the new table schema.

## Modifying a Table

1. Update the schema file in `schema/tables/`.
2. Create a new migration `migrations/V<NNN>__<description>.sql` with ALTER statements.
3. Create a rollback `migrations/rollback/R<NNN>__<description>.sql`.
4. Update seed data if needed.
5. Update this file.

## Migration Numbering

- Format: `V<NNN>__<description>.sql` (Flyway convention, double underscore)
- Rollbacks: `R<NNN>__<description>.sql`
- Sequential numbering starting from V001.

## Connection Details (local)

- Host: `localhost`
- Port: `5433`
- Database: `camper_db`
- User: `postgres`
- Password: `postgres`
- JDBC URL: `jdbc:postgresql://localhost:5433/camper_db`

## Relationships

No foreign key relationships yet. The `worlds` table is standalone.

## Invariants

- World names must be unique (enforced by `uq_worlds_name`).
- All columns are NOT NULL.
