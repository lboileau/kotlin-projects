ALTER TABLE itinerary_events DROP CONSTRAINT IF EXISTS ck_itinerary_events_category;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS category;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS estimated_cost;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS location;
ALTER TABLE itinerary_events DROP COLUMN IF EXISTS event_end_at;
