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

### users

```sql
CREATE TABLE users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    username   VARCHAR(100),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);
CREATE INDEX idx_users_email ON users (email);
```

### plans

```sql
CREATE TABLE plans (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    visibility VARCHAR(10)  NOT NULL DEFAULT 'private',
    owner_id   UUID         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_plans_visibility CHECK (visibility IN ('public', 'private')),
    CONSTRAINT fk_plans_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
```

### plan_members

```sql
CREATE TABLE plan_members (
    plan_id    UUID        NOT NULL,
    user_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (plan_id, user_id),
    CONSTRAINT fk_plan_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_members_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX idx_plan_members_user_id ON plan_members (user_id);
```

### items

```sql
CREATE TABLE items (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id    UUID         REFERENCES plans(id) ON DELETE CASCADE,
    user_id    UUID         REFERENCES users(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    category   VARCHAR(50)  NOT NULL,
    quantity   INTEGER      NOT NULL DEFAULT 1,
    packed     BOOLEAN      NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_items_single_owner CHECK (
        (plan_id IS NOT NULL AND user_id IS NULL) OR
        (plan_id IS NULL AND user_id IS NOT NULL)
    )
);
CREATE INDEX idx_items_plan_id ON items(plan_id);
CREATE INDEX idx_items_user_id ON items(user_id);
```

### itineraries

```sql
CREATE TABLE itineraries (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL    DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL    DEFAULT now(),

    CONSTRAINT uq_itineraries_plan_id UNIQUE (plan_id),
    CONSTRAINT fk_itineraries_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);
```

### itinerary_events

```sql
CREATE TABLE itinerary_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id   UUID         NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    details        TEXT,
    event_at       TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT fk_itinerary_events_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id) ON DELETE CASCADE
);
CREATE INDEX idx_itinerary_events_itinerary_id ON itinerary_events (itinerary_id);
```

### assignments

```sql
CREATE TABLE assignments (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id       UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    type          VARCHAR(10)  NOT NULL,
    max_occupancy INT          NOT NULL,
    owner_id      UUID         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_assignments_type CHECK (type IN ('tent', 'canoe')),
    CONSTRAINT ck_assignments_max_occupancy CHECK (max_occupancy > 0),
    CONSTRAINT uq_assignments_plan_id_name_type UNIQUE (plan_id, name, type),
    CONSTRAINT fk_assignments_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
CREATE INDEX idx_assignments_plan_id ON assignments (plan_id);
CREATE INDEX idx_assignments_owner_id ON assignments (owner_id);
```

A trigger (`trg_transfer_assignment_ownership`) transfers assignment ownership to the plan owner when the owning user is deleted.

### assignment_members

```sql
CREATE TABLE assignment_members (
    assignment_id   UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    plan_id         UUID        NOT NULL,
    assignment_type VARCHAR(10) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (assignment_id, user_id),
    CONSTRAINT ck_assignment_members_type CHECK (assignment_type IN ('tent', 'canoe')),
    CONSTRAINT uq_assignment_members_plan_id_user_id_type UNIQUE (plan_id, user_id, assignment_type),
    CONSTRAINT fk_assignment_members_assignment FOREIGN KEY (assignment_id) REFERENCES assignments (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);
CREATE INDEX idx_assignment_members_user_id ON assignment_members (user_id);
CREATE INDEX idx_assignment_members_plan_id ON assignment_members (plan_id);
CREATE INDEX idx_assignment_members_assignment_id ON assignment_members (assignment_id);
```

## Relationships

- `plans.owner_id` → `users.id` (FK)
- `plan_members.plan_id` → `plans.id` (FK, CASCADE on delete)
- `plan_members.user_id` → `users.id` (FK)
- `items.plan_id` → `plans.id` (FK, CASCADE on delete)
- `items.user_id` → `users.id` (FK, CASCADE on delete)
- `itineraries.plan_id` → `plans.id` (FK, CASCADE on delete, UNIQUE — 1:1 with plans)
- `itinerary_events.itinerary_id` → `itineraries.id` (FK, CASCADE on delete)
- `assignments.plan_id` → `plans.id` (FK, CASCADE on delete)
- `assignments.owner_id` → `users.id` (FK; ownership transfers to plan owner on user deletion via trigger)
- `assignment_members.assignment_id` → `assignments.id` (FK, CASCADE on delete)
- `assignment_members.user_id` → `users.id` (FK, CASCADE on delete)
- `assignment_members.plan_id` → `plans.id` (FK, CASCADE on delete)

## Invariants

- World names must be unique (enforced by `uq_worlds_name`).
- User emails must be unique (enforced by `uq_users_email`).
- Plan visibility must be 'public' or 'private' (enforced by `ck_plans_visibility`).
- Plan members are unique per plan (composite PK `plan_id, user_id`).
- Deleting a plan cascades to plan_members, items, itineraries, assignments, and assignment_members.
- Deleting a user cascades to items and assignment_members.
- `username` in users is nullable (auto-created users may not have one).
- Each item must have exactly one owner — either `plan_id` or `user_id` (enforced by `chk_items_single_owner`).
- Each plan has at most one itinerary (enforced by `uq_itineraries_plan_id`).
- Deleting a plan cascades to itineraries, which cascades to itinerary_events.
- `description` and `details` in itinerary_events are nullable.
- Assignment type must be 'tent' or 'canoe' (enforced by `ck_assignments_type`).
- Assignment max_occupancy must be > 0 (enforced by `ck_assignments_max_occupancy`).
- Assignment names are unique per plan and type (enforced by `uq_assignments_plan_id_name_type`).
- A user can belong to only one assignment of each type per plan (enforced by `uq_assignment_members_plan_id_user_id_type`).
- Deleting an assignment cascades to its assignment_members.
- Deleting a user cascades to their assignment_members and transfers ownership of their assignments to the plan owner.
