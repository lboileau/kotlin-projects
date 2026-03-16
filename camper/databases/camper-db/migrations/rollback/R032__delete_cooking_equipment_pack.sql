-- Rollback: delete cooking equipment gear pack seed data
-- Items are deleted via ON DELETE CASCADE from gear_packs
DELETE FROM gear_packs WHERE id = 'cc000000-0001-4000-8000-000000000001';
