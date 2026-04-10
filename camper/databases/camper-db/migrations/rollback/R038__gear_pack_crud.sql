DROP INDEX IF EXISTS uq_gear_pack_items_pack_name;
DROP INDEX IF EXISTS idx_gear_packs_created_by;
ALTER TABLE gear_packs DROP CONSTRAINT IF EXISTS fk_gear_packs_created_by;
ALTER TABLE gear_packs DROP COLUMN IF EXISTS created_by;
