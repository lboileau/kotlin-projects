package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.api.ExistingIngredient
import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

/** Units the model may emit; also the unit vocabulary used across the app. */
internal val ALLOWED_UNITS = listOf(
    "g", "kg", "ml", "l", "tsp", "tbsp", "cup", "oz", "lb",
    "pieces", "whole", "bunch", "can", "clove", "pinch", "slice", "sprig"
)
internal val ALLOWED_CATEGORIES = listOf(
    "produce", "dairy", "meat", "seafood", "pantry", "spice", "condiment", "frozen", "bakery", "other"
)
internal val ALLOWED_MEALS = listOf("breakfast", "lunch", "dinner", "snack", "dessert", "appetizer", "side", "drink")
internal val ALLOWED_THEMES = listOf(
    "chicken", "beef", "pork", "fish", "seafood", "vegetarian", "vegan", "pasta", "soup", "salad", "other"
)

private const val DEFAULT_SERVINGS = 4
internal const val UNTITLED_NAME = "Untitled recipe"

/**
 * The model's structured response. The model refers to catalogue ingredients by their position
 * in the list we sent (`matchedIngredientRef`) rather than by UUID — a one-character slip in a
 * transcribed UUID used to fail the whole import.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ScrapedRecipeJson(
    val name: String,
    val description: String?,
    val baseServings: Int,
    val meal: String?,
    val theme: String?,
    val ingredients: List<ScrapedIngredientJson>
) {
    /**
     * Resolves refs to ids and derives review flags deterministically. The model only reports what
     * it read (quantity, unit, match + confidence); whether that needs review is decided here.
     */
    fun toDomain(catalogue: List<ExistingIngredient>) = ScrapedRecipe(
        // A photo of an ingredient list can have no title in frame; the model is told to invent one,
        // but when it leaves the name blank the draft still needs one — the user renames it in review.
        name = name.trim().ifEmpty { UNTITLED_NAME },
        description = description,
        baseServings = if (baseServings > 0) baseServings else DEFAULT_SERVINGS,
        meal = meal?.takeIf { it in ALLOWED_MEALS },
        theme = theme?.takeIf { it in ALLOWED_THEMES },
        // Sites put section headings ("*FOR MARINADE*") and notes ("Pro tip: ...") in their ingredient
        // lists; the model labels them and we drop them here rather than asking it to skip lines.
        ingredients = ScrapedIngredientMerger.merge(
            ingredients
                .filter { it.kind == LineKind.INGREDIENT && it.originalText.isNotBlank() }
                .map { it.toDomain(catalogue) }
        )
    )
}

internal enum class LineKind { INGREDIENT, HEADING, NOTE }

@JsonIgnoreProperties(ignoreUnknown = true)
internal data class ScrapedIngredientJson(
    val kind: LineKind = LineKind.INGREDIENT,
    val originalText: String,
    val quantity: BigDecimal,
    val quantityAssumed: Boolean,
    val unit: String,
    val matchedIngredientRef: Int?,
    val matchConfidence: String?,
    val suggestedIngredientName: String?,
    val suggestedCategory: String?,
    val suggestedUnit: String?
) {
    fun toDomain(catalogue: List<ExistingIngredient>): ScrapedIngredient {
        val matched = matchedIngredientRef?.let { catalogue.getOrNull(it) }
        val highConfidence = matched != null && matchConfidence == "HIGH"
        val unit = unit.takeIf { it in ALLOWED_UNITS } ?: "pieces"
        val quantity = quantity.takeIf { it > BigDecimal.ZERO } ?: BigDecimal.ONE

        val flags = buildList {
            if (matched == null) add("NEW_INGREDIENT")
            else if (!highConfidence) add("INGREDIENT_MATCH_UNCERTAIN")
            if (matched != null && unit != matched.defaultUnit) add("UNIT_CONVERSION_NEEDED")
            if (quantityAssumed || quantity != this@ScrapedIngredientJson.quantity || unit != this@ScrapedIngredientJson.unit) {
                add("QUANTITY_ASSUMED")
            }
        }

        return ScrapedIngredient(
            originalText = originalText,
            quantity = quantity,
            unit = unit,
            matchedIngredientId = matched?.id,
            suggestedIngredientName = if (matched == null) suggestedIngredientName?.trim()?.lowercase()?.ifBlank { null } else null,
            suggestedCategory = suggestedCategory?.takeIf { it in ALLOWED_CATEGORIES },
            suggestedUnit = suggestedUnit?.takeIf { it in ALLOWED_UNITS },
            confidence = if (highConfidence) "HIGH" else "LOW",
            reviewFlags = flags
        )
    }
}

/** JSON Schema for [ScrapedRecipeJson], sent as the structured-output format. */
internal fun scrapedRecipeSchema(): Map<String, Any> {
    fun nullable(type: String, extra: Map<String, Any> = emptyMap()) =
        mapOf("type" to listOf(type, "null")) + extra
    fun enumOf(values: List<String>) = mapOf("type" to "string", "enum" to values)
    fun nullableEnum(values: List<String>) = mapOf("anyOf" to listOf(enumOf(values), mapOf("type" to "null")))

    val ingredient = mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
            "kind" to mapOf(
                "type" to "string", "enum" to listOf("INGREDIENT", "HEADING", "NOTE"),
                "description" to "INGREDIENT for a real ingredient line; HEADING for a section label like 'For the sauce'; NOTE for tips, substitutions, serving suggestions or anything else that is not something to buy"
            ),
            "originalText" to mapOf("type" to "string", "description" to "The line exactly as written in the source"),
            "quantity" to mapOf("type" to "number"),
            "quantityAssumed" to mapOf("type" to "boolean", "description" to "true when the source gives no quantity and one was assumed"),
            "unit" to enumOf(ALLOWED_UNITS),
            "matchedIngredientRef" to nullable("integer", mapOf("description" to "ref of the catalogue ingredient this line is, or null if none fits")),
            "matchConfidence" to nullableEnum(listOf("HIGH", "LOW")),
            "suggestedIngredientName" to nullable("string"),
            "suggestedCategory" to nullableEnum(ALLOWED_CATEGORIES),
            "suggestedUnit" to nullableEnum(ALLOWED_UNITS)
        ),
        "required" to listOf(
            "kind", "originalText", "quantity", "quantityAssumed", "unit", "matchedIngredientRef",
            "matchConfidence", "suggestedIngredientName", "suggestedCategory", "suggestedUnit"
        )
    )

    return mapOf(
        "type" to "object",
        "additionalProperties" to false,
        "properties" to mapOf(
            "name" to mapOf("type" to "string"),
            "description" to nullable("string"),
            "baseServings" to mapOf("type" to "integer"),
            "meal" to nullableEnum(ALLOWED_MEALS),
            "theme" to nullableEnum(ALLOWED_THEMES),
            "ingredients" to mapOf("type" to "array", "items" to ingredient)
        ),
        "required" to listOf("name", "description", "baseServings", "meal", "theme", "ingredients")
    )
}
