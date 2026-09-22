package com.acme.clients.recipescraperclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.recipescraperclient.model.ScrapedRecipe

interface RecipeScraperClient {
    /** A recipe from a web page's HTML. */
    fun scrape(param: ScrapeRecipeParam): Result<ScrapedRecipe, AppError>

    /** A recipe from photos of it. Same output, same catalogue matching, no page to fetch. */
    fun scrapeImages(param: ScrapeRecipeImagesParam): Result<ScrapedRecipe, AppError>
}
