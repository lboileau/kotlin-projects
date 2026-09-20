package com.acme.clients.mealplanclient.api

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanAccess
import com.acme.clients.mealplanclient.model.BackingPlanResolution
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanMember
import com.acme.clients.mealplanclient.model.MealPlanMemberRemoval
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import java.util.UUID

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

    /** Retrieve all meal plans created by a given user, newest updated first. */
    fun getByCreatedBy(param: GetByCreatedByParam): Result<List<MealPlan>, AppError>

    /** Retrieve meal plans a user owns plus meal plans shared with them, newest updated first. */
    fun getMine(param: GetMineParam): Result<List<MealPlan>, AppError>

    /**
     * Resolve who owns a meal plan and whether a given user is a member, in one statement.
     * Returns NotFoundError if the meal plan doesn't exist.
     */
    fun getAccess(param: GetAccessParam): Result<MealPlanAccess, AppError>

    /** Update an existing meal plan. Null fields are left unchanged. */
    fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError>

    /** Delete a meal plan by its unique identifier. Cascades to days, recipes, and purchases. */
    fun delete(param: DeleteMealPlanParam): Result<Unit, AppError>

    /**
     * Duplicate a meal plan: copies name, servings, scaling mode, days (preserving day numbers),
     * and meal_plan_recipes (preserving meal types). Does not copy purchases or manual items.
     * The copy is never a template and is not bound to a trip. All-or-nothing (single transaction).
     */
    fun duplicate(param: DuplicateMealPlanParam): Result<MealPlan, AppError>

    // --- Days ---

    /** Add a numbered day to a meal plan. */
    fun addDay(param: AddDayParam): Result<MealPlanDay, AppError>

    /** Retrieve all days for a meal plan, ordered by day number. */
    fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError>

    /** Remove a day from a meal plan. Cascades to recipes on that day. Scoped to mealPlanId — NotFoundError if the day belongs to a different meal plan. */
    fun removeDay(param: RemoveDayParam): Result<Unit, AppError>

    // --- Recipes ---

    /** Add a recipe to a specific meal on a specific day. Scoped to mealPlanId — NotFoundError if the day belongs to a different meal plan. */
    fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError>

    /** Retrieve all recipes for a specific day, ordered by meal type. */
    fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError>

    /** Retrieve all recipes across all days for a meal plan. */
    fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError>

    /** Remove a recipe from a meal. */
    fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError>

    /** Resolve the owning meal plan ID for a meal plan recipe entry. Returns NotFoundError if not found. */
    fun getMealPlanIdForRecipe(param: GetMealPlanIdForRecipeParam): Result<UUID, AppError>

    /** Remove every occurrence of a recipe from a meal plan (all days, all meal types) in one statement. Returns the number of rows removed (0 if none). */
    fun removeRecipeFromPlan(param: RemoveRecipeFromPlanParam): Result<Int, AppError>

    /**
     * Atomically finds-or-creates a meal_plan_recipes row for (mealPlanId, recipeId), locking the
     * meal plan row for the duration of the check-and-insert. Returns NotFoundError if the meal
     * plan doesn't exist. Returns the row plus whether it was newly created (false = already present).
     */
    fun addRecipeToPlanIfAbsent(param: AddRecipeToPlanIfAbsentParam): Result<Pair<MealPlanRecipe, Boolean>, AppError>

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

    /** Remove a manual item by its unique identifier. Scoped to mealPlanId — NotFoundError if not found, or if it belongs to a different meal plan. */
    fun removeManualItem(param: RemoveManualItemParam): Result<Unit, AppError>

    /** Update the purchased quantity of a manual item. Scoped to mealPlanId — NotFoundError if not found, or if it belongs to a different meal plan. */
    fun updateManualItemPurchase(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError>

    /** Reset quantity_purchased to 0 for all manual items in a meal plan. */
    fun resetManualItemPurchases(param: ResetManualItemPurchasesParam): Result<Unit, AppError>

    // --- Sharing & Members ---

    /**
     * Lazily creates the meal plan's backing trip plan on first call and returns its id — used as
     * the meal plan's share token, since there's no separate token column. Race-safe: two
     * concurrent first calls return the same, single persisted plan id. If the meal plan is
     * already bound to a real trip (not one created by this flow), that plan id is still
     * returned, but [BackingPlanResolution.isRealTrip] is true — the caller must not treat it as a
     * usable share token in that case. Returns NotFoundError if the meal plan doesn't exist.
     */
    fun getOrCreateBackingPlan(param: GetShareTokenParam): Result<BackingPlanResolution, AppError>

    /**
     * Resolves a share token (a backing plan id) to its meal plan, but only when that plan was
     * created by the share flow (see `BackingPlanMarker`) — never for a meal plan merely bound to
     * a real trip. Returns null for both an unknown token and a real trip's id, indistinguishably.
     */
    fun getShareBackingMealPlan(param: GetByPlanIdParam): Result<MealPlan?, AppError>

    /**
     * Idempotent insert into the backing plan's plan_members (role 'member'). If the user is
     * already the backing plan's owner_id, they already have implicit access, so no row is
     * inserted. Returns whether a new row was actually created.
     */
    fun addMember(param: AddMealPlanMemberParam): Result<Boolean, AppError>

    /**
     * Retrieve everyone with access to a meal plan other than its own owner: the backing plan's
     * owner_id first (if different from the meal plan's owner), then its plan_members by join
     * time. De-duplicated by user id.
     */
    fun getMembers(param: GetMealPlanMembersParam): Result<List<MealPlanMember>, AppError>

    /** Remove a member from the backing plan's plan_members. See [MealPlanMemberRemoval] for the possible outcomes. */
    fun removeMember(param: RemoveMealPlanMemberParam): Result<MealPlanMemberRemoval, AppError>
}
