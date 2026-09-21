package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.AddRecipeToPlanIfAbsentParam
import com.acme.clients.mealplanclient.api.GetPurchasesParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.services.camperservice.features.mealplan.auth.MealPlanAuthorizer
import com.acme.services.camperservice.features.mealplan.dto.MealPlanRecipeDetailResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.AddRecipeToPlanParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateAddRecipeToPlan
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam

/**
 * Plan-level "add recipe" for flat plans: adds the recipe to the lowest-numbered day
 * (creating day 1 if none exists) with mealType "dinner". Idempotent — if the recipe is
 * already present anywhere in the plan, returns the existing entry instead of duplicating it.
 *
 * The check-and-insert itself is delegated to [MealPlanClient.addRecipeToPlanIfAbsent], which runs
 * atomically under a lock on the meal plan row — meal_plan_recipes has no DB-level uniqueness per
 * plan, so a read-then-insert here (in the action) would let two concurrent requests both pass the
 * "already present" check and double-insert the recipe.
 */
internal class AddRecipeToPlanAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateAddRecipeToPlan()
    private val authorizer = MealPlanAuthorizer(mealPlanClient)

    /** Returns the recipe detail plus whether a new row was created (false = already present, idempotent). */
    fun execute(param: AddRecipeToPlanParam): Result<Pair<MealPlanRecipeDetailResponse, Boolean>, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Access must be checked here, before addRecipeToPlanIfAbsent — that operation takes a row
        // lock inside a transaction and must not be restructured to also do the access check.
        when (val access = authorizer.authorize(param.mealPlanId, param.userId)) {
            is Result.Failure -> return access
            is Result.Success -> {}
        }

        val mealPlan = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        if (recipeClient.getById(RecipeGetByIdParam(param.recipeId)) is Result.Failure) {
            return Result.Failure(MealPlanError.RecipeNotFound(param.recipeId))
        }

        val (mpr, created) = when (val result = mealPlanClient.addRecipeToPlanIfAbsent(
            AddRecipeToPlanIfAbsentParam(mealPlanId = param.mealPlanId, recipeId = param.recipeId)
        )) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("recipe", result.error.message))
            }
        }

        val scalingMode = MealPlanDetailBuilder.parseScalingMode(mealPlan.scalingMode)
        val purchases = when (val result = mealPlanClient.getPurchases(GetPurchasesParam(mealPlan.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("purchases", result.error.message))
        }
        val purchaseMap = purchases.associateBy { "${it.ingredientId}:${it.unit}" }

        val detail = MealPlanDetailBuilder.buildRecipeDetail(mpr, mealPlan, scalingMode, recipeClient, ingredientClient, purchaseMap)
            ?: return Result.Failure(MealPlanError.RecipeNotFound(param.recipeId))

        return Result.Success(detail to created)
    }
}
