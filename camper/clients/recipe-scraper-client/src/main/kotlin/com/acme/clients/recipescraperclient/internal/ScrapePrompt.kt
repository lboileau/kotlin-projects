package com.acme.clients.recipescraperclient.internal

import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam

/** Everything sent to the model for one scrape, independent of the transport. */
internal data class ScrapePrompt(
    val system: String,
    val userMessage: String,
    val schema: Map<String, Any>,
    val content: RecipePageContent
)

internal object ScrapePromptBuilder {

    fun build(param: ScrapeRecipeParam): ScrapePrompt {
        val content = RecipePageExtractor.extract(param.html)
        return ScrapePrompt(
            system = SYSTEM_PROMPT,
            userMessage = buildUserMessage(param, content),
            schema = scrapedRecipeSchema(),
            content = content
        )
    }

    private fun buildUserMessage(param: ScrapeRecipeParam, content: RecipePageContent): String {
        // Ref numbers instead of UUIDs: the model picks an index and we resolve it to the id.
        // Category/default unit are omitted — the unit-conversion flag is derived in Kotlin.
        val catalogue = param.existingIngredients
            .mapIndexed { ref, ing -> "$ref: ${ing.name}" }
            .joinToString("\n")

        val contentLabel = when (content) {
            is RecipePageContent.JsonLd -> "Recipe data (schema.org JSON-LD extracted from the page)"
            is RecipePageContent.VisibleText -> "Recipe page text (visible text only; scripts and styles removed)"
        }

        return """
            Source URL: ${param.sourceUrl}

            Ingredient catalogue (ref: name):
            $catalogue

            $contentLabel:
            ${content.text}
        """.trimIndent()
    }

    val SYSTEM_PROMPT = """
        You extract structured recipe data and normalise the ingredient lines against a catalogue of known ingredients.

        You receive the recipe page's content and an ingredient catalogue as numbered lines ("ref: name").

        Recipe:
        - name: the recipe title. description: a short description, or null if the source has none.
        - baseServings: the number of servings the recipe makes. If given as a range, use the larger value. If not stated, use 4.
        - meal: breakfast, lunch, dinner, snack, dessert, appetizer, side, drink — or null if unclear.
        - theme: chicken, beef, pork, fish, seafood, vegetarian, vegan, pasta, soup, salad, other — based on the primary protein or style, or null if unclear.

        Ingredient lines — one output entry per source line, in source order. Never merge, drop, or invent lines:
        - kind: INGREDIENT for something to buy. Source lists often contain section labels ("For the sauce:", "*FOR MARINADE*") — mark those HEADING — and tips, substitutions, serving ideas or "and more!" lists — mark those NOTE. For HEADING and NOTE still fill every field (quantity 1, unit pieces, no match); they are discarded later.
        - originalText: the line exactly as written in the source.
        - quantity: a decimal. Convert fractions and mixed numbers (1/2 → 0.5, 1 ½ → 1.5). For a range use the larger value. When two measures are given (e.g. "1 cup (200 g)"), use the first. Sum additive amounts in the same unit family ("1/3 cup + 2 tbsp" → 0.458 cup). If the line has no quantity ("salt to taste", "parsley for garnish"), set quantity to 1 with a sensible unit (pinch for seasonings, sprig or pieces for garnish) and quantityAssumed to true; otherwise quantityAssumed is false.
        - unit: g, kg, ml, l, tsp, tbsp, cup, oz, lb, pieces, whole, bunch, can, clove, pinch, slice, sprig. Map teaspoon → tsp, tablespoon → tbsp, ounce → oz, pound → lb, "2 large eggs" → whole, "1 (15 oz) can" → can. Countable items with no unit are "whole"; use "pieces" only when "whole" reads wrongly.

        Catalogue matching — the goal is a correct shopping list, so match on what you would buy:
        - Ignore preparation and state words (diced, minced, fresh, cooked, boneless, chopped, to taste).
        - A more specific variety of a generic catalogue item is a HIGH match (jasmine rice → rice, grape tomatoes → tomato, boneless chicken thighs → chicken thighs).
        - If the catalogue item is only a loose substitute — a different cut, a different product, or a generic entry where the recipe wants something specific (feta → cheese, lemon juice → lemon) — set the ref and matchConfidence LOW.
        - If nothing in the catalogue is the same purchase, set matchedIngredientRef to null and matchConfidence to null.
        - Whenever matchedIngredientRef is null: suggestedIngredientName is the lowercase singular grocery name (e.g. "red pepper flakes", "spinach", "chickpeas"), plus a suggestedCategory (produce, dairy, meat, seafood, pantry, spice, condiment, frozen, bakery, other) and a suggestedUnit the item is normally bought in. Otherwise set all three to null.
    """.trimIndent()
}
