package com.acme.services.camperservice.features.mealplan.validations

import com.acme.clients.common.Result
import com.acme.clients.common.success
import com.acme.services.camperservice.features.mealplan.error.MealPlanError
import com.acme.services.camperservice.features.mealplan.params.*

internal class ValidateCreateMealPlan {
    fun execute(param: CreateMealPlanParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetMealPlanDetail {
    fun execute(param: GetMealPlanDetailParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetMealPlanByPlanId {
    fun execute(param: GetMealPlanByPlanIdParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetTemplates {
    fun execute(param: GetTemplatesParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateUpdateMealPlan {
    fun execute(param: UpdateMealPlanParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateDeleteMealPlan {
    fun execute(param: DeleteMealPlanParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateCopyToTrip {
    fun execute(param: CopyToTripParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateSaveAsTemplate {
    fun execute(param: SaveAsTemplateParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateAddDay {
    fun execute(param: AddDayParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateRemoveDay {
    fun execute(param: RemoveDayParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateAddRecipeToMeal {
    fun execute(param: AddRecipeToMealParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateRemoveRecipeFromMeal {
    fun execute(param: RemoveRecipeFromMealParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateGetShoppingList {
    fun execute(param: GetShoppingListParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateUpdatePurchase {
    fun execute(param: UpdatePurchaseParam): Result<Unit, MealPlanError> = success(Unit)
}

internal class ValidateResetPurchases {
    fun execute(param: ResetPurchasesParam): Result<Unit, MealPlanError> = success(Unit)
}
