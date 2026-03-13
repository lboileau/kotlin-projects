CREATE TABLE IF NOT EXISTS meal_plan_days (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meal_plan_id UUID        NOT NULL,
    day_number   INT         NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plan_days_day_number CHECK (day_number > 0),
    CONSTRAINT uq_meal_plan_days_meal_plan_id_day_number UNIQUE (meal_plan_id, day_number),
    CONSTRAINT fk_meal_plan_days_meal_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plans (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_meal_plan_days_meal_plan_id ON meal_plan_days (meal_plan_id);
