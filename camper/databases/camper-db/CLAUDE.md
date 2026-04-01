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
    role       VARCHAR(20) NOT NULL DEFAULT 'member',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (plan_id, user_id),
    CONSTRAINT fk_plan_members_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_members_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT ck_plan_members_role CHECK (role IN ('member', 'manager'))
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
    packed       BOOLEAN      NOT NULL DEFAULT false,
    gear_pack_id UUID,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_items_owner CHECK (plan_id IS NOT NULL),
    CONSTRAINT fk_items_gear_pack FOREIGN KEY (gear_pack_id) REFERENCES gear_packs (id) ON DELETE SET NULL
);
CREATE INDEX idx_items_plan_id ON items(plan_id);
CREATE INDEX idx_items_user_id ON items(user_id);
CREATE INDEX idx_items_plan_id_user_id ON items (plan_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX idx_items_gear_pack_id ON items (gear_pack_id);
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
    category       VARCHAR(50)  NOT NULL    DEFAULT 'other',
    estimated_cost DECIMAL(10,2),
    location       VARCHAR(500),
    event_end_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT fk_itinerary_events_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id) ON DELETE CASCADE,
    CONSTRAINT ck_itinerary_events_category CHECK (category IN ('travel', 'accommodation', 'activity', 'meal', 'other'))
);
CREATE INDEX idx_itinerary_events_itinerary_id ON itinerary_events (itinerary_id);
```

### itinerary_event_links

```sql
CREATE TABLE itinerary_event_links (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL,
    url        TEXT        NOT NULL,
    label      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_event_links_event FOREIGN KEY (event_id) REFERENCES itinerary_events (id) ON DELETE CASCADE
);
CREATE INDEX idx_event_links_event_id ON itinerary_event_links (event_id);
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

### ingredients

```sql
CREATE TABLE IF NOT EXISTS ingredients (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    default_unit VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_ingredients_name UNIQUE (name),
    CONSTRAINT ck_ingredients_category CHECK (category IN ('produce', 'dairy', 'meat', 'seafood', 'pantry', 'spice', 'condiment', 'frozen', 'bakery', 'other')),
    CONSTRAINT ck_ingredients_default_unit CHECK (default_unit IN ('g', 'kg', 'ml', 'l', 'tsp', 'tbsp', 'cup', 'oz', 'lb', 'pieces', 'whole', 'bunch', 'can', 'clove', 'pinch', 'slice', 'sprig'))
);
CREATE INDEX IF NOT EXISTS idx_ingredients_name ON ingredients (name);
```

### recipes

```sql
CREATE TABLE IF NOT EXISTS recipes (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    web_link       TEXT,
    base_servings  INT          NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft',
    created_by     UUID         NOT NULL,
    duplicate_of_id UUID,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipes_base_servings CHECK (base_servings > 0),
    CONSTRAINT ck_recipes_status CHECK (status IN ('draft', 'published')),
    CONSTRAINT fk_recipes_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recipes_duplicate_of FOREIGN KEY (duplicate_of_id) REFERENCES recipes (id) ON DELETE SET NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_recipes_web_link ON recipes (web_link) WHERE web_link IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recipes_status ON recipes (status);
CREATE INDEX IF NOT EXISTS idx_recipes_created_by ON recipes (created_by);
CREATE INDEX IF NOT EXISTS idx_recipes_duplicate_of_id ON recipes (duplicate_of_id);
```

### recipe_ingredients

```sql
CREATE TABLE IF NOT EXISTS recipe_ingredients (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id                UUID         NOT NULL,
    ingredient_id            UUID,
    original_text            TEXT,
    quantity                 NUMERIC      NOT NULL,
    unit                     VARCHAR(20)  NOT NULL,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'approved',
    matched_ingredient_id    UUID,
    suggested_ingredient_name TEXT,
    review_flags             JSONB        NOT NULL DEFAULT '[]',
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipe_ingredients_quantity CHECK (quantity > 0),
    CONSTRAINT ck_recipe_ingredients_status CHECK (status IN ('pending_review', 'approved')),
    CONSTRAINT fk_recipe_ingredients_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_ingredients_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recipe_ingredients_matched_ingredient FOREIGN KEY (matched_ingredient_id) REFERENCES ingredients (id) ON DELETE SET NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_recipe_ingredients_recipe_id_ingredient_id ON recipe_ingredients (recipe_id, ingredient_id) WHERE ingredient_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_recipe_id ON recipe_ingredients (recipe_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_ingredient_id ON recipe_ingredients (ingredient_id);
CREATE INDEX IF NOT EXISTS idx_recipe_ingredients_matched_ingredient_id ON recipe_ingredients (matched_ingredient_id);
```

