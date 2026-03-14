CREATE TABLE IF NOT EXISTS log_book_journal_entries (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id     UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    page_number INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_log_book_journal_entries_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_log_book_journal_entries_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_id ON log_book_journal_entries (plan_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_log_book_journal_entries_plan_page ON log_book_journal_entries (plan_id, page_number);
