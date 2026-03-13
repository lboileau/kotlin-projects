CREATE TABLE IF NOT EXISTS log_book_faqs (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id        UUID        NOT NULL,
    question       TEXT        NOT NULL,
    asked_by_id    UUID        NOT NULL,
    answer         TEXT,
    answered_by_id UUID,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_log_book_faqs_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_log_book_faqs_asked_by FOREIGN KEY (asked_by_id) REFERENCES users (id),
    CONSTRAINT fk_log_book_faqs_answered_by FOREIGN KEY (answered_by_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_log_book_faqs_plan_id ON log_book_faqs (plan_id);
