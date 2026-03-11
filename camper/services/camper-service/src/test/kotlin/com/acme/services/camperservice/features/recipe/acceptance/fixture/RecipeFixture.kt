package com.acme.services.camperservice.features.recipe.acceptance.fixture

import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class RecipeFixture(private val jdbcTemplate: JdbcTemplate) {

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
        description: String? = null,
        webLink: String? = null,
        baseServings: Int = 4,
        status: String = "published",
        createdBy: UUID,
        duplicateOfId: UUID? = null
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO recipes (id, name, description, web_link, base_servings, status, created_by, duplicate_of_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            id, name, description, webLink, baseServings, status, createdBy, duplicateOfId,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun insertRecipeIngredient(
        id: UUID = UUID.randomUUID(),
        recipeId: UUID,
        ingredientId: UUID? = null,
        originalText: String? = null,
        quantity: BigDecimal = BigDecimal("1.0"),
        unit: String = "pieces",
        status: String = "approved",
        matchedIngredientId: UUID? = null,
        suggestedIngredientName: String? = null
    ): UUID {
        jdbcTemplate.update(
            """
            INSERT INTO recipe_ingredients
                (id, recipe_id, ingredient_id, original_text, quantity, unit, status, matched_ingredient_id, suggested_ingredient_name, review_flags, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CAST('[]' AS jsonb), ?, ?)
            """.trimIndent(),
            id, recipeId, ingredientId, originalText, quantity, unit, status,
            matchedIngredientId, suggestedIngredientName,
            java.sql.Timestamp.from(Instant.now()), java.sql.Timestamp.from(Instant.now())
        )
        return id
    }

    fun truncateAll() {
        jdbcTemplate.execute("TRUNCATE TABLE recipe_ingredients, recipes, ingredients, users CASCADE")
    }
}
