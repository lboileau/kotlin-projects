ALTER TABLE items ADD COLUMN IF NOT EXISTS gear_pack_id UUID;

ALTER TABLE items ADD CONSTRAINT fk_items_gear_pack
    FOREIGN KEY (gear_pack_id) REFERENCES gear_packs (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_items_gear_pack_id ON items (gear_pack_id);
