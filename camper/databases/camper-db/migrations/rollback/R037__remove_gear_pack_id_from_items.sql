DROP INDEX IF EXISTS idx_items_gear_pack_id;
ALTER TABLE items DROP CONSTRAINT IF EXISTS fk_items_gear_pack;
ALTER TABLE items DROP COLUMN IF EXISTS gear_pack_id;
