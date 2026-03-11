package com.acme.clients.recipescraperclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import org.slf4j.LoggerFactory
import java.math.BigDecimal

internal class NoOpRecipeScraperClient : RecipeScraperClient {
    private val logger = LoggerFactory.getLogger(NoOpRecipeScraperClient::class.java)

    override fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError> {
        logger.info("NoOpRecipeScraperClient: would scrape url={}", param.sourceUrl)
        val cannedRecipe = ScrapedRecipe(
            name = "Classic Guacamole",
            description = "A simple and delicious guacamole recipe.",
            baseServings = 4,
            ingredients = listOf(
                ScrapedIngredient(
                    originalText = "3 ripe avocados",
                    quantity = BigDecimal("3"),
                    unit = "whole",
                    matchedIngredientId = null,
                    suggestedIngredientName = "avocado",
                    confidence = "HIGH",
                    reviewFlags = listOf("NEW_INGREDIENT")
                ),
                ScrapedIngredient(
                    originalText = "1/2 cup finely diced red onion",
                    quantity = BigDecimal("0.5"),
                    unit = "cup",
                    matchedIngredientId = null,
                    suggestedIngredientName = "red onion",
                    confidence = "LOW",
                    reviewFlags = listOf("NEW_INGREDIENT", "INGREDIENT_MATCH_UNCERTAIN")
                )
            )
        )
        return success(cannedRecipe)
    }
}
