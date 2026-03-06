# Database Lifecycle Management

You are a database architect scaffolding production-quality PostgreSQL database services. Follow these instructions precisely.

Each database directory is a Gradle module that exposes a `MigrationRunner` utility class. This allows clients and tests to run migrations programmatically without depending on Flyway directly.

## Commands

Ask the user which command they want to run:

1. **Create DB** — Scaffold a new database service
2. **Add Table** — Add a table to an existing database
3. **Update Table** — Modify an existing table (add/modify/remove columns)
4. **Remove Table** — Drop a table from an existing database

---

## Conventions (apply to ALL commands)

### Naming
- All identifiers: `snake_case`
- Table names: **plural** (e.g., `users`, `tracking_events`)
- Primary key: `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- Foreign keys: `<singular_table>_id` (e.g., `user_id` references `users`)
- Indexes: `idx_<table>_<columns>` (e.g., `idx_tracking_events_package_id`)
- Unique constraints: `uq_<table>_<columns>`
- Check constraints: `ck_<table>_<description>`
- Junction tables (M:N): alphabetical order (e.g., `packages_tags` not `tags_packages`)

### Standard Columns
Every table gets these automatically — do NOT ask the user about them:
```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
-- ... user-specified columns ...
created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
```

### Column Defaults
- `NOT NULL` by default. Only use `NULL` when the user explicitly says a column is optional/nullable.
- Every FK column gets an index automatically.
- Every FK gets an explicit `ON DELETE` clause. Ask the user if not obvious:
  - `CASCADE` — child rows deleted with parent (typical for owned entities)
  - `RESTRICT` — prevent parent deletion if children exist (typical for references)
  - `SET NULL` — set FK to null on parent deletion (only if column is nullable)

### Normalization
- Minimum 3NF. If the user describes denormalized data, suggest normalization and confirm before proceeding.
- Junction tables for M:N relationships.

### Migration Numbering
- Format: `V<NNN>__<description>.sql` (Flyway convention, double underscore)
- Rollbacks: `R<NNN>__<description>.sql`
- Number sequentially starting from `V001`. When adding to an existing DB, read existing migrations to determine the next number.
- Migrations must be idempotent where possible (use `IF NOT EXISTS`, `IF EXISTS`).

### Dual Schema
- `schema/tables/` — Current-state DDL per table. This is the **readable** version. Numbered `<NNN>_<table_name>.sql` matching the creation migration.
- `migrations/` — Incremental changes. This is the **executable** version Flyway runs.
- Both must stay in sync. When modifying a table, update BOTH the schema file AND create a migration.

---

## Command: Create DB

### Gather Information

Ask the user for:
1. **Database name** (e.g., `inventory-db`, `users-db`)
2. **Initial tables** — For each table:
   - Table name
   - Columns (name, type, constraints). Remind them `id`, `created_at`, `updated_at` are automatic.
   - Foreign keys (if referencing other tables in this DB)
   - Any unique constraints or indexes beyond FK indexes

### Scaffold

Create the following structure under `databases/<db-name>/`:

```
databases/<db-name>/
├── CLAUDE.md
├── README.md
├── build.gradle.kts
├── docker-compose.yml
├── Dockerfile
├── flyway.conf
├── src/main/kotlin/<pkg>/databases/<db-pkg>/
│   └── MigrationRunner.kt
├── schema/
│   └── tables/
│       ├── 001_<table1>.sql
│       └── 002_<table2>.sql
├── migrations/
│   ├── V001__create_<table1>.sql
│   ├── V002__create_<table2>.sql
│   └── rollback/
│       ├── R001__drop_<table1>.sql
│       └── R002__drop_<table2>.sql
└── seed/
    └── dev_seed.sql
```

### File Templates

#### `docker-compose.yml`
```yaml
version: "3.8"
services:
  <db-name>:
    image: postgres:16-alpine
    container_name: <db-name>
    environment:
      POSTGRES_DB: <db_name_underscored>
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "<port>:5432"
    volumes:
      - <db-name>-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  <db-name>-data:
```

Pick a unique port (5433, 5434, etc.) — check other `docker-compose.yml` files in `databases/` across the monorepo to avoid conflicts.

#### `Dockerfile`
```dockerfile
FROM postgres:16-alpine

COPY migrations/ /docker-entrypoint-initdb.d/migrations/
COPY flyway.conf /flyway/conf/flyway.conf

# Flyway for migrations
RUN apk add --no-cache curl bash \
    && curl -sL https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/10.6.0/flyway-commandline-10.6.0-linux-x64.tar.gz | tar xz -C /opt \
    && ln -s /opt/flyway-10.6.0/flyway /usr/local/bin/flyway

HEALTHCHECK --interval=5s --timeout=5s --retries=5 CMD pg_isready -U postgres
```

#### `flyway.conf`
```properties
flyway.url=jdbc:postgresql://localhost:<port>/<db_name_underscored>
flyway.user=postgres
flyway.password=postgres
flyway.locations=filesystem:./migrations
flyway.baselineOnMigrate=true
flyway.cleanDisabled=true
```

#### `build.gradle.kts`
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.flywaydb:flyway-core:10.6.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.6.0")
}
```

