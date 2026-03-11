package com.acme.clients.recipescraperclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipescraperclient.model.ScrapedRecipe

interface RecipeScraperClient {
    fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError>
}
