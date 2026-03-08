CREATE TABLE IF NOT EXISTS itinerary_events (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    itinerary_id   UUID         NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    details        TEXT,
    event_at       TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL    DEFAULT now(),

    CONSTRAINT fk_itinerary_events_itinerary FOREIGN KEY (itinerary_id) REFERENCES itineraries (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_itinerary_events_itinerary_id ON itinerary_events (itinerary_id);
