package com.acme.clients.recipescraperclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.success
import com.acme.clients.recipescraperclient.api.RecipeScraperClient
import com.acme.clients.recipescraperclient.api.ScrapeRecipeParam
import com.acme.clients.recipescraperclient.model.ScrapedIngredient
import com.acme.clients.recipescraperclient.model.ScrapedRecipe
import java.math.BigDecimal

class FakeRecipeScraperClient : RecipeScraperClient {

    var nextResult: Result<ScrapedRecipe, AppError>? = null
    var lastParam: ScrapeRecipeParam? = null

    override fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError> {
        lastParam = param
        return nextResult ?: success(defaultScrapedRecipe())
    }

    fun reset() {
        nextResult = null
        lastParam = null
    }

    private fun defaultScrapedRecipe() = ScrapedRecipe(
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
            )
        )
    )
}
