package com.acme.clients.mealplanclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.common.error.ConflictError
import com.acme.clients.common.error.NotFoundError
import com.acme.clients.common.failure
import com.acme.clients.common.success
import com.acme.clients.mealplanclient.api.*
import com.acme.clients.mealplanclient.internal.validations.ValidateAddDay
import com.acme.clients.mealplanclient.internal.validations.ValidateAddManualItem
import com.acme.clients.mealplanclient.internal.validations.ValidateAddRecipe
import com.acme.clients.mealplanclient.internal.validations.ValidateCreateMealPlan
import com.acme.clients.mealplanclient.internal.validations.ValidateUpdateManualItemPurchase
import com.acme.clients.mealplanclient.internal.validations.ValidateUpdateMealPlan
import com.acme.clients.mealplanclient.internal.validations.ValidateUpsertPurchase
import java.math.BigDecimal
import com.acme.clients.mealplanclient.model.BackingPlanResolution
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanAccess
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanMember
import com.acme.clients.mealplanclient.model.MealPlanMemberRemoval
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeMealPlanClient : MealPlanClient {
    private val mealPlans = ConcurrentHashMap<UUID, MealPlan>()
    private val days = ConcurrentHashMap<UUID, MealPlanDay>()
    private val recipes = ConcurrentHashMap<UUID, MealPlanRecipe>()
    private val purchases = ConcurrentHashMap<UUID, ShoppingListPurchase>()
    private val manualItems = ConcurrentHashMap<UUID, ShoppingListManualItem>()
    // Mirrors `plans` + `plan_members` for the backing trip plans that meal plan sharing creates
    // (and any real trips a meal plan is bound to via CreateMealPlanParam.planId). Keyed by plan
    // id (== meal plan's `planId`), not meal plan id, since a real trip long outlives the meal
    // plan that was bound to it. The real client has no such table of its own — this reads/writes
    // the same `plans`/`plan_members` tables plan-client owns, directly via SQL.
    private val backingPlans = ConcurrentHashMap<UUID, BackingPlan>()
    // Real reads resolve ownerName via a JOIN on the users table; the fake has no such table, so
    // tests that care about ownerName seed it explicitly via seedOwnerName(). Defaults to "".
    private val ownerNames = ConcurrentHashMap<UUID, String>()
    private val addRecipeToPlanLock = Any()
    private val membersLock = Any()
    private val backingPlanLock = Any()

    /**
     * Mirrors a `plans` row (ownerId) plus its `plan_members` (userId -> joinedAt). `isShareBacking`
     * mirrors the real client's name-based marker (`BackingPlanMarker`): true for a plan created by
     * `getOrCreateBackingPlan`/`seedBackingPlan`'s default, false for a plan standing in for a real
     * trip the meal plan happens to be bound to (seeded via `seedBackingPlan(isShareBacking = false)`).
     */
    private class BackingPlan(val ownerId: UUID, val createdAt: Instant = Instant.now(), val isShareBacking: Boolean = true) {
        val members: MutableMap<UUID, Instant> = mutableMapOf()
    }

    private val validateCreate = ValidateCreateMealPlan()
    private val validateUpdate = ValidateUpdateMealPlan()
    private val validateAddDay = ValidateAddDay()
    private val validateAddRecipe = ValidateAddRecipe()
    private val validateUpsertPurchase = ValidateUpsertPurchase()
    private val validateAddManualItem = ValidateAddManualItem()
    private val validateUpdateManualItemPurchase = ValidateUpdateManualItemPurchase()

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
            recipeCount = 0,
            memberCount = 0,
            ownerName = ownerNames[param.createdBy] ?: "",
        )
        mealPlans[entity.id] = entity
        return success(entity)
    }

    override fun getById(param: GetByIdParam): Result<MealPlan, AppError> {
        val entity = mealPlans[param.id]
        return if (entity != null) success(withComputedFields(entity)) else failure(NotFoundError("MealPlan", param.id.toString()))
    }

    override fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError> {
        return success(mealPlans.values.find { it.planId == param.planId }?.let { withComputedFields(it) })
    }

    override fun getTemplates(): Result<List<MealPlan>, AppError> {
        return success(mealPlans.values.filter { it.isTemplate }.sortedBy { it.name }.map { withComputedFields(it) })
    }

    override fun getByCreatedBy(param: GetByCreatedByParam): Result<List<MealPlan>, AppError> {
        return success(
            mealPlans.values
                .filter { it.createdBy == param.createdBy }
                .sortedByDescending { it.updatedAt }
                .map { withComputedFields(it) }
        )
    }

    override fun getMine(param: GetMineParam): Result<List<MealPlan>, AppError> {
        return success(
            mealPlans.values
                .filter { it.createdBy == param.userId || hasAccessViaBackingPlan(it, param.userId) }
                .sortedByDescending { it.updatedAt }
                .map { withComputedFields(it) }
        )
    }

    override fun getAccess(param: GetAccessParam): Result<MealPlanAccess, AppError> {
        val entity = mealPlans[param.mealPlanId] ?: return failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
        val isMember = hasAccessViaBackingPlan(entity, param.userId)
        return success(MealPlanAccess(createdBy = entity.createdBy, isMember = isMember))
    }

    /** True if userId is the backing plan's owner or a plan_members row of it. */
    private fun hasAccessViaBackingPlan(mealPlan: MealPlan, userId: UUID): Boolean {
        val bp = mealPlan.planId?.let { backingPlans[it] } ?: return false
        return bp.ownerId == userId || bp.members.containsKey(userId)
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
        return success(withComputedFields(updated))
    }

    override fun duplicate(param: DuplicateMealPlanParam): Result<MealPlan, AppError> {
        val source = mealPlans[param.sourceMealPlanId]
            ?: return failure(NotFoundError("MealPlan", param.sourceMealPlanId.toString()))

        val newMealPlanId = UUID.randomUUID()
        val now = Instant.now()

        val sourceDays = days.values.filter { it.mealPlanId == source.id }
        val dayIdMap = mutableMapOf<UUID, UUID>()
        val newDays = sourceDays.map { day ->
            val newDay = MealPlanDay(
                id = UUID.randomUUID(),
                mealPlanId = newMealPlanId,
                dayNumber = day.dayNumber,
                createdAt = now,
                updatedAt = now,
            )
            dayIdMap[day.id] = newDay.id
            newDay
        }

        val newRecipes = recipes.values
            .filter { it.mealPlanDayId in dayIdMap.keys }
            .map { mpr ->
                MealPlanRecipe(
                    id = UUID.randomUUID(),
                    mealPlanDayId = dayIdMap.getValue(mpr.mealPlanDayId),
                    mealType = mpr.mealType,
                    recipeId = mpr.recipeId,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        val newMealPlan = MealPlan(
            id = newMealPlanId,
            planId = null,
            name = param.name,
            servings = source.servings,
            scalingMode = source.scalingMode,
            isTemplate = false,
            sourceTemplateId = null,
            createdBy = param.createdBy,
            createdAt = now,
            updatedAt = now,
            recipeCount = 0,
            memberCount = 0,
            ownerName = ownerNames[param.createdBy] ?: "",
        )

        // Commit all-or-nothing: everything above is computed before any map is mutated.
        mealPlans[newMealPlan.id] = newMealPlan
        newDays.forEach { days[it.id] = it }
        newRecipes.forEach { recipes[it.id] = it }

        return success(withComputedFields(newMealPlan))
    }

    override fun delete(param: DeleteMealPlanParam): Result<Unit, AppError> {
        if (!mealPlans.containsKey(param.id)) return failure(NotFoundError("MealPlan", param.id.toString()))
        mealPlans.remove(param.id)
        // Cascade: remove days, recipes on those days, purchases, and manual items
        val dayIds = days.values.filter { it.mealPlanId == param.id }.map { it.id }.toSet()
        days.keys.removeAll(dayIds)
        recipes.values.removeIf { it.mealPlanDayId in dayIds }
        purchases.values.removeIf { it.mealPlanId == param.id }
        manualItems.values.removeIf { it.mealPlanId == param.id }
        // The backing trip plan (keyed by planId, not meal plan id) is left behind, mirroring the
        // real design: deleting a meal plan doesn't delete its backing trip plan row.
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
        val day = days[param.id]
        if (day == null || day.mealPlanId != param.mealPlanId) return failure(NotFoundError("MealPlanDay", param.id.toString()))
        days.remove(param.id)
        // Cascade: remove recipes on this day
        recipes.values.removeIf { it.mealPlanDayId == param.id }
        return success(Unit)
    }

    // --- Recipes ---

    override fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError> {
        val validation = validateAddRecipe.execute(param)
        if (validation is Result.Failure) return validation

        val day = days[param.mealPlanDayId]
        if (day == null || day.mealPlanId != param.mealPlanId) {
            return failure(NotFoundError("MealPlanDay", param.mealPlanDayId.toString()))
        }

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

    override fun getMealPlanIdForRecipe(param: GetMealPlanIdForRecipeParam): Result<UUID, AppError> {
        val recipe = recipes[param.mealPlanRecipeId]
            ?: return failure(NotFoundError("MealPlanRecipe", param.mealPlanRecipeId.toString()))
        val day = days[recipe.mealPlanDayId]
            ?: return failure(NotFoundError("MealPlanRecipe", param.mealPlanRecipeId.toString()))
        return success(day.mealPlanId)
    }

    override fun removeRecipeFromPlan(param: RemoveRecipeFromPlanParam): Result<Int, AppError> {
        val dayIds = days.values.filter { it.mealPlanId == param.mealPlanId }.map { it.id }.toSet()
        val toRemove = recipes.values.filter { it.mealPlanDayId in dayIds && it.recipeId == param.recipeId }.map { it.id }
        toRemove.forEach { recipes.remove(it) }
        return success(toRemove.size)
    }

    override fun addRecipeToPlanIfAbsent(param: AddRecipeToPlanIfAbsentParam): Result<Pair<MealPlanRecipe, Boolean>, AppError> {
        // Mirrors the client's FOR UPDATE lock on the meal plan row: a single lock object serialises
        // the whole check-and-insert so concurrent calls for the same plan cannot double-insert.
        synchronized(addRecipeToPlanLock) {
            if (!mealPlans.containsKey(param.mealPlanId)) {
                return failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
            }

            val dayIds = days.values.filter { it.mealPlanId == param.mealPlanId }.map { it.id }.toSet()
            val existing = recipes.values.find { it.mealPlanDayId in dayIds && it.recipeId == param.recipeId }
            if (existing != null) {
                return success(existing to false)
            }

            val lowestDay = days.values.filter { it.mealPlanId == param.mealPlanId }.minByOrNull { it.dayNumber }
                ?: MealPlanDay(
                    id = UUID.randomUUID(),
                    mealPlanId = param.mealPlanId,
                    dayNumber = 1,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ).also { days[it.id] = it }

            val entity = MealPlanRecipe(
                id = UUID.randomUUID(),
                mealPlanDayId = lowestDay.id,
                mealType = "dinner",
                recipeId = param.recipeId,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
            recipes[entity.id] = entity
            return success(entity to true)
        }
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

    // --- Shopping List Manual Items ---

    override fun addManualItem(param: AddManualItemParam): Result<ShoppingListManualItem, AppError> {
        val validation = validateAddManualItem.execute(param)
        if (validation is Result.Failure) return validation

        // Enforce partial unique index: (meal_plan_id, ingredient_id, unit) for ingredient-based items
        if (param.ingredientId != null) {
            val duplicate = manualItems.values.any {
                it.mealPlanId == param.mealPlanId && it.ingredientId == param.ingredientId && it.unit == param.unit
            }
            if (duplicate) {
                return failure(ConflictError("ShoppingListManualItem", "ingredient ${param.ingredientId} with unit ${param.unit} already exists for this meal plan"))
            }
        }

        val entity = ShoppingListManualItem(
            id = UUID.randomUUID(),
            mealPlanId = param.mealPlanId,
            ingredientId = param.ingredientId,
            description = param.description,
            quantity = param.quantity,
            unit = param.unit,
            quantityPurchased = BigDecimal.ZERO,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
        manualItems[entity.id] = entity
        return success(entity)
    }

    override fun getManualItems(param: GetManualItemsParam): Result<List<ShoppingListManualItem>, AppError> {
        return success(manualItems.values.filter { it.mealPlanId == param.mealPlanId }.sortedBy { it.createdAt })
    }

    override fun removeManualItem(param: RemoveManualItemParam): Result<Unit, AppError> {
        val item = manualItems[param.id]
        if (item == null || item.mealPlanId != param.mealPlanId) return failure(NotFoundError("ShoppingListManualItem", param.id.toString()))
        manualItems.remove(param.id)
        return success(Unit)
    }

    override fun updateManualItemPurchase(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError> {
        val validation = validateUpdateManualItemPurchase.execute(param)
        if (validation is Result.Failure) return validation

        val existing = manualItems[param.id]
        if (existing == null || existing.mealPlanId != param.mealPlanId) {
            return failure(NotFoundError("ShoppingListManualItem", param.id.toString()))
        }
        val updated = existing.copy(
            quantityPurchased = param.quantityPurchased,
            updatedAt = Instant.now(),
        )
        manualItems[param.id] = updated
        return success(updated)
    }

    override fun resetManualItemPurchases(param: ResetManualItemPurchasesParam): Result<Unit, AppError> {
        manualItems.replaceAll { _, item ->
            if (item.mealPlanId == param.mealPlanId) {
                item.copy(quantityPurchased = BigDecimal.ZERO, updatedAt = Instant.now())
            } else {
                item
            }
        }
        return success(Unit)
    }

    // --- Sharing & Members ---

    override fun getOrCreateBackingPlan(param: GetShareTokenParam): Result<BackingPlanResolution, AppError> {
        synchronized(backingPlanLock) {
            val mealPlan = mealPlans[param.mealPlanId] ?: return failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
            val existingPlanId = mealPlan.planId
            if (existingPlanId != null) {
                val isRealTrip = backingPlans[existingPlanId]?.isShareBacking != true
                return success(BackingPlanResolution(existingPlanId, isRealTrip = isRealTrip))
            }

            val newPlanId = UUID.randomUUID()
            backingPlans[newPlanId] = BackingPlan(ownerId = mealPlan.createdBy, isShareBacking = true)
            mealPlans[mealPlan.id] = mealPlan.copy(planId = newPlanId)
            return success(BackingPlanResolution(newPlanId, isRealTrip = false))
        }
    }

    override fun getShareBackingMealPlan(param: GetByPlanIdParam): Result<MealPlan?, AppError> {
        val bp = backingPlans[param.planId]
        if (bp == null || !bp.isShareBacking) return success(null)
        return success(mealPlans.values.find { it.planId == param.planId }?.let { withComputedFields(it) })
    }

    override fun addMember(param: AddMealPlanMemberParam): Result<Boolean, AppError> {
        val mealPlan = mealPlans[param.mealPlanId] ?: return failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
        val planId = mealPlan.planId ?: return success(false)
        synchronized(membersLock) {
            val bp = backingPlans.getOrPut(planId) { BackingPlan(ownerId = mealPlan.createdBy) }
            if (bp.ownerId == param.userId || bp.members.containsKey(param.userId)) return success(false)
            bp.members[param.userId] = Instant.now()
            return success(true)
        }
    }

    override fun getMembers(param: GetMealPlanMembersParam): Result<List<MealPlanMember>, AppError> {
        val mealPlan = mealPlans[param.mealPlanId] ?: return success(emptyList())
        val bp = mealPlan.planId?.let { backingPlans[it] } ?: return success(emptyList())

        val seen = linkedSetOf<UUID>()
        val result = mutableListOf<MealPlanMember>()
        if (bp.ownerId != mealPlan.createdBy && seen.add(bp.ownerId)) {
            result.add(MealPlanMember(bp.ownerId, bp.createdAt))
        }
        bp.members.entries
            .sortedBy { it.value }
            .forEach { (userId, joinedAt) ->
                if (userId != mealPlan.createdBy && seen.add(userId)) {
                    result.add(MealPlanMember(userId, joinedAt))
                }
            }
        return success(result)
    }

    override fun removeMember(param: RemoveMealPlanMemberParam): Result<MealPlanMemberRemoval, AppError> {
        val mealPlan = mealPlans[param.mealPlanId] ?: return failure(NotFoundError("MealPlan", param.mealPlanId.toString()))
        val bp = mealPlan.planId?.let { backingPlans[it] } ?: return success(MealPlanMemberRemoval.NOT_A_MEMBER)
        if (bp.ownerId == param.userId) return success(MealPlanMemberRemoval.IS_BACKING_PLAN_OWNER)
        val removed = bp.members.remove(param.userId) != null
        return success(if (removed) MealPlanMemberRemoval.REMOVED else MealPlanMemberRemoval.NOT_A_MEMBER)
    }

    // --- recipeCount/memberCount are derived, never stored — recompute on every read, mirroring the SQL subselects ---

    private fun computeRecipeCount(mealPlanId: UUID): Int {
        val dayIds = days.values.filter { it.mealPlanId == mealPlanId }.map { it.id }.toSet()
        return recipes.values.filter { it.mealPlanDayId in dayIds }.map { it.recipeId }.distinct().size
    }

    /** Distinct people with access via the backing plan, not counting the meal plan's own owner. */
    private fun computeMemberCount(mealPlan: MealPlan): Int {
        val bp = mealPlan.planId?.let { backingPlans[it] } ?: return 0
        val people = mutableSetOf(bp.ownerId)
        people.addAll(bp.members.keys)
        people.remove(mealPlan.createdBy)
        return people.size
    }

    private fun withComputedFields(mealPlan: MealPlan): MealPlan = mealPlan.copy(
        recipeCount = computeRecipeCount(mealPlan.id),
        memberCount = computeMemberCount(mealPlan),
        ownerName = ownerNames[mealPlan.createdBy] ?: mealPlan.ownerName,
    )

    // --- Test helpers ---

    fun reset() {
        mealPlans.clear()
        days.clear()
        recipes.clear()
        purchases.clear()
        manualItems.clear()
        backingPlans.clear()
        ownerNames.clear()
    }

    fun seed(vararg entities: MealPlan) = entities.forEach { mealPlans[it.id] = it }

    fun seedDays(vararg entities: MealPlanDay) = entities.forEach { days[it.id] = it }

    fun seedRecipes(vararg entities: MealPlanRecipe) = entities.forEach { recipes[it.id] = it }

    fun seedPurchases(vararg entities: ShoppingListPurchase) = entities.forEach { purchases[it.id] = it }

    fun seedManualItems(vararg entities: ShoppingListManualItem) = entities.forEach { manualItems[it.id] = it }

    /**
     * Seeds a backing trip plan directly — mirrors a `plans` row plus its `plan_members`. Use this
     * to set up a meal plan that's shared (or bound to a real trip with members) without going
     * through `getOrCreateBackingPlan`/`addMember`. Pass the id via `meal_plans.planId` (set it on
     * the seeded [MealPlan], e.g. `seed(mealPlan.copy(planId = planId))`). Pass
     * `isShareBacking = false` to simulate a meal plan bound to a pre-existing real trip (as
     * opposed to one created by the share flow) — `getShareBackingMealPlan`/accept-invite will then
     * treat its id as an unknown token, and `getOrCreateBackingPlan` will report `isRealTrip = true`.
     */
    fun seedBackingPlan(planId: UUID, ownerId: UUID, memberIds: Set<UUID> = emptySet(), isShareBacking: Boolean = true) {
        val bp = BackingPlan(ownerId = ownerId, isShareBacking = isShareBacking)
        memberIds.forEach { bp.members[it] = Instant.now() }
        backingPlans[planId] = bp
    }

    /** Adds a single member to an existing (or newly seeded) backing plan. */
    fun seedBackingPlanMember(planId: UUID, ownerId: UUID, userId: UUID, joinedAt: Instant = Instant.now()) {
        backingPlans.getOrPut(planId) { BackingPlan(ownerId = ownerId) }.members[userId] = joinedAt
    }

    /**
     * Seeds the display name a real JDBI read would resolve via `COALESCE(username, email)`.
     * Real reads join the users table directly; the fake has no such table, so tests that need
     * a resolved ownerName must seed it here.
     */
    fun seedOwnerName(userId: UUID, name: String) {
        ownerNames[userId] = name
    }
}