### meal_plans

```sql
CREATE TABLE IF NOT EXISTS meal_plans (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id            UUID,
    name               VARCHAR(255) NOT NULL,
    servings           INT          NOT NULL,
    scaling_mode       VARCHAR(20)  NOT NULL DEFAULT 'fractional',
    is_template        BOOLEAN      NOT NULL DEFAULT false,
    source_template_id UUID,
    created_by         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plans_servings CHECK (servings > 0),
    CONSTRAINT ck_meal_plans_scaling_mode CHECK (scaling_mode IN ('fractional', 'round_up')),
    CONSTRAINT fk_meal_plans_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_meal_plans_source_template FOREIGN KEY (source_template_id) REFERENCES meal_plans (id) ON DELETE SET NULL,
    CONSTRAINT fk_meal_plans_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_meal_plans_plan_id ON meal_plans (plan_id) WHERE plan_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_meal_plans_plan_id ON meal_plans (plan_id);
CREATE INDEX IF NOT EXISTS idx_meal_plans_is_template ON meal_plans (is_template);
CREATE INDEX IF NOT EXISTS idx_meal_plans_source_template_id ON meal_plans (source_template_id);
CREATE INDEX IF NOT EXISTS idx_meal_plans_created_by ON meal_plans (created_by);
```

### meal_plan_days

```sql
CREATE TABLE IF NOT EXISTS meal_plan_days (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id UUID        NOT NULL,
    day_number   INT         NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plan_days_day_number CHECK (day_number > 0),
    CONSTRAINT uq_meal_plan_days_meal_plan_id_day_number UNIQUE (meal_plan_id, day_number),
    CONSTRAINT fk_meal_plan_days_meal_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_meal_plan_days_meal_plan_id ON meal_plan_days (meal_plan_id);
```

### meal_plan_recipes

```sql
CREATE TABLE IF NOT EXISTS meal_plan_recipes (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_day_id UUID        NOT NULL,
    meal_type        VARCHAR(20) NOT NULL,
    recipe_id        UUID        NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plan_recipes_meal_type CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
    CONSTRAINT fk_meal_plan_recipes_meal_plan_day FOREIGN KEY (meal_plan_day_id) REFERENCES meal_plan_days (id) ON DELETE CASCADE,
    CONSTRAINT fk_meal_plan_recipes_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_meal_plan_recipes_meal_plan_day_id ON meal_plan_recipes (meal_plan_day_id);
CREATE INDEX IF NOT EXISTS idx_meal_plan_recipes_recipe_id ON meal_plan_recipes (recipe_id);
```

### shopping_list_purchases

```sql
CREATE TABLE IF NOT EXISTS shopping_list_purchases (
    id                 UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id       UUID        NOT NULL,
    ingredient_id      UUID        NOT NULL,
    unit               VARCHAR(20) NOT NULL,
    quantity_purchased NUMERIC     NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_shopping_list_purchases_quantity CHECK (quantity_purchased >= 0),
    CONSTRAINT uq_shopping_list_purchases_meal_plan_ingredient_unit UNIQUE (meal_plan_id, ingredient_id, unit),
    CONSTRAINT fk_shopping_list_purchases_meal_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_list_purchases_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_shopping_list_purchases_meal_plan_id ON shopping_list_purchases (meal_plan_id);
CREATE INDEX IF NOT EXISTS idx_shopping_list_purchases_ingredient_id ON shopping_list_purchases (ingredient_id);
```

### log_book_faqs

