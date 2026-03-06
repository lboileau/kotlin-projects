INSERT INTO worlds (id, name, greeting) VALUES
    ('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Earth', 'Hello, World!')
ON CONFLICT DO NOTHING;

INSERT INTO worlds (id, name, greeting) VALUES
    ('b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22', 'Mars', 'Greetings, Earthling!')
ON CONFLICT DO NOTHING;

INSERT INTO worlds (id, name, greeting) VALUES
    ('c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33', 'Vulcan', 'Live long and prosper!')
ON CONFLICT DO NOTHING;
