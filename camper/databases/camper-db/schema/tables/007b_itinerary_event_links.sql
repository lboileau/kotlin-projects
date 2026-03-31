CREATE TABLE IF NOT EXISTS itinerary_event_links (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id   UUID        NOT NULL,
    url        TEXT        NOT NULL,
    label      VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_event_links_event FOREIGN KEY (event_id) REFERENCES itinerary_events (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_event_links_event_id ON itinerary_event_links (event_id);
