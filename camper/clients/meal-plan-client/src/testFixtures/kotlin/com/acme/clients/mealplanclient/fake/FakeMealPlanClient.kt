package com.acme.clients.mealplanclient.fake

import com.acme.clients.common.Result
import com.acme.clients.common.error.AppError
import com.acme.clients.mealplanclient.api.*
import com.acme.clients.mealplanclient.model.MealPlan
import com.acme.clients.mealplanclient.model.MealPlanDay
import com.acme.clients.mealplanclient.model.MealPlanRecipe
import com.acme.clients.mealplanclient.model.ShoppingListPurchase

class FakeMealPlanClient : MealPlanClient {

    override fun create(param: CreateMealPlanParam): Result<MealPlan, AppError> =
        throw NotImplementedError("FakeMealPlanClient.create not yet implemented")

    override fun getById(param: GetByIdParam): Result<MealPlan, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getById not yet implemented")

    override fun getByPlanId(param: GetByPlanIdParam): Result<MealPlan?, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getByPlanId not yet implemented")

    override fun getTemplates(): Result<List<MealPlan>, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getTemplates not yet implemented")

    override fun update(param: UpdateMealPlanParam): Result<MealPlan, AppError> =
        throw NotImplementedError("FakeMealPlanClient.update not yet implemented")

    override fun delete(param: DeleteMealPlanParam): Result<Unit, AppError> =
        throw NotImplementedError("FakeMealPlanClient.delete not yet implemented")

    override fun addDay(param: AddDayParam): Result<MealPlanDay, AppError> =
        throw NotImplementedError("FakeMealPlanClient.addDay not yet implemented")

    override fun getDays(param: GetDaysParam): Result<List<MealPlanDay>, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getDays not yet implemented")

    override fun removeDay(param: RemoveDayParam): Result<Unit, AppError> =
        throw NotImplementedError("FakeMealPlanClient.removeDay not yet implemented")

    override fun addRecipe(param: AddRecipeParam): Result<MealPlanRecipe, AppError> =
        throw NotImplementedError("FakeMealPlanClient.addRecipe not yet implemented")

    override fun getRecipesByDayId(param: GetRecipesByDayIdParam): Result<List<MealPlanRecipe>, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getRecipesByDayId not yet implemented")

    override fun getRecipesByMealPlanId(param: GetRecipesByMealPlanIdParam): Result<List<MealPlanRecipe>, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getRecipesByMealPlanId not yet implemented")

    override fun removeRecipe(param: RemoveRecipeParam): Result<Unit, AppError> =
        throw NotImplementedError("FakeMealPlanClient.removeRecipe not yet implemented")

    override fun getPurchases(param: GetPurchasesParam): Result<List<ShoppingListPurchase>, AppError> =
        throw NotImplementedError("FakeMealPlanClient.getPurchases not yet implemented")

    override fun upsertPurchase(param: UpsertPurchaseParam): Result<ShoppingListPurchase, AppError> =
        throw NotImplementedError("FakeMealPlanClient.upsertPurchase not yet implemented")

    override fun deletePurchases(param: DeletePurchasesParam): Result<Unit, AppError> =
        throw NotImplementedError("FakeMealPlanClient.deletePurchases not yet implemented")
}
