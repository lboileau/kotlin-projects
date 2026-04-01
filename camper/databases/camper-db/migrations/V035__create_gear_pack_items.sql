CREATE TABLE IF NOT EXISTS gear_pack_items (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    gear_pack_id     UUID         NOT NULL,
    name             VARCHAR(255) NOT NULL,
    category         VARCHAR(50)  NOT NULL,
    default_quantity INTEGER      NOT NULL DEFAULT 1,
    scalable         BOOLEAN      NOT NULL DEFAULT false,
    sort_order       INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_gear_pack_items_gear_pack FOREIGN KEY (gear_pack_id)
        REFERENCES gear_packs (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_gear_pack_items_gear_pack_id ON gear_pack_items (gear_pack_id);
