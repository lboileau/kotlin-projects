package com.acme.clients.mealplanclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import com.acme.clients.mealplanclient.model.ShoppingListPurchase

/**
 * Client interface for MealPlan, MealPlanDay, MealPlanRecipe, and ShoppingListPurchase
 * entity operations.
 *
 * Meal plans organize recipes into numbered days and meal types (breakfast, lunch,
 * dinner, snack). They can exist as reusable templates or be bound to a specific trip.
 *
 * All operations return [Result] to represent success or typed failure
 * without throwing exceptions for expected error conditions.
 */
interface MealPlanClient {

    // --- Meal Plans ---

    /** Create a new meal plan (template or trip-bound). */
    fun create(param: CreateMealPlanParam): Result<MealPlan, AppError>

    /** Retrieve a meal plan by its unique identifier. */
    fun getById(param: GetByIdParam): Result<MealPlan, AppError>

    /** Retrieve the meal plan for a specific trip. Returns null if none exists. */
    fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError>

    /** Retrieve all template meal plans. */
    fun getTemplates(): Result<List<MealPlan>, AppError>

    /** Update an existing meal plan. Null fields are left unchanged. */
    fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError>

    /** Delete a meal plan by its unique identifier. Cascades to days, recipes, and purchases. */
    fun delete(param: DeleteMealPlanParam): Result<Unit, AppError>

    // --- Days ---

    /** Add a numbered day to a meal plan. */
    fun addDay(param: AddDayParam): Result<MealPlanDay, AppError>

    /** Retrieve all days for a meal plan, ordered by day number. */
    fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError>

    /** Remove a day from a meal plan. Cascades to recipes on that day. */
    fun removeDay(param: RemoveDayParam): Result<Unit, AppError>

    // --- Recipes ---

    /** Add a recipe to a specific meal on a specific day. */
    fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError>

    /** Retrieve all recipes for a specific day, ordered by meal type. */
    fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError>

    /** Retrieve all recipes across all days for a meal plan. */
    fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError>

    /** Remove a recipe from a meal. */
    fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError>

    // --- Shopping List Purchases ---

    /** Retrieve all purchase records for a meal plan. */
    fun getPurchases(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError>

    /** Create or update a purchase record for an ingredient/unit. Uses UPSERT on (meal_plan_id, ingredient_id, unit). */
    fun upsertPurchase(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError>

    /** Delete all purchase records for a meal plan. */
    fun deletePurchases(param: DeletePurchasesParam): Result<Unit, AppError>

    // --- Shopping List Manual Items ---

    /** Add a manual item to a meal plan's shopping list. */
    fun addManualItem(param: AddManualItemParam): Result<ShoppingListManualItem, AppError>

    /** Retrieve all manual items for a meal plan. */
    fun getManualItems(param: GetManualItemsParam): Result<List<ShoppingListManualItem>, AppError>

    /** Remove a manual item by its unique identifier. Returns NotFoundError if not found. */
    fun removeManualItem(param: RemoveManualItemParam): Result<Unit, AppError>

    /** Update the purchased quantity of a manual item. Returns NotFoundError if not found. */
    fun updateManualItemPurchase(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError>

    /** Reset quantity_purchased to 0 for all manual items in a meal plan. */
    fun resetManualItemPurchases(param: ResetManualItemPurchasesParam): Result<Unit, AppError>
}