```sql
CREATE TABLE IF NOT EXISTS log_book_faqs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id        UUID        NOT NULL,
    question       TEXT        NOT NULL,
    asked_by_id    UUID        NOT NULL,
    answer         TEXT,
    answered_by_id UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_log_book_faqs_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_log_book_faqs_asked_by FOREIGN KEY (asked_by_id) REFERENCES users (id),
    CONSTRAINT fk_log_book_faqs_answered_by FOREIGN KEY (answered_by_id) REFERENCES users (id)
);
CREATE INDEX IF NOT EXISTS idx_log_book_faqs_plan_id ON log_book_faqs (plan_id);
```

### log_book_journal_entries

```sql
CREATE TABLE IF NOT EXISTS log_book_journal_entries (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    page_number INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_log_book_journal_entries_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_log_book_journal_entries_user FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_id ON log_book_journal_entries (plan_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_page ON log_book_journal_entries (plan_id, page_number);
```

## Relationships

- `plans.owner_id` → `users.id` (FK)
- `plan_members.plan_id` → `plans.id` (FK, CASCADE on delete)
- `plan_members.user_id` → `users.id` (FK)
- `items.plan_id` → `plans.id` (FK, CASCADE on delete)
- `items.user_id` → `users.id` (FK, CASCADE on delete)
- `items.gear_pack_id` → `gear_packs.id` (FK, SET NULL on delete — nullable)
- `itineraries.plan_id` → `plans.id` (FK, CASCADE on delete, UNIQUE — 1:1 with plans)
- `itinerary_events.itinerary_id` → `itineraries.id` (FK, CASCADE on delete)
- `itinerary_event_links.event_id` → `itinerary_events.id` (FK, CASCADE on delete)
- `assignments.plan_id` → `plans.id` (FK, CASCADE on delete)
- `assignments.owner_id` → `users.id` (FK; ownership transfers to plan owner on user deletion via trigger)
- `assignment_members.assignment_id` → `assignments.id` (FK, CASCADE on delete)
- `assignment_members.user_id` → `users.id` (FK, CASCADE on delete)
- `assignment_members.plan_id` → `plans.id` (FK, CASCADE on delete)
- `recipes.created_by` → `users.id` (FK, RESTRICT on delete)
- `recipes.duplicate_of_id` → `recipes.id` (FK, SET NULL on delete — self-referential)
- `recipe_ingredients.recipe_id` → `recipes.id` (FK, CASCADE on delete)
- `recipe_ingredients.ingredient_id` → `ingredients.id` (FK, RESTRICT on delete — nullable)
- `recipe_ingredients.matched_ingredient_id` → `ingredients.id` (FK, SET NULL on delete — nullable)
- `meal_plans.plan_id` → `plans.id` (FK, CASCADE on delete — nullable, UNIQUE when not null)
- `meal_plans.source_template_id` → `meal_plans.id` (FK, SET NULL on delete — self-referential, nullable)
- `meal_plans.created_by` → `users.id` (FK, RESTRICT on delete)
- `meal_plan_days.meal_plan_id` → `meal_plans.id` (FK, CASCADE on delete)
- `meal_plan_recipes.meal_plan_day_id` → `meal_plan_days.id` (FK, CASCADE on delete)
- `meal_plan_recipes.recipe_id` → `recipes.id` (FK, CASCADE on delete)
- `shopping_list_purchases.meal_plan_id` → `meal_plans.id` (FK, CASCADE on delete)
- `shopping_list_purchases.ingredient_id` → `ingredients.id` (FK, CASCADE on delete)
- `log_book_faqs.plan_id` → `plans.id` (FK, CASCADE on delete)
- `log_book_faqs.asked_by_id` → `users.id` (FK)
- `log_book_faqs.answered_by_id` → `users.id` (FK, nullable)
- `log_book_journal_entries.plan_id` → `plans.id` (FK, CASCADE on delete)
- `log_book_journal_entries.user_id` → `users.id` (FK)

## Invariants

