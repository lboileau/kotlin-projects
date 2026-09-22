package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.api.ExistingIngredient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class ScrapedRecipeJsonTest {
    private val garlic = ExistingIngredient(UUID.randomUUID(), "garlic", "produce", "clove")
    private val tomato = ExistingIngredient(UUID.randomUUID(), "tomato", "produce", "whole")
    private val catalogue = listOf(garlic, tomato)

    private fun line(
        ref: Int? = null,
        confidence: String? = null,
        quantity: BigDecimal = BigDecimal("2"),
        quantityAssumed: Boolean = false,
        unit: String = "clove",
        suggestedName: String? = null
    ) = ScrapedIngredientJson(
        originalText = "x", quantity = quantity, quantityAssumed = quantityAssumed, unit = unit,
        matchedIngredientRef = ref, matchConfidence = confidence,
        suggestedIngredientName = suggestedName, suggestedCategory = "produce", suggestedUnit = "whole"
    )

    @Test
    fun `high confidence match in the ingredient's default unit needs no review`() {
        val result = line(ref = 0, confidence = "HIGH").toDomain(catalogue)

        assertEquals(garlic.id, result.matchedIngredientId)
        assertEquals("HIGH", result.confidence)
        assertEquals(emptyList<String>(), result.reviewFlags)
        assertNull(result.suggestedIngredientName)
    }

    @Test
    fun `unit conversion flag is derived from the catalogue default unit, not the model`() {
        val result = line(ref = 1, confidence = "HIGH", unit = "oz", quantity = BigDecimal("8")).toDomain(catalogue)

        assertEquals(tomato.id, result.matchedIngredientId)
        assertEquals(listOf("UNIT_CONVERSION_NEEDED"), result.reviewFlags)
    }

    @Test
    fun `low confidence match is flagged uncertain and keeps the id`() {
        val result = line(ref = 1, confidence = "LOW", unit = "whole").toDomain(catalogue)

        assertEquals(tomato.id, result.matchedIngredientId)
        assertEquals("LOW", result.confidence)
        assertEquals(listOf("INGREDIENT_MATCH_UNCERTAIN"), result.reviewFlags)
    }

    @Test
    fun `no match becomes a new ingredient with a normalised suggestion`() {
        val result = line(ref = null, suggestedName = " Red Pepper Flakes ").toDomain(catalogue)

        assertNull(result.matchedIngredientId)
        assertEquals("red pepper flakes", result.suggestedIngredientName)
        assertEquals("LOW", result.confidence)
        assertEquals(listOf("NEW_INGREDIENT"), result.reviewFlags)
    }

    @Test
    fun `out of range ref is treated as no match rather than trusted`() {
        val result = line(ref = 99, confidence = "HIGH", suggestedName = "thing").toDomain(catalogue)

        assertNull(result.matchedIngredientId)
        assertEquals(listOf("NEW_INGREDIENT"), result.reviewFlags)
    }

    @Test
    fun `assumed or non-positive quantities are flagged and made valid`() {
        val assumed = line(ref = 0, confidence = "HIGH", quantityAssumed = true, unit = "pinch").toDomain(catalogue)
        assertEquals(listOf("UNIT_CONVERSION_NEEDED", "QUANTITY_ASSUMED"), assumed.reviewFlags)

        val zero = line(ref = 0, confidence = "HIGH", quantity = BigDecimal.ZERO).toDomain(catalogue)
        assertEquals(BigDecimal.ONE, zero.quantity)
        assertEquals(listOf("QUANTITY_ASSUMED"), zero.reviewFlags)
    }

    @Test
    fun `headings and notes are dropped, ingredients kept in order`() {
        val recipe = ScrapedRecipeJson(
            name = "Koftas", description = null, baseServings = 4, meal = null, theme = null,
            ingredients = listOf(
                line(ref = 0, confidence = "HIGH").copy(kind = LineKind.HEADING, originalText = "*FOR MARINADE*"),
                line(ref = 0, confidence = "HIGH").copy(originalText = "2 cloves garlic"),
                line(ref = null).copy(kind = LineKind.NOTE, originalText = "Pro tip: serve on a platter"),
                line(ref = 1, confidence = "HIGH").copy(originalText = "1 tomato", unit = "whole")
            )
        ).toDomain(catalogue)

        assertEquals(listOf("2 cloves garlic", "1 tomato"), recipe.ingredients.map { it.originalText })
    }

    @Test
    fun `recipe-level enums and servings are validated`() {
        val recipe = ScrapedRecipeJson(
            name = "Soup", description = null, baseServings = 0, meal = "brunch", theme = "soup", ingredients = emptyList()
        ).toDomain(catalogue)

        assertEquals(4, recipe.baseServings)
        assertNull(recipe.meal)
        assertEquals("soup", recipe.theme)
    }

    @Test
    fun `a blank name becomes a placeholder rather than an empty draft title`() {
        // A photo of a meal-kit card with the title out of frame: Sonnet 5 returned "" for the name
        // and a full ingredient list. That is a draft to rename, not an unreadable photo.
        val recipe = ScrapedRecipeJson(
            name = "  ", description = null, baseServings = 4, meal = null, theme = null,
            ingredients = listOf(line(ref = 0, confidence = "HIGH"))
        ).toDomain(catalogue)

        assertEquals(UNTITLED_NAME, recipe.name)
        assertEquals(1, recipe.ingredients.size)
        assertEquals("Pasta", ScrapedRecipeJson("Pasta ", null, 4, null, null, emptyList()).toDomain(catalogue).name)
    }
}
