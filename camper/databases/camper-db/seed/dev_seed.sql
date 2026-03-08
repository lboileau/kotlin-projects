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
    -- User items (Alice's personal gear)
    ('bb220000-0001-4000-8000-000000000001', NULL, 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'Sleeping Bag', 'personal', 1, true, now(), now()),
    ('bb220000-0002-4000-8000-000000000002', NULL, 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'Headlamp', 'personal', 1, false, now(), now()),
    ('bb220000-0003-4000-8000-000000000003', NULL, 'd3bbef22-cf3e-7b2b-ee90-9eece66b3d44', 'First Aid Kit', 'misc', 1, true, now(), now()),
    -- User items (Bob's personal gear)
    ('bb220000-0004-4000-8000-000000000004', NULL, 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', 'Sleeping Pad', 'personal', 1, false, now(), now()),
    ('bb220000-0005-4000-8000-000000000005', NULL, 'e4ccf033-d04f-8c3c-ffa1-affd007c4e55', 'Water Bottle', 'personal', 2, true, now(), now())
ON CONFLICT (id) DO NOTHING;
