CREATE TABLE IF NOT EXISTS itineraries (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id    UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL    DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL    DEFAULT now(),

    CONSTRAINT uq_itineraries_plan_id UNIQUE (plan_id),
    CONSTRAINT fk_itineraries_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE
);
