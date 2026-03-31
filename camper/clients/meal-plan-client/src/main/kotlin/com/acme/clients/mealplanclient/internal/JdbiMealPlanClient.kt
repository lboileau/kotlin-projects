package com.acme.clients.mealplanclient.internal

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.mealplanclient.api.*
import com.acme.clients.mealplanclient.internal.operations.*
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListManualItem
import com.acme.clients.mealplanclient.model.ShoppingListPurchase
import org.jdbi.v3.core.Jdbi

internal class JdbiMealPlanClient(jdbi: Jdbi) : MealPlanClient {

    private val createMealPlan = CreateMealPlan(jdbi)
    private val getMealPlanById = GetMealPlanById(jdbi)
    private val getMealPlanByPlanId = GetMealPlanByPlanId(jdbi)
    private val getTemplates = GetTemplates(jdbi)
    private val updateMealPlan = UpdateMealPlan(jdbi, getMealPlanById)
    private val deleteMealPlan = DeleteMealPlan(jdbi)
    private val addDay = AddDay(jdbi)
    private val getDays = GetDays(jdbi)
    private val removeDay = RemoveDay(jdbi)
    private val addRecipe = AddRecipe(jdbi)
    private val getRecipesByDayId = GetRecipesByDayId(jdbi)
    private val getRecipesByMealPlanId = GetRecipesByMealPlanId(jdbi)
    private val removeRecipe = RemoveRecipe(jdbi)
    private val getPurchases = GetPurchases(jdbi)
    private val upsertPurchase = UpsertPurchase(jdbi)
    private val deletePurchases = DeletePurchases(jdbi)
    private val addManualItemOp = AddManualItem(jdbi)
    private val getManualItemsOp = GetManualItems(jdbi)
    private val removeManualItemOp = RemoveManualItem(jdbi)
    private val updateManualItemPurchaseOp = UpdateManualItemPurchase(jdbi)
    private val resetManualItemPurchasesOp = ResetManualItemPurchases(jdbi)

    override fun create(param: CreateMealPlanParam): Result<MealPlan, AppError> = createMealPlan.execute(param)
    override fun getById(param: GetByIdParam): Result<MealPlan, AppError> = getMealPlanById.execute(param)
    override fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError> = getMealPlanByPlanId.execute(param)
    override fun getTemplates(): Result<List<MealPlan>, AppError> = getTemplates.execute()
    override fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError> = updateMealPlan.execute(param)
    override fun delete(param: DeleteMealPlanParam): Result<Unit, AppError> = deleteMealPlan.execute(param)
    override fun addDay(param: AddDayParam): Result<MealPlanDay, AppError> = addDay.execute(param)
    override fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError> = getDays.execute(param)
    override fun removeDay(param: RemoveDayParam): Result<Unit, AppError> = removeDay.execute(param)
    override fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError> = addRecipe.execute(param)
    override fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError> = getRecipesByDayId.execute(param)
    override fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError> = getRecipesByMealPlanId.execute(param)
    override fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError> = removeRecipe.execute(param)
    override fun getPurchases(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError> = getPurchases.execute(param)
    override fun upsertPurchase(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError> = upsertPurchase.execute(param)
    override fun deletePurchases(param: DeletePurchasesParam): Result<Unit, AppError> = deletePurchases.execute(param)
    override fun addManualItem(param: AddManualItemParam): Result<ShoppingListManualItem, AppError> = addManualItemOp.execute(param)
    override fun getManualItems(param: GetManualItemsParam): Result<List<ShoppingListManualItem>, AppError> = getManualItemsOp.execute(param)
    override fun removeManualItem(param: RemoveManualItemParam): Result<Unit, AppError> = removeManualItemOp.execute(param)
    override fun updateManualItemPurchase(param: UpdateManualItemPurchaseParam): Result<ShoppingListManualItem, AppError> = updateManualItemPurchaseOp.execute(param)
    override fun resetManualItemPurchases(param: ResetManualItemPurchasesParam): Result<Unit, AppError> = resetManualItemPurchasesOp.execute(param)
}
