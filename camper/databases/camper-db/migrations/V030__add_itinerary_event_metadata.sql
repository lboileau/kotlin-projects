ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS category VARCHAR(50) NOT NULL DEFAULT 'other';
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS estimated_cost DECIMAL(10,2);
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS location VARCHAR(500);
ALTER TABLE itinerary_events ADD COLUMN IF NOT EXISTS event_end_at TIMESTAMPTZ;

ALTER TABLE itinerary_events ADD CONSTRAINT ck_itinerary_events_category
    CHECK (category IN ('travel', 'accommodation', 'activity', 'meal', 'other'));
