CREATE TABLE IF NOT EXISTS user_dietary_restrictions (
    user_id     UUID        NOT NULL,
    restriction VARCHAR(30) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    PRIMARY KEY (user_id, restriction),
    CONSTRAINT fk_user_dietary_restrictions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_user_dietary_restrictions_restriction CHECK (
        restriction IN ('gluten_free', 'nut_allergy', 'vegetarian', 'vegan', 'lactose_intolerant', 'shellfish_allergy', 'halal', 'kosher')
    )
);

CREATE INDEX IF NOT EXISTS idx_user_dietary_restrictions_user_id ON user_dietary_restrictions (user_id);