- World names must be unique (enforced by `uq_worlds_name`).
- User emails must be unique (enforced by `uq_users_email`).
- Plan visibility must be 'public' or 'private' (enforced by `ck_plans_visibility`).
- Plan members are unique per plan (composite PK `plan_id, user_id`).
- Plan member role must be 'member' or 'manager' (enforced by `ck_plan_members_role`). Defaults to 'member'.
- Deleting a plan cascades to plan_members, items, itineraries, assignments, and assignment_members.
- Deleting a user cascades to items and assignment_members.
- `username` in users is nullable (auto-created users may not have one).
- Each item must belong to a plan (`plan_id` is required). `user_id` is optional — set for personal gear (enforced by `chk_items_owner`).
- `gear_pack_id` is nullable — items without a gear pack are ungrouped. Deleting a gear pack sets `gear_pack_id` to NULL on its items (SET NULL on delete).
- Each plan has at most one itinerary (enforced by `uq_itineraries_plan_id`).
- Deleting a plan cascades to itineraries, which cascades to itinerary_events.
- `description` and `details` in itinerary_events are nullable.
- Itinerary event category must be one of: travel, accommodation, activity, meal, other (enforced by `ck_itinerary_events_category`). Defaults to 'other'.
- `estimated_cost`, `location`, and `event_end_at` in itinerary_events are nullable.
- Deleting an itinerary event cascades to its itinerary_event_links.
- `label` in itinerary_event_links is nullable.
- Assignment type must be 'tent' or 'canoe' (enforced by `ck_assignments_type`).
- Assignment max_occupancy must be > 0 (enforced by `ck_assignments_max_occupancy`).
- Assignment names are unique per plan and type (enforced by `uq_assignments_plan_id_name_type`).
- A user can belong to only one assignment of each type per plan (enforced by `uq_assignment_members_plan_id_user_id_type`).
- Deleting an assignment cascades to its assignment_members.
- Deleting a user cascades to their assignment_members and transfers ownership of their assignments to the plan owner.
- Ingredient names must be unique globally (enforced by `uq_ingredients_name`).
- Recipe `web_link` must be unique when set (partial unique index `uq_recipes_web_link`).
- Recipe `base_servings` must be > 0 (enforced by `ck_recipes_base_servings`).
- Recipe status must be 'draft' or 'published' (enforced by `ck_recipes_status`).
- A recipe_ingredient `(recipe_id, ingredient_id)` pair must be unique when `ingredient_id` is set (partial unique index).
- Deleting a recipe cascades to its recipe_ingredients.
- Deleting a user is restricted if they created any recipes.
- Meal plan `servings` must be > 0 (enforced by `ck_meal_plans_servings`).
- Meal plan `scaling_mode` must be 'fractional' or 'round_up' (enforced by `ck_meal_plans_scaling_mode`).
- A plan can have at most one meal plan (enforced by partial unique index `uq_meal_plans_plan_id` WHERE plan_id IS NOT NULL).
- If `is_template = true`, `plan_id` should be null (application-enforced invariant).
- Meal plan day numbers must be > 0 (enforced by `ck_meal_plan_days_day_number`).
- Day numbers are unique within a meal plan (enforced by `uq_meal_plan_days_meal_plan_id_day_number`).
- Meal type must be 'breakfast', 'lunch', 'dinner', or 'snack' (enforced by `ck_meal_plan_recipes_meal_type`).
- Deleting a meal plan cascades to meal_plan_days, meal_plan_recipes (via days), and shopping_list_purchases.
- Deleting a recipe cascades to meal_plan_recipes referencing it.
- Shopping list purchase `quantity_purchased` must be >= 0 (enforced by `ck_shopping_list_purchases_quantity`).
- Shopping list purchases are unique per `(meal_plan_id, ingredient_id, unit)` (enforced by `uq_shopping_list_purchases_meal_plan_ingredient_unit`).
- Deleting a user is restricted if they created any meal plans.
- Deleting a plan cascades to log_book_faqs and log_book_journal_entries.
- `answer` and `answered_by_id` in log_book_faqs are nullable (unanswered until a manager replies).
- Journal entry page numbers are unique per plan (enforced by unique index `idx_log_book_journal_entries_plan_page`).
- Page numbers are not renumbered on deletion — gaps are expected.
