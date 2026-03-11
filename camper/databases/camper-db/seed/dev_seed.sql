INSERT INTO worlds (id, name, greeting, created_at, updated_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Earth', 'Hello, Earth!', now(), now()),
    ('b1ffcd00-ad1c-5f09-cc7e-7ccace491b22', 'Mars', 'Hello, Mars!', now(), now()),
    ('c2aade11-be2d-6a1a-dd8f-8ddbdf5a2c33', 'Vulcan', 'Hello, Vulcan!', now(), now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (id, email, username, created_at, updated_at)
VALUES
    ('d3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'alice@example.com', 'Alice', now(), now()),
    ('e4ccf033-d04f-8c3c-ffa1-affd007c4e55', 'bob@example.com', 'Bob', now(), now()),
    ('f5dda144-e150-9d4d-00b2-b00e118d5f66', 'charlie@example.com', NULL, now(), now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at)
VALUES
    ('10aabb00-1111-2222-3333-444455556666', 'Summer Camping Trip', 'private', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now(), now()),
    ('20bbcc00-2222-3333-4444-555566667777', 'Fall Retreat', 'public', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO plan_members (plan_id, user_id, created_at)
VALUES
    ('10aabb00-1111-2222-3333-444455556666', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now()),
    ('10aabb00-1111-2222-3333-444455556666', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now()),
    ('20bbcc00-2222-3333-4444-555566667777', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now())
ON CONFLICT DO NOTHING;

INSERT INTO items (id, plan_id, user_id, name, category, quantity, packed, created_at, updated_at)
VALUES
    -- Plan items (Summer Camping Trip)
    ('aa110000-0001-4000-8000-000000000001', '10aabb00-1111-2222-3333-444455556666', NULL, 'Canoe', 'canoe', 1, true, now(), now()),
    ('aa110000-0002-4000-8000-000000000002', '10aabb00-1111-2222-3333-444455556666', NULL, 'Paddle', 'canoe', 2, false, now(), now()),
    ('aa110000-0003-4000-8000-000000000003', '10aabb00-1111-2222-3333-444455556666', NULL, 'Camp Stove', 'kitchen', 1, true, now(), now()),
    ('aa110000-0004-4000-8000-000000000004', '10aabb00-1111-2222-3333-444455556666', NULL, 'Cooler', 'kitchen', 1, false, now(), now()),
    ('aa110000-0005-4000-8000-000000000005', '10aabb00-1111-2222-3333-444455556666', NULL, 'Tent (4-person)', 'camp', 1, true, now(), now()),
    ('aa110000-0006-4000-8000-000000000006', '10aabb00-1111-2222-3333-444455556666', NULL, 'Firewood Bundle', 'misc', 2, false, now(), now()),
    ('aa110000-0007-4000-8000-000000000007', '10aabb00-1111-2222-3333-444455556666', NULL, 'Hot Dogs', 'food_item', 12, false, now(), now()),
    -- User items (Alice's personal gear for Summer Camping Trip)
    ('bb220000-0001-4000-8000-000000000001', '10aabb00-1111-2222-3333-444455556666', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'Sleeping Bag', 'personal', 1, true, now(), now()),
    ('bb220000-0002-4000-8000-000000000002', '10aabb00-1111-2222-3333-444455556666', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'Headlamp', 'personal', 1, false, now(), now()),
    ('bb220000-0003-4000-8000-000000000003', '10aabb00-1111-2222-3333-444455556666', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'First Aid Kit', 'misc', 1, true, now(), now()),
    -- User items (Bob's personal gear for Summer Camping Trip)
    ('bb220000-0004-4000-8000-000000000004', '10aabb00-1111-2222-3333-444455556666', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', 'Sleeping Pad', 'personal', 1, false, now(), now()),
    ('bb220000-0005-4000-8000-000000000005', '10aabb00-1111-2222-3333-444455556666', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', 'Water Bottle', 'personal', 2, true, now(), now()),
    -- Meal plan items (Summer Camping Trip, Day 1)
    ('cc330000-0001-4000-8000-000000000001', '10aabb00-1111-2222-3333-444455556666', NULL, 'Pancakes', 'day1:breakfast', 1, false, now(), now()),
    ('cc330000-0002-4000-8000-000000000002', '10aabb00-1111-2222-3333-444455556666', NULL, 'Coffee', 'day1:breakfast', 1, false, now(), now()),
    ('cc330000-0003-4000-8000-000000000003', '10aabb00-1111-2222-3333-444455556666', NULL, 'Sandwiches', 'day1:lunch', 6, false, now(), now()),
    ('cc330000-0004-4000-8000-000000000004', '10aabb00-1111-2222-3333-444455556666', NULL, 'Grilled Salmon', 'day1:dinner', 1, false, now(), now()),
    ('cc330000-0005-4000-8000-000000000005', '10aabb00-1111-2222-3333-444455556666', NULL, 'Trail Mix', 'day1:snacks', 3, true, now(), now()),
    -- Meal plan items (Summer Camping Trip, Day 2)
    ('cc330000-0006-4000-8000-000000000006', '10aabb00-1111-2222-3333-444455556666', NULL, 'Eggs & Bacon', 'day2:breakfast', 1, false, now(), now()),
    ('cc330000-0007-4000-8000-000000000007', '10aabb00-1111-2222-3333-444455556666', NULL, 'Hot Dogs', 'day2:lunch', 8, false, now(), now()),
    ('cc330000-0008-4000-8000-000000000008', '10aabb00-1111-2222-3333-444455556666', NULL, 'Campfire Stew', 'day2:dinner', 1, false, now(), now())
ON CONFLICT (id) DO NOTHING;

INSERT INTO itineraries (id, plan_id, created_at, updated_at)
VALUES
    ('aa001100-aaaa-bbbb-cccc-ddddeeee0001', '10aabb00-1111-2222-3333-444455556666', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO itinerary_events (id, itinerary_id, title, description, details, event_at, created_at, updated_at)
VALUES
    ('bb001100-1111-2222-3333-444455550001', 'aa001100-aaaa-bbbb-cccc-ddddeeee0001',
     'Arrive at campsite', 'Set up tents and organize the camp area before dark.',
     'Site #14 at Pine Ridge Campground. Check-in at the ranger station on the way in.',
     '2026-07-10 15:00:00+00', now(), now()),
    ('bb001100-1111-2222-3333-444455550002', 'aa001100-aaaa-bbbb-cccc-ddddeeee0001',
     'Morning hike to Eagle Peak', 'A moderate 5-mile loop trail with great views of the valley.',
     NULL,
     '2026-07-11 08:00:00+00', now(), now()),
    ('bb001100-1111-2222-3333-444455550003', 'aa001100-aaaa-bbbb-cccc-ddddeeee0001',
     'Campfire dinner', 'Cook dinner over the fire and share stories.',
     'Bring foil packets for veggies. Bob is handling the firewood.',
     '2026-07-11 18:30:00+00', now(), now()),
    ('bb001100-1111-2222-3333-444455550004', 'aa001100-aaaa-bbbb-cccc-ddddeeee0001',
     'Pack up and head home', NULL, NULL,
     '2026-07-12 10:00:00+00', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO assignments (id, plan_id, name, type, max_occupancy, owner_id, created_at, updated_at)
VALUES
    -- Summer Camping Trip: 2 tents, 1 canoe
    ('aa110000-0001-0001-0001-000000000001', '10aabb00-1111-2222-3333-444455556666', 'Big Agnes Copper Spur', 'tent', 3, 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now(), now()),
    ('aa110000-0001-0001-0001-000000000002', '10aabb00-1111-2222-3333-444455556666', 'MSR Hubba Hubba', 'tent', 2, 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now(), now()),
    ('aa110000-0001-0001-0001-000000000003', '10aabb00-1111-2222-3333-444455556666', 'Old Town Discovery', 'canoe', 3, 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now(), now()),
    -- Fall Retreat: 1 tent, 1 canoe
    ('aa110000-0002-0002-0002-000000000001', '20bbcc00-2222-3333-4444-555566667777', 'REI Half Dome', 'tent', 4, 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now(), now()),
    ('aa110000-0002-0002-0002-000000000002', '20bbcc00-2222-3333-4444-555566667777', 'Grumman Alumacraft', 'canoe', 2, 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO assignment_members (assignment_id, user_id, plan_id, assignment_type, created_at)
VALUES
    -- Summer Camping Trip tent assignments: Alice in Big Agnes, Bob in MSR Hubba
    ('aa110000-0001-0001-0001-000000000001', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', '10aabb00-1111-2222-3333-444455556666', 'tent', now()),
    ('aa110000-0001-0001-0001-000000000002', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', '10aabb00-1111-2222-3333-444455556666', 'tent', now()),
    -- Summer Camping Trip canoe assignments: Alice and Bob in Old Town Discovery
    ('aa110000-0001-0001-0001-000000000003', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', '10aabb00-1111-2222-3333-444455556666', 'canoe', now()),
    ('aa110000-0001-0001-0001-000000000003', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', '10aabb00-1111-2222-3333-444455556666', 'canoe', now()),
    -- Fall Retreat: Bob in REI Half Dome tent and Grumman canoe
    ('aa110000-0002-0002-0002-000000000001', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', '20bbcc00-2222-3333-4444-555566667777', 'tent', now()),
    ('aa110000-0002-0002-0002-000000000002', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', '20bbcc00-2222-3333-4444-555566667777', 'canoe', now())
ON CONFLICT DO NOTHING;

-- ============================================================
-- Ingredients (global master list for recipe normalization)
-- ============================================================

INSERT INTO ingredients (id, name, category, default_unit, created_at, updated_at)
VALUES
    -- produce
    ('aa130000-0001-4000-8000-000000000000', 'onion',       'produce', 'whole', now(), now()),
    ('aa130000-0002-4000-8000-000000000000', 'garlic',      'produce', 'clove', now(), now()),
    ('aa130000-0003-4000-8000-000000000000', 'tomato',      'produce', 'whole', now(), now()),
    ('aa130000-0004-4000-8000-000000000000', 'potato',      'produce', 'whole', now(), now()),
    ('aa130000-0005-4000-8000-000000000000', 'lettuce',     'produce', 'whole', now(), now()),
    ('aa130000-0006-4000-8000-000000000000', 'avocado',     'produce', 'whole', now(), now()),
    ('aa130000-0007-4000-8000-000000000000', 'lemon',       'produce', 'whole', now(), now()),
    ('aa130000-0008-4000-8000-000000000000', 'lime',        'produce', 'whole', now(), now()),
    ('aa130000-0009-4000-8000-000000000000', 'bell pepper', 'produce', 'whole', now(), now()),
    ('aa130000-0010-4000-8000-000000000000', 'jalapeño',    'produce', 'whole', now(), now()),
    ('aa130000-0011-4000-8000-000000000000', 'cilantro',    'produce', 'bunch', now(), now()),
    ('aa130000-0012-4000-8000-000000000000', 'corn',        'produce', 'whole', now(), now()),
    -- dairy
    ('aa130000-0013-4000-8000-000000000000', 'butter',      'dairy',   'oz',   now(), now()),
    ('aa130000-0014-4000-8000-000000000000', 'cheese',      'dairy',   'oz',   now(), now()),
    ('aa130000-0015-4000-8000-000000000000', 'milk',        'dairy',   'cup',  now(), now()),
    ('aa130000-0016-4000-8000-000000000000', 'egg',         'dairy',   'whole',now(), now()),
    ('aa130000-0017-4000-8000-000000000000', 'sour cream',  'dairy',   'cup',  now(), now()),
    -- meat
    ('aa130000-0018-4000-8000-000000000000', 'ground beef',    'meat', 'lb',   now(), now()),
    ('aa130000-0019-4000-8000-000000000000', 'chicken breast', 'meat', 'lb',   now(), now()),
    ('aa130000-0020-4000-8000-000000000000', 'bacon',          'meat', 'oz',   now(), now()),
    ('aa130000-0021-4000-8000-000000000000', 'sausage',        'meat', 'oz',   now(), now()),
    -- seafood
    ('aa130000-0022-4000-8000-000000000000', 'shrimp',      'seafood', 'lb',   now(), now()),
    -- pantry
    ('aa130000-0023-4000-8000-000000000000', 'flour',          'pantry', 'cup',  now(), now()),
    ('aa130000-0024-4000-8000-000000000000', 'sugar',          'pantry', 'cup',  now(), now()),
    ('aa130000-0025-4000-8000-000000000000', 'rice',           'pantry', 'cup',  now(), now()),
    ('aa130000-0026-4000-8000-000000000000', 'pasta',          'pantry', 'oz',   now(), now()),
    ('aa130000-0027-4000-8000-000000000000', 'olive oil',      'pantry', 'tbsp', now(), now()),
    ('aa130000-0028-4000-8000-000000000000', 'vegetable oil',  'pantry', 'tbsp', now(), now()),
    ('aa130000-0029-4000-8000-000000000000', 'bread',          'pantry', 'slice',now(), now()),
    ('aa130000-0030-4000-8000-000000000000', 'tortilla',       'pantry', 'whole',now(), now()),
    ('aa130000-0031-4000-8000-000000000000', 'canned beans',   'pantry', 'can',  now(), now()),
    ('aa130000-0032-4000-8000-000000000000', 'canned tomato',  'pantry', 'can',  now(), now()),
    -- spice
    ('aa130000-0033-4000-8000-000000000000', 'salt',         'spice', 'tsp',  now(), now()),
    ('aa130000-0034-4000-8000-000000000000', 'pepper',       'spice', 'tsp',  now(), now()),
    ('aa130000-0035-4000-8000-000000000000', 'cumin',        'spice', 'tsp',  now(), now()),
    ('aa130000-0036-4000-8000-000000000000', 'paprika',      'spice', 'tsp',  now(), now()),
    ('aa130000-0037-4000-8000-000000000000', 'chili powder', 'spice', 'tsp',  now(), now()),
    ('aa130000-0038-4000-8000-000000000000', 'oregano',      'spice', 'tsp',  now(), now()),
    ('aa130000-0039-4000-8000-000000000000', 'garlic powder','spice', 'tsp',  now(), now()),
    -- condiment
    ('aa130000-0040-4000-8000-000000000000', 'ketchup',     'condiment', 'tbsp', now(), now()),
    ('aa130000-0041-4000-8000-000000000000', 'mustard',     'condiment', 'tbsp', now(), now()),
    ('aa130000-0042-4000-8000-000000000000', 'mayonnaise',  'condiment', 'tbsp', now(), now()),
    ('aa130000-0043-4000-8000-000000000000', 'hot sauce',   'condiment', 'tbsp', now(), now()),
    ('aa130000-0044-4000-8000-000000000000', 'soy sauce',   'condiment', 'tbsp', now(), now()),
    -- other
    ('aa130000-0045-4000-8000-000000000000', 'water', 'other', 'ml',     now(), now()),
    ('aa130000-0046-4000-8000-000000000000', 'ice',   'other', 'pieces', now(), now())
ON CONFLICT (name) DO NOTHING;

-- ============================================================
-- Sample recipes (for dev testing)
-- ============================================================

INSERT INTO recipes (id, name, description, base_servings, status, created_by, created_at, updated_at)
VALUES
    ('aa140000-0001-4000-8000-000000000000',
     'Camp Guacamole',
     'A simple campsite guacamole — just mash, mix, and eat with tortilla chips.',
     4, 'published', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now(), now()),
    ('aa140000-0002-4000-8000-000000000000',
     'Trail Tacos',
     'Quick ground beef tacos perfect for camp cooking — season the beef, warm the tortillas over the fire, and load up the toppings.',
     6, 'published', 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', now(), now()),
    ('aa140000-0003-4000-8000-000000000000',
     'Campfire Chili',
     'A hearty one-pot chili that simmers over the campfire. Great for a crowd on a cold night.',
     8, 'published', 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', now(), now())
ON CONFLICT (id) DO NOTHING;

-- ============================================================
-- Recipe ingredients
-- ============================================================

INSERT INTO recipe_ingredients (id, recipe_id, ingredient_id, quantity, unit, status, created_at, updated_at)
VALUES
    -- Camp Guacamole
    ('aa150000-0001-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0006-4000-8000-000000000000', 3,   'whole', 'approved', now(), now()),  -- avocado
    ('aa150000-0002-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0008-4000-8000-000000000000', 1,   'whole', 'approved', now(), now()),  -- lime
    ('aa150000-0003-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0001-4000-8000-000000000000', 0.5, 'whole', 'approved', now(), now()),  -- onion
    ('aa150000-0004-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0002-4000-8000-000000000000', 2,   'clove', 'approved', now(), now()),  -- garlic
    ('aa150000-0005-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0010-4000-8000-000000000000', 1,   'whole', 'approved', now(), now()),  -- jalapeño
    ('aa150000-0006-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0011-4000-8000-000000000000', 2,   'tbsp',  'approved', now(), now()),  -- cilantro
    ('aa150000-0007-4000-8000-000000000000', 'aa140000-0001-4000-8000-000000000000', 'aa130000-0033-4000-8000-000000000000', 0.5, 'tsp',   'approved', now(), now()),  -- salt
    -- Trail Tacos
    ('aa150000-0008-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0018-4000-8000-000000000000', 1,   'lb',    'approved', now(), now()),  -- ground beef
    ('aa150000-0009-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0030-4000-8000-000000000000', 6,   'whole', 'approved', now(), now()),  -- tortilla
    ('aa150000-0010-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0014-4000-8000-000000000000', 4,   'oz',    'approved', now(), now()),  -- cheese
    ('aa150000-0011-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0005-4000-8000-000000000000', 0.5, 'whole', 'approved', now(), now()),  -- lettuce
    ('aa150000-0012-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0003-4000-8000-000000000000', 2,   'whole', 'approved', now(), now()),  -- tomato
    ('aa150000-0013-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0017-4000-8000-000000000000', 0.5, 'cup',   'approved', now(), now()),  -- sour cream
    ('aa150000-0014-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0035-4000-8000-000000000000', 1,   'tsp',   'approved', now(), now()),  -- cumin
    ('aa150000-0015-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0037-4000-8000-000000000000', 1,   'tsp',   'approved', now(), now()),  -- chili powder
    ('aa150000-0016-4000-8000-000000000000', 'aa140000-0002-4000-8000-000000000000', 'aa130000-0033-4000-8000-000000000000', 0.5, 'tsp',   'approved', now(), now()),  -- salt
    -- Campfire Chili
    ('aa150000-0017-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0018-4000-8000-000000000000', 1.5, 'lb',    'approved', now(), now()),  -- ground beef
    ('aa150000-0018-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0031-4000-8000-000000000000', 2,   'can',   'approved', now(), now()),  -- canned beans
    ('aa150000-0019-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0032-4000-8000-000000000000', 1,   'can',   'approved', now(), now()),  -- canned tomato
    ('aa150000-0020-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0001-4000-8000-000000000000', 1,   'whole', 'approved', now(), now()),  -- onion
    ('aa150000-0021-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0002-4000-8000-000000000000', 4,   'clove', 'approved', now(), now()),  -- garlic
    ('aa150000-0022-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0009-4000-8000-000000000000', 1,   'whole', 'approved', now(), now()),  -- bell pepper
    ('aa150000-0023-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0035-4000-8000-000000000000', 2,   'tsp',   'approved', now(), now()),  -- cumin
    ('aa150000-0024-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0037-4000-8000-000000000000', 2,   'tsp',   'approved', now(), now()),  -- chili powder
    ('aa150000-0025-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0038-4000-8000-000000000000', 1,   'tsp',   'approved', now(), now()),  -- oregano
    ('aa150000-0026-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0033-4000-8000-000000000000', 1,   'tsp',   'approved', now(), now()),  -- salt
    ('aa150000-0027-4000-8000-000000000000', 'aa140000-0003-4000-8000-000000000000', 'aa130000-0034-4000-8000-000000000000', 0.5, 'tsp',   'approved', now(), now())   -- pepper
ON CONFLICT (id) DO NOTHING;
