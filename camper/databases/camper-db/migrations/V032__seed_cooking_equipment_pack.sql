-- Cooking Equipment Gear Pack
INSERT INTO gear_packs (id, name, description)
VALUES ('cc000000-0001-4000-8000-000000000001', 'Cooking Equipment', 'Essential cooking gear for campfire meals. Includes pots, pans, utensils, and tableware.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO gear_pack_items (id, gear_pack_id, name, category, default_quantity, scalable, sort_order)
VALUES
    -- Cooking surfaces (non-scalable)
    ('cc000000-0001-4000-8000-000000000101', 'cc000000-0001-4000-8000-000000000001', 'Cast Iron Pan', 'kitchen', 1, false, 1),
    ('cc000000-0001-4000-8000-000000000102', 'cc000000-0001-4000-8000-000000000001', 'Grill Grate', 'kitchen', 1, false, 2),
    ('cc000000-0001-4000-8000-000000000103', 'cc000000-0001-4000-8000-000000000001', 'Large Pot', 'kitchen', 1, false, 3),
    -- Utensils (non-scalable)
    ('cc000000-0001-4000-8000-000000000104', 'cc000000-0001-4000-8000-000000000001', 'Spatula', 'kitchen', 1, false, 4),
    ('cc000000-0001-4000-8000-000000000105', 'cc000000-0001-4000-8000-000000000001', 'Tongs', 'kitchen', 1, false, 5),
    ('cc000000-0001-4000-8000-000000000106', 'cc000000-0001-4000-8000-000000000001', 'Cooking Knife', 'kitchen', 1, false, 6),
    ('cc000000-0001-4000-8000-000000000107', 'cc000000-0001-4000-8000-000000000001', 'Cutting Board', 'kitchen', 1, false, 7),
    ('cc000000-0001-4000-8000-000000000108', 'cc000000-0001-4000-8000-000000000001', 'Can Opener', 'kitchen', 1, false, 8),
    -- Tableware (scalable per person)
    ('cc000000-0001-4000-8000-000000000109', 'cc000000-0001-4000-8000-000000000001', 'Plates', 'kitchen', 1, true, 9),
    ('cc000000-0001-4000-8000-000000000110', 'cc000000-0001-4000-8000-000000000001', 'Cups', 'kitchen', 1, true, 10),
    ('cc000000-0001-4000-8000-000000000111', 'cc000000-0001-4000-8000-000000000001', 'Bowls', 'kitchen', 1, true, 11),
    ('cc000000-0001-4000-8000-000000000112', 'cc000000-0001-4000-8000-000000000001', 'Cutlery Set', 'kitchen', 1, true, 12)
ON CONFLICT (id) DO NOTHING;
