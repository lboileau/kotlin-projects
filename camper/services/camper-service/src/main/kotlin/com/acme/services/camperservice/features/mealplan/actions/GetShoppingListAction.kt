package com.acme.services.camperservice.features.mealplan.actions

import com.acme.clients.common.Result
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.ingredientclient.api.IngredientClient
import com.acme.clients.mealplanclient.api.GetDaysParam
import com.acme.clients.mealplanclient.api.GetPurchasesParam
import com.acme.clients.mealplanclient.api.GetRecipesByDayIdParam
import com.acme.clients.mealplanclient.api.MealPlanClient
import com.acme.clients.recipeclient.api.GetRecipeIngredientsParam
import com.acme.clients.recipeclient.api.RecipeClient
import com.acme.libs.mealplancalculator.ShoppingListCalculator
import com.acme.libs.mealplancalculator.model.IngredientInfo
import com.acme.libs.mealplancalculator.model.PurchaseStatus
import com.acme.libs.mealplancalculator.model.RecipeIngredientWithMeta
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListCategoryResponse
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListItemResponse
import com.acme.services.camperservice.features.mealplan.dto.ShoppingListResponse
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.GetShoppingListParam
import com.acme.services.camperservice.features.mealplan.validations.ValidateGetShoppingList
import java.math.BigDecimal
import java.util.UUID
import com.acme.clients.mealplanclient.api.GetByIdParam as ClientGetByIdParam
import com.acme.clients.recipeclient.api.GetByIdParam as RecipeGetByIdParam
import com.acme.clients.ingredientclient.api.GetByIdParam as IngredientGetByIdParam

