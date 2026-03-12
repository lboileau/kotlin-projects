package com.acme.clients.mealplanclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.*
import com.acme.clients.mealplanclient.internal.validations.ValidateAddDay
import com.acme.clients.mealplanclient.internal.validations.ValidateAddRecipe
import com.acme.clients.mealplanclient.internal.validations.ValidateCreateMealPlan
import com.acme.clients.mealplanclient.internal.validations.ValidateUpdateMealPlan
import com.acme.clients.mealplanclient.internal.validations.ValidateUpsertPurchase
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeMealPlanClient : MealPlanClient {
    private val mealPlans = ConcurrentHashMap<UUID, MealPlan>()
    private val days = ConcurrentHashMap<UUID, MealPlanDay>()
    private val recipes = ConcurrentHashMap<UUID, MealPlanRecipe>()
    private val purchases = ConcurrentHashMap<UUID, ShoppingListPurchase>()

    private val validateCreate = ValidateCreateMealPlan()
    private val validateUpdate = ValidateUpdateMealPlan()
    private val validateAddDay = ValidateAddDay()
    private val validateAddRecipe = ValidateAddRecipe()
    private val validateUpsertPurchase = ValidateUpsertPurchase()

    // --- Meal Plans ---

    override fun create(param: CreateMealPlanParam): Result<MealPlan, AppError> {
        val validation = validateCreate.execute(param)
        if (validation is Result.Failure) return validation

        if (param.planId != null && mealPlans.values.any { it.planId == param.planId }) {
            return failure(ConflictError("MealPlan", "plan_id '${param.planId}' already has a meal plan"))
        }

        val entity = MealPlan(
            id = UUID.randomUUID(),
            planId = param.planId,
            name = param.name,
            servings = param.servings,
            scalingMode = param.scalingMode,
            isTemplate = param.isTemplate,
            sourceTemplateId = param.sourceTemplateId,
            createdBy = param.createdBy,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        mealPlans[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<MealPlan, AppError> {
        val entity = mealPlans[param.id]
        return if (entity != null) success(entity) else failure(NotFoundError("MealPlan", param.id.toString()))
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError> {
        return success(mealPlans.values.find { it.planId == param.planId })
    }

    override fun getTemplates(): Result<List<MealPlan>, AppError> {
        return success(mealPlans.values.filter { it.isTemplate }.sortedBy { it.name })
    }

    override fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError> {
        val validation = validateUpdate.execute(param)
        if (validation is Result.Failure) return validation

        val existing = mealPlans[param.id] ?: return failure(NotFoundError("MealPlan", param.id.toString()))
        val updated = existing.copy(
            name = param.name ?: existing.name,
            servings = param.servings ?: existing.servings,
            scalingMode = param.scalingMode ?: existing.scalingMode,
            updatedAt = Instant.now(),
        )
        mealPlans[param.id] = updated
        return success(updated)
    }

    override fun delete(param: DeleteMealPlanParam): Result<Unit, AppError> {
        if (!mealPlans.containsKey(param.id)) return failure(NotFoundError("MealPlan", param.id.toString()))
        mealPlans.remove(param.id)
        // Cascade: remove days, recipes on those days, and purchases
        val dayIds = days.values.filter { it.mealPlanId == param.id }.map { it.id }.toSet()
        days.keys.removeAll(dayIds)
        recipes.values.removeIf { it.mealPlanDayId in dayIds }
        purchases.values.removeIf { it.mealPlanId == param.id }
        return success(Unit)
    }

    // --- Days ---

    override fun addDay(param: AddDayParam): Result<MealPlanDay, AppError> {
        val validation = validateAddDay.execute(param)
        if (validation is Result.Failure) return validation

        if (days.values.any { it.mealPlanId == param.mealPlanId && it.dayNumber == param.dayNumber }) {
            return failure(ConflictError("MealPlanDay", "day_number ${param.dayNumber} already exists for this meal plan"))
        }

        val entity = MealPlanDay(
            id = UUID.randomUUID(),
            mealPlanId = param.mealPlanId,
            dayNumber = param.dayNumber,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        days[entity.id] = entity
        return success(entity)
    }

    override fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError> {
        return success(days.values.filter { it.mealPlanId == param.mealPlanId }.sortedBy { it.dayNumber })
    }

    override fun removeDay(param: RemoveDayParam): Result<Unit, AppError> {
        if (!days.containsKey(param.id)) return failure(NotFoundError("MealPlanDay", param.id.toString()))
        days.remove(param.id)
        // Cascade: remove recipes on this day
        recipes.values.removeIf { it.mealPlanDayId == param.id }
        return success(Unit)
    }

    // --- Recipes ---

    override fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError> {
        val validation = validateAddRecipe.execute(param)
        if (validation is Result.Failure) return validation

        val entity = MealPlanRecipe(
            id = UUID.randomUUID(),
            mealPlanDayId = param.mealPlanDayId,
            mealType = param.mealType,
            recipeId = param.recipeId,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        recipes[entity.id] = entity
        return success(entity)
    }

    override fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError> {
        return success(
            recipes.values
                .filter { it.mealPlanDayId == param.mealPlanDayId }
                .sortedWith(compareBy({ it.mealType }, { it.createdAt }))
        )
    }

    override fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError> {
        val dayIds = days.values.filter { it.mealPlanId == param.mealPlanId }.map { it.id }.toSet()
        return success(
            recipes.values
                .filter { it.mealPlanDayId in dayIds }
                .sortedWith(compareBy({ days[it.mealPlanDayId]?.dayNumber }, { it.mealType }, { it.createdAt }))
        )
    }

    override fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError> {
        if (!recipes.containsKey(param.id)) return failure(NotFoundError("MealPlanRecipe", param.id.toString()))
        recipes.remove(param.id)
        return success(Unit)
    }

    // --- Shopping List Purchases ---

    override fun getPurchases(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError> {
        return success(purchases.values.filter { it.mealPlanId == param.mealPlanId }.sortedBy { it.createdAt })
    }

    override fun upsertPurchase(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError> {
        val validation = validateUpsertPurchase.execute(param)
        if (validation is Result.Failure) return validation

        val existing = purchases.values.find {
            it.mealPlanId == param.mealPlanId && it.ingredientId == param.ingredientId && it.unit == param.unit
        }

        val entity = if (existing != null) {
            existing.copy(
                quantityPurchased = param.quantityPurchased,
                updatedAt = Instant.now(),
            )
        } else {
            ShoppingListPurchase(
                id = UUID.randomUUID(),
                mealPlanId = param.mealPlanId,
                ingredientId = param.ingredientId,
                unit = param.unit,
                quantityPurchased = param.quantityPurchased,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        }
        purchases[entity.id] = entity
        return success(entity)
    }

    override fun deletePurchases(param: DeletePurchasesParam): Result<Unit, AppError> {
        purchases.values.removeIf { it.mealPlanId == param.mealPlanId }
        return success(Unit)
    }

    // --- Test helpers ---

    fun reset() {
        mealPlans.clear()
        days.clear()
        recipes.clear()
        purchases.clear()
    }

    fun seed(vararg entities: MealPlan) = entities.forEach { mealPlans[it.id] = it }

    fun seedDays(vararg entities: MealPlanDay) = entities.forEach { days[it.id] = it }

    fun seedRecipes(vararg entities: MealPlanRecipe) = entities.forEach { recipes[it.id] = it }

    fun seedPurchases(vararg entities: ShoppingListPurchase) = entities.forEach { purchases[it.id] = it }
}
