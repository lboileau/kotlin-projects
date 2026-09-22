-- Photos attached to a recipe. Only metadata lives here; the bytes are an object in the photo
-- bucket at storage_key (recipes/{recipe_id}/{id}.{ext}). `source` says how it got here — added
-- by a person, or the photo an import was read from — and `role` which part of the recipe an
-- import photo showed. The service deletes the object before the row.
CREATE TABLE IF NOT EXISTS recipe_photos (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipe_id   UUID        NOT NULL,
    position    INT         NOT NULL,
    storage_key TEXT        NOT NULL,
    media_type  VARCHAR(50) NOT NULL,
    byte_size   INT         NOT NULL,
    width       INT,
    height      INT,
    source      VARCHAR(20) NOT NULL,
    role        VARCHAR(20),
    created_by  UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipe_photos_position CHECK (position >= 0),
    CONSTRAINT ck_recipe_photos_byte_size CHECK (byte_size > 0),
    CONSTRAINT ck_recipe_photos_source CHECK (source IN ('upload', 'import')),
    CONSTRAINT ck_recipe_photos_role CHECK (role IS NULL OR role IN ('ingredients', 'instructions')),
    CONSTRAINT uq_recipe_photos_recipe_position UNIQUE (recipe_id, position),
    CONSTRAINT uq_recipe_photos_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_recipe_photos_recipe FOREIGN KEY (recipe_id) REFERENCES recipes (id) ON DELETE CASCADE,
    CONSTRAINT fk_recipe_photos_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);
