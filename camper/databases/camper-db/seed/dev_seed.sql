INSERT INTO worlds (id, name, greeting, created_at, updated_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Earth', 'Hello, Earth!', now(), now()),
    ('b1ffcd00-ad1c-5f09-cc7e-7ccace491b22', 'Mars', 'Hello, Mars!', now(), now()),
    ('c2aade11-be2d-6a1a-dd8f-8ddbdf5a2c33', 'Vulcan', 'Hello, Vulcan!', now(), now())
ON CONFLICT (name) DO NOTHING;

INSERT INTO users (id, email, username, created_at, updated_at)
VALUES
    ('d3bbef22-cf3e-7b2b-ee90-9eeceg6b3d44', 'alice@example.com', 'Alice', now(), now()),
    ('e4ccf033-d04f-8c3c-ffa1-affdh07c4e55', 'bob@example.com', 'Bob', now(), now()),
    ('f5dda144-e150-9d4d-00b2-b00ei18d5f66', 'charlie@example.com', NULL, now(), now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at)
VALUES
    ('10aabb00-1111-2222-3333-444455556666', 'Summer Camping Trip', 'private', 'd3bbef22-cf3e-7b2b-ee90-9eeceg6b3d44', now(), now()),
    ('20bbcc00-2222-3333-4444-555566667777', 'Fall Retreat', 'public', 'e4ccf033-d04f-8c3c-ffa1-affdh07c4e55', now(), now())
ON CONFLICT DO NOTHING;

INSERT INTO plan_members (plan_id, user_id, created_at)
VALUES
    ('10aabb00-1111-2222-3333-444455556666', 'd3bbef22-cf3e-7b2b-ee90-9eeceg6b3d44', now()),
    ('10aabb00-1111-2222-3333-444455556666', 'e4ccf033-d04f-8c3c-ffa1-affdh07c4e55', now()),
    ('20bbcc00-2222-3333-4444-555566667777', 'e4ccf033-d04f-8c3c-ffa1-affdh07c4e55', now())
ON CONFLICT DO NOTHING;
