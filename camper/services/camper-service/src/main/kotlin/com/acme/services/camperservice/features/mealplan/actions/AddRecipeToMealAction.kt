package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.GetByIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.libs.mealplancalculator.model.ScalingMode
import com.acme.services.camperservice.features.mealplan.dto.MealPlanIngredientResponse
import com.acme.services.camperservice.features.mealplan.dto.MealPlanRecipeDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.AddRecipeToMealParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateAddRecipeToMeal
import java.math.BigDecimal
import java.math.RoundingMode
import com.acme.clients.mealplanclient.api.AddRecipeParam as ClientAddRecipeParam
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam

internal class AddRecipeToMealAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateAddRecipeToMeal()

    fun execute(param: AddRecipeToMealParam): Result<MealPlanRecipeDetailResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Validate recipe exists
        val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(param.recipeId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.RecipeNotFound(param.recipeId))
                else -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
            }
        }

        // Load meal plan for scaling info
        val mealPlan = when (val result = mealPlanClient.getById(GetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        val mpr = when (val result = mealPlanClient.addRecipe(
            ClientAddRecipeParam(
                mealPlanDayId = param.dayId,
                mealType = param.mealType,
                recipeId = param.recipeId,
            )
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
        }

        // Build the recipe detail response
        val scalingMode = MealPlanDetailBuilder.parseScalingMode(mealPlan.scalingMode)
        val ratio = BigDecimal(mealPlan.servings).divide(BigDecimal(recipe.baseServings), 10, RoundingMode.HALF_UP)
        val scaleFactor = when (scalingMode) {
            ScalingMode.FRACTIONAL -> ratio
            ScalingMode.ROUND_UP -> ratio.setScale(0, RoundingMode.CEILING)
        }

        val recipeIngredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
            is Result.Success -> result.value
            is Result.Failure -> emptyList()
        }

        val approvedIngredients = recipeIngredients.filter { it.status == "approved" && it.ingredientId != null }
        val ingredientResponses = approvedIngredients.mapNotNull { ri ->
            val ingredient = when (val result = ingredientClient.getById(IngredientGetByIdParam(ri.ingredientId!!))) {
                is Result.Success -> result.value
                is Result.Failure -> return@mapNotNull null
            }
            MealPlanIngredientResponse(
                recipeIngredientId = ri.id,
                ingredientId = ingredient.id,
                ingredientName = ingredient.name,
                category = ingredient.category,
                quantity = ri.quantity,
                scaledQuantity = ri.quantity.multiply(scaleFactor),
                unit = ri.unit,
            )
        }

        return Result.Success(
            MealPlanRecipeDetailResponse(
                id = mpr.id,
                recipeId = recipe.id,
                recipeName = recipe.name,
                baseServings = recipe.baseServings,
                scaleFactor = scaleFactor,
                isFullyPurchased = false,
                ingredients = ingredientResponses,
            )
        )
    }
}
