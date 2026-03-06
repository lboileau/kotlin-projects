INSERT INTO worlds (id, name, greeting, created_at, updated_at)
VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Earth', 'Hello, Earth!', now(), now()),
    ('b1ffcd00-ad1c-5f09-cc7e-7ccace491b22', 'Mars', 'Hello, Mars!', now(), now()),
    ('c2aade11-be2d-6a1a-dd8f-8ddbdf5a2c33', 'Vulcan', 'Hello, Vulcan!', now(), now())
ON CONFLICT (name) DO NOTHING;