internal class GetShoppingListAction(
    private val mealPlanClient: MealPlanClient,
    private val recipeClient: RecipeClient,
    private val ingredientClient: IngredientClient,
) {
    private val validate = ValidateGetShoppingList()

    fun execute(param: GetShoppingListParam): Result<ShoppingListResponse, MealPlanError> {
        when (val validation = validate.execute(param)) {
            is Result.Failure -> return validation
            is Result.Success -> {}
        }

        // Load meal plan
        val mealPlan = when (val result = mealPlanClient.getById(ClientGetByIdParam(param.mealPlanId))) {
            is Result.Success -> result.value
            is Result.Failure -> return when (result.error) {
                is NotFoundError -> Result.Failure(MealPlanError.MealPlanNotFound(param.mealPlanId))
                else -> Result.Failure(MealPlanError.Invalid("mealPlan", result.error.message))
            }
        }

        val scalingMode = MealPlanDetailBuilder.parseScalingMode(mealPlan.scalingMode)

        // Load all days
        val days = when (val result = mealPlanClient.getDays(GetDaysParam(mealPlan.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("days", result.error.message))
        }

        // Collect all meal plan recipes across all days
        val allMealPlanRecipes = mutableListOf<com.acme.clients.mealplanclient.model.MealPlanRecipe>()
        for (day in days) {
            when (val result = mealPlanClient.getRecipesByDayId(GetRecipesByDayIdParam(day.id))) {
                is Result.Success -> allMealPlanRecipes.addAll(result.value)
                is Result.Failure -> return Result.Failure(MealPlanError.Invalid("recipes", result.error.message))
            }
        }

        // Get unique recipe IDs and load their details + approved ingredients
        val recipeIngredientsMeta = mutableListOf<RecipeIngredientWithMeta>()
        val ingredientInfoMap = mutableMapOf<UUID, IngredientInfo>()
        val recipeNamesById = mutableMapOf<UUID, String>()

        // For each meal plan recipe occurrence (not unique — same recipe on multiple days counts multiple times)
        for (mpr in allMealPlanRecipes) {
            val recipe = when (val result = recipeClient.getById(RecipeGetByIdParam(mpr.recipeId))) {
                is Result.Success -> result.value
                is Result.Failure -> continue
            }
            recipeNamesById[recipe.id] = recipe.name

            val ingredients = when (val result = recipeClient.getIngredients(GetRecipeIngredientsParam(recipe.id))) {
                is Result.Success -> result.value
                is Result.Failure -> continue
            }

            // Only approved ingredients with resolved ingredientId
            val approved = ingredients.filter { it.status == "approved" && it.ingredientId != null }

            for (ri in approved) {
                val ingredientId = ri.ingredientId!!

                // Load ingredient info if not cached
                if (ingredientId !in ingredientInfoMap) {
                    when (val result = ingredientClient.getById(IngredientGetByIdParam(ingredientId))) {
                        is Result.Success -> {
                            val ing = result.value
                            ingredientInfoMap[ingredientId] = IngredientInfo(
                                ingredientId = ing.id,
                                name = ing.name,
                                category = ing.category,
                            )
                        }
                        is Result.Failure -> continue
                    }
                }

                recipeIngredientsMeta.add(
                    RecipeIngredientWithMeta(
                        recipeIngredientId = ri.id,
                        ingredientId = ingredientId,
                        ingredientName = ingredientInfoMap[ingredientId]!!.name,
                        category = ingredientInfoMap[ingredientId]!!.category,
                        quantity = ri.quantity,
                        unit = ri.unit,
                        recipeName = recipe.name,
                        baseServings = recipe.baseServings,
                    )
                )
            }
        }

        // Scale ingredients
        val scaledIngredients = ShoppingListCalculator.scaleIngredients(
            recipeIngredients = recipeIngredientsMeta,
            servings = mealPlan.servings,
            scalingMode = scalingMode,
        )

        // Aggregate shopping list
        val shoppingListRows = ShoppingListCalculator.aggregateShoppingList(
            scaledIngredients = scaledIngredients,
            ingredientLookup = ingredientInfoMap,
        )

        // Load purchases
        val purchases = when (val result = mealPlanClient.getPurchases(GetPurchasesParam(mealPlan.id))) {
            is Result.Success -> result.value
            is Result.Failure -> return Result.Failure(MealPlanError.Invalid("purchases", result.error.message))
        }
        val purchaseMap = purchases.associateBy { "${it.ingredientId}:${it.unit}" }

        // Build items with purchase status
        val items = shoppingListRows.map { row ->
            val purchase = purchaseMap["${row.ingredientId}:${row.unit}"]
            val quantityPurchased = purchase?.quantityPurchased ?: BigDecimal.ZERO
            val status = PurchaseStatus.derive(row.quantityRequired, quantityPurchased).name.lowercase()

            ShoppingListItemResponse(
                ingredientId = row.ingredientId,
                ingredientName = row.ingredientName,
                quantityRequired = row.quantityRequired,
                quantityPurchased = quantityPurchased,
                unit = row.unit,
                status = status,
                usedInRecipes = row.usedInRecipes,
            )
        }

        // Add "no longer needed" items — purchases that have no matching shopping list row
        val shoppingListKeys = shoppingListRows.map { "${it.ingredientId}:${it.unit}" }.toSet()
        val orphanedPurchases = purchases.filter { "${it.ingredientId}:${it.unit}" !in shoppingListKeys }
        val orphanedItems = orphanedPurchases.mapNotNull { purchase ->
            val ingredientInfo = ingredientInfoMap[purchase.ingredientId]
                ?: when (val result = ingredientClient.getById(IngredientGetByIdParam(purchase.ingredientId))) {
                    is Result.Success -> IngredientInfo(
                        ingredientId = result.value.id,
                        name = result.value.name,
                        category = result.value.category,
                    ).also { ingredientInfoMap[purchase.ingredientId] = it }
                    is Result.Failure -> null
                }
                ?: return@mapNotNull null

            ShoppingListItemResponse(
                ingredientId = purchase.ingredientId,
                ingredientName = ingredientInfo.name,
                quantityRequired = BigDecimal.ZERO,
                quantityPurchased = purchase.quantityPurchased,
                unit = purchase.unit,
                status = PurchaseStatus.NO_LONGER_NEEDED.name.lowercase(),
                usedInRecipes = emptyList(),
            )
        }

        val allItems = items + orphanedItems

        // Group by category
        val categories = allItems
            .groupBy { ingredientInfoMap[it.ingredientId]?.category ?: "other" }
            .map { (category, categoryItems) ->
                ShoppingListCategoryResponse(category = category, items = categoryItems)
            }
            .sortedBy { it.category }

        val fullyPurchasedCount = allItems.count { it.status == PurchaseStatus.DONE.name.lowercase() }

        return Result.Success(
            ShoppingListResponse(
                mealPlanId = mealPlan.id,
                servings = mealPlan.servings,
                scalingMode = mealPlan.scalingMode,
                totalItems = allItems.size,
                fullyPurchasedCount = fullyPurchasedCount,
                categories = categories,
            )
        )
    }

}
