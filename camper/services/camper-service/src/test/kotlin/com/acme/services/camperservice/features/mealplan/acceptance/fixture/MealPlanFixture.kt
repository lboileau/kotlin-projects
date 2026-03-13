package com.acme.services.camperservice.features.mealplan.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class MealPlanFixture(private val jdbcTemplate: JdbcTemplate) {

    fun insertUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user-${UUID.randomUUID().toString().take(8)}@example.com",
        username: String? = "user-${UUID.randomUUID().toString().take(8)}"
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO users (id, email, username, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, email, username,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertPlan(
        id: UUID = UUID.randomUUID(),
        name: String = "Plan-${UUID.randomUUID().toString().take(8)}",
        visibility: String = "private",
        ownerId: UUID
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO plans (id, name, visibility, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, name, visibility, ownerId,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertPlanMember(planId: UUID, userId: UUID) {
        jdbcTemplate.update(
            "INSERT INTO plan_members (plan_id, user_id, created_at) VALUES (?, ?, ?)",
            planId, userId, java.sql.Timestamp.from(Instant.now())
        )
    }

    fun insertIngredient(
        id: UUID = UUID.randomUUID(),
        name: String = "Ingredient-${UUID.randomUUID().toString().take(8)}",
        category: String = "produce",
        defaultUnit: String = "pieces"
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO ingredients (id, name, category, default_unit, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, name, category, defaultUnit,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertRecipe(
        id: UUID = UUID.randomUUID(),
        name: String = "Recipe-${UUID.randomUUID().toString().take(8)}",
        baseServings: Int = 4,
        status: String = "published",
        createdBy: UUID
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO recipes (id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, created_at, updated_at)
            VALUES (?, ?, NULL, NULL, ?, ?, ?, NULL, ?, ?)
            """.trimIndent(),
            id, name, baseServings, status, createdBy,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertRecipeIngredient(
        id: UUID = UUID.randomUUID(),
        recipeId: UUID,
        ingredientId: UUID,
        quantity: BigDecimal,
        unit: String,
        status: String = "approved"
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO recipe_ingredients
                (id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at)
            VALUES (?, ?, ?, NULL, ?, ?, ?, NULL, NULL, CAST('[]' AS jsonb), ?, ?)
            """.trimIndent(),
            id, recipeId, ingredientId, quantity, unit, status,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertMealPlan(
        id: UUID = UUID.randomUUID(),
        planId: UUID? = null,
        name: String = "Meal Plan",
        servings: Int = 4,
        scalingMode: String = "fractional",
        isTemplate: Boolean = false,
        sourceTemplateId: UUID? = null,
        createdBy: UUID
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO meal_plans (id, plan_id, name, servings, scaling_mode, is_template, source_template_id, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, planId, name, servings, scalingMode, isTemplate, sourceTemplateId, createdBy,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertMealPlanDay(
        id: UUID = UUID.randomUUID(),
        mealPlanId: UUID,
        dayNumber: Int
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO meal_plan_days (id, meal_plan_id, day_number, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
            id, mealPlanId, dayNumber,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertMealPlanRecipe(
        id: UUID = UUID.randomUUID(),
        mealPlanDayId: UUID,
        mealType: String,
        recipeId: UUID
    ): UUID {
        jdbcTemplate.update(
            "INSERT INTO meal_plan_recipes (id, meal_plan_day_id, meal_type, recipe_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
            id, mealPlanDayId, mealType, recipeId,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertShoppingListPurchase(
        id: UUID = UUID.randomUUID(),
        mealPlanId: UUID,
        ingredientId: UUID,
        unit: String,
        quantityPurchased: BigDecimal
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO shopping_list_purchases (id, meal_plan_id, ingredient_id, unit, quantity_purchased, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, mealPlanId, ingredientId, unit, quantityPurchased,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute(
            "TRUNCATE TABLE shopping_list_purchases, meal_plan_recipes, meal_plan_days, meal_plans, recipe_ingredients, recipes, ingredients, plan_members, plans, users CASCADE"
        )
    }
}
