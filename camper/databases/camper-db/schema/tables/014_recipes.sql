CREATE TABLE IF NOT EXISTS recipes (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name           VARCHAR(255) NOT NULL,
    description    TEXT,
    web_link       TEXT,
    base_servings  INT          NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'draft',
    created_by     UUID         NOT NULL,
    duplicate_of_id UUID,
    meal           VARCHAR(20),
    theme          VARCHAR(20),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_recipes_base_servings CHECK (base_servings > 0),
    CONSTRAINT ck_recipes_status CHECK (status IN ('draft', 'published')),
    CONSTRAINT fk_recipes_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_recipes_duplicate_of FOREIGN KEY (duplicate_of_id) REFERENCES recipes (id) ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_recipes_web_link ON recipes (web_link) WHERE web_link IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recipes_status ON recipes (status);
CREATE INDEX IF NOT EXISTS idx_recipes_created_by ON recipes (created_by);
CREATE INDEX IF NOT EXISTS idx_recipes_duplicate_of_id ON recipes (duplicate_of_id);
