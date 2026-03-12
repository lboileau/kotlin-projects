package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.InternalError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.UUID

internal class AnthropicRecipeScraperClient(
    private val client: AnthropicClient
) : RecipeScraperClient {
    private val logger = LoggerFactory.getLogger(AnthropicRecipeScraperClient::class.java)
    private val mapper = jacksonObjectMapper()

    override fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError> {
        logger.info("Scraping recipe from url={}", param.sourceUrl)
        return try {
            val params = MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_4_6)
                .maxTokens(4096L)
                .system(SYSTEM_PROMPT)
                .addUserMessage(buildUserMessage(param))
                .build()

            val responseText = StringBuilder()
            client.messages().createStreaming(params).use { stream ->
                stream.stream()
                    .flatMap { event -> event.contentBlockDelta().stream() }
                    .flatMap { deltaEvent -> deltaEvent.delta().text().stream() }
                    .forEach { textDelta -> responseText.append(textDelta.text()) }
            }

            val json = extractJson(responseText.toString())
            val scraped = mapper.readValue<ScrapedRecipeJson>(json)
            success(scraped.toDomain())
        } catch (e: com.anthropic.errors.RateLimitException) {
            logger.warn("Rate limited while scraping url={}", param.sourceUrl)
            failure(InternalError("Rate limited by AI service — please wait a moment and try again"))
        } catch (e: com.anthropic.errors.UnauthorizedException) {
            logger.error("Authentication failed for AI service")
            failure(InternalError("AI service authentication failed — check your API key"))
        } catch (e: com.anthropic.errors.PermissionDeniedException) {
            logger.error("Permission denied by AI service")
            failure(InternalError("AI service permission denied — check your API key permissions"))
        } catch (e: com.anthropic.errors.BadRequestException) {
            logger.error("Bad request to AI service: {}", e.message)
            failure(InternalError("AI service rejected the request — the recipe page may be too large"))
        } catch (e: com.fasterxml.jackson.core.JsonProcessingException) {
            logger.error("Failed to parse AI response for url={}: {}", param.sourceUrl, e.message)
            failure(InternalError("Could not read the recipe from this page — the AI returned an unexpected format"))
        } catch (e: Exception) {
            logger.error("Failed to scrape recipe from url={}: {}", param.sourceUrl, e.message)
            failure(InternalError("Something went wrong importing this recipe — please try again"))
        }
    }

    private fun buildUserMessage(param: ScrapeRecipeParam): String {
        val ingredientsJson = mapper.writeValueAsString(
            param.existingIngredients.map { ing ->
                mapOf(
                    "id" to ing.id.toString(),
                    "name" to ing.name,
                    "category" to ing.category,
                    "defaultUnit" to ing.defaultUnit
                )
            }
        )
        // Trim to ~100KB to avoid excessive token usage
        val html = if (param.html.length > 100_000) param.html.take(100_000) else param.html

        return """
            Source URL: ${param.sourceUrl}

            Existing global ingredients (JSON array):
            $ingredientsJson

            Recipe page HTML:
            $html
        """.trimIndent()
    }

    /** Strip markdown code fences if Claude wraps the JSON in ```json ... ``` */
    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("```")) {
            val start = trimmed.indexOf('\n') + 1
            val end = trimmed.lastIndexOf("```")
            if (start > 0 && end > start) return trimmed.substring(start, end).trim()
        }
        return trimmed
    }

    // Internal DTOs for parsing Claude's JSON response

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ScrapedRecipeJson(
        val name: String,
        val description: String?,
        @JsonProperty("baseServings") val baseServings: Int,
        val meal: String?,
        val theme: String?,
        val ingredients: List<ScrapedIngredientJson>
    ) {
        fun toDomain() = ScrapedRecipe(
            name = name,
            description = description,
            baseServings = baseServings,
            meal = meal,
            theme = theme,
            ingredients = ingredients.map { it.toDomain() }
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private data class ScrapedIngredientJson(
        val originalText: String,
        val quantity: BigDecimal,
        val unit: String,
        val matchedIngredientId: String?,
        val suggestedIngredientName: String?,
        val suggestedCategory: String?,
        val suggestedUnit: String?,
        val confidence: String,
        val reviewFlags: List<String>
    ) {
        fun toDomain() = ScrapedIngredient(
            originalText = originalText,
            quantity = quantity,
            unit = unit,
            matchedIngredientId = matchedIngredientId?.let { UUID.fromString(it) },
            suggestedIngredientName = suggestedIngredientName,
            suggestedCategory = suggestedCategory,
            suggestedUnit = suggestedUnit,
            confidence = confidence,
            reviewFlags = reviewFlags
        )
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are a recipe parsing assistant. Extract structured recipe data from HTML and normalize ingredients against a provided global list.

            Given HTML from a recipe page and a list of existing global ingredients, you will:
            1. Extract the recipe name, description (if available), and number of servings
            2. Classify the recipe's meal type and theme
            3. Extract each ingredient with its quantity and unit
            4. Match each ingredient against the provided existing ingredients list by name similarity
            5. Normalize units to one of: g, kg, ml, l, tsp, tbsp, cup, oz, lb, pieces, whole, bunch, can, clove, pinch, slice, sprig
            6. Flag ingredients that need review

            Meal classification (pick the best fit, or null if unclear):
            - One of: breakfast, lunch, dinner, snack, dessert, appetizer, side, drink

            Theme classification (pick the best fit based on the primary protein or style, or null if unclear):
            - One of: chicken, beef, pork, fish, seafood, vegetarian, vegan, pasta, soup, salad, other

            Matching rules:
            - Clear match (high confidence): set matchedIngredientId to the existing ingredient's id UUID string, confidence = "HIGH", reviewFlags = []
            - Uncertain match: set matchedIngredientId to the closest match UUID string, confidence = "LOW", reviewFlags = ["INGREDIENT_MATCH_UNCERTAIN"]
            - No match found: set matchedIngredientId = null, suggestedIngredientName = normalized lowercase singular name, confidence = "LOW", reviewFlags = ["NEW_INGREDIENT"]
            - Unit differs from ingredient defaultUnit: also add "UNIT_CONVERSION_NEEDED" to reviewFlags

            For unmatched or uncertain ingredients, also suggest a category and default unit:
            - suggestedCategory: one of: produce, dairy, meat, seafood, pantry, spice, condiment, frozen, bakery, other
            - suggestedUnit: one of: g, kg, ml, l, tsp, tbsp, cup, oz, lb, pieces, whole, bunch, can, clove, pinch, slice, sprig
            These help pre-populate the "create new ingredient" form during review.

            Respond with ONLY valid JSON (no markdown, no explanation) matching this exact schema:
            {
              "name": "string",
              "description": "string or null",
              "baseServings": number,
              "meal": "string or null",
              "theme": "string or null",
              "ingredients": [
                {
                  "originalText": "string",
                  "quantity": number,
                  "unit": "string",
                  "matchedIngredientId": "UUID string or null",
                  "suggestedIngredientName": "string or null",
                  "suggestedCategory": "string or null",
                  "suggestedUnit": "string or null",
                  "confidence": "HIGH or LOW",
                  "reviewFlags": []
                }
              ]
            }
        """.trimIndent()
    }
}
