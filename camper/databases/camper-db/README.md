# camper-db

PostgreSQL database for the Camper project.

## Schema

### worlds

| Column     | Type          | Constraints                          |
|------------|---------------|--------------------------------------|
| id         | UUID          | PK, DEFAULT gen_random_uuid()        |
| name       | VARCHAR(100)  | NOT NULL, UNIQUE (uq_worlds_name)    |
| greeting   | VARCHAR(255)  | NOT NULL                             |
| created_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |
| updated_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

**Indexes:** `idx_worlds_name` on `name`

### users

| Column     | Type          | Constraints                          |
|------------|---------------|--------------------------------------|
| id         | UUID          | PK, DEFAULT gen_random_uuid()        |
| email      | VARCHAR(255)  | NOT NULL, UNIQUE (uq_users_email)    |
| username   | VARCHAR(100)  | nullable                             |
| created_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |
| updated_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

**Indexes:** `idx_users_email` on `email`

### plans

| Column     | Type          | Constraints                          |
|------------|---------------|--------------------------------------|
| id         | UUID          | PK, DEFAULT gen_random_uuid()        |
| name       | VARCHAR(255)  | NOT NULL                             |
| visibility | VARCHAR(10)   | NOT NULL, DEFAULT 'private', CHECK IN ('public', 'private') |
| owner_id   | UUID          | NOT NULL, FK -> users(id)            |
| created_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |
| updated_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

### plan_members

| Column     | Type          | Constraints                          |
|------------|---------------|--------------------------------------|
| plan_id    | UUID          | PK (composite), FK -> plans(id) ON DELETE CASCADE |
| user_id    | UUID          | PK (composite), FK -> users(id)     |
| created_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

**Indexes:** `idx_plan_members_user_id` on `user_id`

### itineraries

| Column     | Type          | Constraints                          |
|------------|---------------|--------------------------------------|
| id         | UUID          | PK, DEFAULT gen_random_uuid()        |
| plan_id    | UUID          | NOT NULL, UNIQUE, FK -> plans(id) ON DELETE CASCADE |
| created_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |
| updated_at | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

### itinerary_events

| Column       | Type          | Constraints                          |
|--------------|---------------|--------------------------------------|
| id           | UUID          | PK, DEFAULT gen_random_uuid()        |
| itinerary_id | UUID          | NOT NULL, FK -> itineraries(id) ON DELETE CASCADE |
| title        | VARCHAR(255)  | NOT NULL                             |
| description  | TEXT          | nullable                             |
| details      | TEXT          | nullable                             |
| event_at     | TIMESTAMPTZ   | NOT NULL                             |
| created_at   | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |
| updated_at   | TIMESTAMPTZ   | NOT NULL, DEFAULT now()              |

**Indexes:** `idx_itinerary_events_itinerary_id` on `itinerary_id`

### ingredients

| Column        | Type          | Constraints                                                                          |
|---------------|---------------|--------------------------------------------------------------------------------------|
| id            | UUID          | PK, DEFAULT gen_random_uuid()                                                        |
| name          | VARCHAR(255)  | NOT NULL, UNIQUE (uq_ingredients_name)                                               |
| category      | VARCHAR(50)   | NOT NULL, CHECK IN ('produce','dairy','meat','seafood','pantry','spice','condiment','frozen','bakery','other') |
| default_unit  | VARCHAR(20)   | NOT NULL, CHECK IN ('g','kg','ml','l','tsp','tbsp','cup','oz','lb','pieces','whole','bunch','can','clove','pinch','slice','sprig') |
| created_at    | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                                              |
| updated_at    | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                                              |

**Indexes:** `idx_ingredients_name` on `name`

### recipes

| Column          | Type          | Constraints                                                  |
|-----------------|---------------|--------------------------------------------------------------|
| id              | UUID          | PK, DEFAULT gen_random_uuid()                                |
| name            | VARCHAR(255)  | NOT NULL                                                     |
| description     | TEXT          | nullable                                                     |
| web_link        | TEXT          | nullable, UNIQUE (uq_recipes_web_link) WHERE NOT NULL        |
| base_servings   | INT           | NOT NULL, CHECK > 0                                          |
| status          | VARCHAR(20)   | NOT NULL, DEFAULT 'draft', CHECK IN ('draft','published')    |
| created_by      | UUID          | NOT NULL, FK -> users(id) ON DELETE RESTRICT                 |
| duplicate_of_id | UUID          | nullable, FK -> recipes(id) ON DELETE SET NULL               |
| created_at      | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                      |
| updated_at      | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                      |

**Indexes:** `idx_recipes_status`, `idx_recipes_created_by`, `idx_recipes_duplicate_of_id`

### recipe_ingredients

| Column                   | Type          | Constraints                                                        |
|--------------------------|---------------|--------------------------------------------------------------------|
| id                       | UUID          | PK, DEFAULT gen_random_uuid()                                      |
| recipe_id                | UUID          | NOT NULL, FK -> recipes(id) ON DELETE CASCADE                      |
| ingredient_id            | UUID          | nullable, FK -> ingredients(id) ON DELETE RESTRICT                 |
| original_text            | TEXT          | nullable                                                           |
| quantity                 | NUMERIC       | NOT NULL, CHECK > 0                                                |
| unit                     | VARCHAR(20)   | NOT NULL                                                           |
| status                   | VARCHAR(20)   | NOT NULL, DEFAULT 'approved', CHECK IN ('pending_review','approved') |
| matched_ingredient_id    | UUID          | nullable, FK -> ingredients(id) ON DELETE SET NULL                 |
| suggested_ingredient_name| TEXT          | nullable                                                           |
| review_flags             | JSONB         | NOT NULL, DEFAULT '[]'                                             |
| created_at               | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                            |
| updated_at               | TIMESTAMPTZ   | NOT NULL, DEFAULT now()                                            |

**Indexes:** `uq_recipe_ingredients_recipe_id_ingredient_id` (unique WHERE ingredient_id IS NOT NULL), `idx_recipe_ingredients_recipe_id`, `idx_recipe_ingredients_ingredient_id`, `idx_recipe_ingredients_matched_ingredient_id`

## Local Setup

### Start the database

```bash
cd databases/camper-db
docker-compose up -d
```

### Connect via psql

```bash
psql -h localhost -p 5433 -U postgres -d camper_db
```

### Run migrations

Migrations are managed by Flyway. You can run them via the `MigrationRunner` utility class programmatically, or using the Flyway CLI:

```bash
flyway -configFiles=flyway.conf migrate
```

### Seed data

```bash
psql -h localhost -p 5433 -U postgres -d camper_db -f seed/dev_seed.sql
```

## Migration Workflow

1. Create a new migration file in `migrations/` following the naming convention `V<NNN>__<description>.sql`.
2. Create a corresponding rollback in `migrations/rollback/R<NNN>__<description>.sql`.
3. Update the current-state schema file in `schema/tables/`.
4. Run the migration and verify.
