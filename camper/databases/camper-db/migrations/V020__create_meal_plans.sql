CREATE TABLE IF NOT EXISTS meal_plans (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id            UUID,
    name               VARCHAR(255) NOT NULL,
    servings           INT          NOT NULL,
    scaling_mode       VARCHAR(20)  NOT NULL DEFAULT 'fractional',
    is_template        BOOLEAN      NOT NULL DEFAULT false,
    source_template_id UUID,
    created_by         UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_meal_plans_servings CHECK (servings > 0),
    CONSTRAINT ck_meal_plans_scaling_mode CHECK (scaling_mode IN ('fractional', 'round_up')),
    CONSTRAINT fk_meal_plans_plan FOREIGN KEY (plan_id) REFERENCES plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_meal_plans_source_template FOREIGN KEY (source_template_id) REFERENCES meal_plans (id) ON DELETE SET NULL,
    CONSTRAINT fk_meal_plans_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_meal_plans_plan_id ON meal_plans (plan_id) WHERE plan_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_meal_plans_plan_id ON meal_plans (plan_id);
CREATE INDEX IF NOT EXISTS idx_meal_plans_is_template ON meal_plans (is_template);
CREATE INDEX IF NOT EXISTS idx_meal_plans_source_template_id ON meal_plans (source_template_id);
CREATE INDEX IF NOT EXISTS idx_meal_plans_created_by ON meal_plans (created_by);
