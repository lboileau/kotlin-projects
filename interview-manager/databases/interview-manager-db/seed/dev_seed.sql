INSERT INTO worlds (id, name, greeting) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Earth', 'Hello, Earth!')
ON CONFLICT DO NOTHING;

INSERT INTO worlds (id, name, greeting) VALUES
    ('b1ffcd00-ad1c-5f09-cc7e-7ccace491b22', 'Mars', 'Hello, Mars!')
ON CONFLICT DO NOTHING;

INSERT INTO worlds (id, name, greeting) VALUES
    ('c2aade11-be2d-6a1a-dd8f-8ddbdf5a2c33', 'Vulcan', 'Hello, Vulcan!')
ON CONFLICT DO NOTHING;
