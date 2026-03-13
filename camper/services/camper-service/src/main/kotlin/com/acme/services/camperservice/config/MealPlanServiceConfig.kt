package com.acme.services.camperservice.config

import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.mealplan.service.MealPlanService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MealPlanServiceConfig {
    @Bean
    fun mealPlanService(
        mealPlanClient: MealPlanClient,
        recipeClient: RecipeClient,
        ingredientClient: IngredientClient,
    ): MealPlanService = MealPlanService(mealPlanClient, recipeClient, ingredientClient)
}
