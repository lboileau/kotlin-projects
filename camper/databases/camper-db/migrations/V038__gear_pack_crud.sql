-- Add created_by column (nullable — NULL means system-created/immutable)
ALTER TABLE gear_packs ADD COLUMN IF NOT EXISTS created_by UUID;

ALTER TABLE gear_packs ADD CONSTRAINT fk_gear_packs_created_by
    FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_gear_packs_created_by ON gear_packs (created_by);

-- Enforce unique item names within a pack (case-insensitive)
CREATE UNIQUE INDEX IF NOT EXISTS uq_gear_pack_items_pack_name
    ON gear_pack_items (gear_pack_id, LOWER(name));
