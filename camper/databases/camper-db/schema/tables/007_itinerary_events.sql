CREATE TABLE IF NOT EXISTS itinerary_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id   UUID         NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    details        TEXT,
    event_at       TIMESTAMPTZ  NOT NULL,
    category       VARCHAR(50)  NOT NULL    DEFAULT 'other',
    estimated_cost DECIMAL(10,2),
    location       VARCHAR(500),
    event_end_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT fk_itinerary_events_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id) ON DELETE CASCADE,
    CONSTRAINT ck_itinerary_events_category CHECK (category IN ('travel', 'accommodation', 'activity', 'meal', 'other'))
);

CREATE INDEX IF NOT EXISTS idx_itinerary_events_itinerary_id ON itinerary_events (itinerary_id);