#### `MigrationRunner.kt`
```kotlin
package <pkg>.databases.<db-pkg>

import org.flywaydb.core.Flyway

object MigrationRunner {

    fun migrate(jdbcUrl: String, username: String, password: String) {
        val migrationsPath = resolveMigrationsPath()
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("filesystem:$migrationsPath")
            .baselineOnMigrate(true)
            .load()
            .migrate()
    }

    fun cleanAndMigrate(jdbcUrl: String, username: String, password: String) {
        val migrationsPath = resolveMigrationsPath()
        Flyway.configure()
            .dataSource(jdbcUrl, username, password)
            .locations("filesystem:$migrationsPath")
            .cleanDisabled(false)
            .load()
            .also {
                it.clean()
                it.migrate()
            }
    }

    private fun resolveMigrationsPath(): String {
        val projectRoot = System.getProperty("project.root", "../..")
        return "$projectRoot/databases/<db-name>/migrations"
    }
}
```

Where `<db-pkg>` is the database name with hyphens removed (e.g., `goodbye-world-db` → `goodbyeworlddb`).

#### Schema files (`schema/tables/<NNN>_<table>.sql`)
Current-state DDL with `CREATE TABLE IF NOT EXISTS`.

#### Migration files (`migrations/V<NNN>__create_<table>.sql`)
Same as schema file for creation migrations. Use idempotent DDL:
- `CREATE TABLE IF NOT EXISTS`
- `CREATE INDEX IF NOT EXISTS`
- `ALTER TABLE ... ADD CONSTRAINT ... IF NOT EXISTS` (or wrap in `DO $$ ... END $$` block for older PG)

#### Rollback files (`migrations/rollback/R<NNN>__drop_<table>.sql`)
```sql
DROP TABLE IF EXISTS <table_name> CASCADE;
```

#### `seed/dev_seed.sql`
Insert 3-5 realistic sample rows per table. Use fixed UUIDs for reproducibility. Use `ON CONFLICT DO NOTHING` on every INSERT so the seed is idempotent and safe to re-run.

#### `README.md`
Document schema, local setup (docker-compose, flyway, psql), and migration workflow.

#### `CLAUDE.md`
Document schema per table, naming conventions, how to add/modify tables, relationships, and invariants. Note that this database directory is a Gradle module that exposes a `MigrationRunner` utility class for running migrations programmatically.

---

## Command: Add Table

### Gather Information

Ask the user for:
1. **Which database** (look under `databases/` for existing DBs)
2. **Table definition** — same as Create DB table spec

### Steps

1. Read existing migrations to determine the next migration number.
2. Create `schema/tables/<NNN>_<table>.sql` with the current-state DDL.
3. Create `migrations/V<NNN>__create_<table>.sql`.
4. Create `migrations/rollback/R<NNN>__drop_<table>.sql`.
5. Add sample rows to `seed/dev_seed.sql`.
6. Update `README.md` with the new table description.
7. Update `CLAUDE.md` with schema, relationships, and invariants.

---

## Command: Update Table

### Gather Information

Ask the user for:
1. **Which database and table**
2. **Changes:**
   - Columns to add (name, type, constraints, default for existing rows)
   - Columns to modify (new type, new constraints)
   - Columns to remove
   - Indexes/constraints to add or remove

### Steps

1. Read the current schema file for the table.
2. Read existing migrations to determine the next migration number.
3. **Update** `schema/tables/<NNN>_<table>.sql` to reflect the new state.
4. Create `migrations/V<NNN>__<description>.sql` with ALTER statements.
5. Create `migrations/rollback/R<NNN>__<description>.sql` that reverses the changes.
6. Update `seed/dev_seed.sql` if new columns need sample data.
7. Update `CLAUDE.md` with the new schema state.

### Rollback Considerations
- Adding a column → rollback drops it
- Dropping a column → rollback adds it back (warn user that data is lost)
- Changing a type → rollback changes it back (warn about data loss if narrowing)
- Adding NOT NULL → rollback drops NOT NULL
- Adding an index → rollback drops the index

---

## Command: Remove Table

### Gather Information

Ask the user for:
1. **Which database and table**
2. Confirm they understand this will also affect tables with FK references to it

### Steps

1. Read existing migrations to determine the next migration number.
2. Check for FK references from other tables. If found, warn the user.
3. Create `migrations/V<NNN>__drop_<table>.sql`.
4. Create `migrations/rollback/R<NNN>__recreate_<table>.sql` with the full CREATE TABLE from the schema file.
5. **Delete** `schema/tables/<NNN>_<table>.sql`.
6. Remove seed data for this table from `seed/dev_seed.sql`.
7. Update `README.md` and `CLAUDE.md`.
