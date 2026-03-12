package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetPurchasesParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.libs.mealplancalculator.UnitConverter
import com.acme.libs.mealplancalculator.model.ScalingMode
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDetailResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanDayResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanIngredientResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanRecipeDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.mapper.MealPlanMapper
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.UUID
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam

/**
 * Shared logic for building [MealPlanDetailResponse] from a [MealPlan].
 * Used by GetMealPlanDetailAction, GetMealPlanByPlanIdAction, and CopyToTripAction.
 */
internal object MealPlanDetailBuilder {

    fun buildDetail(
        mealPlan: MealPlan,
        mealPlanClient: MealPlanClient,
        recipeClient: RecipeClient,
        ingredientClient: IngredientClient,
    ): Result<MealPlanDetailResponse, MealPlanError> {
        val days = when (val result = mealPlanClient.getDays(GetDaysParam(mealPlan.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("days", result.error.message))
        }

        // Load purchases for fully-purchased check
        val purchases = when (val result = mealPlanClient.getPurchases(GetPurchasesParam(mealPlan.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("purchases", result.error.message))
        }
        val purchaseMap = purchases.associateBy { "${it.ingredientId}:${it.unit}" }

        val scalingMode = parseScalingMode(mealPlan.scalingMode)

        val dayResponses = mutableListOf<MealPlanDayResponse>()
        for (day in days) {
            val mealPlanRecipes = when (val result = mealPlanClient.getRecipesByDayId(GetRecipesByDayIdParam(day.id))) {
                is Result.Success -> result.value
                is Result.Failure -> return Result.Failure(MealPlanError.Invalid("recipes", result.error.message))
            }

            val recipesByMealType = mutableMapOf<String, MutableList<MealPlanRecipeDetailResponse>>()
            for (mpr in mealPlanRecipes) {
                val recipeDetail = buildRecipeDetail(mpr, mealPlan, scalingMode, recipeClient, ingredientClient, purchaseMap)
                    ?: continue
                recipesByMealType.getOrPut(mpr.mealType) { mutableListOf() }.add(recipeDetail)
            }

            dayResponses.add(MealPlanMapper.toMealPlanDayResponse(day, recipesByMealType))
        }

        return Result.Success(
            MealPlanDetailResponse(
                id = mealPlan.id,
                planId = mealPlan.planId,
                name = mealPlan.name,
                servings = mealPlan.servings,
                scalingMode = mealPlan.scalingMode,
                isTemplate = mealPlan.isTemplate,
                sourceTemplateId = mealPlan.sourceTemplateId,
                createdBy = mealPlan.createdBy,
                days = dayResponses,
                createdAt = mealPlan.createdAt,
                updatedAt = mealPlan.updatedAt,
            )
        )
    }

    private fun buildRecipeDetail(
        mpr: MealPlanRecipe,
        mealPlan: MealPlan,
        scalingMode: ScalingMode,
        recipeClient: RecipeClient,
        ingredientClient: IngredientClient,
        purchaseMap: Map<String, com.acme.clients.mealplanclient.model.ShoppingListPurchase>,
    ): MealPlanRecipeDetailResponse? {
        val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(mpr.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return null
        }

        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return null
        }

        // Only approved ingredients with resolved ingredientId
        val approvedIngredients = recipeIngredients.filter { it.status == "approved" && it.ingredientId != null }

        val scaleFactor = computeScaleFactor(mealPlan.servings, recipe.baseServings, scalingMode)

        // Build ingredient responses with scaling
        val ingredientResponses = approvedIngredients.mapNotNull { ri ->
            val ingredient = when (val result = ingredientClient.getById(IngredientGetByIdParam(ri.ingredientId!!))) {
                is Result.Success -> result.value
                is Result.Failure -> return@mapNotNull null
            }

            val scaledQuantity = ri.quantity.multiply(scaleFactor)

            MealPlanIngredientResponse(
                recipeIngredientId = ri.id,
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                category = ingredient.category,
                quantity = ri.quantity,
                scaledQuantity = scaledQuantity,
                unit = ri.unit,
            )
        }

        // Check if all ingredients are fully purchased.
        // Purchases are stored with the bestFit-resolved unit (e.g., "cup" not "tsp"),
        // so we must convert each ingredient's scaled quantity to bestFit before comparing.
        val isFullyPurchased = ingredientResponses.isNotEmpty() && ingredientResponses.all { ing ->
            val (bestFitQty, bestFitUnit) = UnitConverter.bestFit(ing.scaledQuantity, ing.unit)
            val purchase = purchaseMap["${ing.ingredientId}:${bestFitUnit}"]
            purchase != null && purchase.quantityPurchased >= bestFitQty
        }

        return MealPlanRecipeDetailResponse(
            id = mpr.id,
            recipeId = recipe.id,
            recipeName = recipe.name,
            baseServings = recipe.baseServings,
            scaleFactor = scaleFactor,
            isFullyPurchased = isFullyPurchased,
            ingredients = ingredientResponses,
        )
    }

    private fun computeScaleFactor(servings: Int, baseServings: Int, scalingMode: ScalingMode): BigDecimal {
        val ratio = BigDecimal(servings).divide(BigDecimal(baseServings), 10, RoundingMode.HALF_UP)
        return when (scalingMode) {
            ScalingMode.FRACTIONAL -> ratio
            ScalingMode.ROUND_UP -> ratio.setScale(0, RoundingMode.CEILING)
        }
    }

    internal fun parseScalingMode(mode: String): ScalingMode = when (mode) {
        "round_up" -> ScalingMode.ROUND_UP
        else -> ScalingMode.FRACTIONAL
    }
}
